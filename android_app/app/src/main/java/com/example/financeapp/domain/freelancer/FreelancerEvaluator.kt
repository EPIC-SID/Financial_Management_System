package com.example.financeapp.domain.freelancer

import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sqrt

enum class ForecastModel { BOOTSTRAP, HISTORICAL_AVERAGE, RECURRING_ONLY }
enum class ForecastTarget { NET_FLOW, ENDING_BALANCE }

data class ForecastCalibration(
    val target: ForecastTarget, val factor: Double, val sampleSize: Int, val halfWidthFloor: Double = 0.01
)

data class ForecastObservation(
    val model: ForecastModel, val target: ForecastTarget, val originMonth: YearMonth,
    val historyMonths: Int, val step: Int, val actual: Double,
    val rawBand: ForecastBand, val band: ForecastBand = rawBand
)

data class ForecastMetrics(
    val sampleCount: Int, val mae: Double, val rmse: Double, val coverage: Double,
    val meanWidth: Double, val pinballP10: Double, val pinballP50: Double, val pinballP90: Double
)

/** Step zero aggregates all held-out origin-step pairs; overlapping pairs are not independent. */
data class ForecastMetricGroup(
    val model: ForecastModel, val target: ForecastTarget, val step: Int, val metrics: ForecastMetrics
)

data class FreelancerEvaluationReport(
    val seed: Int,
    val calibration: List<ForecastCalibration>,
    val validationPredictions: List<ForecastObservation>,
    val predictions: List<ForecastObservation>,
    val metrics: List<ForecastMetricGroup>
)

class FreelancerEvaluator {
    /**
     * A fixed 30-month replay: initial fit 1-18, interval calibration 19-24, test 25-30.
     * Each rolling origin refits using only already observed months. Targets are net bank cash
     * flow and cumulative bank cash, including actual refunds. No future commitments/shocks
     * are exposed to predictors; last observed fixed obligations are persisted instead.
     * recurringIncomePaise is an externally declared recurring receipt, NOT detected salary.
     */
    fun evaluate(
        months: List<FinancialHistoryMonth>,
        initialCashPaise: Long,
        recurringIncomePaise: Long,
        seed: Int
    ): FreelancerEvaluationReport {
        require(months.size == 30)
        require(initialCashPaise in 0..MAX_FORECAST_PAISE && recurringIncomePaise in 0..MAX_FORECAST_PAISE)
        require(months.zipWithNext().all { ChronoUnit.MONTHS.between(it.first.month, it.second.month) == 1L })
        require(months.all { month -> listOf(month.incomePaise, month.cashEssentialsPaise,
            month.cashDebtServicePaise, month.cashSipPaise, month.cashDiscretionaryPaise,
            month.cashRefundsPaise).all { it in 0..MAX_FORECAST_PAISE } })
        val openingCash = ArrayList<Long>(31).apply { add(initialCashPaise) }
        months.forEach { openingCash.add(Math.addExact(openingCash.last(), netPaise(it))) }
        require((18..29).all { openingCash[it] in 0..MAX_FORECAST_PAISE }) {
            "Replay requires supported nonnegative cash anchors at forecast origins"
        }
        val validation = (18 until 24).flatMap { origin ->
            predict(months, openingCash[origin], recurringIncomePaise, origin, 24, seed)
        }
        // shortcut: pooled overlapping validation pairs; stratify with more independent history.
        val calibration = ForecastTarget.values().map { target ->
            val observations = validation.filter { it.model == ForecastModel.BOOTSTRAP && it.target == target }
            val scores = observations.map {
                abs(it.actual - it.rawBand.p50) / halfWidth(it.rawBand)
            }.sorted()
            val rank = ceil((scores.size + 1) * 0.8).toInt()
            ForecastCalibration(target, maxOf(1.0, scores[rank - 1]), scores.size)
        }
        val predictions = (24 until 30).flatMap { origin ->
            predict(months, openingCash[origin], recurringIncomePaise, origin, 30, seed)
        }.map { observation ->
            if (observation.model != ForecastModel.BOOTSTRAP) observation else {
                val factor = calibration.single { it.target == observation.target }.factor
                val raw = observation.rawBand
                val width = factor * halfWidth(raw)
                // Preserve asymmetric raw bounds as well as the median; never shrink an interval.
                observation.copy(band = ForecastBand(minOf(raw.p10, raw.p50 - width), raw.p50,
                    maxOf(raw.p90, raw.p50 + width)))
            }
        }
        val metrics = ForecastModel.values().flatMap { model -> ForecastTarget.values().flatMap { target ->
            (0..6).map { step ->
                val rows = predictions.filter { it.model == model && it.target == target && (step == 0 || it.step == step) }
                ForecastMetricGroup(model, target, step, summarize(rows))
            }
        } }
        return FreelancerEvaluationReport(seed, calibration, validation, predictions, metrics)
    }

