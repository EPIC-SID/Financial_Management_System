package com.example.financeapp.domain.model

/** Response schema from 06-backend.md section 13.3. ResultBundle is proposed by Track 4 and added here on agreement. */
data class StructuredResponse(
    val observed: List<ObservedItem>,
    val predicted: List<PredictedItem>,
    val recommended: List<RecommendedItem>,
)

data class ObservedItem(val text: String, val refs: List<String>)

enum class ConfidenceLabel { High, Medium, Low }

data class PredictionRange(val p10: Double, val p50: Double, val p90: Double)

data class PredictedItem(
    val text: String,
    val range: PredictionRange,
    val confidenceLabel: ConfidenceLabel,
    val horizon: String,
)

data class ActionImpact(val runwayDeltaMonths: Double, val riskDelta: Double, val interestSaved: Double)

data class RecommendedItem(val text: String, val impact: ActionImpact, val tradeOff: String)
