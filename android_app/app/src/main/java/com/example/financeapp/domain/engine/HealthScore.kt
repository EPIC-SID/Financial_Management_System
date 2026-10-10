package com.example.financeapp.domain.engine

import java.util.Locale
import kotlin.math.roundToInt

data class HealthScoreInputs(
    val cash: Double?,
    val monthlyObligations: Double?,
    val monthlyDebtService: Double?,
    val monthlyIncome: Double?,
    val minimumReserve: Double?,
    val netCashFlowStdDev: Double?,
    val meanIncome: Double?
)

data class HealthScoreInput(
    val label: String,
    val value: Double?
)

enum class HealthScoreBand { RISK, WATCH, SAFE }

enum class HealthScoreComponentKey { CASH_RUNWAY, DEBT_LOAD, BUFFER, STABILITY }

data class HealthScoreComponent(
    val key: HealthScoreComponentKey,
    val label: String,
    val score: Double,
    val inputs: List<HealthScoreInput>,
    val reading: String,
    val whatWouldRaiseThis: String
)

data class HealthScoreResult(
    val score: Int,
    val band: HealthScoreBand,
    val isKnown: Boolean,
    val components: List<HealthScoreComponent>
)

object HealthScore {
    fun calculate(inputs: HealthScoreInputs): HealthScoreResult {
        if (!inputs.areUsable()) return unknown(inputs)

        val cash = inputs.cash!!
        val obligations = inputs.monthlyObligations!!
        val debtService = inputs.monthlyDebtService!!
        val income = inputs.monthlyIncome!!
        val reserve = inputs.minimumReserve!!
        val variability = inputs.netCashFlowStdDev!!
        val meanIncome = inputs.meanIncome!!

        val runwayMonths = cash / obligations
        val debtToIncome = debtService / income
        val reserveCoverage = cash / reserve
        val variabilityRatio = variability / meanIncome
        val components = listOf(
            HealthScoreComponent(
                HealthScoreComponentKey.CASH_RUNWAY,
                "Cash runway",
                25.0 * clamp(runwayMonths / 6.0),
                listOf(HealthScoreInput("Cash", cash), HealthScoreInput("Monthly obligations", obligations)),
                "Cash covers ${oneDecimal(runwayMonths)} months of essentials and debt service.",
                "More cash or lower required monthly obligations would raise this."
            ),
            HealthScoreComponent(
                HealthScoreComponentKey.DEBT_LOAD,
                "Debt load",
                25.0 * (1.0 - clamp(debtToIncome / 0.5)),
                listOf(HealthScoreInput("Monthly debt service", debtService), HealthScoreInput("Monthly income", income)),
                "Debt service uses ${percent(debtToIncome)} of monthly income.",
                "Lower required debt payments or higher income would raise this."
            ),
            HealthScoreComponent(
                HealthScoreComponentKey.BUFFER,
                "Buffer",
                25.0 * clamp(reserveCoverage),
                listOf(HealthScoreInput("Cash", cash), HealthScoreInput("Minimum reserve", reserve)),
                "Cash covers ${percent(reserveCoverage)} of the declared cushion.",
                "Building cash toward the declared cushion would raise this."
            ),
            HealthScoreComponent(
                HealthScoreComponentKey.STABILITY,
                "Stability",
                25.0 * (1.0 - clamp(variabilityRatio / 0.3)),
                listOf(HealthScoreInput("Net cash-flow variability", variability), HealthScoreInput("Mean income", meanIncome)),
                "Net cash-flow variability is ${percent(variabilityRatio)} of mean income.",
                "More stable income or steadier monthly spending would raise this."
            )
        )
        val score = components.sumOf { it.score }.roundToInt().coerceIn(0, 100)
        return HealthScoreResult(score, bandFor(score), true, components)
    }

    fun bandFor(score: Int): HealthScoreBand = when {
        score < 50 -> HealthScoreBand.RISK
        score <= 75 -> HealthScoreBand.WATCH
        else -> HealthScoreBand.SAFE
    }

    private fun HealthScoreInputs.areUsable(): Boolean {
        val values = listOf(cash, monthlyObligations, monthlyDebtService, monthlyIncome, minimumReserve, netCashFlowStdDev, meanIncome)
        if (values.any { it == null || !it.isFinite() }) return false
        return cash!! >= 0.0 && monthlyDebtService!! >= 0.0 && netCashFlowStdDev!! >= 0.0 &&
            monthlyObligations!! > 0.0 && monthlyIncome!! > 0.0 && minimumReserve!! > 0.0 && meanIncome!! > 0.0
    }

    private fun unknown(inputs: HealthScoreInputs): HealthScoreResult = HealthScoreResult(
        score = 0,
        band = HealthScoreBand.RISK,
        isKnown = false,
        components = listOf(
            unavailable(HealthScoreComponentKey.CASH_RUNWAY, "Cash runway", "Cash" to inputs.cash, "Monthly obligations" to inputs.monthlyObligations),
            unavailable(HealthScoreComponentKey.DEBT_LOAD, "Debt load", "Monthly debt service" to inputs.monthlyDebtService, "Monthly income" to inputs.monthlyIncome),
            unavailable(HealthScoreComponentKey.BUFFER, "Buffer", "Cash" to inputs.cash, "Minimum reserve" to inputs.minimumReserve),
            unavailable(HealthScoreComponentKey.STABILITY, "Stability", "Net cash-flow variability" to inputs.netCashFlowStdDev, "Mean income" to inputs.meanIncome)
        )
    )

    private fun unavailable(
        key: HealthScoreComponentKey,
        label: String,
        vararg inputs: Pair<String, Double?>
    ) = HealthScoreComponent(
        key = key,
        label = label,
        score = 0.0,
        inputs = inputs.map { HealthScoreInput(it.first, it.second) },
        reading = "Not enough valid data to calculate this component.",
        whatWouldRaiseThis = "Add the missing financial inputs to calculate this component."
    )

    private fun clamp(value: Double) = value.coerceIn(0.0, 1.0)
    private fun oneDecimal(value: Double) = String.format(Locale.ROOT, "%.1f", value)
    private fun percent(value: Double) = String.format(Locale.ROOT, "%.0f%%", value * 100.0)
}
