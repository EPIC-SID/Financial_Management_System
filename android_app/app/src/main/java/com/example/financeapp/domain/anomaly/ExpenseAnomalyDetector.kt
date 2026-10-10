package com.example.financeapp.domain.anomaly

import com.example.financeapp.data.TransactionEntity
import kotlin.math.abs
import kotlin.math.max

/**
 * Pure-Kotlin Unusual-Expense Detector.
 *
 * Uses historical Median and Median Absolute Deviation (MAD) to detect anomalous
 * debit amounts per merchant and category.
 *
 * Guarantees:
 * 1. Prior-Only Observations: Never looks ahead at future transactions.
 * 2. Non-discretionary Exclusion: Excludes credits (income), investments, peer transfers,
 *    and recurring bills from anomaly alerting.
 * 3. Hierarchical Baselines: Tests against Merchant baseline first, falls back to Category,
 *    then Global baseline if merchant sample size is small.
 * 4. Zero `android.*` dependencies.
 */
object ExpenseAnomalyDetector {

    /**
     * Calculates the median of a non-empty list of doubles.
     */
    fun computeMedian(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val n = sorted.size
        return if (n % 2 == 1) {
            sorted[n / 2]
        } else {
            (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0
        }
    }

    /**
     * Calculates Median Absolute Deviation (MAD):
     * MAD = median(|x_i - median(X)|)
     */
    fun computeMad(values: List<Double>, median: Double = computeMedian(values)): Double {
        if (values.isEmpty()) return 0.0
        val deviations = values.map { abs(it - median) }
        return computeMedian(deviations)
    }

    /**
     * Evaluates a single target transaction against prior historical transactions.
     *
     * @param target The transaction being checked.
     * @param history All known transactions (this function strictly filters prior-only).
     * @param config Detection parameters (multipliers, min sample size, excluded categories).
     */
    fun evaluate(
        target: TransactionEntity,
        history: List<TransactionEntity>,
        config: AnomalyConfig = AnomalyConfig()
    ): AnomalyResult {
        // Absolute expense amount (expenses may be stored as negative or positive debits)
        val targetAmount = abs(target.amount)

        // Rule 1: Exclude income, transfers, and zero amounts
        if (target.amount > 0 || targetAmount == 0.0) {
            return AnomalyResult(
                status = AnomalyStatus.NORMAL,
                scope = AnomalyScope.GLOBAL,
                amount = targetAmount,
                historicalMedian = 0.0,
                mad = 0.0,
                score = 0.0,
                sampleSize = 0,
                explanation = "Credits and zero amounts are excluded from expense anomaly detection."
            )
        }

        // Rule 2: Exclude non-discretionary categories (e.g. BILLS, INVESTMENTS, TRANSFER)
        val upperCategory = target.category.trim().uppercase()
        if (upperCategory in config.excludedCategories) {
            return AnomalyResult(
                status = AnomalyStatus.NORMAL,
                scope = AnomalyScope.CATEGORY,
                amount = targetAmount,
                historicalMedian = 0.0,
                mad = 0.0,
                score = 0.0,
                sampleSize = 0,
                explanation = "Excluded category '$upperCategory' (routine obligation or transfer)."
            )
        }

        // Prior-Only Filter: strictly prior observations occurring before target transaction
        val priorTransactions = history.filter { candidate ->
            candidate.smsId != target.smsId &&
                candidate.amount < 0 &&
                isPriorTransaction(candidate, target) &&
                candidate.category.trim().uppercase() !in config.excludedCategories
        }

        // Hierarchy Level 1: Merchant-specific baseline
        val targetMerchantClean = target.merchant.trim().lowercase()
        val priorMerchantExpenses = priorTransactions
            .filter { it.merchant.trim().lowercase() == targetMerchantClean }
            .map { abs(it.amount) }

        if (priorMerchantExpenses.size >= config.minSampleSize) {
            return evaluateAgainstSamples(
                targetAmount = targetAmount,
                samples = priorMerchantExpenses,
                scope = AnomalyScope.MERCHANT,
                contextName = target.merchant,
                config = config
            )
        }

        // Hierarchy Level 2: Category-specific baseline
        val priorCategoryExpenses = priorTransactions
            .filter { it.category.trim().uppercase() == upperCategory }
            .map { abs(it.amount) }

        if (priorCategoryExpenses.size >= config.minSampleSize) {
            return evaluateAgainstSamples(
                targetAmount = targetAmount,
                samples = priorCategoryExpenses,
                scope = AnomalyScope.CATEGORY,
                contextName = upperCategory,
                config = config
            )
        }

        // Hierarchy Level 3: Global expense baseline
        val priorGlobalExpenses = priorTransactions.map { abs(it.amount) }
        if (priorGlobalExpenses.size >= config.minSampleSize) {
            return evaluateAgainstSamples(
                targetAmount = targetAmount,
                samples = priorGlobalExpenses,
                scope = AnomalyScope.GLOBAL,
                contextName = "Overall Expenses",
                config = config
            )
        }

        // Insufficient prior data to form statistical baseline
        return AnomalyResult(
            status = AnomalyStatus.NORMAL,
            scope = AnomalyScope.INSUFFICIENT_DATA,
            amount = targetAmount,
            historicalMedian = 0.0,
            mad = 0.0,
            score = 0.0,
            sampleSize = priorGlobalExpenses.size,
            explanation = "Insufficient prior history (< ${config.minSampleSize} transactions) to detect anomalies."
        )
    }

    /**
     * Evaluates all transactions in chronological order, using strictly prior history for each.
     */
    fun detectAll(
        transactions: List<TransactionEntity>,
        config: AnomalyConfig = AnomalyConfig()
    ): List<Pair<TransactionEntity, AnomalyResult>> {
        val sortedChronological = transactions.sortedWith(
            compareBy<TransactionEntity> { it.transactionDate }.thenBy { it.createdAt }
        )

        return sortedChronological.map { currentTxn ->
            val result = evaluate(currentTxn, sortedChronological, config)
            currentTxn to result
        }
    }

    private fun evaluateAgainstSamples(
        targetAmount: Double,
        samples: List<Double>,
        scope: AnomalyScope,
        contextName: String,
        config: AnomalyConfig
    ): AnomalyResult {
        val median = computeMedian(samples)
        val rawMad = computeMad(samples, median)
        // Guard against zero / tiny MAD to avoid false-alert noise on fixed-cost items
        val effectiveMad = max(rawMad, config.minAbsoluteDeviation)

        val difference = targetAmount - median
        val score = if (difference > 0) difference / effectiveMad else 0.0

        val isUnusual = score >= config.madMultiplier && targetAmount > median

        val explanation = if (isUnusual) {
            "Unusual spend of ₹${targetAmount.toInt()} (Median: ₹${median.toInt()}, MAD: ₹${rawMad.toInt()}, Score: ${"%.1f".format(score)}x at $scope scope for $contextName)."
        } else {
            "Normal spend within expected range (Median: ₹${median.toInt()} at $scope scope for $contextName)."
        }

        return AnomalyResult(
            status = if (isUnusual) AnomalyStatus.UNUSUAL else AnomalyStatus.NORMAL,
            scope = scope,
            amount = targetAmount,
            historicalMedian = median,
            mad = rawMad,
            score = score,
            sampleSize = samples.size,
            explanation = explanation
        )
    }

    private fun isPriorTransaction(candidate: TransactionEntity, target: TransactionEntity): Boolean {
        // If ISO transaction dates differ, compare chronologically
        if (candidate.transactionDate != target.transactionDate) {
            return candidate.transactionDate < target.transactionDate
        }
        // Fallback to createdAt timestamp if dates match
        return candidate.createdAt < target.createdAt
    }
}

