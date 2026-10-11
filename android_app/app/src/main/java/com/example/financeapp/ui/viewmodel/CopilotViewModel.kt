package com.example.financeapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.planning.DemoFinanceResult
import com.example.financeapp.data.remote.BackendClient
import com.example.financeapp.domain.ai.IntentRouter
import com.example.financeapp.domain.ai.NumberVerifier
import com.example.financeapp.domain.ai.localization.LocalizedAdvisor
import com.example.financeapp.domain.ai.localization.NarrationLanguage
import com.example.financeapp.domain.model.ResponseSource
import com.example.financeapp.ui.util.formatInr
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CopilotContentState { EMPTY, ANSWER, THINKING, OFFLINE, ERROR }

data class CopilotAnswerUi(
    val observed: String,
    val observedSource: String,
    val predicted: String,
    val predictedRange: String,
    val predictedHorizon: String,
    val confidence: String,
    val recommended: String,
    val options: List<CopilotOptionUi>,
    val isLong: Boolean = true
)

data class CopilotOptionUi(val action: String, val impact: String, val tradeOff: String)
data class CopilotMessageUi(val text: String, val fromUser: Boolean, val answer: CopilotAnswerUi? = null)

data class CopilotUiState(
    val contentState: CopilotContentState = CopilotContentState.EMPTY,
    val messages: List<CopilotMessageUi> = emptyList(),
    val draft: String = "",
    val languageCode: String = "en",
    val error: String? = null
)

class CopilotViewModel(
    private val snapshotProvider: () -> DemoFinanceResult? = { null },
    private val client: BackendClient = BackendClient(localizedAdvisor = LocalizedAdvisor::adviseBundle),
    private val scope: CoroutineScope? = null
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(CopilotUiState())
    val uiState: StateFlow<CopilotUiState> = mutableUiState.asStateFlow()
    private var request: Job? = null
    private var lastQuestion = ""

    fun updateDraft(value: String) { mutableUiState.value = mutableUiState.value.copy(draft = value.take(1000)) }

    fun invalidate() {
        request?.cancel()
        lastQuestion = ""
        mutableUiState.value = CopilotUiState(languageCode = mutableUiState.value.languageCode)
    }

    fun selectLanguage(code: String) {
        require(NarrationLanguage.fromCode(code) != null)
        invalidate()
        mutableUiState.value = mutableUiState.value.copy(languageCode = code)
    }

    fun submit(question: String = mutableUiState.value.draft) {
        if (question.isBlank() || question.length > 1000) return
        request?.cancel()
        lastQuestion = question
        val snapshot = snapshotProvider()
        val bundle = snapshot?.bundle
        if (snapshot == null || bundle == null) {
            mutableUiState.value = mutableUiState.value.copy(contentState = CopilotContentState.ERROR,
                error = "Demo facts are not available yet. Choose a scenario or retry after calculation.")
            return
        }
        val language = mutableUiState.value.languageCode
        val messages = mutableUiState.value.messages.takeLast(18) + CopilotMessageUi(question, true)
        mutableUiState.value = CopilotUiState(CopilotContentState.THINKING, messages, languageCode = language)
        request = (scope ?: viewModelScope).launch {
            try {
                val intent = IntentRouter().route(question)
                var fallback = LocalizedAdvisor.adviseBundle(intent, bundle, language)
                val review = snapshot.decision.noPlanMeetsLimits || snapshot.conflict != null
                if (review && language == "en") fallback = fallback.copy(recommended = fallback.recommended.map {
                    it.copy(text = "Review the computed conflict and risk warnings before choosing a demo plan.",
                        tradeOff = "This answer does not authorize a financial action.")
                })
                val candidate = if (review) fallback else client.narrate(intent, bundle, language)
                ensureActive()
                if (snapshotProvider() !== snapshot) return@launch
                val answer = NumberVerifier().verifyOrFallback(candidate, intent, bundle, fallback)
                val prediction = answer.predicted.single()
                val ui = CopilotAnswerUi(answer.observed.joinToString("\n") { it.text },
                    "Synthetic data only | " + if (answer.source == ResponseSource.REMOTE) "Verified backend narration" else "Verified local template",
                    prediction.text, "${formatInr(prediction.range.p10)} to ${formatInr(prediction.range.p90)}",
                    prediction.horizon, prediction.confidenceLabel.name,
                    answer.recommended.joinToString("\n") { it.text }, emptyList(), false)
                mutableUiState.value = CopilotUiState(
                    if (answer.source == ResponseSource.REMOTE) CopilotContentState.ANSWER else CopilotContentState.OFFLINE,
                    messages + CopilotMessageUi("", false, ui), languageCode = language)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableUiState.value = CopilotUiState(CopilotContentState.ERROR, messages,
                languageCode = language, error = "Verified narration unavailable. Retry or choose another scenario.") }
        }
    }

    fun retry() { if (lastQuestion.isNotBlank()) submit(lastQuestion) }
}

object CopilotFixtures {
    const val starter = "Can I afford a ₹30,000 trip next month?"
    val suggestions = listOf(starter, "Should I pause my SIP?", "Can I pay extra on my loan?")

    fun empty() = CopilotUiState()
    fun thinking() = CopilotUiState(CopilotContentState.THINKING, listOf(CopilotMessageUi(starter, true)))
    fun error() = CopilotUiState(CopilotContentState.ERROR, listOf(CopilotMessageUi(starter, true)))
    fun answer(question: String = starter, offline: Boolean = false): CopilotUiState {
        val answer = CopilotAnswerUi(
            observed = "You have ₹92,400 in cash. ₹18,600 of card dues and ₹31,500 of rent, EMI and SIP are committed next month.",
            observedSource = "Source: 2 bank accounts, 1 card and 90 days of transactions",
            predicted = "After a ₹30,000 trip, your month-end balance is likely to stay above your ₹40,000 cushion.",
            predictedRange = "₹38,200–₹55,800",
            predictedHorizon = "By the end of next month",
            confidence = "Medium confidence",
            recommended = "Keep the trip budget at ₹24,000, or split the remaining ₹6,000 into the following month.",
            options = listOf(
                CopilotOptionUi("Cap the trip at ₹24,000", "Shortfall chance falls from 18% to 9%", "Less room for activities"),
                CopilotOptionUi("Split ₹6,000 into next month", "Keeps this month above the cushion", "Uses next month's discretionary budget")
            )
        )
        return CopilotUiState(
            contentState = if (offline) CopilotContentState.OFFLINE else CopilotContentState.ANSWER,
            messages = listOf(CopilotMessageUi(question, true), CopilotMessageUi("", false, answer))
        )
    }

    val all get() = listOf(empty(), answer(), thinking(), answer(offline = true), error())
}
