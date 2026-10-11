package com.example.financeapp.ui.viewmodel

import com.example.financeapp.data.TransactionEntity
import com.example.financeapp.data.CategorizationPolicy
import com.example.financeapp.data.ingest.SyntheticDataSeeder
import com.example.financeapp.data.ingest.Persona
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ActivityStateTest {
    private val today = LocalDate.of(2026, 10, 11)
    @Test fun liveMappingDoesNotImportDemoFactsOrRawDescriptions() {
        val rows = ActivityData.live(listOf(TransactionEntity("sms", "Local shop", -264.0, "MISCELLANEOUS", "private SMS", "2026-10-10T20:00:00")))
        assertEquals(1, rows.size)
        assertEquals("sms", rows.single().id)
        assertFalse(rows.toString().contains("private SMS"))
        assertEquals(LocalDate.of(2026, 10, 11), rows.single().date)
    }
    @Test fun filtersExcludeFutureAndUnknownDatesFromBoundedWindows() {
        val rows = listOf(ActivityEntry("old", "Shop", -100.0, "FOOD", today.minusDays(30)),
            ActivityEntry("now", "Shop", -200.0, "FOOD", today),
            ActivityEntry("future", "Shop", -400.0, "FOOD", today.plusDays(1)),
            ActivityEntry("bad", "Shop", -500.0, "FOOD", null))
        assertEquals(listOf("now"), ActivityData.filter(rows, ActivityPeriod.LAST_30, today).map { it.id })
        assertEquals(setOf("old", "now", "bad"), ActivityData.filter(rows, ActivityPeriod.ALL, today).map { it.id }.toSet())
    }
    @Test fun categoriesRecomputeAfterRoomCorrectionAndCreditsAreNotExpenses() {
        val debit = ActivityEntry("debit", "Shop", -264.0, "MISCELLANEOUS", today)
        val credit = ActivityEntry("credit", "Refund", 50.0, "INCOME", today)
        val before = ActivityData.summary(listOf(debit, credit))
        val after = ActivityData.summary(listOf(debit.copy(category = "FOOD"), credit))
        assertEquals(264.0, after.debits, 0.0)
        assertEquals(50.0, after.credits, 0.0)
        assertEquals(1, before.unclassified)
        assertEquals(0, after.unclassified)
        assertEquals(mapOf("FOOD" to 264.0), after.categories)
    }
    @Test fun demoActivityHasNoHeldOutFutureOutcomes() {
        val dataset = SyntheticDataSeeder.generate(Persona.B)
        assertTrue(ActivityData.demo(dataset).all { it.date!! < dataset.demoDate })
    }
    @Test fun knownMerchantAliasDoesNotGuessAmbiguousGoogleDebit() {
        assertEquals("FOOD", CategorizationPolicy.knownMerchantCategory("BLINK COMMERCE PRIVATE LIMITED"))
        assertNull(CategorizationPolicy.knownMerchantCategory("Google"))
        assertNull(CategorizationPolicy.knownMerchantCategory("Notblink commerce"))
    }
}
