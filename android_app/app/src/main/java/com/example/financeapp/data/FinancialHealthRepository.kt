package com.example.financeapp.data

import com.example.financeapp.domain.model.FinancialSnapshot
import com.example.financeapp.domain.model.RecurringObligation
import com.example.financeapp.domain.model.Scenario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Single in-memory source of truth for the Financial Health features.
 * Derived values (health score, forecast, plans) should be computed from [snapshot]
 * (e.g. `snapshot.map { engine.run(it) }`) so they recalculate on every change.
 *
 * shortcut: no Room persistence, state resets on app restart; add Room sync only if the demo needs it.
 */
class FinancialHealthRepository(
    initialScenario: Scenario = Scenario.STABLE_INCOME,
    private val seed: Long = SyntheticDataSeeder.DEFAULT_SEED,
) {
    private val _snapshot = MutableStateFlow(SyntheticDataSeeder.generate(initialScenario, seed).snapshot)
    val snapshot: StateFlow<FinancialSnapshot> = _snapshot.asStateFlow()

    fun switchScenario(scenario: Scenario) {
        _snapshot.value = SyntheticDataSeeder.generate(scenario, seed).snapshot
    }

    /** Amount is signed like [TransactionEntity.amount]: negative = expense, positive = income. */
    fun injectTransaction(tx: TransactionEntity) {
        require(tx.amount.isFinite() && tx.amount != 0.0) { "Transaction amount must be a non-zero number" }
        _snapshot.update {
            it.copy(
                currentBalance = it.currentBalance + tx.amount,
                recentTransactions = it.recentTransactions + tx,
            )
        }
    }

    /** Adds a monthly bill and raises essential expenses for every planning period. */
    fun addRecurringObligation(obligation: RecurringObligation) {
        require(obligation.amount.isFinite() && obligation.amount > 0) { "Obligation amount must be positive" }
        _snapshot.update { s ->
            s.copy(
                recurringObligations = s.recurringObligations + obligation,
                planningPeriods = s.planningPeriods.map { it.copy(essentialExpenses = it.essentialExpenses + obligation.amount) },
            )
        }
    }

    fun updateMinReserve(amount: Double) {
        require(amount.isFinite() && amount >= 0) { "Minimum reserve must be zero or positive" }
        _snapshot.update { it.copy(minReserve = amount) }
    }

    companion object {
        /** Shared by all screens so they observe the same state. */
        val instance by lazy { FinancialHealthRepository() }
    }
}
