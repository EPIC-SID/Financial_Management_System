package com.example.financeapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.ingest.Persona
import com.example.financeapp.data.ingest.Scenario
import com.example.financeapp.data.ingest.SyntheticPersonas
import com.example.financeapp.data.planning.DemoFinanceCalculator
import com.example.financeapp.data.planning.DemoFinanceResult
import com.example.financeapp.domain.engine.*
import com.example.financeapp.domain.freelancer.FreelancerPlanId
import com.example.financeapp.ui.components.HealthStatus
import com.example.financeapp.ui.components.StatementType
import com.example.financeapp.ui.util.formatInr
import com.example.financeapp.ui.util.formatPercent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt
import kotlin.math.roundToInt

data class HybridState(
    val scenario: Scenario = Scenario.STABLE_INCOME,
    val persona: Persona = Persona.B,
    val result: DemoFinanceResult? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val plan: PlanKind = PlanKind.PLAN_A,
    val compare: Boolean = false,
    val appliedPlan: PlanKind? = null,
    val savedSimulation: WhatIfChange? = null
)

class HybridViewModel : ViewModel() {
    private val mutableState = MutableStateFlow(HybridState())
    val state = mutableState.asStateFlow()
    private var calculation: Job? = null

    init { selectScenario(Scenario.STABLE_INCOME) }

    fun selectScenario(scenario: Scenario, persona: Persona = Persona.B) {
        recalculate(HybridState(scenario = scenario, persona = persona))
    }

    fun retry() = recalculate(mutableState.value.copy(result = null, loading = true, error = null))

    private fun recalculate(next: HybridState, relaxation: Relaxation? = null) {
        calculation?.cancel()
        mutableState.value = next.copy(loading = true, result = null, error = null, appliedPlan = null, savedSimulation = null)
        calculation = viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    DemoFinanceCalculator().calculate(next.scenario, next.persona, relaxation)
                }
                ensureActive()
                mutableState.value = next.copy(result = result, loading = false,
                    plan = PlanKind.valueOf(result.decision.recommended.name), appliedPlan = null, savedSimulation = null)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableState.value = next.copy(result = null, loading = false, error = "Demo calculation unavailable. Retry or choose another scenario.") }
        }
    }

    fun selectPlan(plan: PlanKind) { mutableState.value = mutableState.value.copy(plan = plan) }
    fun compare(enabled: Boolean) { mutableState.value = mutableState.value.copy(compare = enabled) }
    fun applyPlan() {
        val current = mutableState.value
        val result = current.result ?: return
        val plan = result.decision.plans.single { it.id.name == current.plan.name }
        if (result.conflict == null && plan.deterministicFeasible && plan.shortfallRisk <= result.decision.riskThreshold)
            mutableState.value = current.copy(appliedPlan = current.plan)
    }
    fun applyRelaxation(index: Int) {
        val current = mutableState.value
        val option = current.result?.conflict?.relaxations?.getOrNull(index) ?: return
        if (option.feasible) recalculate(current, option)
    }
    fun saveSimulation(change: WhatIfChange) {
        mutableState.value = mutableState.value.copy(savedSimulation = change)
    }
    fun clearSessionSelections() {
        mutableState.value = mutableState.value.copy(appliedPlan = null, savedSimulation = null, compare = false)
    }
}

