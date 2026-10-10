package com.example.financeapp.domain.engine

import com.example.financeapp.domain.model.ActionItem
import com.example.financeapp.domain.model.ActionSequence
import com.example.financeapp.domain.model.ActionType
import com.example.financeapp.domain.model.Debt
import com.example.financeapp.domain.model.FinancialSnapshot
import com.example.financeapp.domain.model.PlanningPeriod
import com.example.financeapp.domain.model.StrategyType
import kotlin.math.max
import kotlin.math.min

/**
 * Period-by-period financial line items produced by a candidate plan.
 */
data class PlanPeriodBreakdown(
    val periodIndex: Int,
    val label: String,
    val openingBalance: Double,
    val projectedIncome: Double,
    val essentialExpenses: Double,
    val regularDebtService: Double,
    val discretionarySpend: Double,
    val discretionaryCutAmount: Double,
    val availableCash: Double,
    val surplusAboveBuffer: Double,
    val debtPrepayment: Double,
    val endingBalance: Double,
    val totalRemainingDebt: Double,
    val interestPaidInPeriod: Double,
    val constraintOk: Boolean,
)

/**
 * Complete evaluation of a candidate plan including balance trajectory, amortisation,
 * Monte Carlo shortfall risk, and terminal net position.
 */
data class EvaluatedPlan(
    val id: String,
    val name: String,
    val strategyType: StrategyType,
    val intentDescription: String,
    val parameters: PolicyParameters,
    val periodBreakdowns: List<PlanPeriodBreakdown>,
    val projectedBalances: List<Double>,
    val terminalBalance: Double,
    val terminalDebt: Double,
    val terminalNetPosition: Double, // terminalBalance - terminalDebt
    val totalInterestPaid: Double,
    val interestSavedVsStatusQuo: Double,
    val shortfallRisk: Double,
    val periodShortfallProbabilities: List<Double>,
    val isFeasibleUnderThreshold: Boolean,
    val actionSequence: ActionSequence,
)

/**
 * Result of comparing candidate action sequences against each other and the status quo baseline.
 */
data class PlanComparisonResult(
    val planA: EvaluatedPlan,
    val planB: EvaluatedPlan,
    val statusQuo: EvaluatedPlan,
    val recommendedPlan: EvaluatedPlan,
    val riskThresholdTheta: Double,
    val whyThisPlan: String,
)

/**
 * Addition 1 / Phase 2B: Constrained Financial Action Planner.
 *
 * Evaluates state-dependent policies:
 * - Plan A: Aggressive Debt Payoff (Avalanche) — c = 0.30, p = 0.80, m = 1.0x buffer.
 * - Plan B: Balanced Reserve Buffer — c = 0.30, p = 0.80, m = 2.5x buffer.
 * - Status Quo: Baseline reference — c = 0, p = 0.
 *
 * Implements loan amortisation, interest saved vs. status quo, Monte Carlo risk simulation,
 * and the theta-threshold risk selection rule with dynamic rationale generation.
 *
 * Pure Kotlin JVM — zero Android dependencies.
 */
object PlannerEngine {

    const val DEFAULT_RISK_THRESHOLD_THETA = 0.15 // 15% shortfall risk ceiling

    val POLICY_PLAN_A = PolicyParameters(
        name = "Plan A: Pay Debt Faster",
        cutFraction = 0.30,
        prepayShare = 0.80,
        bufferMultiple = 1.0
    )

    val POLICY_PLAN_B = PolicyParameters(
        name = "Plan B: Build Buffer First",
        cutFraction = 0.30,
        prepayShare = 0.80,
        bufferMultiple = 2.5
    )

    val POLICY_STATUS_QUO = PolicyParameters(
        name = "Status Quo (No Change)",
        cutFraction = 0.0,
        prepayShare = 0.0,
        bufferMultiple = 1.0
    )

