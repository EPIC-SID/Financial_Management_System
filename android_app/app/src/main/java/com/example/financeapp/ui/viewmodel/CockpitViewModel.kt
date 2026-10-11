package com.example.financeapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.financeapp.domain.engine.HealthScore
import com.example.financeapp.domain.engine.HealthScoreComponent
import com.example.financeapp.domain.engine.HealthScoreInputs
import com.example.financeapp.ui.components.HealthStatus
import com.example.financeapp.ui.components.StatementType
import com.example.financeapp.ui.util.formatInr
import com.example.financeapp.ui.util.formatInrCompact
import com.example.financeapp.ui.util.formatPercent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CockpitContentState { FULL, SKELETON, NO_DATA, LOW_COMPLETENESS, ALERT_ACTIVE }
enum class AccountKind { CASH, CARD, LOAN, SIP }

data class AccountSummaryUi(val kind: AccountKind, val label: String, val amount: String?)
data class ObligationUi(val label: String, val date: String, val amount: String, val isToday: Boolean = false)
data class MetricUi(val title: String, val value: String, val type: StatementType, val subtitle: String? = null)
data class ConfidenceUi(val label: String, val interval: String, val reading: String, val improvement: String)
data class AccountDetailUi(val heading: String, val lines: List<String>, val isStale: Boolean = false, val isEmpty: Boolean = false)

data class CockpitUiState(
    val contentState: CockpitContentState,
    val name: String,
    val dataSource: String,
    val verdict: String,
    val verdictReading: String,
    val p10: Double,
    val p50: Double,
    val p90: Double,
    val confidence: ConfidenceUi,
    val healthScore: Int,
    val healthStatus: HealthStatus,
    val healthComponents: List<HealthScoreComponent>,
    val accounts: List<AccountSummaryUi>,
    val accountDetails: List<AccountDetailUi>,
    val obligations: List<ObligationUi>,
    val metrics: List<MetricUi>,
    val completeness: Int,
    val missingData: String?,
    val alert: String?,
    val cashAfterCardBills: String = "Unavailable"
)

private data class CockpitScenario(
    val id: String,
    val p10: Double,
    val p50: Double,
    val p90: Double,
    val scoreInputs: HealthScoreInputs,
    val alert: String? = null
)

class CockpitViewModel : ViewModel() {
    private val scenarios = listOf(
        CockpitScenario("steady", 18_000.0, 22_000.0, 26_000.0, fixtureScoreInputs()),
        CockpitScenario(
            "pressure", 4_000.0, 8_000.0, 12_000.0,
            fixtureScoreInputs().copy(cash = 24_000.0, monthlyDebtService = 18_000.0, netCashFlowStdDev = 17_000.0),
            "Card bill and loan EMI land in the same week."
        )
    )
    private val _uiState = MutableStateFlow(scenarios.first().toUiState())
    val uiState: StateFlow<CockpitUiState> = _uiState.asStateFlow()

    fun selectScenario(id: String) {
        scenarios.firstOrNull { it.id == id }?.let { _uiState.value = it.toUiState() }
    }
}

object CockpitFixtures {
    fun full() = fixtureScenario().toUiState()
    fun skeleton() = full().copy(contentState = CockpitContentState.SKELETON)
    fun noData(): CockpitUiState {
        val unknown = HealthScore.calculate(HealthScoreInputs(null, null, null, null, null, null, null))
        return full().copy(
            contentState = CockpitContentState.NO_DATA,
            verdict = "Choose a data source",
            verdictReading = "Add account data to build your first forecast.",
            healthScore = unknown.score,
            healthStatus = HealthStatus.RISK,
            healthComponents = unknown.components,
            accounts = AccountKind.entries.map { AccountSummaryUi(it, it.name.lowercase().replaceFirstChar { char -> char.uppercase() }, null) },
            obligations = emptyList(), metrics = emptyList(), completeness = 0,
            missingData = "Connect an account or try the synthetic demo.", alert = null
        )
    }
    fun lowCompleteness() = full().copy(
        contentState = CockpitContentState.LOW_COMPLETENESS,
        completeness = 56,
        missingData = "Add your car-loan rate and one income source to narrow the range.",
        accounts = full().accounts.map { if (it.kind == AccountKind.SIP) it.copy(amount = null) else it }
    )
    fun alertActive() = pressureScenario().toUiState().copy(contentState = CockpitContentState.ALERT_ACTIVE)

