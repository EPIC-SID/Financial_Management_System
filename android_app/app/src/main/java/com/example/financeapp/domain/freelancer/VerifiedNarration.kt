package com.example.financeapp.domain.freelancer

import com.example.financeapp.domain.ai.NumberVerifier
import com.example.financeapp.domain.model.*

/** Numerically checked output, not a certification of translated meaning or financial advice. */
class VerifiedNarration private constructor(
    val response: StructuredResponse,
    val languageCode: String,
    val speechLocale: String,
    val speechText: String,
    val requiresReview: Boolean
) {
    val provenance: ResponseSource get() = response.source

    companion object {
        fun verify(response: StructuredResponse, intent: CopilotIntent, bundle: ResultBundle,
            languageCode: String, fallback: StructuredResponse, requiresReview: Boolean = false): VerifiedNarration {
            val locale = when (languageCode) {
                "en" -> "en-IN"
                "hi-Latn", "hi-Deva" -> "hi-IN"
                "mr-Deva" -> "mr-IN"
                else -> throw IllegalArgumentException("Unsupported narration language")
            }
            val checked = NumberVerifier().verifyOrFallback(response, intent, bundle, fallback)
            val snapshot = checked.copy(observed = checked.observed.map { it.copy(refs = it.refs.toList()) },
                predicted = checked.predicted.toList(), recommended = checked.recommended.toList())
            val text = (snapshot.observed.map { it.text } + snapshot.predicted.map { it.text } +
                snapshot.recommended.flatMap { listOf(it.text, it.tradeOff) }).joinToString("\n")
            return VerifiedNarration(snapshot, languageCode, locale, text, requiresReview)
        }
    }
}