    /**
     * Evaluates all candidate plans for the given snapshot and selects the optimal plan based on risk threshold theta.
     */
    fun evaluatePlans(
        snapshot: FinancialSnapshot,
        riskThresholdTheta: Double = DEFAULT_RISK_THRESHOLD_THETA,
        numSimulationPaths: Int = 1500,
        seed: Long = 42L,
    ): PlanComparisonResult {
        // 1. Simulate Status Quo (Reference baseline for interest saved)
        val statusQuo = evaluateSinglePlan(
            id = "plan_status_quo",
            name = "Status Quo",
            strategyType = StrategyType.BALANCED_RESERVE,
            intent = "Maintain current spending without extra cuts or prepayments",
            policy = POLICY_STATUS_QUO,
            snapshot = snapshot,
            baselineInterestPaid = null,
            riskThresholdTheta = riskThresholdTheta,
            numPaths = numSimulationPaths,
            seed = seed
        )

        // 2. Simulate Plan A (Aggressive Debt Payoff)
        val planA = evaluateSinglePlan(
            id = "plan_a_debt_avalanche",
            name = "Plan A (Aggressive Debt Payoff)",
            strategyType = StrategyType.AGGRESSIVE_DEBT_PAYOFF,
            intent = "Maximise interest saved by channeling 80% surplus to early loan prepayment with a 1.0x buffer",
            policy = POLICY_PLAN_A,
            snapshot = snapshot,
            baselineInterestPaid = statusQuo.totalInterestPaid,
            riskThresholdTheta = riskThresholdTheta,
            numPaths = numSimulationPaths,
            seed = seed
        )

        // 3. Simulate Plan B (Balanced Reserve Buffer)
        val planB = evaluateSinglePlan(
            id = "plan_b_balanced_buffer",
            name = "Plan B (Balanced Reserve Buffer)",
            strategyType = StrategyType.BALANCED_RESERVE,
            intent = "Build cash buffer to 2.5x minimum reserve before accelerating debt; prioritises liquidity safety",
            policy = POLICY_PLAN_B,
            snapshot = snapshot,
            baselineInterestPaid = statusQuo.totalInterestPaid,
            riskThresholdTheta = riskThresholdTheta,
            numPaths = numSimulationPaths,
            seed = seed
        )

        // 4. Selection Rule per Section 9.4:
        // feasible = { plans with shortfallRisk <= theta }
        val candidatePlans = listOf(planA, planB)
        val feasiblePlans = candidatePlans.filter { it.shortfallRisk <= riskThresholdTheta }

        val recommended: EvaluatedPlan
        val whyString: String

        if (feasiblePlans.isNotEmpty()) {
            // Pick plan with highest terminalNetPosition among feasible
            recommended = feasiblePlans.maxByOrNull { it.terminalNetPosition } ?: planB
            whyString = generateRecommendationRationale(recommended, planA, planB, riskThresholdTheta, isAnyPlanFeasible = true)
        } else {
            // Pick minimum-risk plan
            recommended = candidatePlans.minByOrNull { it.shortfallRisk } ?: planB
            whyString = generateRecommendationRationale(recommended, planA, planB, riskThresholdTheta, isAnyPlanFeasible = false)
        }

        return PlanComparisonResult(
            planA = planA,
            planB = planB,
            statusQuo = statusQuo,
            recommendedPlan = recommended,
            riskThresholdTheta = riskThresholdTheta,
            whyThisPlan = whyString
        )
    }

