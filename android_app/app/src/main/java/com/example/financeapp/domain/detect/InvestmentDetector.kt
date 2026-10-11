package com.example.financeapp.domain.detect

import com.example.financeapp.domain.model.Direction
import com.example.financeapp.domain.model.Investment
import com.example.financeapp.domain.model.Txn
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

object InvestmentDetector {
    private const val SIP_CV = 0.05

    /** Monthly investment debits with a tight amount. One-off investment debits are ignored. */
    fun detect(txns: List<Txn>): List<Investment> {
        val debits = txns.filter {
            it.direction == Direction.DEBIT && !it.isTransfer && it.category == "INVESTMENTS"
        }
        return debits.groupBy { it.counterpartyHash }.mapNotNull { (hash, rows) ->
            val sorted = rows.sortedBy { it.date }
            if (sorted.size < 3) return@mapNotNull null
            val gaps = sorted.zipWithNext { a, b -> ChronoUnit.DAYS.between(a.date, b.date).toInt() }
            val median = gaps.sorted()[gaps.size / 2]
            if (abs(median - 30) > 3) return@mapNotNull null
            val amounts = sorted.map { it.amount }
            val mean = amounts.average()
            val cv = if (mean == 0.0) 1.0 else sqrt(amounts.map { (it - mean) * (it - mean) }.average()) / mean
            if (cv > SIP_CV) return@mapNotNull null
            val regularity = gaps.count { abs(it - 30) <= 3 }.toDouble() / gaps.size
            val confidence = 0.5 * regularity + 0.3 * (1.0 - cv / SIP_CV).coerceIn(0.0, 1.0) +
                0.2 * min(1.0, sorted.size / 6.0)
            if (confidence < 0.6) return@mapNotNull null
            Investment(
                id = hash,
                platform = "SIP",
                monthlyAmount = mean,
                debitDay = sorted.map { it.date.dayOfMonth }.sorted()[sorted.size / 2],
                assetType = "MUTUAL_FUND",
                accountId = sorted.last().accountId,
                confidence = confidence,
            )
        }
    }
}
