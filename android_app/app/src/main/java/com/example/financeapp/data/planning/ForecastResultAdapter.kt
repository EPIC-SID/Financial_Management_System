package com.example.financeapp.data.planning

import com.example.financeapp.domain.engine.SimulationInputs
import com.example.financeapp.domain.freelancer.*
import com.example.financeapp.domain.model.ConfidenceLabel
import com.example.financeapp.domain.model.ResultBundle

/** Copilot wire semantics: terminal BANK CASH of the selected plan, never component quantiles. */
class ForecastResultAdapter {
    fun adapt(history: NormalizedFinancialHistory, inputs: SimulationInputs, decision: FreelancerDecision,
        cardDuesPaise: Long, confidence: ConfidenceLabel = ConfidenceLabel.Low): ResultBundle {
        require(inputs.periods.size == 6 && cardDuesPaise in 0..100_000_000_000_000L)
        val plan = decision.plans.single { it.id == decision.recommended }
        require(plan.periods.size == 6 && history.months.isNotEmpty())
        val income = plan.periods.sumOf { it.income }
        val cost = plan.periods.sumOf { it.essentials + it.regularDebtService + it.discretionary }
        val monthlyObligations = inputs.periods.map { it.essentials + it.debtService }.average()
        require(income.isFinite() && income > 0 && cost.isFinite() && monthlyObligations.isFinite() && monthlyObligations > 0)
        // Savings includes SIP/prepayment allocations, not just the final bank-cash difference.
        val savingsRate = (income - cost) / income
        val runway = (inputs.cash - inputs.minReserve).coerceAtLeast(0.0) / monthlyObligations
        val discretionary = history.months.map { it.discretionaryConsumptionPaise / 100.0 }.average()
        val bundle = ResultBundle(runway, plan.shortfallRisk, when (plan.id) {
            FreelancerPlanId.PLAN_A -> "Plan A"
            FreelancerPlanId.PLAN_B -> "Plan B"
            FreelancerPlanId.STATUS_QUO -> "Status quo"
        }, savingsRate, inputs.cash, cardDuesPaise / 100.0, inputs.loans.sumOf { it.emi },
            inputs.periods.map { it.sip }.average(), discretionary,
            plan.terminalCash.p10, plan.terminalCash.p50, plan.terminalCash.p90, "6 months", confidence)
        require(bundle.liquidRunwayMonths.isFinite() && bundle.liquidRunwayMonths in 0.0..1200.0)
        require(bundle.projectedShortfallRisk.isFinite() && bundle.projectedShortfallRisk in 0.0..1.0)
        require(bundle.savingsRate.isFinite() && bundle.savingsRate in 0.0..1.0)
        require(listOf(bundle.bankCash, bundle.cardDues, bundle.monthlyLoanEmis, bundle.monthlySip,
            bundle.avgDiscretionary).all { it.isFinite() && it in 0.0..1e12 })
        require(listOf(bundle.forecastP10, bundle.forecastP50, bundle.forecastP90)
            .all { it.isFinite() && it in -1e12..1e12 })
        return bundle
    }
}