    private fun evaluateSinglePlan(
        id: String,
        name: String,
        strategyType: StrategyType,
        intent: String,
        policy: PolicyParameters,
        snapshot: FinancialSnapshot,
        baselineInterestPaid: Double?,
        riskThresholdTheta: Double,
        numPaths: Int,
        seed: Long,
    ): EvaluatedPlan {
        val periods = snapshot.planningPeriods
        val minReserve = snapshot.minReserve
        var currentBalance = snapshot.currentBalance

        // Clone active debts for amortisation tracking
        val activeDebts = snapshot.debts.map { MutableDebtState(it.name, it.outstandingPrincipal, it.annualInterestRate, it.monthlyEmi) }

        val periodBreakdowns = mutableListOf<PlanPeriodBreakdown>()
        val projectedBalances = mutableListOf<Double>()
        val actionsPerPeriod = mutableMapOf<Int, MutableList<ActionItem>>()
        var totalInterestPaid = 0.0

        periods.forEach { period ->
            val opening = currentBalance
            val income = period.projectedIncome
            val essential = period.essentialExpenses
            val scheduledDebtService = period.debtObligations

            // 1. Discretionary spending cut (capped at 60%)
            val cutAmount = period.projectedDiscretionary * policy.cutFraction.coerceIn(0.0, 0.60)
            val actualDiscretionary = period.projectedDiscretionary - cutAmount

            // 2. Available cash before extra prepayment
            val available = opening + income - essential - scheduledDebtService - actualDiscretionary

            // 3. Buffer threshold and surplus
            val requiredBuffer = policy.bufferMultiple * minReserve
            val surplus = max(0.0, available - requiredBuffer)

            // 4. Prepayment allocated to highest-interest loan first
            val totalOutstanding = activeDebts.sumOf { it.currentPrincipal }
            val totalPrepayment = min(policy.prepayShare * surplus, totalOutstanding)

            // 5. Amortise debts
            val periodInterest = amortiseDebts(activeDebts, totalPrepayment)
            totalInterestPaid += periodInterest

            // 6. Ending balance
            val ending = available - totalPrepayment
            currentBalance = ending
            projectedBalances.add(ending)

            // Actions for this period
            val actionItems = mutableListOf<ActionItem>()
            if (cutAmount > 0) {
                actionItems.add(
                    ActionItem(
                        actionType = ActionType.CUT_DISCRETIONARY,
                        targetCategory = "Discretionary Spending",
                        amountDelta = -cutAmount,
                        description = "Trim discretionary spend by ₹${cutAmount.toInt()} (c = ${(policy.cutFraction * 100).toInt()}%)"
                    )
                )
            }
            if (totalPrepayment > 0) {
                actionItems.add(
                    ActionItem(
                        actionType = ActionType.PREPAY_DEBT,
                        targetCategory = "Loan Prepayment",
                        amountDelta = totalPrepayment,
                        description = "Prepay ₹${totalPrepayment.toInt()} to loan principal"
                    )
                )
            }
            if (policy.bufferMultiple > 1.0) {
                actionItems.add(
                    ActionItem(
                        actionType = ActionType.BUILD_RESERVE,
                        targetCategory = "Liquid Reserve",
                        amountDelta = min(surplus, requiredBuffer - minReserve),
                        description = "Allocate to liquid buffer (${policy.bufferMultiple}x reserve target)"
                    )
                )
            }
            actionsPerPeriod[period.periodIndex] = actionItems

            val constraintSatisfied = ending >= minReserve

            periodBreakdowns.add(
                PlanPeriodBreakdown(
                    periodIndex = period.periodIndex,
                    label = period.label,
                    openingBalance = opening,
                    projectedIncome = income,
                    essentialExpenses = essential,
                    regularDebtService = scheduledDebtService,
                    discretionarySpend = actualDiscretionary,
                    discretionaryCutAmount = cutAmount,
                    availableCash = available,
                    surplusAboveBuffer = surplus,
                    debtPrepayment = totalPrepayment,
                    endingBalance = ending,
                    totalRemainingDebt = activeDebts.sumOf { it.currentPrincipal },
                    interestPaidInPeriod = periodInterest,
                    constraintOk = constraintSatisfied
                )
            )
        }

        // Run Monte Carlo simulation for shortfall risk
        val riskResult = RiskSimulator.evaluatePolicyRisk(snapshot, policy, numPaths = numPaths, seed = seed)
        val interestSaved = if (baselineInterestPaid != null) max(0.0, baselineInterestPaid - totalInterestPaid) else 0.0

        val terminalBal = currentBalance
        val terminalDebt = activeDebts.sumOf { it.currentPrincipal }
        val terminalNet = terminalBal - terminalDebt

        val tradeOff = if (policy.name.contains("Plan A")) {
            "Saves ₹${interestSaved.toInt()} in loan interest, but holds a tighter ₹${terminalBal.toInt()} liquid buffer with ${(riskResult.shortfallRisk * 100).toInt()}% shortfall risk."
        } else if (policy.name.contains("Plan B")) {
            "Maintains safe ₹${terminalBal.toInt()} liquid buffer with low ${(riskResult.shortfallRisk * 100).toInt()}% shortfall risk; saves ₹${interestSaved.toInt()} in interest."
        } else {
            "Reference status quo with zero prepayment: pays ₹${totalInterestPaid.toInt()} in interest."
        }

        val actionSequence = ActionSequence(
            id = id,
            name = name,
            strategyType = strategyType,
            actionsPerPeriod = actionsPerPeriod,
            projectedBalances = projectedBalances,
            terminalReserve = terminalBal,
            shortfallRisk = riskResult.shortfallRisk,
            tradeOffSummary = tradeOff
        )

        return EvaluatedPlan(
            id = id,
            name = name,
            strategyType = strategyType,
            intentDescription = intent,
            parameters = policy,
            periodBreakdowns = periodBreakdowns,
            projectedBalances = projectedBalances,
            terminalBalance = terminalBal,
            terminalDebt = terminalDebt,
            terminalNetPosition = terminalNet,
            totalInterestPaid = totalInterestPaid,
            interestSavedVsStatusQuo = interestSaved,
            shortfallRisk = riskResult.shortfallRisk,
            periodShortfallProbabilities = riskResult.periodShortfallProbabilities,
            isFeasibleUnderThreshold = riskResult.shortfallRisk <= riskThresholdTheta,
            actionSequence = actionSequence
        )
    }