fun DemoFinanceResult.planUi(plan: PlanKind, compare: Boolean, applied: PlanKind? = null, simulationSaved: Boolean = false): PlanUiState {
    fun summary(id: PlanKind): PlanSummaryUi {
        val assessment = decision.plans.single { it.id.name == id.name }
        return PlanSummaryUi(assessment.terminalCash.p50, assessment.interestSavedVsStatusQuo, assessment.shortfallRisk,
            when (id) {
                PlanKind.PLAN_A -> "Spend 30% less on non-essential purchases paid from your bank. Use spare cash above your safety balance to pay debt early."
                PlanKind.PLAN_B -> "Make the same spending cut, but keep more cash before paying debt early."
                PlanKind.STATUS_QUO -> "Keep your current spending and payments."
            }, if (decision.noPlanMeetsLimits) "Neither action plan meets the safety balance and risk limits. The lower-risk option is shown, but it is not a safe-plan guarantee."
            else "The suggested plan leaves the most money after remaining debt, among plans that meet the safety balance and risk limits.", assessment.pathsEvaluated,
            (assessment.shortfallRisk * assessment.pathsEvaluated).roundToInt())
    }
    val assessment = decision.plans.single { it.id.name == plan.name }
    val points = assessment.cashBands.mapIndexed { i, band -> ForecastPointUi("Month ${i + 1}", band.p10,
        band.p50, band.p90, inputs.minReserve, assessment.periodShortfallRisk[i]) }
    val rows = assessment.periods.map { p -> PeriodRowUi("Month ${p.index}", p.income, p.essentials,
        p.regularDebtService + p.extraPrepayment, p.sip, p.discretionary, p.ending, points[p.index - 1]) }
    val conflictUi = conflict?.let { c -> ConflictUi(
        "The sample goal of ${formatInr(goal!!.targetAmount)} cannot be met while keeping ${formatInr(inputs.minReserve)} as a safety balance. " +
            "The most you can save is ${formatInr(c.maximumAchievable)}. The first problem appears in month ${c.firstViolatedPeriod}.",
        c.relaxations.map { option -> ConflictOptionUi(when (option.kind) {
            RelaxationKind.LOWER_TARGET -> "Save a smaller amount"
            RelaxationKind.EXTEND_TIMELINE -> "Allow more time"
            RelaxationKind.REDUCE_RESERVE -> "Keep a smaller safety balance"
            else -> "Reduce monthly investing"
        },
            option.description + " | Change: ${if (option.kind == RelaxationKind.EXTEND_TIMELINE) "${option.delta.toInt()} months" else formatInr(option.delta)}",
            option.riskAfter, option.notRecommended, option.feasible) }) }
    val note = "Sample data as of ${dataset.demoDate}. " +
        "We tested ${assessment.pathsEvaluated} possible cash-flow outcomes. ${(assessment.shortfallRisk * assessment.pathsEvaluated).roundToInt()} fell below the safety balance of ${formatInr(inputs.minReserve)} at least once in six months. " +
        "This is not the chance of reaching your goal, and it is not a proven real-world probability. " +
        "Regular salary and payments repeat each month. The planned balance uses the entered amounts; the low, typical and high estimates show different simulated outcomes. " +
        (if (decision.noPlanMeetsLimits) "Neither action plan meets the safety limits. " else "") +
        (if (applied != null) "${applied.label} selected for this demo session only. " else "") +
        (if (simulationSaved) "Your trial change was saved separately. The original plan is unchanged." else "")
    return PlanUiState(if (conflict != null) PlanContentState.INFEASIBLE else PlanContentState.NORMAL,
        PlanPeriod.SIX_MONTHS, plan, compare, points, rows, summary(plan), note, conflictUi,
        PlanKind.entries.associateWith(::summary), listOf(PlanPeriod.SIX_MONTHS),
        conflict == null && assessment.deterministicFeasible && assessment.shortfallRisk <= decision.riskThreshold)
}

