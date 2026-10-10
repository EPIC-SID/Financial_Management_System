package com.example.financeapp.data.ingest

import com.example.financeapp.domain.model.AccountType
import com.example.financeapp.domain.model.Direction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class SyntheticDataSeederTest {

    private val seeder = SyntheticDataSeeder

    private fun months(data: SyntheticDataset) =
        data.transactions.map { YearMonth.from(it.date) }.toSortedSet()

    private fun bankId(data: SyntheticDataset) =
        data.accounts.first { it.type == AccountType.SAVINGS }.id

    private fun monthIndex(date: java.time.LocalDate): Int {
        val ym = YearMonth.from(date)
        return (1..seeder.TOTAL_MONTHS).first { seeder.calendarMonth(it) == ym }
    }

    private fun bankFlow(data: SyntheticDataset, month: Int, includeTransfers: Boolean = false): Double {
        val ym = seeder.calendarMonth(month)
        val bank = bankId(data)
        return data.transactions
            .filter { it.accountId == bank && YearMonth.from(it.date) == ym && (includeTransfers || !it.isTransfer) }
            .sumOf { seeder.signed(it) }
    }

    @Test
    fun sameSeedIsIdenticalAndDifferentSeedIsNot() {
        Persona.entries.forEach { p ->
            assertEquals(seeder.generate(p, seeds = Seeds(7, 8, 9)), seeder.generate(p, seeds = Seeds(7, 8, 9)))
            assertNotEquals(seeder.generate(p, seeds = Seeds(7, 8, 9)), seeder.generate(p, seeds = Seeds(7, 8, 10)))
        }
    }

    @Test
    fun windowsDoNotOverlapAndHaveOwnSeeds() {
        val data = seeder.generate(Persona.B)
        assertEquals(listOf(1..18, 19..24, 25..30), data.windows.map { it.months })
        assertEquals(3, data.windows.map { it.seed }.toSet().size)
        assertTrue(data.windows.zipWithNext().all { (a, b) -> a.months.last < b.months.first })

        val trainOnly = { d: SyntheticDataset ->
            d.transactions.filter { monthIndex(it.date) in seeder.TRAIN }
        }
        assertEquals(trainOnly(seeder.generate(Persona.B, seeds = Seeds(test = 1))), trainOnly(seeder.generate(Persona.B, seeds = Seeds(test = 99))))
        val valOnly = { d: SyntheticDataset ->
            d.transactions.filter { monthIndex(it.date) in seeder.VALIDATION }
        }
        assertEquals(valOnly(seeder.generate(Persona.B, seeds = Seeds(test = 1))), valOnly(seeder.generate(Persona.B, seeds = Seeds(test = 99))))
        assertEquals(
            trainOnly(seeder.generate(Persona.B, Scenario.STABLE_INCOME)),
            trainOnly(seeder.generate(Persona.B, Scenario.INCOME_REDUCTION)),
        )
    }

    @Test
    fun thirtyMonthsGeneratedForEveryPersona() {
        val expected = (1..30).map { seeder.calendarMonth(it) }.toSet()
        Persona.entries.forEach { p ->
            val data = seeder.generate(p)
            assertEquals(p.name, expected, months(data))
            assertTrue(data.transactions.all { it.recurringId == null })
        }
    }

    @Test
    fun personaBBankBalanceReconcilesWithLedgerOnDemoDate() {
        val data = seeder.generate(Persona.B)
        val bank = bankId(data)
        val opening = data.openingBalances.getValue(bank)
        val net = data.transactions.filter { it.accountId == bank && it.date < data.demoDate }.sumOf { seeder.signed(it) }
        val expected = SyntheticPersonas.profile(Persona.B).bank.balanceAtDemo
        assertEquals(expected, opening + net, 0.5)
        assertEquals(expected, data.accounts.first { it.type == AccountType.SAVINGS }.latestBalance!!, 0.5)
        assertEquals(seeder.DEMO_DATE, data.demoDate)
        assertEquals(seeder.DEMO_DATE, seeder.calendarMonth(25).atDay(1))
    }

    @Test
    fun cardUseDoesNotReduceBankCashAndBillIsPaidOnce() {
        val data = seeder.generate(Persona.B)
        val cardId = data.accounts.first { it.type == AccountType.CARD }.id
        val cardSpend = data.transactions.filter { it.accountId == cardId && it.direction == Direction.DEBIT && !it.isTransfer }
        assertTrue(cardSpend.isNotEmpty())
        assertTrue(cardSpend.none { it.accountId == bankId(data) })
        val bills = data.transactions.filter { it.category == "CARD_BILL" && it.isTransfer }
        assertTrue(bills.isNotEmpty())
        assertEquals(bills.count { it.direction == Direction.DEBIT }, bills.count { it.direction == Direction.CREDIT })
    }

    @Test
    fun personaBBaselineSurplusIsPositive() {
        val data = seeder.generate(Persona.B, Scenario.STABLE_INCOME)
        val mean = seeder.TRAIN.map { bankFlow(data, it) }.average()
        assertTrue("persona B train surplus=$mean", mean > 0.0)
    }

    @Test
    fun fiveScenarioOverlaysOnPersonaB() {
        val stable = seeder.generate(Persona.B, Scenario.STABLE_INCOME)
        val drop = seeder.generate(Persona.B, Scenario.INCOME_REDUCTION)
        val emergency = seeder.generate(Persona.B, Scenario.UNEXPECTED_EXPENSE)
        val infeasible = seeder.generate(Persona.B, Scenario.INFEASIBLE_TARGET)
        val uncertain = seeder.generate(Persona.B, Scenario.HIGH_UNCERTAINTY)

        fun incomeIn(data: SyntheticDataset, month: Int) = data.transactions
            .filter { it.category == "INCOME" && monthIndex(it.date) == month }
            .sumOf { it.amount }

        assertEquals(85_000.0, incomeIn(drop, 25), 0.5)
        assertEquals(55_000.0, incomeIn(drop, 26), 0.5)
        assertTrue(emergency.transactions.any {
            it.category == "HEALTH" && it.amount == 25_000.0 && monthIndex(it.date) == 27
        })
        assertEquals(40_000.0, infeasible.declared.minReserve, 0.0)
        assertEquals(1_00_000.0, infeasible.declared.goal!!.targetAmount, 0.0)
        assertEquals(55_000.0, incomeIn(infeasible, 25), 0.5)
        val uncertainIncomes = (25..30).map { incomeIn(uncertain, it) }
        assertTrue(uncertainIncomes.max() > uncertainIncomes.min())
        assertEquals(stable.transactions.filter { monthIndex(it.date) in seeder.TRAIN }, drop.transactions.filter { monthIndex(it.date) in seeder.TRAIN })
    }

    @Test
    fun sparsePersonaOmitsKnownGapsAndHasTwoMonthsOfSipHistory() {
        val data = seeder.generate(Persona.B_SPARSE)
        val car = data.liabilities.first { it.kind == "VEHICLE_LOAN" }
        val card = data.liabilities.first { it.kind == "CARD" }
        assertNull(car.annualRate)
        assertNull(card.creditLimit)
        assertNull(card.minDue)
        val sipBeforeDemo = data.transactions.count {
            it.category == "INVESTMENTS" && it.date < data.demoDate
        }
        assertEquals(2, sipBeforeDemo)
    }
}