    private fun netPaise(m: FinancialHistoryMonth) = m.incomePaise + m.cashRefundsPaise -
        m.cashEssentialsPaise - m.cashDebtServicePaise - m.cashSipPaise - m.cashDiscretionaryPaise

    private fun halfWidth(band: ForecastBand) = maxOf(0.01, (band.p90 - band.p10) / 2.0)

    private fun predict(
        months: List<FinancialHistoryMonth>, cashPaise: Long, recurringPaise: Long,
        origin: Int, endExclusive: Int, seed: Int
    ): List<ForecastObservation> {
        val prior = months.take(origin)
        val originMonth = months[origin].month
        val asOf = originMonth.atDay(1).atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant()
        val result = FreelancerForecaster().forecast(NormalizedFinancialHistory(prior, CashAnchor(cashPaise, asOf)), seed)
        require(result is FreelancerForecast.Ready) { "Forecast history is unavailable" }
        val last = prior.last()
        // shortcut: persist observed obligations; use declared schedules at ledger integration.
        val fixed = MonthlyCommitments(last.cashEssentialsPaise, last.cashDebtServicePaise, last.cashSipPaise)
        val inputs = ForecastPathAdapter().adapt(result.forecast, List(6) { fixed }, 0)
        val sampledFlows = inputs.sampledPaths.map { path -> path.map {
            it.income - it.essentials - it.debtService - it.sip - it.discretionary
        } }
        val sampledBalances = sampledFlows.map { path ->
            var running = inputs.cash
            path.map { flow -> running += flow; running }
        }
        val historicalFlows = prior.map { netPaise(it) / 100.0 }
        val mean = historicalFlows.average()
        val std = sqrt(historicalFlows.sumOf { (it - mean) * (it - mean) } / (prior.size - 1))
        val recurringFlow = (recurringPaise - fixed.essentialsPaise - fixed.debtServicePaise - fixed.sipPaise) / 100.0
        var actualBalance = inputs.cash
        return (0 until minOf(6, endExclusive - origin)).flatMap { index ->
            val actualFlow = netPaise(months[origin + index]) / 100.0
            actualBalance += actualFlow
            val step = index + 1
            ForecastTarget.values().flatMap { target ->
                val flowTarget = target == ForecastTarget.NET_FLOW
                val actual = if (flowTarget) actualFlow else actualBalance
                val center = if (flowTarget) mean else inputs.cash + step * mean
                val width = 1.28155 * std * if (flowTarget) 1.0 else sqrt(step.toDouble())
                val recurring = if (flowTarget) recurringFlow else inputs.cash + step * recurringFlow
                val bands = listOf(
                    ForecastModel.BOOTSTRAP to empiricalBand((if (flowTarget) sampledFlows else sampledBalances).map { it[index] }),
                    ForecastModel.HISTORICAL_AVERAGE to ForecastBand(center - width, center, center + width),
                    ForecastModel.RECURRING_ONLY to ForecastBand(recurring, recurring, recurring)
                )
                bands.map { (model, band) -> ForecastObservation(model, target, originMonth, origin, step, actual, band) }
            }
        }
    }

    private fun summarize(rows: List<ForecastObservation>): ForecastMetrics {
        require(rows.isNotEmpty())
        val errors = rows.map { it.actual - it.band.p50 }
        fun pinball(q: Double, prediction: (ForecastBand) -> Double) = rows.map {
            val error = it.actual - prediction(it.band)
            maxOf(q * error, (q - 1.0) * error)
        }.average()
        return ForecastMetrics(rows.size, errors.map { abs(it) }.average(),
            sqrt(errors.map { it * it }.average()),
            rows.count { it.actual in it.band.p10..it.band.p90 }.toDouble() / rows.size,
            rows.map { it.band.p90 - it.band.p10 }.average(),
            pinball(0.1) { it.p10 }, pinball(0.5) { it.p50 }, pinball(0.9) { it.p90 })
    }
}
