package com.example.financeapp.domain.ai

import com.example.financeapp.domain.model.CopilotIntent
import com.example.financeapp.domain.model.ResultBundle
import com.example.financeapp.domain.model.StructuredResponse
import com.example.financeapp.domain.model.NumberRange
import com.example.financeapp.domain.model.RecommendationImpact
import com.example.financeapp.domain.model.ResponseSource
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.logging.Logger

class NumberVerifier(private val localAdvisor: LocalAdvisor = LocalAdvisor()) {
    private val units = "(?:thousand|million|billion|lakhs?|crores?|cr|k|l)(?![A-Za-z])"
    private val numbers = Regex("(?:[+\\-\u2212]\\s*)?(?:[0-9]+(?:,[0-9]+)*(?:\\.[0-9]+)?|\\.[0-9]+)(?:[eE][+\\-]?[0-9]+)?(?:\\s*$units)?(?:\\s*%)?", RegexOption.IGNORE_CASE)
    private val token = Regex("([+\\-]?\\s*[0-9][0-9,]*(?:\\.[0-9]+)?)(?:\\s*($units))?(\\s*%)?", RegexOption.IGNORE_CASE)
    private val grouped = Regex("(?:[0-9]+|[0-9]{1,3}(?:,[0-9]{3})+|[0-9]{1,2}(?:,[0-9]{2})*,[0-9]{3})(?:\\.[0-9]+)?")
    // MVP accepts ASCII amounts/% only; unparsed localized multipliers can change magnitude.
    private val unsupportedUnits = Regex(
        "(?<![\\p{L}\\p{M}])(?:हजार|हज़ार|हज़ार|लाख|लक्ष|करोड़|करोड|कोटी|कोटि|सौ|शत|प्रतिशत|टक्के|hazaar|hazar|karod|koti|pratishat|takk[eay])(?![\\p{L}\\p{M}])",
        RegexOption.IGNORE_CASE)

    fun isVerified(text: String, bundle: ResultBundle): Boolean {
        if (text.length > 2000 || text.any { it.isDigit() && it !in '0'..'9' } || unsupportedUnits.containsMatchIn(text)) return false
        val values = listOf(bundle.liquidRunwayMonths, bundle.projectedShortfallRisk, bundle.savingsRate,
            bundle.bankCash, bundle.cardDues, bundle.monthlyLoanEmis, bundle.monthlySip,
            bundle.avgDiscretionary, bundle.forecastP10, bundle.forecastP50, bundle.forecastP90)
        if (values.any { !it.isFinite() }) return false
        val labels = numbers.findAll(bundle.horizon + " " + bundle.recommendedPlanId)
            .mapNotNull { it.value.toDoubleOrNull() }.toList()
        return numbers.findAll(text).all { match ->
            val parsed = token.matchEntire(match.value.replace('\u2212', '-')) ?: return@all false
            val raw = parsed.groupValues[1].replace(Regex("\\s"), "")
            if (!grouped.matches(raw.removePrefix("-").removePrefix("+"))) return@all false
            val amount = raw.replace(",", "").toBigDecimalOrNull() ?: return@all false
            val scale = when (parsed.groupValues[2].lowercase()) {
                "k", "thousand" -> 1000.0
                "l", "lakh", "lakhs" -> 100000.0
                "cr", "crore", "crores" -> 10000000.0
                "million" -> 1000000.0
                "billion" -> 1000000000.0
                else -> 1.0
            }
            val percentage = parsed.groupValues[3].isNotBlank()
            if (percentage && scale != 1.0) return@all false
            val candidates = if (percentage) listOf(bundle.projectedShortfallRisk * 100, bundle.savingsRate * 100)
                else values + labels
            candidates.any { value ->
                BigDecimal.valueOf(value / scale).setScale(amount.scale(), RoundingMode.HALF_UP).compareTo(amount) == 0
            }
        }
    }

    fun verifyOrFallback(response: StructuredResponse, intent: CopilotIntent, bundle: ResultBundle,
        fallback: StructuredResponse = localAdvisor.advise(intent, bundle)): StructuredResponse {
        require(fallback.source == ResponseSource.LOCAL && isValidResponse(fallback, intent, bundle)) {
            "Invalid local narration template"
        }
        if (isValidResponse(response, intent, bundle)) return response
        Logger.getLogger(NumberVerifier::class.java.name).warning("Copilot narration verification failed; using local template")
        return fallback
    }

    private fun isValidResponse(response: StructuredResponse, intent: CopilotIntent, bundle: ResultBundle): Boolean {
        val texts = response.observed.map { it.text } + response.predicted.map { it.text } +
            response.recommended.flatMap { listOf(it.text, it.tradeOff) }
        val metadataMatches = response.intent == intent &&
            response.observed.isNotEmpty() && response.predicted.isNotEmpty() && response.recommended.isNotEmpty() &&
            response.observed.all { fact -> fact.refs.all { it in allowedRefs } } &&
            response.predicted.all { it.range == NumberRange(bundle.forecastP10, bundle.forecastP50, bundle.forecastP90) &&
                it.confidenceLabel == bundle.confidenceLabel && it.horizon == bundle.horizon } &&
            response.recommended.all { it.impact == RecommendationImpact() }
        return metadataMatches && texts.all { it.isNotBlank() && isVerified(it, bundle) }
    }

    private val allowedRefs = setOf("liquidRunwayMonths", "projectedShortfallRisk", "recommendedPlanId", "savingsRate",
        "bankCash", "cardDues", "monthlyLoanEmis", "monthlySip", "avgDiscretionary", "forecastP10", "forecastP50", "forecastP90")
}
