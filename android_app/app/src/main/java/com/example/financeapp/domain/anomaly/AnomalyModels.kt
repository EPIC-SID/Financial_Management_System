package com.example.financeapp.domain.anomaly

/**
 * Result classification of an evaluated transaction.
 */
enum class AnomalyStatus {
    NORMAL,
    UNUSUAL
}

/**
 * The reference baseline level used to evaluate the transaction.
 */
enum class AnomalyScope {
    MERCHANT,
    CATEGORY,
    GLOBAL,
    INSUFFICIENT_DATA
}

/**
 * Detailed evaluation output for a single expense transaction.
 */
data class AnomalyResult(
    val status: AnomalyStatus,
    val scope: AnomalyScope,
    val amount: Double,
    val historicalMedian: Double,
    val mad: Double,
    val score: Double,
    val sampleSize: Int,
    val explanation: String
)

/**
 * Configuration parameters for the Median & MAD anomaly detector.
 *
 * @property madMultiplier Number of MADs above median required to flag an anomaly (default: 3.0).
 * @property minSampleSize Minimum historical data points required before forming a baseline (default: 3).
 * @property minAbsoluteDeviation Floor on MAD to prevent hyper-sensitive alerts on ultra-tight variance (default: 50.0).
 * @property excludedCategories Categories that represent non-discretionary recurring obligations, transfers, or income.
 */
data class AnomalyConfig(
    val madMultiplier: Double = 3.0,
    val minSampleSize: Int = 3,
    val minAbsoluteDeviation: Double = 50.0,
    val excludedCategories: Set<String> = setOf("INVESTMENTS", "INCOME", "TRANSFER", "BILLS")
)

