package com.example.financeapp.domain.detect

import com.example.financeapp.data.ingest.Persona
import com.example.financeapp.data.ingest.SyntheticDataSeeder
import com.example.financeapp.domain.model.Direction
import com.example.financeapp.domain.model.Source
import com.example.financeapp.domain.model.Txn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class InvestmentDetectorTest {

    private fun history(persona: Persona) =
        SyntheticDataSeeder.generate(persona).transactions.filter { it.date < SyntheticDataSeeder.DEMO_DATE }

    @Test
    fun findsMonthlySipOnBAndC() {
        val b = InvestmentDetector.detect(history(Persona.B))
        val c = InvestmentDetector.detect(history(Persona.C))
        assertEquals(1, b.size)
        assertEquals(10_000.0, b.single().monthlyAmount, 1.0)
        assertEquals("MUTUAL_FUND", b.single().assetType)
        assertTrue(b.single().debitDay in 3..7)
        assertEquals(1, c.size)
        assertEquals(15_000.0, c.single().monthlyAmount, 1.0)
    }

    @Test
    fun personaAHasNoSip() {
        assertTrue(InvestmentDetector.detect(history(Persona.A)).isEmpty())
    }

    @Test
    fun twoMonthsOfSipHistoryIsNotRecurring() {
        val sparse = InvestmentDetector.detect(history(Persona.B_SPARSE))
        assertTrue(sparse.isEmpty())
    }

    @Test
    fun oneOffInvestmentDebitIsNotASip() {
        val day = LocalDate.of(2026, 3, 5)
        val txns = listOf(
            Txn("once", day, 50_000.0, Direction.DEBIT, "INVESTMENTS", "groww", "bank", false, null, Source.SYNTHETIC),
        )
        assertTrue(InvestmentDetector.detect(txns).isEmpty())
    }

    @Test
    fun foodDebitsAreNotSips() {
        val found = InvestmentDetector.detect(history(Persona.B))
        assertTrue(found.none { it.monthlyAmount < 5_000 })
    }
}
