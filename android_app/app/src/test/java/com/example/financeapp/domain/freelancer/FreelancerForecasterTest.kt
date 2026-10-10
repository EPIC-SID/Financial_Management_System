package com.example.financeapp.domain.freelancer

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.YearMonth

class FreelancerForecasterTest {
    private val forecaster = FreelancerForecaster()
    private val anchor = CashAnchor(5_500_000, Instant.parse("2026-10-11T12:00:00Z"))

    private fun history(incomes: List<Long> = listOf(0, 6_000_000, 0, 8_000_000, 0, 4_000_000)) =
        NormalizedFinancialHistory(incomes.mapIndexed { i, income ->
            FinancialHistoryMonth(YearMonth.of(2026, 4).plusMonths(i.toLong()), incomePaise = income)
        }, anchor)

    private fun ready(history: NormalizedFinancialHistory = history(), seed: Int = 42,
                      delay: OneMonthReceiptDelay? = null) =
        (forecaster.forecast(history, seed, delay) as FreelancerForecast.Ready).forecast

    @Test fun seededPathsAreReproducibleAndRetainZeroIncome() {
        val first = ready()
        assertEquals(first, ready())
        assertNotEquals(first.sampledPaths, ready(seed = 43).sampledPaths)
        assertEquals(2_000, first.sampledPaths.size)
        assertTrue(first.sampledPaths.all { it.size == 6 })
        val receipts = first.sampledPaths.flatten().map { it.incomePaise }.toSet()
        assertEquals(setOf(0L, 4_000_000L, 6_000_000L, 8_000_000L), receipts)
    }

    @Test fun eachUndelayedBlockRetainsHistoricalAdjacency() {
        val data = history(listOf(1, 2, 3, 4, 5, 6))
        val pairs = data.months.zipWithNext().map { it.first.incomePaise to it.second.incomePaise }.toSet()
        assertTrue(ready(data).sampledPaths.all { path ->
            path.chunked(2).all { (it[0].incomePaise to it[1].incomePaise) in pairs }
        })
    }

    @Test fun percentilesAreOrderedAndObservedConstantIncomeStaysConstant() {
        val result = ready(history(List(6) { 123_456L }))
        for (i in 0 until 6) {
            assertEquals(123_456L, result.p10[i].incomePaise)
            assertEquals(result.p10[i], result.p50[i])
            assertEquals(result.p50[i], result.p90[i])
        }
        val irregular = ready()
        for (i in 0 until 6) {
            assertTrue(irregular.p10[i].incomePaise <= irregular.p50[i].incomePaise)
            assertTrue(irregular.p50[i].incomePaise <= irregular.p90[i].incomePaise)
        }
        assertTrue(result.method.contains("uncalibrated"))
        assertEquals(6, result.historyMonths)
    }

    @Test fun allZeroHistoryDoesNotInventIncomeOrSpending() {
        assertTrue(ready(history(List(6) { 0L })).sampledPaths.flatten().all {
            it.incomePaise == 0L && it.discretionaryPaise == 0L
        })
    }

    @Test fun insufficientHistoryHasExplicitOutcome() {
        val data = history().copy(months = history().months.takeLast(5))
        assertEquals(FreelancerForecast.Unavailable(ForecastIssue.INSUFFICIENT_HISTORY),
            forecaster.forecast(data, 42))
    }

    @Test fun rejectsGapsDuplicatesUnsortedStaleAndFutureHistory() {
        val data = history()
        val invalid = listOf(data.months.reversed(), data.months.dropLast(1),
            data.months + data.months.last(), data.months.drop(1) +
                data.months.last().copy(month = YearMonth.of(2026, 10)),
            data.months.toMutableList().apply { removeAt(2) })
        for (months in invalid) {
            assertEquals(FreelancerForecast.Unavailable(ForecastIssue.INVALID_HISTORY),
                forecaster.forecast(data.copy(months = months), 42))
        }
    }

    @Test fun rejectsNegativeOversizedAmountsAndInvalidAnchor() {
        val data = history()
        for (value in listOf(-1L, Long.MAX_VALUE)) {
            assertEquals(FreelancerForecast.Unavailable(ForecastIssue.INVALID_HISTORY),
                forecaster.forecast(data.copy(months = data.months.map { it.copy(incomePaise = value) }), 42))
            assertEquals(FreelancerForecast.Unavailable(ForecastIssue.INVALID_HISTORY),
                forecaster.forecast(data.copy(cashAnchor = anchor.copy(amountPaise = value)), 42))
        }
        assertEquals(FreelancerForecast.Unavailable(ForecastIssue.INVALID_HISTORY),
            forecaster.forecast(data.copy(cashAnchor = anchor.copy(asOf = Instant.MAX)), 42))
    }

    @Test fun delayMovesReceiptsOnceWithoutChangingOtherPeriodsOrSpending() {
        val before = ready()
        val after = ready(delay = OneMonthReceiptDelay(2))
        before.sampledPaths.zip(after.sampledPaths).forEach { (a, b) ->
            assertEquals(a.sumOf { it.incomePaise }, b.sumOf { it.incomePaise })
            assertEquals(0L, b[1].incomePaise)
            assertEquals(a[1].incomePaise + a[2].incomePaise, b[2].incomePaise)
            assertEquals(a.map { it.discretionaryPaise }, b.map { it.discretionaryPaise })
            for (i in listOf(0, 3, 4, 5)) assertEquals(a[i], b[i])
        }
    }

