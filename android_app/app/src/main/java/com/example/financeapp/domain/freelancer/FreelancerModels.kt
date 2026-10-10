package com.example.financeapp.domain.freelancer

import java.time.Instant
import java.time.YearMonth

enum class FinancialEventKind {
    INCOME, EXPENSE, DEBT_PAYMENT, SIP, INTERNAL_TRANSFER, CARD_SETTLEMENT, REFUND, UNKNOWN
}

enum class FundingSource { BANK, CREDIT_CARD, UNKNOWN }
enum class ExpenseClass { ESSENTIAL, DISCRETIONARY, UNKNOWN }

/** Local-only boundary values. Amounts are positive INR paise; never supply SMS text or account numbers. */
data class FinancialEvent(
    val id: String,
    val occurredAt: Instant,
    val amountPaise: Long,
    val kind: FinancialEventKind,
    val funding: FundingSource = FundingSource.UNKNOWN,
    val expenseClass: ExpenseClass = ExpenseClass.UNKNOWN,
    val category: String = "MISCELLANEOUS"
)

/** An explicit bank-cash snapshot, not a balance inferred by summing imported transactions. */
data class CashAnchor(val amountPaise: Long, val asOf: Instant)

/** Consumption is gross; refunds are separate. Cash debt service includes card settlements. */
data class FinancialHistoryMonth(
    val month: YearMonth,
    val incomePaise: Long = 0,
    val essentialConsumptionPaise: Long = 0,
    val discretionaryConsumptionPaise: Long = 0,
    val cashEssentialsPaise: Long = 0,
    val cashDiscretionaryPaise: Long = 0,
    val cashDebtServicePaise: Long = 0,
    val cashSipPaise: Long = 0,
    val refundsPaise: Long = 0,
    val cashRefundsPaise: Long = 0,
    val consumptionByCategoryPaise: Map<String, Long> = emptyMap()
)

data class NormalizedFinancialHistory(
    val months: List<FinancialHistoryMonth>,
    val cashAnchor: CashAnchor
)

enum class HistoryIssue {
    INVALID_HISTORY_WINDOW, TOO_MANY_EVENTS, MISSING_CASH_ANCHOR, INVALID_CASH_ANCHOR,
    INVALID_EVENT, FUTURE_EVENT, UNKNOWN_EVENT_KIND, UNKNOWN_EXPENSE_CLASS,
    CONFLICTING_DUPLICATE, AMOUNT_OVERFLOW
}

sealed class HistoryNormalization {
    data class Ready(val history: NormalizedFinancialHistory) : HistoryNormalization()
    // Fixed reason codes intentionally omit transaction ids and financial payloads.
    data class Unavailable(val issues: Set<HistoryIssue>) : HistoryNormalization()
}
