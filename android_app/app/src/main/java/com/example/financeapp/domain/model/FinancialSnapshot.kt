package com.example.financeapp.domain.model

import com.example.financeapp.data.TransactionEntity

data class RecurringObligation(
    val name: String,
    val amount: Double,
    val dayOfMonth: Int,
    val category: String,
)

/** Everything the engines and screens need for one state of the world. */
data class FinancialSnapshot(
    val activeScenario: Scenario,
    val currentBalance: Double,
    val minReserve: Double,
    val debts: List<Debt>,
    val recurringObligations: List<RecurringObligation>,
    /** Full 6-month history (Month -6 to -1) plus any injected transactions, oldest first. */
    val recentTransactions: List<TransactionEntity>,
    /** Month +1 to +6. */
    val planningPeriods: List<PlanningPeriod>,
    val savingsGoal: SavingsGoal? = null,
)
