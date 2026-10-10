package com.example.financeapp.domain.engine

import com.example.financeapp.data.SyntheticDataSeeder
import com.example.financeapp.domain.model.PeriodType
import com.example.financeapp.domain.model.PlanningPeriod
import com.example.financeapp.domain.model.Scenario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForecastEngineTest {

    @Test
    fun testStatsQuantileAndEwma() {
        val numbers = listOf(10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0)
        val p10 = Stats.quantile(numbers, 0.10)
        val p50 = Stats.quantile(numbers, 0.50)
        val p90 = Stats.quantile(numbers, 0.90)

        assertTrue("P10 <= P50", p10 <= p50)
        assertTrue("P50 <= P90", p50 <= p90)
        assertEquals(55.0, p50, 0.01)

        val series = listOf(100.0, 100.0, 100.0)
        assertEquals(100.0, Stats.ewma(series, 0.4), 0.001)

        val mean = Stats.mean(numbers)
        assertEquals(55.0, mean, 0.001)

        val stdDev = Stats.sampleStdDev(numbers)
        assertTrue("StdDev should be positive", stdDev > 0)
    }

    @Test
    fun testForecastOrderingP10P50P90() {
        val snapshot = SyntheticDataSeeder.generate(Scenario.STABLE_INCOME).snapshot
        val forecastOutput = ForecastEngine.generateForecast(snapshot, numPaths = 2000, seed = 42L)

        assertEquals("6-Month", forecastOutput.horizon)
        assertEquals(PeriodType.MONTHLY, forecastOutput.periodType)
        assertEquals(6, forecastOutput.balanceSummaries.size)

        // Check each period balance summary: P10 <= P50 <= P90
        forecastOutput.balanceSummaries.forEach { summary ->
            assertTrue("P10 <= P50 in ${summary.label}", summary.p10 <= summary.p50)
            assertTrue("P50 <= P90 in ${summary.label}", summary.p50 <= summary.p90)
            assertTrue("Shortfall probability in [0, 1]", summary.shortfallProbability in 0.0..1.0)
        }

        // Check all probabilistic forecast records
        forecastOutput.probabilisticForecasts.forEach { pf ->
            assertTrue("Invariant P10 <= P50 for ${pf.metric} at period ${pf.periodIndex}", pf.p10 <= pf.p50)
            assertTrue("Invariant P50 <= P90 for ${pf.metric} at period ${pf.periodIndex}", pf.p50 <= pf.p90)
        }
    }

    @Test
    fun testForecastDeterminismWithSeed() {
        val snapshot = SyntheticDataSeeder.generate(Scenario.STABLE_INCOME).snapshot
        val run1 = ForecastEngine.generateForecast(snapshot, numPaths = 1000, seed = 12345L)
        val run2 = ForecastEngine.generateForecast(snapshot, numPaths = 1000, seed = 12345L)

        assertEquals(run1.shortfallRisk, run2.shortfallRisk, 0.0001)
        for (i in run1.balanceSummaries.indices) {
            assertEquals(run1.balanceSummaries[i].p10, run2.balanceSummaries[i].p10, 0.001)
            assertEquals(run1.balanceSummaries[i].p50, run2.balanceSummaries[i].p50, 0.001)
            assertEquals(run1.balanceSummaries[i].p90, run2.balanceSummaries[i].p90, 0.001)
        }
    }

    @Test
    fun testForecastExecutionSpeed() {
        val snapshot = SyntheticDataSeeder.generate(Scenario.STABLE_INCOME).snapshot

        // Warm up JIT
        ForecastEngine.generateForecast(snapshot, numPaths = 500, seed = 1L)

        val startTime = System.currentTimeMillis()
        val result = ForecastEngine.generateForecast(snapshot, numPaths = 2000, seed = 42L)
        val duration = System.currentTimeMillis() - startTime

        println("ForecastEngine 2,000 paths executed in ${duration}ms")
        assertTrue("Execution should be fast (< 250ms on JVM)", duration < 250)
        assertNotNull(result)
    }

    @Test
    fun testShortfallRiskUnderHighUncertaintyVsStable() {
        val stableSnapshot = SyntheticDataSeeder.generate(Scenario.STABLE_INCOME).snapshot
        val uncertainSnapshot = SyntheticDataSeeder.generate(Scenario.HIGH_UNCERTAINTY).snapshot

        val stableResult = ForecastEngine.generateForecast(stableSnapshot, numPaths = 2000, seed = 42L)
        val uncertainResult = ForecastEngine.generateForecast(uncertainSnapshot, numPaths = 2000, seed = 42L)

        println("Stable Shortfall Risk: ${stableResult.shortfallRisk * 100}%")
        println("High Uncertainty Shortfall Risk: ${uncertainResult.shortfallRisk * 100}%")

        // High uncertainty scenario introduces ±40% volatility shocks, so shortfall risk must be higher
        assertTrue(
            "High uncertainty shortfall risk (${uncertainResult.shortfallRisk}) should exceed stable (${stableResult.shortfallRisk})",
            uncertainResult.shortfallRisk > stableResult.shortfallRisk
        )
    }

    @Test
    fun testBaselineHistoricalAverage() {
        val snapshot = SyntheticDataSeeder.generate(Scenario.STABLE_INCOME).snapshot
        val baseline = BaselineForecasts.runHistoricalAverage(snapshot)

        assertEquals("Historical Average Baseline", baseline.modelName)
        assertEquals(6, baseline.periods.size)
        assertEquals(6, baseline.projectedBalances.size)

        baseline.periods.forEach { period ->
            assertNotNull(period.p10)
            assertNotNull(period.p90)
            assertTrue("P10 <= P50 in baseline", period.p10!! <= period.p50)
            assertTrue("P50 <= P90 in baseline", period.p50 <= period.p90!!)
        }
    }

    @Test
    fun testBaselineRecurringOnly() {
        val snapshot = SyntheticDataSeeder.generate(Scenario.STABLE_INCOME).snapshot
        val baseline = BaselineForecasts.runRecurringOnly(snapshot)

        assertEquals("Recurring-Only Fixed Baseline", baseline.modelName)
        assertEquals(6, baseline.periods.size)

        // Recurring baseline has 0 variance (P10 and P90 are null)
        baseline.periods.forEach { period ->
            assertEquals(null, period.p10)
            assertEquals(null, period.p90)
            assertTrue("Balance is non-zero", period.projectedEndingBalance != 0.0)
        }
    }

    @Test
    fun testWeeklyForecastHorizon() {
        val weeklyPeriods = (1..8).map { i ->
            PlanningPeriod(
                periodIndex = i,
                label = "W+$i",
                startDate = "2026-10-${i * 7}",
                projectedIncome = 21250.0,
                essentialExpenses = 10000.0,
                debtObligations = 6000.0,
                projectedDiscretionary = 3500.0
            )
        }

        val output = ForecastEngine.generateWeeklyForecast(
            currentBalance = 45000.0,
            minReserve = 25000.0,
            weeklyPeriods = weeklyPeriods,
            numPaths = 1000,
            seed = 42L
        )

        assertEquals("8-Week", output.horizon)
        assertEquals(PeriodType.WEEKLY, output.periodType)
        assertEquals(8, output.balanceSummaries.size)

        output.balanceSummaries.forEach { summary ->
            assertTrue("P10 <= P50 in ${summary.label}", summary.p10 <= summary.p50)
            assertTrue("P50 <= P90 in ${summary.label}", summary.p50 <= summary.p90)
        }
    }
}

