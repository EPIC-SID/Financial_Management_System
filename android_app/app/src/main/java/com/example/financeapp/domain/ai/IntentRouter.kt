package com.example.financeapp.domain.ai

import com.example.financeapp.domain.model.CopilotIntent

class IntentRouter {
    fun route(question: String): CopilotIntent {
        val normalized = question.lowercase()
        return when {
            listOf("compare", "plan", "option", "sequence").any(normalized::contains) ->
                CopilotIntent.PLAN_COMPARISON
            listOf("risk", "shortfall", "dip", "unsafe", "cushion").any(normalized::contains) ->
                CopilotIntent.RISK_EXPLANATION
            listOf("what if", "simulate", "expense", "income cut", "pause sip").any(normalized::contains) ->
                CopilotIntent.WHAT_IF_QUERY
            listOf("account", "cash", "card", "loan", "sip", "overview").any(normalized::contains) ->
                CopilotIntent.ACCOUNT_OVERVIEW
            else -> CopilotIntent.GENERAL_INQUIRY
        }
    }
}
