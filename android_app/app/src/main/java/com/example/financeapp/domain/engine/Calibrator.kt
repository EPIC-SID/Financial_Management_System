package com.example.financeapp.domain.engine

import com.example.financeapp.domain.model.ProbabilisticForecast
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

/**
 * Result of the conformal calibration process on the validation set.
 */
data class CalibrationResult(
    val scalingFactor: Double,
    val sampleSize: Int,
    val rawNonconformityScores: List<Double>,
    val missingInputsInflation: Double,
    val effectiveScalingFactor: Double,
)

/**
 * Conformal Prediction Interval Calibrator (Addition 2 / Phase 2F).
 *
 * Implements finite-sample conformal calibration on the validation window (Month -1)
 * to guarantee that empirical prediction interval coverage targets 80% without lookahead bias.
 *
 * Reference: Section 8.3 & 8.4 of docs/06-backend.md.
 * Pure Kotlin JVM — zero Android dependencies.
 */
object Calibrator {

    private const val NOMINAL_COVERAGE = 0.80

    /**
     * Learns the scaling factor `s` from a set of validation actuals vs predicted percentiles.
     *
     * Protocol:
     * 1. Compute nonconformity score for every validation point:
     *    r_i = |actual_i - P50_i| / halfWidth_i, where halfWidth_i = (P90_i - P10_i) / 2
     * 2. Compute s using the finite-sample correction: the ceil((n + 1) * 0.8)-th smallest r value.
     * 3. Where s < 1.0, enforce s = 1.0 (do not shrink below the empirical bootstrap).
     * 4. Add 10% inflation per missing key input (cash anchor, income, loan balance).
     */
    fun learnScalingFactor(
        actuals: List<Double>,
        p10s: List<Double>,
        p50s: List<Double>,
        p90s: List<Double>,
        missingInputsCount: Int = 0,
    ): CalibrationResult {
        require(actuals.size == p10s.size && actuals.size == p50s.size && actuals.size == p90s.size) {
            "Arrays must have matching lengths: actuals=${actuals.size}, p10s=${p10s.size}, p50s=${p50s.size}, p90s=${p90s.size}"
        }

        if (actuals.isEmpty()) {
            return CalibrationResult(
                scalingFactor = 1.0,
                sampleSize = 0,
                rawNonconformityScores = emptyList(),
                missingInputsInflation = 0.0,
                effectiveScalingFactor = 1.0 + (missingInputsCount * 0.10)
            )
        }

        val scores = actuals.indices.map { i ->
            val halfWidth = max(0.0001, (p90s[i] - p10s[i]) / 2.0)
            abs(actuals[i] - p50s[i]) / halfWidth
        }

        val sortedScores = scores.sorted()
        val n = sortedScores.size

        // Finite-sample quantile index: ceil((n + 1) * 0.8) (1-indexed)
        val targetRank = ceil((n + 1) * NOMINAL_COVERAGE).toInt()
        val clampedIndex = (targetRank - 1).coerceIn(0, n - 1)
        val rawScalingFactor = sortedScores[clampedIndex]

        // Rule: Where s < 1.0, keep raw width (do not shrink below bootstrap)
        val boundedScalingFactor = max(1.0, rawScalingFactor)

        val inflation = missingInputsCount * 0.10
        val effectiveScalingFactor = boundedScalingFactor * (1.0 + inflation)

        return CalibrationResult(
            scalingFactor = boundedScalingFactor,
            sampleSize = n,
            rawNonconformityScores = sortedScores,
            missingInputsInflation = inflation,
            effectiveScalingFactor = effectiveScalingFactor
        )
    }

    /**
     * Applies conformal calibration to an interval: P50 ± s * halfWidth.
     */
    fun calibrateInterval(
        p10: Double,
        p50: Double,
        p90: Double,
        scalingFactor: Double,
    ): Pair<Double, Double> {
        val halfWidth = max(0.0, (p90 - p10) / 2.0)
        val calibratedHalfWidth = halfWidth * max(1.0, scalingFactor)
        return Pair(p50 - calibratedHalfWidth, p50 + calibratedHalfWidth)
    }

    /**
     * Calibrates a ProbabilisticForecast record in place.
     */
    fun calibrateForecast(
        forecast: ProbabilisticForecast,
        scalingFactor: Double,
    ): ProbabilisticForecast {
        val (calibratedP10, calibratedP90) = calibrateInterval(
            p10 = forecast.p10,
            p50 = forecast.p50,
            p90 = forecast.p90,
            scalingFactor = scalingFactor
        )

        return forecast.copy(
            p10 = calibratedP10,
            p90 = calibratedP90,
            distributionType = "Conformally Calibrated Bootstrap (s = ${String.format("%.2f", scalingFactor)})"
        )
    }

    /**
     * Computes the confidence label per Section 8.4:
     * relativeWidth = (P90 - P10) / max(|P50|, 1)
     * - High: relativeWidth < 0.25 and at least 6 months of history
     * - Low: relativeWidth > 0.60 or under 3 months of history
     * - Medium: otherwise
     */
    fun computeConfidenceLabel(p10: Double, p50: Double, p90: Double, historyMonths: Int): String {
        val denominator = max(abs(p50), 1.0)
        val relativeWidth = (p90 - p10) / denominator

        return when {
            relativeWidth < 0.25 && historyMonths >= 6 -> "High"
            relativeWidth > 0.60 || historyMonths < 3 -> "Low"
            else -> "Medium"
        }
    }
}