    @Test fun rejectsDelayOutsideHorizonInsteadOfSilentlyLosingReceipt() {
        for (period in listOf(0, 6, 7)) {
            assertThrows(IllegalArgumentException::class.java) { OneMonthReceiptDelay(period) }
        }
    }

    @Test fun rollingPeriodsStartAtCashTimestampWithoutSkippingCurrentMonth() {
        val result = ready()
        assertEquals(anchor.asOf, result.periodBoundaries.first())
        assertEquals(Instant.parse("2027-04-11T12:00:00Z"), result.periodBoundaries.last())
        assertEquals(7, result.periodBoundaries.size)
        assertTrue(result.periodBoundaries.zipWithNext().all { it.first < it.second })
    }

    @Test fun variableSpendUsesCashNotCardConsumptionAndCannotBecomeNegative() {
        val data = history().copy(months = history().months.mapIndexed { i, month ->
            month.copy(cashDiscretionaryPaise = if (i == 0) 100_000 else 0,
                discretionaryConsumptionPaise = 90_000_000)
        })
        val result = ready(data)
        assertTrue(result.sampledPaths.flatten().all { it.discretionaryPaise in 0L..100_000L })
        assertTrue(result.p10.zip(result.p50).all { it.first.discretionaryPaise <= it.second.discretionaryPaise })
        assertTrue(result.p50.zip(result.p90).all { it.first.discretionaryPaise <= it.second.discretionaryPaise })
        val onlyCards = data.copy(months = data.months.map { it.copy(cashDiscretionaryPaise = 0) })
        assertTrue(ready(onlyCards).sampledPaths.flatten().all { it.discretionaryPaise == 0L })
    }

    @Test fun percentileValuesMatchNearestRankOfActualSamples() {
        val result = ready()
        for (i in 0 until 6) {
            val sorted = result.sampledPaths.map { it[i].incomePaise }.sorted()
            assertEquals(sorted[199], result.p10[i].incomePaise)
            assertEquals(sorted[999], result.p50[i].incomePaise)
            assertEquals(sorted[1799], result.p90[i].incomePaise)
        }
    }

    @Test fun ewmaResidualSpendingRemainsPairedWithIncomeBlocks() {
        val data = history(listOf(1, 2, 3, 4, 5, 6)).let { base ->
            base.copy(months = base.months.mapIndexed { i, month ->
                month.copy(cashDiscretionaryPaise = (i + 1) * 100L)
            })
        }
        // Final EWMA = 461.664; causal residuals = 0, 100, 160, 196, 217.6, 230.56.
        val expected = listOf(462L, 562L, 622L, 658L, 679L, 692L)
        ready(data).sampledPaths.flatten().forEach {
            assertEquals(expected[it.incomePaise.toInt() - 1], it.discretionaryPaise)
        }
    }

    @Test fun fifthPeriodDelayArrivesInSixthEvenAtMaximumAmounts() {
        val before = ready(history(List(6) { 100_000_000_000_000L }))
        val after = ready(history(List(6) { 100_000_000_000_000L }), delay = OneMonthReceiptDelay(5))
        before.sampledPaths.zip(after.sampledPaths).forEach { (a, b) ->
            assertEquals(a.sumOf { it.incomePaise }, b.sumOf { it.incomePaise })
            assertEquals(0L, b[4].incomePaise)
            assertEquals(200_000_000_000_000L, b[5].incomePaise)
        }
    }

    @Test fun emptyAndOversizedHistoryHaveExplicitOutcomes() {
        assertEquals(FreelancerForecast.Unavailable(ForecastIssue.INSUFFICIENT_HISTORY),
            forecaster.forecast(history().copy(months = emptyList()), 42))
        assertEquals(FreelancerForecast.Unavailable(ForecastIssue.INVALID_HISTORY),
            forecaster.forecast(history().copy(months = List(121) { history().months.first() }), 42))
        assertEquals(FreelancerForecast.Unavailable(ForecastIssue.INVALID_HISTORY),
            forecaster.forecast(history().copy(months = history().months.map {
                it.copy(cashDiscretionaryPaise = -1)
            }), 42))
    }

    @Test fun extremeHistoricalMonthReturnsUnavailableWithoutThrowing() {
        val data = history().let { base ->
            base.copy(months = listOf(base.months.first().copy(month = YearMonth.of(999_999_999, 12))) +
                base.months.drop(1))
        }
        assertEquals(FreelancerForecast.Unavailable(ForecastIssue.INVALID_HISTORY),
            forecaster.forecast(data, 42))
        assertEquals(FreelancerForecast.Unavailable(ForecastIssue.INVALID_HISTORY),
            forecaster.forecast(history().copy(cashAnchor = anchor.copy(
                asOf = Instant.parse("-999999999-01-01T00:00:00Z"))), 42))
    }
}
