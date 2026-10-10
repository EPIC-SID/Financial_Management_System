package com.example.financeapp.domain.engine

import com.example.financeapp.domain.model.AccountType
import com.example.financeapp.domain.model.ConsolidatedView
import com.example.financeapp.domain.model.FinancialSnapshot
import java.time.temporal.ChronoUnit

object ConsolidatedViewBuilder {
    fun from(snapshot: FinancialSnapshot): ConsolidatedView {
        val savings = snapshot.accounts.filter { it.type == AccountType.SAVINGS }
        val cards = snapshot.liabilities.filter { it.kind == "CARD" }
        val loans = snapshot.liabilities.filter { it.kind != "CARD" }
        val trailing = snapshot.history.takeLast(6)
        return ConsolidatedView(
            liquidCash = savings.sumOf { it.latestBalance ?: 0.0 },
            cardDues = cards.sumOf { it.totalDue ?: it.unbilled ?: 0.0 },
            monthlyEmis = loans.sumOf { it.emi ?: 0.0 },
            monthlySip = snapshot.investments.sumOf { it.monthlyAmount },
            avgDiscretionary = if (trailing.isEmpty()) 0.0 else trailing.map { it.discretionary }.average(),
            staleAccountIds = savings.filter { acc ->
                val asOf = acc.balanceAsOf
                asOf == null || ChronoUnit.DAYS.between(asOf, snapshot.asOf) > 7
            }.map { it.id },
        )
    }
}
