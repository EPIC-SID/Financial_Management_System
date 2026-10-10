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

data class NumberRange(
    val p10: Double,
    val p50: Double,
    val p90: Double
)

data class RecommendationImpact(
    val runwayDeltaMonths: Double = 0.0,
    val riskDelta: Double = 0.0,
    val interestSaved: Double = 0.0
)

data class ObservedFact(
    val text: String,
    val refs: List<String>
)

data class PredictedFact(
    val text: String,
    val range: NumberRange,
    val confidenceLabel: ConfidenceLabel,
    val horizon: String
)

data class RecommendedAction(
    val text: String,
    val impact: RecommendationImpact,
    val tradeOff: String
)

data class StructuredResponse(
    val observed: List<ObservedFact>,
    val predicted: List<PredictedFact>,
    val recommended: List<RecommendedAction>,
    val intent: CopilotIntent,
    val source: ResponseSource
)
