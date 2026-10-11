package com.example.financeapp.domain.model

data class ProbabilisticForecast(
    val periodIndex: Int,
    /** "income", "expenses", "balance" */
    val metric: String,
    val p10: Double,
    val p50: Double,
    val p90: Double,
    val method: String,
    val horizon: String,
    /** 0.80 for [p10, p90]. */
    val intervalLevel: Double,
)
