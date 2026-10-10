package com.example.financeapp.domain.ai

import com.example.financeapp.domain.model.CopilotIntent
import com.example.financeapp.domain.model.NumberRange
import com.example.financeapp.domain.model.ObservedFact
import com.example.financeapp.domain.model.PredictedFact
import com.example.financeapp.domain.model.RecommendationImpact
import com.example.financeapp.domain.model.RecommendedAction
import com.example.financeapp.domain.model.ResponseSource
import com.example.financeapp.domain.model.ResultBundle
import com.example.financeapp.domain.model.StructuredResponse
import java.util.Locale
import kotlin.math.roundToInt

class LocalAdvisor {
    fun advise(intent: CopilotIntent, bundle: ResultBundle): StructuredResponse {
        return StructuredResponse(
            observed = listOf(observed(intent, bundle)),
            predicted = listOf(predicted(bundle)),
            recommended = listOf(recommended(intent, bundle)),
            intent = intent,
            source = ResponseSource.LOCAL
        )
    }

    private fun observed(intent: CopilotIntent, bundle: ResultBundle): ObservedFact {
        val text = when (intent) {
            CopilotIntent.ACCOUNT_OVERVIEW ->
                "Observed: bank cash is ${money(bundle.bankCash)}, card dues are ${money(bundle.cardDues)}, loan EMIs are ${money(bundle.monthlyLoanEmis)}, SIP is ${money(bundle.monthlySip)}, and average discretionary spend is ${money(bundle.avgDiscretionary)}."
            CopilotIntent.PLAN_COMPARISON ->
                "Observed: the current bundle supports plan ${bundle.recommendedPlanId} with a savings rate of ${percent(bundle.savingsRate)}."
            CopilotIntent.RISK_EXPLANATION ->
                "Observed: the current liquid runway is ${oneDecimal(bundle.liquidRunwayMonths)} months before the declared cushion is stressed."
            CopilotIntent.WHAT_IF_QUERY ->
                "Observed: the baseline has ${money(bundle.bankCash)} cash and ${money(bundle.monthlySip)} monthly SIP committed."
            CopilotIntent.GENERAL_INQUIRY ->
                "Observed: I can answer from aggregate cash, dues, loan EMI, SIP, discretionary spend, runway, risk, and the recommended plan."
        }
        return ObservedFact(text, observedRefs(intent))
    }

    private fun predicted(bundle: ResultBundle): PredictedFact {
        return PredictedFact(
            text = "Predicted: shortfall risk is ${percent(bundle.projectedShortfallRisk)} over ${bundle.horizon}, with forecast balance centered near ${money(bundle.forecastP50)}.",
            range = NumberRange(bundle.forecastP10, bundle.forecastP50, bundle.forecastP90),
            confidenceLabel = bundle.confidenceLabel,
            horizon = bundle.horizon
        )
    }

    private fun recommended(intent: CopilotIntent, bundle: ResultBundle): RecommendedAction {
        val text = when (intent) {
            CopilotIntent.PLAN_COMPARISON ->
                "Recommended: choose plan ${bundle.recommendedPlanId} because it best balances payoff with verified shortfall risk."
            CopilotIntent.RISK_EXPLANATION ->
                "Recommended: keep the cushion protected first; only take higher-payoff actions when risk stays inside your limit."
            CopilotIntent.WHAT_IF_QUERY ->
                "Recommended: run the what-if simulator before committing, then compare the runway and risk deltas."
            CopilotIntent.GENERAL_INQUIRY ->
                "Recommended: ask about accounts, plan comparison, risk, or a what-if change for a more specific answer."
            CopilotIntent.ACCOUNT_OVERVIEW ->
                "Recommended: follow plan ${bundle.recommendedPlanId} and keep SIP separate from debt pressure."
        }
        return RecommendedAction(
            text = text,
            impact = RecommendationImpact(),
            tradeOff = "This answer uses deterministic local templates, so it is available offline."
        )
    }

    private fun observedRefs(intent: CopilotIntent): List<String> = when (intent) {
        CopilotIntent.ACCOUNT_OVERVIEW -> listOf("bankCash", "cardDues", "monthlyLoanEmis", "monthlySip", "avgDiscretionary")
        CopilotIntent.PLAN_COMPARISON -> listOf("recommendedPlanId", "savingsRate")
        CopilotIntent.RISK_EXPLANATION -> listOf("liquidRunwayMonths")
        CopilotIntent.WHAT_IF_QUERY -> listOf("bankCash", "monthlySip")
        CopilotIntent.GENERAL_INQUIRY -> emptyList()
    }

    private fun money(value: Double): String = "Rs. ${value.roundToInt()}"

    private fun percent(value: Double): String = "${oneDecimal(value * 100)}%"

    private fun oneDecimal(value: Double): String = String.format(Locale.US, "%.1f", value)
}
