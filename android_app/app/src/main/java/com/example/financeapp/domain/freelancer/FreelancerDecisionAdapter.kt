package com.example.financeapp.domain.freelancer

import com.example.financeapp.domain.engine.CashFlowPeriod
import com.example.financeapp.domain.engine.SimulationInputs
import kotlin.math.ceil

data class ForecastBand(val p10: Double, val p50: Double, val p90: Double) {
    init {
        require(listOf(p10, p50, p90).all { it.isFinite() } && p10 <= p50 && p50 <= p90)
    }
}

internal fun empiricalBand(values: List<Double>): ForecastBand {
    require(values.isNotEmpty())
    val sorted = values.sorted()
    fun at(q: Double) = sorted[ceil(q * sorted.size).toInt() - 1]
    return ForecastBand(at(0.1), at(0.5), at(0.9))
}

enum class FreelancerPlanId { PLAN_A, PLAN_B, STATUS_QUO }

data class FreelancerPlanPeriod(
    val index: Int,
    val opening: Double,
    val income: Double,
    val essentials: Double,
    val regularDebtService: Double,
    val discretionary: Double,
    val sip: Double,
    val extraPrepayment: Double,
    val interest: Double,
    val ending: Double,
    val remainingDebt: Double,
    val loanPrincipals: List<Double>
)

data class FreelancerPlanAssessment(
    val id: FreelancerPlanId,
    val periods: List<FreelancerPlanPeriod>,
    val terminalNetPosition: Double,
    val interestSavedVsStatusQuo: Double,
    val investedTotal: Double,
    val deterministicFeasible: Boolean,
    val shortfallRisk: Double,
    val periodShortfallRisk: List<Double>,
    val expectedWorstShortfall: Double,
    val terminalCash: ForecastBand,
    val pathsEvaluated: Int
)

data class FreelancerDecision(
    val plans: List<FreelancerPlanAssessment>,
    val recommended: FreelancerPlanId,
    val riskThreshold: Double,
    val noPlanMeetsLimits: Boolean,
    val reason: String
)

/** Snapshot-free port of the section 9 state-dependent policies; no new random draws. INR units. */
class FreelancerDecisionAdapter {
    fun compare(inputs: SimulationInputs, riskThreshold: Double = 0.15): FreelancerDecision {
        require(riskThreshold.isFinite() && riskThreshold in 0.0..1.0)
        require(inputs.sampledPaths.size <= 10_000 && inputs.loans.size <= 100)
        require(inputs.loans.sumOf { it.outstanding }.isFinite() && inputs.periods.sumOf { it.sip }.isFinite())
        val ids = FreelancerPlanId.values().toList()
        val pointRuns = ids.associateWith { run(inputs, inputs.periods, it, true) }
        val baselineInterest = pointRuns.getValue(FreelancerPlanId.STATUS_QUO).interest
        val assessments = ids.map { id ->
            val point = pointRuns.getValue(id)
            val samples = inputs.sampledPaths.map { run(inputs, it, id, false) }
            require((point.ending - point.debt).isFinite()) { "Unsupported financial magnitude" }
            FreelancerPlanAssessment(id, point.periods, point.ending - point.debt,
                baselineInterest - point.interest, inputs.periods.sumOf { it.sip }, !point.breached,
                samples.count { it.breached }.toDouble() / samples.size,
                inputs.periods.indices.map { i -> samples.count { it.periodBreaches[i] }.toDouble() / samples.size },
                samples.sumOf { it.worstShortfall / samples.size }, empiricalBand(samples.map { it.ending }), samples.size)
        }
        // Status quo is the reference; the two action policies compete for recommendation.
        val candidates = assessments.take(2)
        val eligible = candidates.filter { it.deterministicFeasible && it.shortfallRisk <= riskThreshold }
        val selected = if (eligible.isNotEmpty()) eligible.maxWithOrNull(
            compareBy<FreelancerPlanAssessment> { it.terminalNetPosition }.thenBy { it.periods.last().ending })!!
        else candidates.minWithOrNull(compareBy<FreelancerPlanAssessment> { it.shortfallRisk }
            .thenByDescending { it.terminalNetPosition })!!
        return FreelancerDecision(assessments, selected.id, riskThreshold, eligible.isEmpty(),
            if (eligible.isEmpty()) "No plan meets the deterministic reserve and declared risk limits; " +
                "selected the lowest-risk policy, not a safe-plan guarantee."
            else "Selected the highest terminal net position among policies meeting both limits. " +
                "Risk is the empirical chance of any reserve breach, not goal-success probability.")
    }

    private data class Run(
        val periods: List<FreelancerPlanPeriod>, val ending: Double, val debt: Double,
        val interest: Double, val breached: Boolean, val periodBreaches: BooleanArray,
        val worstShortfall: Double
    )

    private fun run(inputs: SimulationInputs, path: List<CashFlowPeriod>, id: FreelancerPlanId, table: Boolean): Run {
        val principal = inputs.loans.map { it.outstanding }.toDoubleArray()
        val order = inputs.loans.indices.sortedByDescending { inputs.loans[it].annualRate }
        val declaredEmis = inputs.loans.sumOf { it.emi }
        val cut = if (id == FreelancerPlanId.STATUS_QUO) 0.0 else 0.30
        val bufferMultiple = if (id == FreelancerPlanId.PLAN_B) 2.5 else 1.0
        var cash = inputs.cash
        var interest = 0.0
        var breached = cash < inputs.minReserve
        var shortfall = (inputs.minReserve - cash).coerceAtLeast(0.0)
        val periodBreaches = BooleanArray(path.size)
        val periods = if (table) ArrayList<FreelancerPlanPeriod>(path.size) else null
        path.forEachIndexed { index, period ->
            val opening = cash
            var payments = 0.0
            var accruedTotal = 0.0
            inputs.loans.forEachIndexed { i, loan ->
                val accrued = principal[i] * loan.annualRate / 12.0
                val payment = minOf(loan.emi, principal[i] + accrued)
                principal[i] = (principal[i] + accrued - payment).coerceAtLeast(0.0)
                payments += payment
                accruedTotal += accrued
            }
            interest += accruedTotal
            val debtService = period.debtService - declaredEmis + payments
            val discretionary = period.discretionary * (1.0 - cut)
            cash += period.income - period.essentials - debtService - discretionary - period.sip
            val prepay = if (id == FreelancerPlanId.STATUS_QUO) 0.0 else
                minOf(0.8 * (cash - bufferMultiple * inputs.minReserve).coerceAtLeast(0.0), principal.sum())
            var remaining = prepay
            for (i in order) {
                val payment = minOf(remaining, principal[i])
                principal[i] -= payment
                remaining -= payment
            }
            cash -= prepay
            require(cash.isFinite() && interest.isFinite() && principal.sum().isFinite()) {
                "Unsupported financial magnitude"
            }
            periodBreaches[index] = cash < inputs.minReserve
            breached = breached || periodBreaches[index]
            shortfall = maxOf(shortfall, inputs.minReserve - cash)
            require(shortfall.isFinite()) { "Unsupported financial magnitude" }
            periods?.add(FreelancerPlanPeriod(index + 1, opening, period.income, period.essentials,
                debtService, discretionary, period.sip, prepay, accruedTotal, cash, principal.sum(), principal.toList()))
        }
        return Run(periods ?: emptyList(), cash, principal.sum(), interest, breached, periodBreaches, shortfall)
    }
}
