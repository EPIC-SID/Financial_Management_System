package com.example.financeapp

import com.example.financeapp.data.SyntheticDataSeeder
import com.example.financeapp.data.SyntheticDataSeeder.ANCHOR
import com.example.financeapp.data.TransactionEntity
import com.example.financeapp.domain.model.Scenario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyntheticDataSeederTest {

    private fun months(txs: List<TransactionEntity>) = txs.map { it.transactionDate.take(7) }.toSortedSet().toList()
    private fun monthKeys(range: IntRange) = range.map { ANCHOR.plusMonths(it.toLong()).toString() }

    @Test
    fun everyScenarioHasSixHistoryAndSixFutureMonths() {
        Scenario.entries.forEach { s ->
            val data = SyntheticDataSeeder.generate(s)
            assertEquals(s.name, monthKeys(-6..-1), months(data.snapshot.recentTransactions))
            assertEquals(s.name, monthKeys(1..6), months(data.futureActuals))
            assertEquals(s.name, (1..6).toList(), data.snapshot.planningPeriods.map { it.periodIndex })
            val ids = (data.snapshot.recentTransactions + data.futureActuals).map { it.smsId }
            assertEquals(s.name, ids.size, ids.toSet().size)
        }
    }

    @Test
    fun sameSeedIsReproducibleAndDifferentSeedIsNot() {
        Scenario.entries.forEach { s ->
            assertEquals(SyntheticDataSeeder.generate(s, 7), SyntheticDataSeeder.generate(s, 7))
            assertNotEquals(SyntheticDataSeeder.generate(s, 7), SyntheticDataSeeder.generate(s, 8))
        }
    }

    @Test
    fun incomeReductionDropsFromMonthTwo() {
        val incomes = SyntheticDataSeeder.generate(Scenario.INCOME_REDUCTION).snapshot.planningPeriods.map { it.projectedIncome }
        assertEquals(listOf(85_000.0, 55_000.0, 55_000.0, 55_000.0, 55_000.0, 55_000.0), incomes)
    }

    @Test
    fun emergencyExpenseLandsInMonthThree() {
        val data = SyntheticDataSeeder.generate(Scenario.UNEXPECTED_EXPENSE)
        val periods = data.snapshot.planningPeriods
        assertEquals(25_000.0, periods[2].essentialExpenses - periods[1].essentialExpenses, 0.0)
        val bill = data.futureActuals.single { it.category == "HEALTH" }
        assertEquals(-25_000.0, bill.amount, 0.0)
        assertEquals(ANCHOR.plusMonths(3).toString(), bill.transactionDate.take(7))
    }

    @Test
    fun infeasibleTargetCannotBeMetWhileKeepingReserve() {
        val s = SyntheticDataSeeder.generate(Scenario.INFEASIBLE_TARGET).snapshot
        val goal = s.savingsGoal!!
        assertEquals(40_000.0, s.minReserve, 0.0)
        val surplus = s.planningPeriods.sumOf { it.projectedIncome - it.essentialExpenses - it.debtObligations - it.projectedDiscretionary }
        assertTrue("Target must exceed what is reachable", s.currentBalance + surplus - s.minReserve < goal.targetAmount)
    }

    @Test
    fun highUncertaintyIncomeStaysWithinFortyPercent() {
        val base = SyntheticDataSeeder.HIGH_UNCERTAINTY_BASE_INCOME
        val data = SyntheticDataSeeder.generate(Scenario.HIGH_UNCERTAINTY)
        val incomes = (data.snapshot.recentTransactions + data.futureActuals).filter { it.category == "INCOME" }.map { it.amount }
        assertEquals(12, incomes.size)
        assertTrue(incomes.all { it in base * 0.6..base * 1.4 })
        assertTrue("Income should actually vary", incomes.max() - incomes.min() > base * 0.2)
    }

    /** Calibration: across seeds, how often the realised future path breaches the reserve. */
    private fun breachRate(s: Scenario): Double = (1..200).count { seed ->
        val data = SyntheticDataSeeder.generate(s, seed.toLong())
        val byMonth = data.futureActuals.groupBy { it.transactionDate.take(7) }.toSortedMap().values
        var balance = data.snapshot.currentBalance
        byMonth.any { month -> balance += month.sumOf { it.amount }; balance < data.snapshot.minReserve }
    } / 200.0

    @Test
    fun highUncertaintyHasShortfallRiskAboveThirtyFivePercent() {
        assertTrue("risk=${breachRate(Scenario.HIGH_UNCERTAINTY)}", breachRate(Scenario.HIGH_UNCERTAINTY) > 0.35)
        assertTrue("stable risk=${breachRate(Scenario.STABLE_INCOME)}", breachRate(Scenario.STABLE_INCOME) < 0.05)
    }
}
