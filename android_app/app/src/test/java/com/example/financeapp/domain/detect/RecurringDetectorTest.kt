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

class RecurringDetectorTest {

    private fun history(persona: Persona) =
        SyntheticDataSeeder.generate(persona).transactions.filter { it.date < SyntheticDataSeeder.DEMO_DATE }

    @Test
    fun findsRentEmisAndUtilitiesOnEveryPersona() {
        Persona.entries.forEach { persona ->
            val found = RecurringDetector.detect(history(persona))
            assertTrue("$persona emi", found.any { it.kind == "EMI" })
            assertTrue("$persona utility", found.any { it.kind == "UTILITY" })
        }
        listOf(Persona.A, Persona.B, Persona.B_SPARSE).forEach { persona ->
            assertTrue("$persona rent", RecurringDetector.detect(history(persona)).any { it.kind == "RENT" })
        }
    }

    @Test
    fun personaBAmountsMatchTheSeed() {
        val found = RecurringDetector.detect(history(Persona.B))
        assertTrue(found.any { it.kind == "RENT" && kotlin.math.abs(it.amount - 22_000.0) < 1 })
        assertTrue(found.any { it.kind == "EMI" && kotlin.math.abs(it.amount - 18_500.0) < 1 })
        assertTrue(found.any { it.kind == "EMI" && kotlin.math.abs(it.amount - 8_200.0) < 1 })
        assertTrue(found.any { it.kind == "UTILITY" && kotlin.math.abs(it.amount - 2_500.0) < 800 })
    }

    @Test
    fun variableSpendIsNotRecurring() {
        val found = RecurringDetector.detect(history(Persona.B))
        assertTrue(found.none { it.label == "FOOD" || it.label == "SHOPPING" || it.label == "TRAVEL" || it.label == "GROCERIES" || it.label == "ENTERTAINMENT" })
    }

    @Test
    fun incomeOnPersonaBIsFixedSalary() {
        val income = IncomeDetector.detect(history(Persona.B))
        assertEquals(1, income.size)
        assertEquals("FIXED", income.single().incomeType)
        assertEquals(85_000.0, income.single().typicalAmount, 20_000.0)
    }

    @Test
    fun liabilitiesComeFromDetectedEmisAndMarkUnknownRate() {
        val txns = history(Persona.B)
        val extracted = LiabilityExtractor.from(RecurringDetector.detect(txns), txns)
        assertEquals(2, extracted.liabilities.count { it.kind == "EMI" })
        assertTrue(extracted.liabilities.all { it.annualRate == null })
        assertTrue(extracted.missing.contains("loan interest rate"))
    }

    @Test
    fun threeEqualDebitsAreNotEnoughWhenTheyAreWeeklyNoise() {
        val start = LocalDate.of(2026, 1, 1)
        val noise = (0 until 8).map { i ->
            Txn("n$i", start.plusDays(i * 3L), 400.0 + i, Direction.DEBIT, "FOOD", "swiggy", "bank", false, null, Source.SYNTHETIC)
        }
        assertTrue(RecurringDetector.detect(noise).none { it.label == "FOOD" })
    }
}
