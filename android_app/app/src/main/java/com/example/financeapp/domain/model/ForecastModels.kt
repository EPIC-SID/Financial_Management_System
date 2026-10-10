package com.example.financeapp.domain.model

data class ProbabilisticForecast(
    val periodIndex: Int,
    val metric: String,
    val p10: Double,
    val p50: Double,
    val p90: Double,
    val distributionType: String = "Empirical Historical Bootstrap",
    val horizon: String = "6-Month",
)