    val all get() = listOf(skeleton(), noData(), lowCompleteness(), alertActive(), full())
}

private fun fixtureScenario() = CockpitScenario("steady", 18_000.0, 22_000.0, 26_000.0, fixtureScoreInputs())
private fun pressureScenario() = CockpitScenario(
    "pressure", 4_000.0, 8_000.0, 12_000.0,
    fixtureScoreInputs().copy(cash = 24_000.0, monthlyDebtService = 18_000.0, netCashFlowStdDev = 17_000.0),
    "Card bill and loan EMI land in the same week."
)

private fun fixtureScoreInputs() = HealthScoreInputs(
    cash = 45_200.0, monthlyObligations = 31_000.0, monthlyDebtService = 12_500.0,
    monthlyIncome = 85_000.0, minimumReserve = 40_000.0,
    netCashFlowStdDev = 8_500.0, meanIncome = 85_000.0
)

private fun CockpitScenario.toUiState(): CockpitUiState {
    val score = HealthScore.calculate(scoreInputs)
    val status = when {
        score.score < 50 -> HealthStatus.RISK
        score.score <= 75 -> HealthStatus.WATCH
        else -> HealthStatus.SAFE
    }
    val range = "${formatInrCompact(p10)}–${formatInrCompact(p90).removePrefix("₹")}"
    return CockpitUiState(
        contentState = if (alert == null) CockpitContentState.FULL else CockpitContentState.ALERT_ACTIVE,
        name = "Asha", dataSource = "Demo data",
        verdict = "Likely $range left on 25 Oct",
        verdictReading = "After upcoming commitments",
        p10 = p10, p50 = p50, p90 = p90,
        confidence = ConfidenceUi("Medium", range, "The middle 80% of fixture outcomes falls in this range.", "Add more confirmed income and obligations to narrow it."),
        healthScore = score.score, healthStatus = status, healthComponents = score.components,
        accounts = listOf(
            AccountSummaryUi(AccountKind.CASH, "Cash", formatInr(45_200.0)),
            AccountSummaryUi(AccountKind.CARD, "Card dues", formatInr(14_300.0)),
            AccountSummaryUi(AccountKind.LOAN, "Loan EMIs", formatInr(12_500.0)),
            AccountSummaryUi(AccountKind.SIP, "SIP", formatInr(5_000.0))
        ),
        accountDetails = fixtureAccountDetails(),
        obligations = listOf(
            ObligationUi("Credit card bill", "Today", formatInr(4_200.0), true),
            ObligationUi("Home loan EMI", "18 Oct", formatInr(12_500.0)),
            ObligationUi("SIP debit", "22 Oct", formatInr(5_000.0))
        ),
        metrics = listOf(
            MetricUi("Discretionary runway", "2.4 months", StatementType.PREDICTION, "At the current pace"),
            MetricUi("Debt pressure", formatPercent(12_500.0 / 85_000.0), StatementType.FACT, "Of monthly income"),
            MetricUi("Essentials share", formatPercent(31_000.0 / 85_000.0), StatementType.FACT, "Includes required dues"),
            MetricUi("Shortfall risk", formatPercent(if (alert == null) 0.08 else 0.31), StatementType.PREDICTION, "Next 30 days")
        ),
        completeness = if (alert == null) 88 else 92, missingData = null, alert = alert
    )
}

private fun fixtureAccountDetails() = listOf(
    AccountDetailUi("Bank accounts · Observed", listOf("HDFC Bank •• 4091", "Balance ${formatInr(45_200.0)} · as of today")),
    AccountDetailUi("Credit cards · Observed", listOf("HDFC Card · limit ${formatInr(1_20_000.0)}", "Total due ${formatInr(14_300.0)} · minimum ${formatInr(1_200.0)} · 25 Oct")),
    AccountDetailUi("Loans · Observed", listOf("Home loan · outstanding ${formatInr(18_40_000.0)}", "8.6% · EMI ${formatInr(12_500.0)} · 148 months left")),
    AccountDetailUi("SIPs · Observed", listOf("Groww · ${formatInr(5_000.0)} monthly · debit day 22"))
)
