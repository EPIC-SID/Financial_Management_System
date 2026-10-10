package com.example.financeapp.domain.engine

import com.example.financeapp.domain.model.FinancialSnapshot
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class HealthScoreBreakdown(
    val total: Int,
    val runway: Double,
    val debtLoad: Double,
    val buffer: Double,
    val stability: Double,
    val dti: Double,
    val monthlyObligations: Double,
)

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
    /** Snapshot-based scoring used by FinancialHealthRepository (06-backend.md §7.3). */
    fun of(snapshot: FinancialSnapshot): HealthScoreBreakdown {
        val last = snapshot.history.lastOrNull()
        val obligations = (last?.essentials ?: 0.0) + (last?.debtService ?: 0.0)
        val income = snapshot.declared.typicalIncome
            ?: snapshot.history.map { it.income }.filter { it > 0 }.average().takeIf { !it.isNaN() }
            ?: 0.0
        val emis = snapshot.liabilities.filter { it.kind != "CARD" }.sumOf { it.emi ?: 0.0 }
        val cardMins = snapshot.liabilities.filter { it.kind == "CARD" }.sumOf { it.minDue ?: 0.0 }
        val dti = if (income <= 0) 1.0 else (emis + cardMins) / income
        val nets = snapshot.history.map { it.income - it.essentials - it.debtService - it.discretionary - it.investments }
        val meanNet = if (nets.isEmpty()) 0.0 else nets.average()
        val sigma = if (nets.size < 2) 0.0 else {
            val v = nets.map { (it - meanNet) * (it - meanNet) }.average()
            sqrt(v)
        }
        val runway = 25.0 * clamp((if (obligations <= 0) 1.0 else snapshot.cash / obligations) / 6.0)
        val debtLoad = 25.0 * (1.0 - clamp(dti / 0.5))
        val buffer = 25.0 * clamp(if (snapshot.declared.minReserve <= 0) 1.0 else snapshot.cash / snapshot.declared.minReserve)
        val stability = 25.0 * (1.0 - clamp((if (income <= 0) 1.0 else sigma / income) / 0.3))
        return HealthScoreBreakdown(
            total = (runway + debtLoad + buffer + stability).toInt().coerceIn(0, 100),
            runway = runway, debtLoad = debtLoad, buffer = buffer, stability = stability,
            dti = dti, monthlyObligations = obligations,
        )
    }

    /** Input-based detailed component scoring used by CockpitViewModel & UI sheets. */
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
