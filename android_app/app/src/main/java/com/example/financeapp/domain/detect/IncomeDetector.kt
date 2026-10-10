package com.example.financeapp.domain.detect

import com.example.financeapp.domain.model.Direction
import com.example.financeapp.domain.model.Txn
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.sqrt

data class DetectedIncome(
    val counterpartyHash: String,
    val typicalAmount: Double,
    val payDay: Int,
    /** FIXED when salary-like, otherwise VARIABLE. */
    val incomeType: String,
    val mean: Double,
    val stdDev: Double,
    val shareOfMonthsWithZero: Double,
)

object IncomeDetector {
    private const val MIN_CREDIT = 2_000.0

    fun detect(txns: List<Txn>): List<DetectedIncome> {
        val credits = txns.filter {
            it.direction == Direction.CREDIT && !it.isTransfer && it.amount >= MIN_CREDIT &&
                it.category != "Rewards" && it.category != "INVESTMENTS"
        }
        if (credits.isEmpty()) return emptyList()
        val months = credits.map { it.date.withDayOfMonth(1) }.toSortedSet()
        val span = ChronoUnit.MONTHS.between(months.first(), months.last()).toInt() + 1
        return credits.groupBy { it.counterpartyHash }.map { (hash, rows) ->
            val sorted = rows.sortedBy { it.date }
            val amounts = sorted.map { it.amount }
            val mean = amounts.average()
            val sd = sqrt(amounts.map { (it - mean) * (it - mean) }.average())
            val cv = if (mean == 0.0) 1.0 else sd / mean
            val gaps = sorted.zipWithNext { a, b -> ChronoUnit.DAYS.between(a.date, b.date) }
            val monthly = sorted.size >= 3 && gaps.all { abs(it - 30) <= 5 } && cv <= 0.1
            val monthsWithPay = sorted.map { it.date.withDayOfMonth(1) }.toSet().size
            DetectedIncome(
                counterpartyHash = hash,
                typicalAmount = mean,
                payDay = sorted.map { it.date.dayOfMonth }.sorted()[sorted.size / 2],
                incomeType = if (monthly) "FIXED" else "VARIABLE",
                mean = mean,
                stdDev = sd,
                shareOfMonthsWithZero = if (span <= 0) 0.0 else 1.0 - monthsWithPay.toDouble() / span,
            )
        }
    }
}
