package com.example.financeapp.data

import com.example.financeapp.data.ingest.SyntheticDataSeeder
import com.example.financeapp.data.ingest.SyntheticDataset
import com.example.financeapp.data.ingest.SyntheticPersonas
import com.example.financeapp.domain.model.AccountType
import com.example.financeapp.domain.model.DeclaredInputs
import com.example.financeapp.domain.model.Direction
import com.example.financeapp.domain.model.FinancialSnapshot
import com.example.financeapp.domain.model.MonthlyAggregate
import com.example.financeapp.domain.model.RecurringItem
import com.example.financeapp.domain.model.Txn
import java.time.LocalDate
import java.time.YearMonth

object SnapshotBuilder {

    fun from(
        dataset: SyntheticDataset,
        extra: List<Txn> = emptyList(),
        declared: DeclaredInputs = dataset.declared,
    ): FinancialSnapshot {
        val asOf = dataset.demoDate
        val history = aggregates((dataset.transactions + extra).filter { it.date < asOf })
        val bank = dataset.accounts.first { it.type == AccountType.SAVINGS }
        val extraCash = extra.filter { it.accountId == bank.id }.sumOf { SyntheticDataSeeder.signed(it) }
        val cash = (bank.latestBalance ?: 0.0) + extraCash
        val recurring = recurring(dataset, asOf)
        val (completeness, missing) = completeness(cash, declared, dataset, history, recurring)
        return FinancialSnapshot(
            asOf = asOf,
            cash = cash,
            accounts = dataset.accounts.map { if (it.id == bank.id) it.copy(latestBalance = cash) else it },
            liabilities = dataset.liabilities,
            investments = dataset.investments,
            recurring = recurring,
            history = history,
            declared = declared,
            completeness = completeness,
            missing = missing,
        )
    }

    private fun aggregates(txns: List<Txn>): List<MonthlyAggregate> =
        txns.groupBy { YearMonth.from(it.date) }.toSortedMap().map { (month, rows) ->
            fun sum(pred: (Txn) -> Boolean) = rows.filter(pred).sumOf { it.amount }
            MonthlyAggregate(
                month = month,
                income = sum { it.direction == Direction.CREDIT && it.category == "INCOME" && !it.isTransfer },
                essentials = sum { it.direction == Direction.DEBIT && it.category in ESSENTIAL && !it.isTransfer },
                debtService = sum { it.direction == Direction.DEBIT && it.category in DEBT },
                discretionary = sum { it.direction == Direction.DEBIT && it.category in DISCRETIONARY && !it.isTransfer },
                investments = sum { it.direction == Direction.DEBIT && it.category == "INVESTMENTS" && !it.isTransfer },
            )
        }

    private fun recurring(dataset: SyntheticDataset, asOf: LocalDate): List<RecurringItem> {
        val p = SyntheticPersonas.profile(dataset.persona)
        val items = mutableListOf<RecurringItem>()
        items += RecurringItem("inc", p.income.employer, p.income.base, 30, nextDue(asOf, p.income.payDay), 1.0, "INCOME")
        p.bills.forEach { b ->
            val kind = when (b.category) {
                "RENT" -> "RENT"
                "UTILITIES" -> "UTILITY"
                "INSURANCE" -> "INSURANCE"
                else -> "OTHER"
            }
            items += RecurringItem("bill-${b.merchant}", b.merchant, b.amount, 30, nextDue(asOf, b.day), 1.0, kind)
        }
        dataset.liabilities.filter { it.kind != "CARD" && it.emi != null && it.dueDay != null }.forEach {
            items += RecurringItem(it.id, it.kind, it.emi!!, 30, nextDue(asOf, it.dueDay!!), 1.0, "EMI")
        }
        dataset.investments.forEach {
            items += RecurringItem(it.id, it.platform, it.monthlyAmount, 30, nextDue(asOf, it.debitDay), 1.0, "OTHER")
        }
        val card = dataset.liabilities.firstOrNull { it.kind == "CARD" }
        val due = dataset.cardStatements.lastOrNull { it.dueDate >= asOf }
        if (card != null && due != null) {
            items += RecurringItem(card.id, "Card bill", due.totalDue, 30, due.dueDate, 1.0, "CARD_BILL")
        }
        return items
    }

    private fun completeness(
        cash: Double,
        declared: DeclaredInputs,
        dataset: SyntheticDataset,
        history: List<MonthlyAggregate>,
        recurring: List<RecurringItem>,
    ): Pair<Double, List<String>> {
        val missing = mutableListOf<String>()
        var score = 0.0
        if (cash > 0) score += 0.25 else missing += "cash balance"
        if (declared.typicalIncome != null && declared.typicalIncome > 0) score += 0.25 else missing += "income"
        if (recurring.any { it.kind != "INCOME" }) score += 0.2 else missing += "recurring obligations"
        val loans = dataset.liabilities.filter { it.kind != "CARD" }
        val cards = dataset.liabilities.filter { it.kind == "CARD" }
        val loanBits = loans.flatMap { listOf(it.emi, it.annualRate, it.outstanding, it.monthsLeft) }
        val cardBits = cards.flatMap { listOf(it.creditLimit, it.minDue) }
        val bits = loanBits + cardBits
        if (bits.isNotEmpty()) score += 0.2 * bits.count { it != null }.toDouble() / bits.size
        if (loans.any { it.annualRate == null }) missing += "loan interest rate"
        if (loans.any { it.outstanding == null }) missing += "loan balance"
        if (cards.any { it.creditLimit == null }) missing += "card limit"
        if (cards.any { it.minDue == null }) missing += "card minimum due"
        if (history.size >= 3) score += 0.1 else missing += "transaction history"
        return score to missing
    }

    fun nextDue(asOf: LocalDate, day: Int): LocalDate {
        val thisMonth = asOf.withDayOfMonth(day.coerceAtMost(asOf.lengthOfMonth()))
        return if (!thisMonth.isBefore(asOf)) thisMonth
        else asOf.plusMonths(1).let { it.withDayOfMonth(day.coerceAtMost(it.lengthOfMonth())) }
    }

    private val ESSENTIAL = setOf("RENT", "UTILITIES", "INSURANCE", "EDUCATION", "HEALTH", "MAINTENANCE")
    private val DEBT = setOf("EMI", "CARD_BILL")
    private val DISCRETIONARY = setOf("FOOD", "GROCERIES", "SHOPPING", "TRAVEL", "ENTERTAINMENT")
}
