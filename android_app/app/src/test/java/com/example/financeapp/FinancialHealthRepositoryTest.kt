package com.example.financeapp

import com.example.financeapp.data.FinancialHealthRepository
import com.example.financeapp.data.TransactionEntity
import com.example.financeapp.domain.model.RecurringObligation
import com.example.financeapp.domain.model.Scenario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FinancialHealthRepositoryTest {

    private val bill = TransactionEntity(
        smsId = "inject-1", merchant = "Apollo Hospital", amount = -15_000.0, category = "HEALTH",
        description = null, transactionDate = "2026-10-05T10:00:00", isCategorizedByAi = true, createdAt = 0,
    )

    @Test
    fun switchScenarioEmitsNewSnapshotImmediately() {
        val repo = FinancialHealthRepository()
        assertEquals(Scenario.STABLE_INCOME, repo.snapshot.value.activeScenario)
        repo.switchScenario(Scenario.INFEASIBLE_TARGET)
        assertEquals(Scenario.INFEASIBLE_TARGET, repo.snapshot.value.activeScenario)
        assertEquals(40_000.0, repo.snapshot.value.minReserve, 0.0)
    }

    @Test
    fun injectTransactionUpdatesBalanceAndHistory() {
        val repo = FinancialHealthRepository()
        val before = repo.snapshot.value
        repo.injectTransaction(bill)
        val after = repo.snapshot.value
        assertEquals(before.currentBalance - 15_000.0, after.currentBalance, 0.0)
        assertEquals(bill, after.recentTransactions.last())
    }

    @Test
    fun switchingScenarioDiscardsInjectedTransactions() {
        val repo = FinancialHealthRepository()
        val clean = repo.snapshot.value
        repo.injectTransaction(bill)
        repo.switchScenario(Scenario.STABLE_INCOME)
        assertEquals(clean, repo.snapshot.value)
    }

    @Test
    fun recurringObligationRaisesEveryPeriodsEssentials() {
        val repo = FinancialHealthRepository()
        val before = repo.snapshot.value.planningPeriods.map { it.essentialExpenses }
        repo.addRecurringObligation(RecurringObligation("Gym", 5_000.0, 5, "BILLS"))
        val after = repo.snapshot.value.planningPeriods.map { it.essentialExpenses }
        assertEquals(before.map { it + 5_000.0 }, after)
    }

    @Test
    fun rejectsInvalidInputs() {
        val repo = FinancialHealthRepository()
        assertThrows(IllegalArgumentException::class.java) { repo.updateMinReserve(-1.0) }
        assertThrows(IllegalArgumentException::class.java) { repo.injectTransaction(bill.copy(amount = Double.NaN)) }
        assertThrows(IllegalArgumentException::class.java) {
            repo.addRecurringObligation(RecurringObligation("Bad", 0.0, 1, "BILLS"))
        }
        repo.updateMinReserve(30_000.0)
        assertEquals(30_000.0, repo.snapshot.value.minReserve, 0.0)
    }
}