    /**
     * Amortises debt balances by scheduled EMI and extra prepayment (highest rate first).
     * Returns total interest paid across all loans in this period.
     */
    private fun amortiseDebts(
        debts: List<MutableDebtState>,
        prepaymentTotal: Double,
    ): Double {
        var totalInterestThisPeriod = 0.0
        var remainingPrepay = prepaymentTotal

        // Sort loans descending by interest rate (Avalanche method)
        val sortedDebts = debts.sortedByDescending { it.annualRate }

        sortedDebts.forEach { debt ->
            if (debt.currentPrincipal > 0) {
                val monthlyRate = debt.annualRate / 12.0
                val monthlyInterest = debt.currentPrincipal * monthlyRate
                totalInterestThisPeriod += monthlyInterest

                val scheduledPrincipalReduction = max(0.0, debt.monthlyEmi - monthlyInterest)
                val extraPrepayForThisLoan = min(remainingPrepay, debt.currentPrincipal - scheduledPrincipalReduction)
                remainingPrepay -= extraPrepayForThisLoan

                val totalReduction = scheduledPrincipalReduction + extraPrepayForThisLoan
                debt.currentPrincipal = max(0.0, debt.currentPrincipal - totalReduction)
            }
        }

        return totalInterestThisPeriod
    }

    private fun generateRecommendationRationale(
        recommended: EvaluatedPlan,
        planA: EvaluatedPlan,
        planB: EvaluatedPlan,
        theta: Double,
        isAnyPlanFeasible: Boolean,
    ): String {
        val thetaPercent = (theta * 100).toInt()
        val riskAPercent = (planA.shortfallRisk * 100).toInt()
        val riskBPercent = (planB.shortfallRisk * 100).toInt()
        val savedA = planA.interestSavedVsStatusQuo.toInt()
        val savedB = planB.interestSavedVsStatusQuo.toInt()

        return if (!isAnyPlanFeasible) {
            "Warning: High cash flow volatility prevents any plan from meeting your ${thetaPercent}% risk limit. We recommend ${recommended.name} as it minimises shortfall risk to ${riskBPercent}%."
        } else if (recommended.id == planA.id) {
            "Plan A is recommended: It saves ₹$savedA in interest while keeping shortfall risk at ${riskAPercent}%, safely below your ${thetaPercent}% threshold."
        } else {
            "Plan B is recommended: Plan A has a higher interest payoff (₹$savedA saved) but a ${riskAPercent}% chance of dipping below your reserve, exceeding your ${thetaPercent}% limit. Plan B preserves liquidity safety with only ${riskBPercent}% shortfall risk and saves ₹$savedB."
        }
    }

    private class MutableDebtState(
        val name: String,
        var currentPrincipal: Double,
        val annualRate: Double,
        val monthlyEmi: Double,
    )
}

