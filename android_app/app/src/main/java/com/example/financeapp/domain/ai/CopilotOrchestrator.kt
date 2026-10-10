package com.example.financeapp.domain.ai

import com.example.financeapp.domain.model.CopilotIntent
import com.example.financeapp.domain.model.ResultBundle
import com.example.financeapp.domain.model.StructuredResponse

class CopilotOrchestrator(
    private val resultProvider: (CopilotIntent) -> ResultBundle,
    private val intentRouter: IntentRouter = IntentRouter(),
    private val localAdvisor: LocalAdvisor = LocalAdvisor()
) {
    fun answer(question: String): StructuredResponse {
        val intent = intentRouter.route(question)
        return localAdvisor.advise(intent, resultProvider(intent))
    }
}
