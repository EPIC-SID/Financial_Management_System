package com.example.financeapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PlanPeriod(val label: String) { SIX_MONTHS("6 months"), EIGHT_WEEKS("8 weeks") }
enum class PlanKind(val label: String) { PLAN_A("Plan A"), PLAN_B("Plan B"), STATUS_QUO("Status quo") }
enum class PlanContentState { NORMAL, COMPUTING, THIN_HISTORY, INFEASIBLE }

data class ForecastPointUi(
    val period: String,
    val p10: Double,
    val p50: Double,
    val p90: Double,
    val reserve: Double,
    val shortfallChance: Double
)

data class PeriodRowUi(
    val period: String,
    val income: Double,
    val essentials: Double,
    val debt: Double,
    val sip: Double,
    val discretionary: Double,
    val endingBalance: Double
)

data class PlanSummaryUi(
    val endingReserve: Double,
    val interestSaved: Double,
    val shortfallRisk: Double,
    val tradeOff: String,
    val why: String
)

data class ConflictOptionUi(
    val title: String,
    val detail: String,
    val shortfallRiskAfter: Double,
    val notRecommended: Boolean = false
)

data class ConflictUi(
    val statement: String,
    val options: List<ConflictOptionUi>
)

data class PlanUiState(
    val contentState: PlanContentState,
    val period: PlanPeriod,
    val plan: PlanKind,
    val compareMode: Boolean,
    val forecast: List<ForecastPointUi>,
    val rows: List<PeriodRowUi>,
    val summary: PlanSummaryUi,
    val confidenceNote: String? = null,
    val conflict: ConflictUi? = null
)

private data class PlanFixture(
    val forecast: List<ForecastPointUi>,
    val rows: List<PeriodRowUi>,
    val summary: PlanSummaryUi
)

class PlanViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PlanFixtures.normal())
    val uiState: StateFlow<PlanUiState> = _uiState.asStateFlow()

    fun selectPlan(plan: PlanKind) {
        _uiState.value = PlanFixtures.state(_uiState.value.period, plan).copy(compareMode = _uiState.value.compareMode)
    }

    fun selectPeriod(period: PlanPeriod) {
        _uiState.value = PlanFixtures.state(period, _uiState.value.plan).copy(compareMode = _uiState.value.compareMode)
    }

    fun setCompareMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(compareMode = enabled)
    }
}

object PlanFixtures {
    fun normal() = state(PlanPeriod.SIX_MONTHS, PlanKind.PLAN_A)
    fun computing() = normal().copy(contentState = PlanContentState.COMPUTING)
    fun thinHistory() = state(PlanPeriod.EIGHT_WEEKS, PlanKind.PLAN_B).copy(
        contentState = PlanContentState.THIN_HISTORY,
        confidenceNote = "Low confidence · Only six weeks of history are available, so the range is wider."
    )
    fun infeasible() = normal().copy(contentState = PlanContentState.INFEASIBLE, conflict = conflictFixture())
    val all get() = listOf(normal(), computing(), thinHistory(), infeasible())

    fun state(period: PlanPeriod, plan: PlanKind): PlanUiState {
        val fixture = fixture(period, plan)
        return PlanUiState(PlanContentState.NORMAL, period, plan, false, fixture.forecast, fixture.rows, fixture.summary)
    }
}

private fun fixture(period: PlanPeriod, plan: PlanKind): PlanFixture {
    val count = if (period == PlanPeriod.SIX_MONTHS) 6 else 8
    val step = when (plan) { PlanKind.PLAN_A -> 3_900.0; PlanKind.PLAN_B -> 5_100.0; PlanKind.STATUS_QUO -> 1_700.0 }
    val debt = when (plan) { PlanKind.PLAN_A -> 19_000.0; PlanKind.PLAN_B -> 14_500.0; PlanKind.STATUS_QUO -> 12_500.0 }
    val discretionary = when (plan) { PlanKind.PLAN_A -> 15_000.0; PlanKind.PLAN_B -> 13_500.0; PlanKind.STATUS_QUO -> 18_000.0 }
    val forecast = (1..count).map { index ->
        val median = 43_000.0 + step * index
        val spread = 7_000.0 + index * 900.0
        ForecastPointUi(if (period == PlanPeriod.SIX_MONTHS) "Month $index" else "Week $index", median - spread, median, median + spread, 40_000.0, (0.18 - index * .015).coerceAtLeast(.03))
    }
    val rows = forecast.map { point ->
        PeriodRowUi(point.period, 85_000.0, 31_000.0, debt, 5_000.0, discretionary, point.p50)
    }
    val last = forecast.last()
    val summary = when (plan) {
        PlanKind.PLAN_A -> PlanSummaryUi(last.p50, 28_400.0, .07, "Lower interest, but a thinner cash buffer early on.", "This plan pays costly debt first while keeping your ₹40,000 cushion intact.")
        PlanKind.PLAN_B -> PlanSummaryUi(last.p50, 17_200.0, .04, "More cash on hand, with slower debt reduction.", "This plan builds the buffer first because your income history is less predictable.")
        PlanKind.STATUS_QUO -> PlanSummaryUi(last.p50, 0.0, .16, "No behaviour change, but higher interest and risk.", "Status quo keeps current payments so you can compare the cost of taking no action.")
    }
    return PlanFixture(forecast, rows, summary)
}

private fun conflictFixture() = ConflictUi(
    statement = "Your goal of ₹1,00,000 breaks your ₹40,000 cushion in Month 3 by ₹18,200.",
    options = listOf(
        ConflictOptionUi("Lower the target", "Reduce the goal by ₹18,200.", .11),
        ConflictOptionUi("Extend the timeline", "Add 2 months to the goal date.", .08),
        ConflictOptionUi("Reduce the cushion", "Lower the cushion by ₹12,000.", .19, notRecommended = true),
        ConflictOptionUi("Pause the SIP", "Pause the ₹5,000 SIP for 3 months.", .09)
    )
)
