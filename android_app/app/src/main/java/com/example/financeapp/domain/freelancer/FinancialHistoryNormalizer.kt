package com.example.financeapp.domain.freelancer

import java.time.DateTimeException
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale

class FinancialHistoryNormalizer(private val zone: ZoneId = ZoneId.of("Asia/Kolkata")) {
    /**
     * historyStart declares complete coverage, including months with no receipts. Only completed
     * months are returned. The caller must classify events and supply a cash anchor at asOf.
     * No declared salary is added to received income, and the anchor is never debited again.
     * Identity is the upstream economic-event id: equal amounts alone are not duplicates.
     */
    fun normalize(
        events: List<FinancialEvent>,
        historyStart: YearMonth,
        asOf: Instant,
        cashAnchor: CashAnchor?
    ): HistoryNormalization {
        if (events.size > 100_000) return unavailable(HistoryIssue.TOO_MANY_EVENTS)
        val currentMonth = try {
            YearMonth.from(asOf.atZone(zone))
        } catch (_: DateTimeException) {
            return unavailable(HistoryIssue.INVALID_HISTORY_WINDOW)
        }
        val monthCount = ChronoUnit.MONTHS.between(historyStart, currentMonth)
        if (monthCount !in 1..120) return unavailable(HistoryIssue.INVALID_HISTORY_WINDOW)
        val issues = linkedSetOf<HistoryIssue>()
        if (cashAnchor == null) issues.add(HistoryIssue.MISSING_CASH_ANCHOR)
        else if (cashAnchor.amountPaise < 0 || cashAnchor.asOf != asOf) {
            issues.add(HistoryIssue.INVALID_CASH_ANCHOR)
        }
        val start = historyStart.atDay(1).atStartOfDay(zone).toInstant()
        val seen = HashMap<String, FinancialEvent>()
        val eligible = ArrayList<FinancialEvent>()
        for (event in events) {
            if (event.occurredAt < start) continue
            if (event.occurredAt > asOf) {
                issues.add(HistoryIssue.FUTURE_EVENT)
                continue
            }
            if (event.id.isBlank() || event.id.length > 128 || event.amountPaise <= 0 ||
                event.category.length > 64 || event.funding == FundingSource.UNKNOWN ||
                (event.funding == FundingSource.CREDIT_CARD && event.kind != FinancialEventKind.EXPENSE &&
                    event.kind != FinancialEventKind.REFUND && event.kind != FinancialEventKind.UNKNOWN)) {
                issues.add(HistoryIssue.INVALID_EVENT)
            }
            if (event.kind == FinancialEventKind.UNKNOWN) issues.add(HistoryIssue.UNKNOWN_EVENT_KIND)
            if (event.kind == FinancialEventKind.EXPENSE && event.expenseClass == ExpenseClass.UNKNOWN) {
                issues.add(HistoryIssue.UNKNOWN_EXPENSE_CLASS)
            }
            val previous = seen.putIfAbsent(event.id, event)
            if (previous != null && previous != event) issues.add(HistoryIssue.CONFLICTING_DUPLICATE)
            if (previous == null && YearMonth.from(event.occurredAt.atZone(zone)) < currentMonth) {
                eligible.add(event)
            }
        }
        if (issues.isNotEmpty()) return HistoryNormalization.Unavailable(issues.toSet())
        val months = MutableList(monthCount.toInt()) { FinancialHistoryMonth(historyStart.plusMonths(it.toLong())) }
        try {
            for (event in eligible) {
                val index = ChronoUnit.MONTHS.between(historyStart, YearMonth.from(event.occurredAt.atZone(zone))).toInt()
                months[index] = add(months[index], event)
            }
        } catch (_: ArithmeticException) {
            return unavailable(HistoryIssue.AMOUNT_OVERFLOW)
        }
        return HistoryNormalization.Ready(NormalizedFinancialHistory(months.toList(), cashAnchor!!))
    }

    private fun add(month: FinancialHistoryMonth, event: FinancialEvent): FinancialHistoryMonth {
        val amount = event.amountPaise
        fun plus(value: Long) = Math.addExact(value, amount)
        return when (event.kind) {
            FinancialEventKind.INCOME -> month.copy(incomePaise = plus(month.incomePaise))
            FinancialEventKind.EXPENSE -> {
                val category = canonicalCategory(event.category)
                val categories = month.consumptionByCategoryPaise +
                    (category to plus(month.consumptionByCategoryPaise[category] ?: 0))
                val bank = event.funding == FundingSource.BANK
                if (event.expenseClass == ExpenseClass.ESSENTIAL) month.copy(
                    essentialConsumptionPaise = plus(month.essentialConsumptionPaise),
                    cashEssentialsPaise = if (bank) plus(month.cashEssentialsPaise) else month.cashEssentialsPaise,
                    consumptionByCategoryPaise = categories
                ) else month.copy(
                    discretionaryConsumptionPaise = plus(month.discretionaryConsumptionPaise),
                    cashDiscretionaryPaise = if (bank) plus(month.cashDiscretionaryPaise) else month.cashDiscretionaryPaise,
                    consumptionByCategoryPaise = categories
                )
            }
            FinancialEventKind.DEBT_PAYMENT, FinancialEventKind.CARD_SETTLEMENT ->
                month.copy(cashDebtServicePaise = plus(month.cashDebtServicePaise))
            FinancialEventKind.SIP -> month.copy(cashSipPaise = plus(month.cashSipPaise))
            FinancialEventKind.REFUND -> month.copy(
                refundsPaise = plus(month.refundsPaise),
                cashRefundsPaise = if (event.funding == FundingSource.BANK) plus(month.cashRefundsPaise)
                    else month.cashRefundsPaise
            )
            FinancialEventKind.INTERNAL_TRANSFER -> month
            FinancialEventKind.UNKNOWN -> error("Unknown events must be rejected at the boundary")
        }
    }

    private fun canonicalCategory(value: String): String = when (val category = value.trim().uppercase(Locale.ROOT)) {
        "MEDICAL", "HEALTHCARE", "HEALTH" -> "HEALTH"
        "DINING", "GROCERIES", "FOOD" -> "FOOD"
        "TRANSPORTATION", "TRAVEL", "TRANSPORT" -> "TRAVEL"
        "MISC", "OTHER", "OTHERS", "MISCELLANEOUS" -> "MISCELLANEOUS"
        "SHOPPING", "ENTERTAINMENT", "BILLS", "RENT", "EDUCATION", "INVESTMENTS" -> category
        else -> "MISCELLANEOUS"
    }

    private fun unavailable(issue: HistoryIssue) = HistoryNormalization.Unavailable(setOf(issue))
}
