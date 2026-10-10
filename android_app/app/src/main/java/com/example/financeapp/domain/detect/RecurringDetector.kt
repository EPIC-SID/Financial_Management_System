package com.example.financeapp.domain.detect

import com.example.financeapp.domain.model.Direction
import com.example.financeapp.domain.model.RecurringItem
import com.example.financeapp.domain.model.Txn
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

object RecurringDetector {
    private val SNAPS = listOf(7, 14, 30, 90, 365)
    private const val MIN_HITS = 3
    private const val MIN_CONFIDENCE = 0.6

    /** Confirmed items only (confidence >= 0.6). INVESTMENTS are left to [InvestmentDetector]. */
    fun detect(txns: List<Txn>): List<RecurringItem> =
        candidates(txns).filter { it.confidence >= MIN_CONFIDENCE }

    fun candidates(txns: List<Txn>): List<RecurringItem> {
        val debits = txns.filter { it.direction == Direction.DEBIT && it.category != "INVESTMENTS" && keep(it) }
        val groups = debits.groupBy { key(it) }
        return groups.mapNotNull { (_, rows) -> score(rows.sortedBy { it.date }) }
    }

    private fun keep(t: Txn) = !t.isTransfer || t.category == "CARD_BILL"

    private fun key(t: Txn) = if (t.category == "UTILITIES") "UTILITIES" else "${t.category}|${t.counterpartyHash}"

    private fun score(rows: List<Txn>): RecurringItem? {
        if (rows.size < MIN_HITS) return null
        val gaps = rows.zipWithNext { a, b -> ChronoUnit.DAYS.between(a.date, b.date).toInt() }
        if (gaps.any { it <= 0 }) return null
        val median = gaps.sorted()[gaps.size / 2]
        val interval = SNAPS.firstOrNull { abs(median - it) <= 3 } ?: return null
        val amounts = rows.map { it.amount }
        val mean = amounts.average()
        val limit = if (rows.first().category == "UTILITIES" || rows.first().category == "CARD_BILL") 0.5 else 0.15
        val cv = cv(amounts, mean)
        if (cv > limit) return null
        val regularity = gaps.count { abs(it - interval) <= 3 }.toDouble() / gaps.size
        val amountStability = (1.0 - cv / limit).coerceIn(0.0, 1.0)
        val confidence = 0.5 * regularity + 0.3 * amountStability + 0.2 * min(1.0, rows.size / 6.0)
        val last = rows.last()
        return RecurringItem(
            id = key(last),
            label = last.category,
            amount = mean,
            intervalDays = interval,
            nextDue = nextDue(last.date, interval),
            confidence = confidence,
            kind = kind(last.category),
        )
    }

    private fun nextDue(last: LocalDate, interval: Int): LocalDate =
        if (interval == 30) last.plusMonths(1) else last.plusDays(interval.toLong())

    private fun kind(category: String) = when (category) {
        "RENT" -> "RENT"
        "EMI" -> "EMI"
        "UTILITIES" -> "UTILITY"
        "CARD_BILL" -> "CARD_BILL"
        "INSURANCE" -> "INSURANCE"
        else -> "OTHER"
    }

    internal fun cv(amounts: List<Double>, mean: Double = amounts.average()): Double {
        if (mean == 0.0) return 1.0
        val variance = amounts.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance) / mean
    }
}
