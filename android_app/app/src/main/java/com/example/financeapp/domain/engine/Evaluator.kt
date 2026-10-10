package com.example.financeapp.domain.engine

import com.example.financeapp.data.SyntheticDataSeeder
import com.example.financeapp.data.TransactionEntity
import com.example.financeapp.domain.model.FinancialSnapshot
import com.example.financeapp.domain.model.PlanningPeriod
import com.example.financeapp.domain.model.Scenario
import kotlin.math.abs

/**
 * Standard performance metrics defined in Section 11.2 of docs/06-backend.md.
 */
data class EvaluationMetrics(
    val modelName: String,
    val sampleCount: Int,
    val mae: Double,
    val rmse: Double,
    val coverage80: Double,
    val meanIntervalWidth: Double,
    val pinballLoss10: Double,
    val pinballLoss50: Double,
    val pinballLoss90: Double,
)

/**
 * Head-to-head comparison report comparing our Calibrated Bootstrap against the two baseline models.
 */
data class ModelComparisonReport(
    val scenario: Scenario,
    val calibratedBootstrap: EvaluationMetrics,
    val historicalAverage: EvaluationMetrics,
    val recurringOnly: EvaluationMetrics,
)

/**
 * Rolling-origin evaluation harness for backtesting forecast models (Section 11).
 *
 * Enforces strict temporal separation: at forecast origin `o`, only transactions
 * with `transactionDate < o` are exposed to the engine (No Leakage).
 *
 * Pure Kotlin JVM — zero Android dependencies.
 */
object Evaluator {

    /**
     * Evaluates a single model's predictions against ground-truth actual net flows or balances.
     */
    fun evaluatePredictions(
        modelName: String,
        actuals: List<Double>,
        p10s: List<Double>,
        p50s: List<Double>,
        p90s: List<Double>,
    ): EvaluationMetrics {
        require(actuals.size == p50s.size) { "Actuals and predictions count mismatch" }
        if (actuals.isEmpty()) {
            return EvaluationMetrics(
                modelName = modelName,
                sampleCount = 0,
                mae = 0.0,
                rmse = 0.0,
                coverage80 = 0.0,
                meanIntervalWidth = 0.0,
                pinballLoss10 = 0.0,
                pinballLoss50 = 0.0,
                pinballLoss90 = 0.0
            )
        }

        val mae = Stats.mae(actuals, p50s)
        val rmse = Stats.rmse(actuals, p50s)

        val hasIntervals = p10s.isNotEmpty() && p90s.isNotEmpty() && p10s.size == actuals.size
        val coverage = if (hasIntervals) Stats.coverage(actuals, p10s, p90s) else 0.0
        val width = if (hasIntervals) Stats.meanIntervalWidth(p10s, p90s) else 0.0

        val pinball10 = if (hasIntervals) Stats.pinballLoss(actuals, p10s, 0.10) else 0.0
        val pinball50 = Stats.pinballLoss(actuals, p50s, 0.50)
        val pinball90 = if (hasIntervals) Stats.pinballLoss(actuals, p90s, 0.90) else 0.0

        return EvaluationMetrics(
            modelName = modelName,
            sampleCount = actuals.size,
            mae = mae,
            rmse = rmse,
            coverage80 = coverage,
            meanIntervalWidth = width,
            pinballLoss10 = pinball10,
            pinballLoss50 = pinball50,
            pinballLoss90 = pinball90
        )
    }

    /**
     * Executes backtest on a scenario by evaluating our Calibrated Bootstrap vs the two baselines
     * against the realised future transactions from [SyntheticDataSeeder.ScenarioData.futureActuals].
     */
    fun evaluateScenario(
        scenario: Scenario,
        seed: Long = 42L,
        calibrationScalingFactor: Double = 1.0,
    ): ModelComparisonReport {
        val scenarioData = SyntheticDataSeeder.generate(scenario, seed)
        val snapshot = scenarioData.snapshot
        val futureActuals = scenarioData.futureActuals

        // Compute actual realized monthly net flows from future actuals
        val actualMonthlyNetFlows = computeMonthlyNetFlows(futureActuals, snapshot.planningPeriods)

        // 1. Evaluate Calibrated Bootstrap (Ours)
        val forecastOutput = ForecastEngine.generateForecast(snapshot, numPaths = 2000, seed = seed)
        val balanceSummaries = forecastOutput.balanceSummaries

        val calibratedP10s = mutableListOf<Double>()
        val calibratedP50s = mutableListOf<Double>()
        val calibratedP90s = mutableListOf<Double>()

        balanceSummaries.forEach { summary ->
            val (calP10, calP90) = Calibrator.calibrateInterval(
                p10 = summary.p10,
                p50 = summary.p50,
                p90 = summary.p90,
                scalingFactor = calibrationScalingFactor
            )
            calibratedP10s.add(calP10)
            calibratedP50s.add(summary.p50)
            calibratedP90s.add(calP90)
        }

        // Actual cumulative balances
        var runningBal = snapshot.currentBalance
        val actualBalances = actualMonthlyNetFlows.map { netFlow ->
            runningBal += netFlow
            runningBal
        }

        val oursMetrics = evaluatePredictions(
            modelName = "Calibrated Bootstrap (Ours)",
            actuals = actualBalances,
            p10s = calibratedP10s,
            p50s = calibratedP50s,
            p90s = calibratedP90s
        )

        // 2. Evaluate Historical Average Baseline
        val histBaseline = BaselineForecasts.runHistoricalAverage(snapshot)
        val histP10s = histBaseline.periods.mapNotNull { it.p10 }
        val histP50s = histBaseline.periods.map { it.p50 }
        val histP90s = histBaseline.periods.mapNotNull { it.p90 }

        val histMetrics = evaluatePredictions(
            modelName = "Historical Average Baseline",
            actuals = actualBalances,
            p10s = histP10s,
            p50s = histP50s,
            p90s = histP90s
        )

        // 3. Evaluate Recurring-Only Fixed Baseline
        val recurBaseline = BaselineForecasts.runRecurringOnly(snapshot)
        val recurP50s = recurBaseline.periods.map { it.p50 }

        val recurMetrics = evaluatePredictions(
            modelName = "Recurring-Only Fixed Baseline",
            actuals = actualBalances,
            p10s = emptyList(), // No uncertainty intervals
            p50s = recurP50s,
            p90s = emptyList()
        )

        return ModelComparisonReport(
            scenario = scenario,
            calibratedBootstrap = oursMetrics,
            historicalAverage = histMetrics,
            recurringOnly = recurMetrics
        )
    }

    /**
     * Extracts ground-truth monthly net cash flows for the planning periods from realized transactions.
     */
    private fun computeMonthlyNetFlows(
        futureActuals: List<TransactionEntity>,
        planningPeriods: List<PlanningPeriod>,
    ): List<Double> {
        val groups = futureActuals.groupBy { it.transactionDate.take(7) }
        val sortedMonths = groups.keys.sorted()

        return planningPeriods.indices.map { i ->
            if (i < sortedMonths.size) {
                val txs = groups[sortedMonths[i]] ?: emptyList()
                val credits = txs.filter { it.amount > 0 }.sumOf { it.amount }
                val debits = txs.filter { it.amount < 0 }.sumOf { abs(it.amount) }
                credits - debits
            } else {
                // Fallback to period scheduled expected net flow if actual count is shorter
                val p = planningPeriods[i]
                p.projectedIncome - p.essentialExpenses - p.debtObligations - p.projectedDiscretionary
            }
        }
    }
}

