package com.example.financeapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.financeapp.data.AccountSetup
import com.example.financeapp.data.Resource
import com.example.financeapp.ui.util.formatInr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class OnboardingPage { WELCOME, DATA_SOURCE, SMS_CONSENT, ABOUT_MONEY, SAFETY_NET, BUILDING }

enum class OnboardingDataSource(val title: String, val detail: String) {
    SMS("Use bank alerts on this phone", "Find amounts, dates and merchants from supported alerts."),
    CSV("Upload a bank statement", "Import a CSV from your bank."),
    SAMPLE("Explore with sample data", "See how RupAI works before adding anything personal.")
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
    val cushionSelected: Boolean = false,
    val goalName: String = "",
    val goalDigits: String = "",
    val ingestRows: List<IngestRowUi> = emptyList(),
    val completeness: String? = null,
    val ingestFailed: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null
)

class OnboardingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun next() {
        val state = _uiState.value
        if (state.busy) return
        _uiState.value = when (state.page) {
            OnboardingPage.WELCOME -> state.copy(page = OnboardingPage.DATA_SOURCE)
            OnboardingPage.DATA_SOURCE -> if (state.selectedSource == null || state.selectedSource == OnboardingDataSource.CSV) state
            else if (state.selectedSource == OnboardingDataSource.SMS) {
                state.copy(page = OnboardingPage.SMS_CONSENT)
            } else state.copy(page = OnboardingPage.ABOUT_MONEY)
            OnboardingPage.SMS_CONSENT -> state.copy(page = OnboardingPage.ABOUT_MONEY)
            OnboardingPage.ABOUT_MONEY -> if (state.moneyStep == 0 && state.moneyArrival != null) state.copy(moneyStep = 1)
                else if (state.moneyStep == 1 && state.obligations.isNotEmpty()) state.copy(page = OnboardingPage.SAFETY_NET)
                else state
            OnboardingPage.SAFETY_NET -> state
            OnboardingPage.BUILDING -> state
        }
    }

    fun back() {
        val state = _uiState.value
        if (state.busy) return
        _uiState.value = when {
            state.page == OnboardingPage.ABOUT_MONEY && state.moneyStep > 0 -> state.copy(moneyStep = state.moneyStep - 1)
            state.page == OnboardingPage.DATA_SOURCE -> state.copy(page = OnboardingPage.WELCOME)
            state.page == OnboardingPage.SMS_CONSENT -> state.copy(page = OnboardingPage.DATA_SOURCE)
            state.page == OnboardingPage.ABOUT_MONEY -> state.copy(page = OnboardingPage.DATA_SOURCE)
            state.page == OnboardingPage.SAFETY_NET -> state.copy(page = OnboardingPage.ABOUT_MONEY, moneyStep = 1)
            else -> state
        }
    }

    private fun edit(change: (OnboardingUiState) -> OnboardingUiState) {
        if (!_uiState.value.busy) _uiState.value = change(_uiState.value).copy(error = null)
    }
    fun selectSource(source: OnboardingDataSource) { edit { it.copy(selectedSource = source) } }
    fun chooseAlternative(source: OnboardingDataSource) {
        edit { it.copy(page = OnboardingPage.DATA_SOURCE, selectedSource = source, ingestFailed = false) }
    }
    fun setPermissionState(state: SmsPermissionState) { _uiState.value = _uiState.value.copy(permissionState = state) }
    fun setMoneyArrival(value: String) { edit { it.copy(moneyArrival = value) } }
    fun toggleObligation(value: String) {
        if (_uiState.value.busy) return
        if (value !in AccountSetup.OBLIGATIONS && value != "None") return
        val choices = _uiState.value.obligations.toMutableSet()
        if (value == "None") { choices.clear(); choices.add("None") }
        else { choices.remove("None"); if (!choices.add(value)) choices.remove(value) }
        _uiState.value = _uiState.value.copy(obligations = choices)
    }
    fun setAgeRange(value: String) { edit { it.copy(ageRange = value) } }
    fun setCushion(value: String) {
        if (value.length <= 9 && value.all { it in '0'..'9' }) edit { it.copy(cushionDigits = value, cushionSelected = true) }
    }
    fun setGoalName(value: String) { edit { it.copy(goalName = value.take(80)) } }
    fun setGoalAmount(value: String) {
        if (value.length <= 9 && value.all { it in '0'..'9' }) edit { it.copy(goalDigits = value) }
    }

    fun load(setup: AccountSetup?) {
        if (setup == null) return
        _uiState.value = _uiState.value.copy(moneyArrival = setup.incomePattern,
            obligations = setup.obligations.toSet(), cushionDigits = setup.minimumReserve?.toString().orEmpty(),
            cushionSelected = setup.minimumReserve != null,
            goalName = setup.goalName.orEmpty(), goalDigits = setup.goalAmount?.toString().orEmpty())
    }

    fun error(message: String?) { _uiState.value = _uiState.value.copy(error = message) }
    fun busy(value: Boolean) { _uiState.value = _uiState.value.copy(busy = value) }

    suspend fun finish(userId: String?, cloudConsent: Boolean, save: suspend (AccountSetup) -> Resource<Unit>) {
        val state = _uiState.value
        if (state.busy) return
        val draft = AccountSetup(userId ?: "00000000-0000-0000-0000-000000000000", state.moneyArrival,
            state.obligations.filter { it != "None" }.sorted(), state.cushionDigits.toLongOrNull(),
            state.goalName.trim().takeIf { it.isNotEmpty() }, state.goalDigits.toLongOrNull())
        val validation = draft.validationError()
        if (validation != null) { error(validation); return }
        if (cloudConsent && userId == null) { error("Sign in before saving to Supabase."); return }
        busy(true)
        error(null)
        try {
            val result = if (cloudConsent) save(draft) else Resource.Success(Unit)
            when (result) {
                is Resource.Success -> _uiState.value = _uiState.value.copy(page = OnboardingPage.BUILDING,
                    ingestRows = emptyList(), completeness = if (cloudConsent) "Setup saved to your account. SMS transactions remain on this phone."
                    else "No financial setup was saved. You can add it to your account later from Profile.")
                is Resource.Error -> error(result.message)
                Resource.Loading -> error("Please try saving again.")
            }
        } finally { busy(false) }
    }
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
    fun safetyNet() = sample.copy(page = OnboardingPage.SAFETY_NET, cushionDigits = "50000", cushionSelected = true)
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
        aboutMoney(0), aboutMoney(1), safetyNet(), building(), complete(), failed()
    )
}
