package com.example.financeapp.domain.freelancer

import java.time.DateTimeException
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.roundToLong
import kotlin.random.Random

internal const val MAX_FORECAST_PAISE = 100_000_000_000_000L

/** Delays ALL sampled receipts in one rolling period; not an individual invoice prediction. */
data class OneMonthReceiptDelay(val period: Int) {
    init {
        // A last-period delay needs an explicit beyond-horizon receivable contract first.
        require(period in 1..5) { "Delay must arrive within the six-period horizon" }
    }
}

data class ForecastComponents(val incomePaise: Long, val discretionaryPaise: Long)

data class FreelancerProjection internal constructor(
    val cashAnchor: CashAnchor,
    val periodBoundaries: List<Instant>,
    val sampledPaths: List<List<ForecastComponents>>,
    val p10: List<ForecastComponents>,
    val p50: List<ForecastComponents>,
    val p90: List<ForecastComponents>,
    val historyMonths: Int,
    val seed: Int,
    val delay: OneMonthReceiptDelay?,
    val method: String,
    val limitations: List<String>
)

enum class ForecastIssue { INSUFFICIENT_HISTORY, INVALID_HISTORY }

sealed class FreelancerForecast {
    data class Ready(val forecast: FreelancerProjection) : FreelancerForecast()
    data class Unavailable(val issue: ForecastIssue) : FreelancerForecast()
}

class FreelancerForecaster(private val zone: ZoneId = ZoneId.of("Asia/Kolkata")) {
    /**
     * Six rolling months beginning at the cash anchor (not six calendar statements).
     * Only consecutive completed calendar months ending before the anchor month train the model.
     * Income uses empirical two-month blocks; cash discretionary spend uses EWMA(0.4) plus
     * causal one-step residuals sampled at the same indices. Quantiles use nearest ranks in paise.
     */
    fun forecast(
        history: NormalizedFinancialHistory,
        seed: Int,
        delay: OneMonthReceiptDelay? = null
    ): FreelancerForecast {
        val months = history.months
        if (months.size > 120 || history.cashAnchor.amountPaise !in 0..MAX_FORECAST_PAISE) {
            return unavailable(ForecastIssue.INVALID_HISTORY)
        }
        val start = try {
            history.cashAnchor.asOf.atZone(zone)
        } catch (_: DateTimeException) {
            return unavailable(ForecastIssue.INVALID_HISTORY)
        }
        val boundaries = try {
            List(7) { start.plusMonths(it.toLong()).toInstant() }
        } catch (_: DateTimeException) {
            return unavailable(ForecastIssue.INVALID_HISTORY)
        }
        val currentMonth = YearMonth.from(start)
        if (months.any { it.incomePaise !in 0..MAX_FORECAST_PAISE ||
                    it.cashDiscretionaryPaise !in 0..MAX_FORECAST_PAISE } ||
            months.zipWithNext().any { ChronoUnit.MONTHS.between(it.first.month, it.second.month) != 1L } ||
            (months.isNotEmpty() && ChronoUnit.MONTHS.between(months.last().month, currentMonth) != 1L)) {
            return unavailable(ForecastIssue.INVALID_HISTORY)
        }
        if (months.size < 6) return unavailable(ForecastIssue.INSUFFICIENT_HISTORY)

        var spendingLevel = months.first().cashDiscretionaryPaise.toDouble()
        val residuals = months.mapIndexed { i, month ->
            val spend = month.cashDiscretionaryPaise.toDouble()
            val residual = if (i == 0) 0.0 else spend - spendingLevel
            spendingLevel = 0.4 * spend + 0.6 * spendingLevel
            residual
        }
        val random = Random(seed)
        val paths = List(2_000) {
            val path = ArrayList<ForecastComponents>(6)
            repeat(3) {
                val blockStart = random.nextInt(months.size - 1)
                repeat(2) { offset ->
                    val index = blockStart + offset
                    path.add(ForecastComponents(months[index].incomePaise,
                        (spendingLevel + residuals[index]).coerceAtLeast(0.0).roundToLong()))
                }
            }
            if (delay != null) {
                val from = delay.period - 1
                val receipt = path[from].incomePaise
                path[from] = path[from].copy(incomePaise = 0)
                path[from + 1] = path[from + 1].copy(incomePaise =
                    Math.addExact(path[from + 1].incomePaise, receipt))
            }
            path.toList()
        }
        val incomes = List(6) { i -> paths.map { it[i].incomePaise }.sorted() }
        val spending = List(6) { i -> paths.map { it[i].discretionaryPaise }.sorted() }
        fun quantiles(q: Double) = List(6) { i ->
            val rank = ceil(q * paths.size).toInt() - 1
            ForecastComponents(incomes[i][rank], spending[i][rank])
        }
        return FreelancerForecast.Ready(FreelancerProjection(
            history.cashAnchor, boundaries, paths, quantiles(0.1), quantiles(0.5), quantiles(0.9),
            months.size, seed, delay,
            "Empirical income block bootstrap (length 2), EWMA(0.4) cash-spend residuals, " +
                "2,000 seeded paths, six rolling monthly periods; uncalibrated P10/P50/P90",
            listOf(
                "Raw intervals have no demonstrated 80% coverage until held-out calibration.",
                "Rolling monthly cash flows do not resolve intramonth due-date shortfalls or seasonality.",
                "Component P50 values are not the median terminal bank balance.",
                "Declared obligations must cover these rolling periods, including future card settlements.",
                "No future refunds or new income sources are assumed; delay shifts all period receipts."
            )
        ))
    }

    private fun unavailable(issue: ForecastIssue) = FreelancerForecast.Unavailable(issue)
}
