package com.example.financeapp.domain.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Statistical utilities for probabilistic forecasting and baseline evaluation.
 * Pure Kotlin JVM — zero Android dependencies.
 */
object Stats {

    /**
     * Calculates the mean of a sequence of numbers.
     */
    fun mean(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        return values.sum() / values.size
    }

    /**
     * Calculates the sample standard deviation (Bessel's correction n-1).
     */
    fun sampleStdDev(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val avg = mean(values)
        val variance = values.sumOf { (it - avg).pow(2) } / (values.size - 1)
        return sqrt(variance)
    }

    /**
     * Computes the q-th quantile (0.0 <= q <= 1.0) using linear interpolation.
     * E.g., q = 0.1 for P10, q = 0.5 for median, q = 0.9 for P90.
     */
    fun quantile(values: List<Double>, q: Double): Double {
        require(q in 0.0..1.0) { "Quantile must be in [0.0, 1.0]" }
        if (values.isEmpty()) return 0.0
        if (values.size == 1) return values.first()

        val sorted = values.sorted()
        val index = q * (sorted.size - 1)
        val lower = index.toInt()
        val upper = min(lower + 1, sorted.size - 1)
        val fraction = index - lower

        return sorted[lower] + fraction * (sorted[upper] - sorted[lower])
    }

    /**
     * Computes the Exponentially Weighted Moving Average (EWMA) on a historical series.
     * s_0 = y_0
     * s_t = alpha * y_t + (1 - alpha) * s_{t-1}
     */
    fun ewma(series: List<Double>, alpha: Double = 0.4): Double {
        require(alpha in 0.0..1.0) { "Alpha must be in (0.0, 1.0]" }
        if (series.isEmpty()) return 0.0
        var s = series.first()
        for (i in 1 until series.size) {
            s = alpha * series[i] + (1.0 - alpha) * s
        }
        return s
    }

    /**
     * Mean Absolute Error (MAE) between actual observations and median predictions (P50).
     */
    fun mae(actuals: List<Double>, predictions: List<Double>): Double {
        require(actuals.size == predictions.size) { "Size mismatch: ${actuals.size} vs ${predictions.size}" }
        if (actuals.isEmpty()) return 0.0
        return actuals.indices.sumOf { abs(actuals[it] - predictions[it]) } / actuals.size
    }

    /**
     * Root Mean Squared Error (RMSE).
     */
    fun rmse(actuals: List<Double>, predictions: List<Double>): Double {
        require(actuals.size == predictions.size) { "Size mismatch: ${actuals.size} vs ${predictions.size}" }
        if (actuals.isEmpty()) return 0.0
        val mse = actuals.indices.sumOf { (actuals[it] - predictions[it]).pow(2) } / actuals.size
        return sqrt(mse)
    }

    /**
     * Prediction Interval Coverage Probability (PICP).
     * Proportion of actuals falling within [lower, upper] interval.
     */
    fun coverage(actuals: List<Double>, lowerBounds: List<Double>, upperBounds: List<Double>): Double {
        require(actuals.size == lowerBounds.size && actuals.size == upperBounds.size) { "Size mismatch" }
        if (actuals.isEmpty()) return 0.0
        val countInside = actuals.indices.count { i ->
            actuals[i] in lowerBounds[i]..upperBounds[i]
        }
        return countInside.toDouble() / actuals.size
    }

    /**
     * Mean interval width: mean of (upper - lower).
     */
    fun meanIntervalWidth(lowerBounds: List<Double>, upperBounds: List<Double>): Double {
        require(lowerBounds.size == upperBounds.size) { "Size mismatch" }
        if (lowerBounds.isEmpty()) return 0.0
        return lowerBounds.indices.sumOf { upperBounds[it] - lowerBounds[it] } / lowerBounds.size
    }

    /**
     * Pinball (Quantile) Loss for quantile q in [0, 1].
     * L_q(y, y_hat) = max(q * (y - y_hat), (q - 1) * (y - y_hat))
     */
    fun pinballLoss(actuals: List<Double>, quantiles: List<Double>, q: Double): Double {
        require(actuals.size == quantiles.size) { "Size mismatch" }
        if (actuals.isEmpty()) return 0.0
        return actuals.indices.sumOf { i ->
            val error = actuals[i] - quantiles[i]
            max(q * error, (q - 1.0) * error)
        } / actuals.size
    }
}

