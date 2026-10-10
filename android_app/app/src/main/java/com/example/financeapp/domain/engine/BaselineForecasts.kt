package com.example.financeapp.domain.engine

import com.example.financeapp.domain.model.FinancialSnapshot
import com.example.financeapp.domain.model.PlanningPeriod

/**
 * Baseline forecast result for a single horizon period.
 */
data class BaselinePeriodForecast(
    val periodIndex: Int,
    val label: String,
    val projectedNetFlow: Double,
    val projectedEndingBalance: Double,
    val p10: Double?,
    val p50: Double,
    val p90: Double?,
    val modelName: String,
)

/**
 * Full forecast output produced by a baseline model across all planning periods.
 */
data class BaselineForecastResult(
    val modelName: String,
    val periods: List<BaselinePeriodForecast>,
    val projectedBalances: List<Double>,
    val meanNetFlow: Double,
)

/**
 * Evaluates baseline models required by Addition 2:
 * 1. Historical-Average Baseline (Trailing mean ± 1.28σ normal approximation for 80% interval)
 * 2. Recurring-Only Fixed Baseline (Deterministic salary minus fixed recurring obligations, zero variance)
 */
object BaselineForecasts {

    private const val NORMAL_QUANTILE_80 = 1.28155 // Z-score for central 80% interval (P10 to P90)

    /**
     * Baseline 1: Historical Average Baseline.
     * Projects future net cash flows based on the mean of historical monthly net cash flows.
     * Uses normal distribution approximation (mean ± 1.28σ) for 80% prediction intervals.
     */
    fun runHistoricalAverage(
        snapshot: FinancialSnapshot,
        horizonPeriods: List<PlanningPeriod> = snapshot.planningPeriods,
    ): BaselineForecastResult {
        val historicalMonthlyNetFlows = computeHistoricalMonthlyNetFlows(snapshot)
        val avgNetFlow = if (historicalMonthlyNetFlows.isNotEmpty()) {
            Stats.mean(historicalMonthlyNetFlows)
        } else {
            0.0
        }
        val stdDev = if (historicalMonthlyNetFlows.size >= 2) {
            Stats.sampleStdDev(historicalMonthlyNetFlows)
        } else {
            0.0
        }

        val halfWidth = NORMAL_QUANTILE_80 * stdDev
        var runningBalance = snapshot.currentBalance
        val periodOutputs = mutableListOf<BaselinePeriodForecast>()
        val projectedBalances = mutableListOf<Double>()

        horizonPeriods.forEach { period ->
            runningBalance += avgNetFlow
            projectedBalances.add(runningBalance)

            periodOutputs.add(
                BaselinePeriodForecast(
                    periodIndex = period.periodIndex,
                    label = period.label,
                    projectedNetFlow = avgNetFlow,
                    projectedEndingBalance = runningBalance,
                    p10 = runningBalance - halfWidth,
                    p50 = runningBalance,
                    p90 = runningBalance + halfWidth,
                    modelName = "Historical Average Baseline"
                )
            )
        }

        return BaselineForecastResult(
            modelName = "Historical Average Baseline",
            periods = periodOutputs,
            projectedBalances = projectedBalances,
            meanNetFlow = avgNetFlow
        )
    }

    /**
     * Baseline 2: Fixed Recurring-Only Baseline.
     * Point forecast assuming scheduled income minus deterministic fixed obligations (debt + essentials)
     * minus last month's variable spend. Assumes 0 variance (P10 = P50 = P90).
     */
    fun runRecurringOnly(
        snapshot: FinancialSnapshot,
        horizonPeriods: List<PlanningPeriod> = snapshot.planningPeriods,
    ): BaselineForecastResult {
        val lastMonthVariable = computeLastMonthVariableSpend(snapshot)
        var runningBalance = snapshot.currentBalance
        val periodOutputs = mutableListOf<BaselinePeriodForecast>()
        val projectedBalances = mutableListOf<Double>()
        val netFlows = mutableListOf<Double>()

        horizonPeriods.forEach { period ->
            val netFlow = period.projectedIncome - period.essentialExpenses - period.debtObligations - lastMonthVariable
            netFlows.add(netFlow)
            runningBalance += netFlow
            projectedBalances.add(runningBalance)

            periodOutputs.add(
                BaselinePeriodForecast(
                    periodIndex = period.periodIndex,
                    label = period.label,
                    projectedNetFlow = netFlow,
                    projectedEndingBalance = runningBalance,
                    p10 = null, // No uncertainty interval defined
                    p50 = runningBalance,
                    p90 = null,
                    modelName = "Recurring-Only Fixed Baseline"
                )
            )
        }

        return BaselineForecastResult(
            modelName = "Recurring-Only Fixed Baseline",
            periods = periodOutputs,
            projectedBalances = projectedBalances,
            meanNetFlow = if (netFlows.isNotEmpty()) Stats.mean(netFlows) else 0.0
        )
    }

    /**
     * Derives monthly net flows from the historical transaction stream.
     */
    private fun computeHistoricalMonthlyNetFlows(snapshot: FinancialSnapshot): List<Double> {
        if (snapshot.recentTransactions.isEmpty()) return emptyList()

        // Group by YYYY-MM
        val monthlyGroups = snapshot.recentTransactions.groupBy { it.transactionDate.take(7) }
        return monthlyGroups.values.map { txList ->
            val credits = txList.filter { it.amount > 0 }.sumOf { it.amount }
            val debits = txList.filter { it.amount < 0 }.sumOf { kotlin.math.abs(it.amount) }
            credits - debits
        }
    }

    /**
     * Extracts discretionary spend from the most recent historical month (Month -1).
     */
    private fun computeLastMonthVariableSpend(snapshot: FinancialSnapshot): Double {
        if (snapshot.recentTransactions.isEmpty()) return 10000.0 // Default fallback

        val months = snapshot.recentTransactions.map { it.transactionDate.take(7) }.distinct().sorted()
        val latestMonth = months.lastOrNull() ?: return 10000.0

        val discretionaryCategories = setOf("Food", "Shopping", "Entertainment", "Dining", "Travel", "Personal Care")
        val spend = snapshot.recentTransactions
            .filter { it.transactionDate.startsWith(latestMonth) && it.amount < 0 && it.category in discretionaryCategories }
            .sumOf { kotlin.math.abs(it.amount) }

        return if (spend > 0) spend else 12000.0
    }
}

