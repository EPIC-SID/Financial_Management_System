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
                PlanKind.PLAN_A -> "Cuts bank-paid discretionary spending by 30%; prepays debt above the reserve."
                PlanKind.PLAN_B -> "Uses the same cut; holds a larger cash buffer before prepaying debt."
                PlanKind.STATUS_QUO -> "Keeps the declared payments and spending unchanged."
            }, decision.reason, assessment.pathsEvaluated,
            (assessment.shortfallRisk * assessment.pathsEvaluated).roundToInt())
    }
    val assessment = decision.plans.single { it.id.name == plan.name }
    val points = assessment.cashBands.mapIndexed { i, band -> ForecastPointUi("Month ${i + 1}", band.p10,
        band.p50, band.p90, inputs.minReserve, assessment.periodShortfallRisk[i]) }
    val rows = assessment.periods.map { p -> PeriodRowUi("Month ${p.index}", p.income, p.essentials,
        p.regularDebtService + p.extraPrepayment, p.sip, p.discretionary, p.ending, points[p.index - 1]) }
    val conflictUi = conflict?.let { c -> ConflictUi(
        "Synthetic goal ${formatInr(goal!!.targetAmount)} conflicts with reserve ${formatInr(inputs.minReserve)}. " +
            "Maximum achievable: ${formatInr(c.maximumAchievable)}. First violation: period ${c.firstViolatedPeriod}.",
        c.relaxations.map { option -> ConflictOptionUi(option.kind.name.lowercase().replace('_', ' '),
            option.description + " | Change: ${if (option.kind == RelaxationKind.EXTEND_TIMELINE) "${option.delta.toInt()} months" else formatInr(option.delta)}",
            option.riskAfter, option.notRecommended, option.feasible) }) }
    val note = "Synthetic planning demo | ${dataset.demoDate} | ${dataset.scenario.label}. " +
        "${assessment.pathsEvaluated} seeded paths; ${(assessment.shortfallRisk * assessment.pathsEvaluated).roundToInt()} breached reserve ${formatInr(inputs.minReserve)} at least once. " +
        "Intervals are not calibrated real-world probabilities. Fixed salary and declared recurring obligations repeat by design. " +
        "Point ending is the deterministic plan; P10/median/P90 show simulated balances. " +
        (if (decision.noPlanMeetsLimits) "No action plan meets the reserve/risk limits. " else "") +
        (if (applied != null) "${applied.label} selected for this demo session only. " else "") +
        (if (simulationSaved) "What-if saved separately; baseline plan unchanged." else "")
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
        "Demo", "Synthetic planning", "Six-month median cash ${formatInr(band.p50)}",
        "${dataset.persona.name} | ${dataset.scenario.label} | As of ${dataset.demoDate}", band.p10, band.p50, band.p90,
        ConfidenceUi("Low", range, "Empirical P10-P90 terminal cash from 2000 seeded paths.",
            "Synthetic assumptions; not a forecast of your SMS accounts."), score.score, status, score.components,
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
            MetricUi("Liquid runway", bundle?.let { "%.1f months".format(java.util.Locale.US, it.liquidRunwayMonths) } ?: "Unavailable", StatementType.PREDICTION),
            MetricUi("Debt pressure", formatPercent(monthly.debtService / monthly.income), StatementType.FACT, "Includes projected card settlement"),
            MetricUi("Essentials share", formatPercent(monthly.essentials / monthly.income), StatementType.FACT),
            MetricUi("Shortfall risk", com.example.financeapp.ui.util.formatRiskPercent(selected.shortfallRisk), StatementType.PREDICTION,
                "${(selected.shortfallRisk * selected.pathsEvaluated).roundToInt()}/${selected.pathsEvaluated} paths breached reserve over six months; ${selected.id.name}")),
        100, null, if (conflict != null) "Goal conflict: review relaxations before choosing a plan."
            else if (decision.noPlanMeetsLimits) decision.reason else null,
        formatInr(cash - cardDues))
}
