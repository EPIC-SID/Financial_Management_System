package com.example.financeapp.domain.ai

import com.example.financeapp.domain.model.ConfidenceLabel
import com.example.financeapp.domain.model.CopilotIntent
import com.example.financeapp.domain.model.ResponseSource
import com.example.financeapp.domain.model.ResultBundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CopilotOrchestratorTest {
    private val bundle = ResultBundle(
        liquidRunwayMonths = 3.4,
        projectedShortfallRisk = 0.12,
        recommendedPlanId = "Plan B",
        savingsRate = 0.18,
        bankCash = 45200.0,
        cardDues = 14300.0,
        monthlyLoanEmis = 26700.0,
        monthlySip = 10000.0,
        avgDiscretionary = 6400.0,
        forecastP10 = 38000.0,
        forecastP50 = 52000.0,
        forecastP90 = 69000.0,
        horizon = "6 months",
        confidenceLabel = ConfidenceLabel.Medium
    )

    @Test
    fun everySupportedIntentReturnsOfflineThreeSectionAnswer() {
        val questionsByIntent = mapOf(
            CopilotIntent.ACCOUNT_OVERVIEW to "Give me an account overview",
            CopilotIntent.PLAN_COMPARISON to "Compare my plan options",
            CopilotIntent.RISK_EXPLANATION to "Why is shortfall risk high?",
            CopilotIntent.WHAT_IF_QUERY to "What if I have a surprise expense?",
            CopilotIntent.GENERAL_INQUIRY to "Can you help?"
        )
        val orchestrator = CopilotOrchestrator(resultProvider = { bundle })

        questionsByIntent.forEach { (intent, question) ->
            val response = orchestrator.answer(question)

            assertEquals(intent, response.intent)
            assertEquals(ResponseSource.LOCAL, response.source)
            assertTrue(response.observed.single().text.isNotBlank())
            assertTrue(response.predicted.single().text.isNotBlank())
            assertTrue(response.recommended.single().text.isNotBlank())
        }
    }

    @Test
    fun resultProviderReceivesRoutedIntent() {
        var routedIntent: CopilotIntent? = null
        val orchestrator = CopilotOrchestrator(resultProvider = { intent ->
            routedIntent = intent
            bundle
        })

        orchestrator.answer("Should I pause SIP in a what if?")

        assertEquals(CopilotIntent.WHAT_IF_QUERY, routedIntent)
    }
}
