package com.example.financeapp.domain.model

enum class CopilotIntent {
    ACCOUNT_OVERVIEW,
    PLAN_COMPARISON,
    RISK_EXPLANATION,
    WHAT_IF_QUERY,
    GENERAL_INQUIRY
}

enum class ConfidenceLabel {
    High,
    Medium,
    Low
}

enum class ResponseSource {
    LOCAL,
    REMOTE
}

data class ResultBundle(
    val liquidRunwayMonths: Double,
    val projectedShortfallRisk: Double,
    val recommendedPlanId: String,
    val savingsRate: Double,
    val bankCash: Double,
    val cardDues: Double,
    val monthlyLoanEmis: Double,
    val monthlySip: Double,
    val avgDiscretionary: Double,
    val forecastP10: Double,
    val forecastP50: Double,
    val forecastP90: Double,
    val horizon: String,
    val confidenceLabel: ConfidenceLabel
)

data class PredictionRange(
    val p10: Double,
    val p50: Double,
    val p90: Double
)
typealias NumberRange = PredictionRange

data class ActionImpact(
    val runwayDeltaMonths: Double = 0.0,
    val riskDelta: Double = 0.0,
    val interestSaved: Double = 0.0
)
typealias RecommendationImpact = ActionImpact

data class ObservedItem(
    val text: String,
    val refs: List<String>
)
typealias ObservedFact = ObservedItem

data class PredictedItem(
    val text: String,
    val range: PredictionRange,
    val confidenceLabel: ConfidenceLabel,
    val horizon: String
)
typealias PredictedFact = PredictedItem

data class RecommendedItem(
    val text: String,
    val impact: ActionImpact,
    val tradeOff: String
)
typealias RecommendedAction = RecommendedItem

/**
 * Standard Copilot StructuredResponse schema (06-backend.md §13.3).
 * Compatible with 3-arg call (Snehal LocalizedAdvisor) and 5-arg call (Track 4 Orchestrator).
 */
data class StructuredResponse(
    val observed: List<ObservedItem>,
    val predicted: List<PredictedItem>,
    val recommended: List<RecommendedItem>,
    val intent: CopilotIntent = CopilotIntent.GENERAL_INQUIRY,
    val source: ResponseSource = ResponseSource.LOCAL
)
