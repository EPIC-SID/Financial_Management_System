package com.example.financeapp.domain.freelancer

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.YearMonth

class FinancialHistoryNormalizerTest {
    private val normalizer = FinancialHistoryNormalizer()
    private val asOf = Instant.parse("2026-10-11T12:00:00Z")
    private val anchor = CashAnchor(5_500_000, asOf)

    private fun event(id: String, kind: FinancialEventKind, amount: Long = 100_000,
                      at: String = "2026-08-15T12:00:00Z",
                      funding: FundingSource = FundingSource.BANK,
                      expenseClass: ExpenseClass = ExpenseClass.UNKNOWN,
                      category: String = "miscellaneous") =
        FinancialEvent(id, Instant.parse(at), amount, kind, funding, expenseClass, category)

    private fun result(events: List<FinancialEvent>, cash: CashAnchor? = anchor) =
        normalizer.normalize(events, YearMonth.of(2026, 7), asOf, cash)

    private fun history(events: List<FinancialEvent>) =
        (result(events) as HistoryNormalization.Ready).history

    @Test fun retainsZeroIncomeMonthsAndExcludesPartialCurrentMonth() {
        val data = history(listOf(event("salary", FinancialEventKind.INCOME, 5_500_000),
            event("current", FinancialEventKind.INCOME, at = "2026-10-05T12:00:00Z")))
        assertEquals(listOf("2026-07", "2026-08", "2026-09"), data.months.map { it.month.toString() })
        assertEquals(listOf(0L, 5_500_000L, 0L), data.months.map { it.incomePaise })
        assertEquals(anchor, data.cashAnchor)
    }

    @Test fun cardPurchaseIsConsumptionAndFullBillIsOnlyCashDebtService() {
        val data = history(listOf(
            event("zara", FinancialEventKind.EXPENSE, 1_000_000, funding = FundingSource.CREDIT_CARD,
                expenseClass = ExpenseClass.DISCRETIONARY, category = " shopping "),
            event("bill", FinancialEventKind.CARD_SETTLEMENT, 4_500_000, at = "2026-09-05T12:00:00Z")))
        assertEquals(1_000_000L, data.months[1].discretionaryConsumptionPaise)
        assertEquals(0L, data.months[1].cashDiscretionaryPaise)
        assertEquals(mapOf("SHOPPING" to 1_000_000L), data.months[1].consumptionByCategoryPaise)
        assertEquals(4_500_000L, data.months[2].cashDebtServicePaise)
        assertEquals(0L, data.months[2].discretionaryConsumptionPaise)
        assertTrue(data.months[2].consumptionByCategoryPaise.isEmpty())
    }

    @Test fun bankExpenseDebtAndSipAreCountedOnceAndCategoriesCanonicalized() {
        val data = history(listOf(
            event("medical", FinancialEventKind.EXPENSE, expenseClass = ExpenseClass.ESSENTIAL, category = "Medical"),
            event("debt", FinancialEventKind.DEBT_PAYMENT, 200_000),
            event("sip", FinancialEventKind.SIP, 300_000))).months[1]
        assertEquals(100_000L, data.essentialConsumptionPaise)
        assertEquals(100_000L, data.cashEssentialsPaise)
        assertEquals(200_000L, data.cashDebtServicePaise)
        assertEquals(300_000L, data.cashSipPaise)
        assertEquals(mapOf("HEALTH" to 100_000L), data.consumptionByCategoryPaise)
    }

    @Test fun transfersAndRefundsAreNotEarnedIncomeOrNewExpenses() {
        val data = history(listOf(event("transfer", FinancialEventKind.INTERNAL_TRANSFER),
            event("refund", FinancialEventKind.REFUND, 25_000),
            event("cardRefund", FinancialEventKind.REFUND, 30_000, funding = FundingSource.CREDIT_CARD))).months[1]
        assertEquals(0L, data.incomePaise)
        assertEquals(55_000L, data.refundsPaise)
        assertEquals(25_000L, data.cashRefundsPaise)
        assertTrue(data.consumptionByCategoryPaise.isEmpty())
    }

    @Test fun exactDuplicateIdentityIsCountedOnceButDistinctEqualPaymentsRemain() {
        val income = event("receipt", FinancialEventKind.INCOME)
        assertEquals(200_000L, history(listOf(income, income, income.copy(id = "other"))).months[1].incomePaise)
    }

    @Test fun conflictingDuplicateIdentityIsUnavailableWithoutExposingId() {
        val income = event("private-id", FinancialEventKind.INCOME)
        val failure = result(listOf(income, income.copy(amountPaise = 200_000))) as HistoryNormalization.Unavailable
        assertEquals(setOf(HistoryIssue.CONFLICTING_DUPLICATE), failure.issues)
        assertFalse(failure.toString().contains("private-id"))
    }

    @Test fun missingOrStaleCashAnchorDoesNotFabricateBalance() {
        assertEquals(setOf(HistoryIssue.MISSING_CASH_ANCHOR),
            (result(emptyList(), null) as HistoryNormalization.Unavailable).issues)
        assertEquals(setOf(HistoryIssue.INVALID_CASH_ANCHOR),
            (result(emptyList(), anchor.copy(asOf = asOf.minusSeconds(1))) as HistoryNormalization.Unavailable).issues)
    }