fun DemoFinanceResult.cockpitUi(): CockpitUiState {
    val selected = decision.plans.single { it.id == decision.recommended }
    val cash = inputs.cash
    val monthly = inputs.periods.first()
    val nets = history.months.map { (it.incomePaise - it.cashEssentialsPaise - it.cashDebtServicePaise - it.cashDiscretionaryPaise - it.cashSipPaise) / 100.0 }
    val mean = nets.average()
    val sigma = sqrt(nets.map { (it - mean) * (it - mean) }.average())
    val score = HealthScore.calculate(HealthScoreInputs(cash, monthly.essentials + monthly.debtService,
        monthly.debtService, monthly.income, inputs.minReserve, sigma, history.months.map { it.incomePaise / 100.0 }.average()))
    val status = HealthStatus.valueOf(score.band.name)
    val band = selected.terminalCash
    val range = "${formatInr(band.p10)} to ${formatInr(band.p90)}"
    val profile = SyntheticPersonas.profile(dataset.persona)
    val obligations = buildList {
        profile.bills.filter { it.calendarMonths == null || dataset.demoDate.monthValue in it.calendarMonths }.forEach {
            add(ObligationUi(it.merchant, "${dataset.demoDate.month} ${it.day}", formatInr(it.amount)))
        }
        profile.loans.forEach { add(ObligationUi(it.lender, "${dataset.demoDate.month} ${it.dueDay}", formatInr(it.emi))) }
        profile.sip?.let { add(ObligationUi("SIP", "${dataset.demoDate.month} ${it.debitDay}", formatInr(it.amount))) }
        add(ObligationUi("Estimated card settlement", "${dataset.demoDate.month} ${profile.card.dueDay}",
            formatInr(monthly.debtService - inputs.loans.sumOf { it.emi })))
    }
    return CockpitUiState(
        if (decision.noPlanMeetsLimits || conflict != null) CockpitContentState.ALERT_ACTIVE else CockpitContentState.FULL,
        "Demo", "Sample data", formatInr(band.p50),
        "A typical result from ${selected.pathsEvaluated} simulations. Sample data as of ${dataset.demoDate}.", band.p10, band.p50, band.p90,
        ConfidenceUi("Low", range, "The middle 80% of simulated ending balances fall in this range.",
            "These estimates use sample data, not your SMS transactions. They are not guarantees."), score.score, status, score.components,
        listOf(AccountSummaryUi(AccountKind.CASH, "Cash", formatInr(cash)),
            AccountSummaryUi(AccountKind.CARD, "Card dues", formatInr(cardDues)),
            AccountSummaryUi(AccountKind.LOAN, "Loan EMIs", formatInr(inputs.loans.sumOf { it.emi })),
            AccountSummaryUi(AccountKind.SIP, "SIP", formatInr(monthly.sip))),
        listOf(AccountDetailUi("Synthetic bank", listOf("${profile.bank.institution} | cash ${formatInr(cash)}")),
            AccountDetailUi("Synthetic card", listOf("${profile.card.issuer} | dues / unbilled ${formatInr(cardDues)}")),
            AccountDetailUi("Synthetic loans", dataset.liabilities.filter { it.kind != "CARD" }.map {
                "${it.kind} | principal ${formatInr(it.outstanding!!)} | EMI ${formatInr(it.emi!!)}"
            }), AccountDetailUi("Synthetic SIP", listOf("Monthly ${formatInr(monthly.sip)}"))),
        obligations, listOf(
            MetricUi("How long cash lasts", bundle?.let { "%.1f months".format(java.util.Locale.US, it.liquidRunwayMonths) } ?: "Unavailable", StatementType.PREDICTION, "Based on essential costs and debt payments"),
            MetricUi("Income used for debt", formatPercent(monthly.debtService / monthly.income), StatementType.FACT, "Includes estimated card bill payments"),
            MetricUi("Income used for essentials", formatPercent(monthly.essentials / monthly.income), StatementType.FACT),
            MetricUi("Risk of low cash", com.example.financeapp.ui.util.formatRiskPercent(selected.shortfallRisk), StatementType.PREDICTION,
                "${(selected.shortfallRisk * selected.pathsEvaluated).roundToInt()} of ${selected.pathsEvaluated} simulations fell below your safety balance during six months. Not the chance of reaching your goal.")),
        100, null, if (conflict != null) "Your goal needs a change. Review the options before choosing a plan."
            else if (decision.noPlanMeetsLimits) "Neither action plan meets the safety balance and risk limits. Review the plans before deciding." else null,
        formatInr(cash - cardDues))
}
