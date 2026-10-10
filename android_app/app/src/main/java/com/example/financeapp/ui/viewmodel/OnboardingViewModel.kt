package com.example.financeapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.financeapp.ui.util.formatInr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class OnboardingPage { WELCOME, DATA_SOURCE, SMS_CONSENT, ABOUT_MONEY, SAFETY_NET, BUILDING }

enum class OnboardingDataSource(val title: String, val detail: String) {
    SMS("Read bank SMS", "Find payments from supported bank alerts on this phone."),
    CSV("Upload a bank statement", "Import a CSV from your bank."),
    SAMPLE("Try a sample profile", "Explore RupAI with realistic demo data.")
}

enum class SmsPermissionState { EXPLAINER, DENIED, PERMANENTLY_DENIED }
enum class IngestStatus { RUNNING, DONE, FAILED }

data class IngestRowUi(val label: String, val status: IngestStatus, val detail: String = "")

data class OnboardingUiState(
    val page: OnboardingPage = OnboardingPage.WELCOME,
    val selectedSource: OnboardingDataSource? = null,
    val permissionState: SmsPermissionState = SmsPermissionState.EXPLAINER,
    val moneyStep: Int = 0,
    val moneyArrival: String? = null,
    val obligations: Set<String> = emptySet(),
    val ageRange: String? = null,
    val cushionDigits: String = "",
    val goalName: String = "",
    val goalDigits: String = "",
    val ingestRows: List<IngestRowUi> = emptyList(),
    val completeness: String? = null,
    val ingestFailed: Boolean = false
)

class OnboardingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingFixtures.welcome())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun next() {
        val state = _uiState.value
        _uiState.value = when (state.page) {
            OnboardingPage.WELCOME -> state.copy(page = OnboardingPage.DATA_SOURCE)
            OnboardingPage.DATA_SOURCE -> if (state.selectedSource == OnboardingDataSource.SMS) {
                state.copy(page = OnboardingPage.SMS_CONSENT)
            } else state.copy(page = OnboardingPage.ABOUT_MONEY)
            OnboardingPage.SMS_CONSENT -> state.copy(page = OnboardingPage.ABOUT_MONEY)
            OnboardingPage.ABOUT_MONEY -> if (state.moneyStep < 2) state.copy(moneyStep = state.moneyStep + 1)
                else state.copy(page = OnboardingPage.SAFETY_NET)
            OnboardingPage.SAFETY_NET -> OnboardingFixtures.building()
            OnboardingPage.BUILDING -> state
        }
    }

    fun back() {
        val state = _uiState.value
        _uiState.value = when {
            state.page == OnboardingPage.ABOUT_MONEY && state.moneyStep > 0 -> state.copy(moneyStep = state.moneyStep - 1)
            state.page == OnboardingPage.DATA_SOURCE -> state.copy(page = OnboardingPage.WELCOME)
            state.page == OnboardingPage.SMS_CONSENT -> state.copy(page = OnboardingPage.DATA_SOURCE)
            state.page == OnboardingPage.ABOUT_MONEY -> state.copy(page = OnboardingPage.DATA_SOURCE)
            state.page == OnboardingPage.SAFETY_NET -> state.copy(page = OnboardingPage.ABOUT_MONEY, moneyStep = 2)
            else -> state
        }
    }

    fun selectSource(source: OnboardingDataSource) { _uiState.value = _uiState.value.copy(selectedSource = source) }
    fun chooseAlternative(source: OnboardingDataSource) {
        _uiState.value = _uiState.value.copy(page = OnboardingPage.DATA_SOURCE, selectedSource = source, ingestFailed = false)
    }
    fun setPermissionState(state: SmsPermissionState) { _uiState.value = _uiState.value.copy(permissionState = state) }
    fun setMoneyArrival(value: String) { _uiState.value = _uiState.value.copy(moneyArrival = value) }
    fun toggleObligation(value: String) {
        val choices = _uiState.value.obligations.toMutableSet()
        if (!choices.add(value)) choices.remove(value)
        _uiState.value = _uiState.value.copy(obligations = choices)
    }
    fun setAgeRange(value: String) { _uiState.value = _uiState.value.copy(ageRange = value) }
    fun setCushion(value: String) { _uiState.value = _uiState.value.copy(cushionDigits = value.filter(Char::isDigit).take(9)) }
    fun setGoalName(value: String) { _uiState.value = _uiState.value.copy(goalName = value) }
    fun setGoalAmount(value: String) { _uiState.value = _uiState.value.copy(goalDigits = value.filter(Char::isDigit).take(9)) }
}

fun formatOnboardingCurrency(digits: String): String =
    digits.filter(Char::isDigit).take(9).toDoubleOrNull()?.let(::formatInr) ?: ""

object OnboardingFixtures {
    private val sample = OnboardingUiState(selectedSource = OnboardingDataSource.SAMPLE)
    fun welcome() = OnboardingUiState(page = OnboardingPage.WELCOME)
    fun source(selected: OnboardingDataSource? = null) = OnboardingUiState(page = OnboardingPage.DATA_SOURCE, selectedSource = selected)
    fun sms(state: SmsPermissionState = SmsPermissionState.EXPLAINER) = sample.copy(
        page = OnboardingPage.SMS_CONSENT, selectedSource = OnboardingDataSource.SMS, permissionState = state
    )
    fun aboutMoney(step: Int = 0) = sample.copy(
        page = OnboardingPage.ABOUT_MONEY, moneyStep = step, moneyArrival = "A mix",
        obligations = setOf("Rent", "Credit card"), ageRange = "26–38"
    )
    fun safetyNet() = sample.copy(page = OnboardingPage.SAFETY_NET, cushionDigits = "50000")
    fun building() = sample.copy(
        page = OnboardingPage.BUILDING,
        ingestRows = listOf(
            IngestRowUi("Reading sample entries", IngestStatus.DONE, "24 entries"),
            IngestRowUi("Finding repeat payments", IngestStatus.DONE, "3 found"),
            IngestRowUi("Estimating income", IngestStatus.RUNNING),
            IngestRowUi("Checking upcoming dues", IngestStatus.RUNNING)
        )
    )
    fun complete() = building().copy(
        ingestRows = building().ingestRows.map { it.copy(status = IngestStatus.DONE) },
        completeness = "1 bank account, 1 card, 2 loans, 1 SIP, 3 repeat payments and 1 income source found. Missing: car loan rate."
    )
    fun failed() = building().copy(
        ingestRows = listOf(IngestRowUi("Building your picture", IngestStatus.FAILED, "No supported entries found")),
        ingestFailed = true
    )
    val previews get() = listOf(
        welcome(), source(), source(OnboardingDataSource.SAMPLE), sms(), sms(SmsPermissionState.DENIED), sms(SmsPermissionState.PERMANENTLY_DENIED),
        aboutMoney(0), aboutMoney(1), aboutMoney(2), safetyNet(), building(), complete(), failed()
    )
}
