package com.example.financeapp.ui.viewmodel

import androidx.lifecycle.ViewModel
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
    val draft: String = ""
)

class CopilotViewModel : ViewModel() {
    private val mutableUiState = MutableStateFlow(CopilotFixtures.empty())
    val uiState: StateFlow<CopilotUiState> = mutableUiState.asStateFlow()

    fun updateDraft(value: String) { mutableUiState.value = mutableUiState.value.copy(draft = value) }

    fun submit(question: String = mutableUiState.value.draft) {
        if (question.isBlank()) return
        mutableUiState.value = CopilotFixtures.answer(question)
    }

    fun retry() { mutableUiState.value = CopilotFixtures.answer() }
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
