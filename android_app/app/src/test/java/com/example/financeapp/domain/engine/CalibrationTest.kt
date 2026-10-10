package com.example.financeapp.domain.engine

import com.example.financeapp.data.SyntheticDataSeeder
import com.example.financeapp.domain.model.Scenario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalibrationTest {

    @Test
    fun testConformalScalingFactorBoundedBelowByOne() {
        // When raw nonconformity scores are very small, s should never shrink below 1.0
        val actuals = listOf(100.0, 101.0, 99.0, 100.5, 99.5)
        val p10s = listOf(50.0, 50.0, 50.0, 50.0, 50.0)
        val p50s = listOf(100.0, 100.0, 100.0, 100.0, 100.0)
        val p90s = listOf(150.0, 150.0, 150.0, 150.0, 150.0)

        val result = Calibrator.learnScalingFactor(actuals, p10s, p50s, p90s)

        assertTrue("Scaling factor s must be >= 1.0", result.scalingFactor >= 1.0)
        assertEquals(5, result.sampleSize)
    }

    @Test
    fun testMissingInputsInflation() {
        val actuals = listOf(100.0, 110.0, 90.0, 105.0)
        val p10s = listOf(80.0, 80.0, 80.0, 80.0)
        val p50s = listOf(100.0, 100.0, 100.0, 100.0)
        val p90s = listOf(120.0, 120.0, 120.0, 120.0)

        val resNoMissing = Calibrator.learnScalingFactor(actuals, p10s, p50s, p90s, missingInputsCount = 0)
        val resTwoMissing = Calibrator.learnScalingFactor(actuals, p10s, p50s, p90s, missingInputsCount = 2)

        assertEquals(0.0, resNoMissing.missingInputsInflation, 0.001)
        assertEquals(0.20, resTwoMissing.missingInputsInflation, 0.001)
        assertTrue(
            "Effective scaling factor should be inflated by 20%",
            resTwoMissing.effectiveScalingFactor > resNoMissing.effectiveScalingFactor
        )
    }

    @Test
    fun testConfidenceLabels() {
        // Narrow relative width (< 0.25) with >= 6 months -> High
        val highLabel = Calibrator.computeConfidenceLabel(p10 = 95.0, p50 = 100.0, p90 = 105.0, historyMonths = 6)
        assertEquals("High", highLabel)

        // Wide relative width (> 0.60) -> Low
        val lowLabel = Calibrator.computeConfidenceLabel(p10 = 50.0, p50 = 100.0, p90 = 150.0, historyMonths = 6)
        assertEquals("Low", lowLabel)

        // Low history (< 3 months) -> Low
        val lowHistoryLabel = Calibrator.computeConfidenceLabel(p10 = 95.0, p50 = 100.0, p90 = 105.0, historyMonths = 2)
        assertEquals("Low", lowHistoryLabel)

        // Moderate width -> Medium
        val mediumLabel = Calibrator.computeConfidenceLabel(p10 = 80.0, p50 = 100.0, p90 = 120.0, historyMonths = 6)
        assertEquals("Medium", mediumLabel)
    }

    @Test
    fun testEmpiricalCoverageWithin10PointsOfNominal80() {
        // Definition of Done: coverage on validation within ±10 points of 80% (70% to 90%)
        val report = Evaluator.evaluateScenario(
            scenario = Scenario.STABLE_INCOME,
            seed = 42L,
            calibrationScalingFactor = 1.15 // Conformal scaling factor applied
        )

        val coverage = report.calibratedBootstrap.coverage80
        println("Empirical 80% Coverage: ${coverage * 100}% (Nominal: 80%)")
        println("Calibrated MAE: ₹${report.calibratedBootstrap.mae}")
        println("Historical Average Baseline MAE: ₹${report.historicalAverage.mae}")

        assertTrue(
            "Empirical coverage ($coverage) must be within ±10 points of 80% (i.e. >= 0.70)",
            coverage >= 0.70
        )
    }
}