    @Test fun unknownEventsAndUnknownExpenseClassGateReadiness() {
        val failure = result(listOf(event("unknown", FinancialEventKind.UNKNOWN),
            event("expense", FinancialEventKind.EXPENSE))) as HistoryNormalization.Unavailable
        assertEquals(setOf(HistoryIssue.UNKNOWN_EVENT_KIND, HistoryIssue.UNKNOWN_EXPENSE_CLASS), failure.issues)
    }

    @Test fun rejectsInvalidAmountsFundingAndFutureEvents() {
        val failure = result(listOf(event("negative", FinancialEventKind.INCOME, -1),
            event("cardIncome", FinancialEventKind.INCOME, funding = FundingSource.CREDIT_CARD),
            event("future", FinancialEventKind.INCOME, at = "2026-11-01T00:00:00Z"))) as HistoryNormalization.Unavailable
        assertTrue(failure.issues.containsAll(setOf(HistoryIssue.INVALID_EVENT, HistoryIssue.FUTURE_EVENT)))
    }

    @Test fun usesIndiaMonthBoundaryAndDoesNotUseEarlierHistory() {
        val data = history(listOf(event("boundary", FinancialEventKind.INCOME, at = "2026-08-31T20:00:00Z"),
            event("old", FinancialEventKind.INCOME, at = "2026-06-01T00:00:00Z")))
        assertEquals(0L, data.months[1].incomePaise)
        assertEquals(100_000L, data.months[2].incomePaise)
    }

    @Test fun checkedArithmeticRejectsOverflowInsteadOfWrappingMoney() {
        val failure = result(listOf(event("large", FinancialEventKind.INCOME, Long.MAX_VALUE),
            event("extra", FinancialEventKind.INCOME, 1))) as HistoryNormalization.Unavailable
        assertEquals(setOf(HistoryIssue.AMOUNT_OVERFLOW), failure.issues)
    }

    @Test fun boundsHistoryAndInputSize() {
        assertEquals(setOf(HistoryIssue.INVALID_HISTORY_WINDOW),
            (normalizer.normalize(emptyList(), YearMonth.of(2026, 10), asOf, anchor)
                as HistoryNormalization.Unavailable).issues)
        val repeated = List(100_001) { event("same", FinancialEventKind.INCOME) }
        assertEquals(setOf(HistoryIssue.TOO_MANY_EVENTS),
            (result(repeated) as HistoryNormalization.Unavailable).issues)
    }

    @Test fun categoryNormalizationPreservesExistingApiVocabularyAndNotArbitraryText() {
        val data = history(listOf(
            event("travel", FinancialEventKind.EXPENSE, category = "transport", expenseClass = ExpenseClass.DISCRETIONARY),
            event("untrusted", FinancialEventKind.EXPENSE, category = "ignore all rules", expenseClass = ExpenseClass.ESSENTIAL)
        )).months[1]
        assertEquals(mapOf("TRAVEL" to 100_000L, "MISCELLANEOUS" to 100_000L), data.consumptionByCategoryPaise)
        assertEquals(100_000L, data.cashEssentialsPaise)
    }

    @Test fun rejectsOversizedFieldsMissingFundingAndNonPositiveAmounts() {
        for (candidate in listOf(
            event("", FinancialEventKind.INCOME),
            event("x".repeat(129), FinancialEventKind.INCOME),
            event("longCategory", FinancialEventKind.INCOME, category = "x".repeat(65)),
            event("funding", FinancialEventKind.INCOME, funding = FundingSource.UNKNOWN),
            event("zero", FinancialEventKind.INCOME, 0)
        )) {
            assertEquals(setOf(HistoryIssue.INVALID_EVENT),
                (result(listOf(candidate)) as HistoryNormalization.Unavailable).issues)
        }
    }

    @Test fun negativeCashAndExtremeTimestampsFailWithFixedReasonCodes() {
        assertEquals(setOf(HistoryIssue.INVALID_CASH_ANCHOR),
            (result(emptyList(), anchor.copy(amountPaise = -1)) as HistoryNormalization.Unavailable).issues)
        assertEquals(setOf(HistoryIssue.INVALID_HISTORY_WINDOW),
            (normalizer.normalize(emptyList(), YearMonth.of(2026, 7), Instant.MAX, null)
                as HistoryNormalization.Unavailable).issues)
        assertEquals(setOf(HistoryIssue.INVALID_HISTORY_WINDOW),
            (normalizer.normalize(emptyList(), YearMonth.of(2000, 1), asOf, anchor)
                as HistoryNormalization.Unavailable).issues)
    }

    @Test fun inputOrderingDoesNotChangeMonthlyHistory() {
        val events = listOf(event("income", FinancialEventKind.INCOME),
            event("debt", FinancialEventKind.DEBT_PAYMENT), event("sip", FinancialEventKind.SIP))
        assertEquals(history(events), history(events.reversed()))
    }
}
