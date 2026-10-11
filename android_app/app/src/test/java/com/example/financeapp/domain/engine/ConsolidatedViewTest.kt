package com.example.financeapp.domain.engine

import com.example.financeapp.data.SnapshotBuilder
import com.example.financeapp.data.ingest.Persona
import com.example.financeapp.data.ingest.Scenario
import com.example.financeapp.data.ingest.SyntheticDataSeeder
import com.example.financeapp.domain.model.AccountType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsolidatedViewTest {

    private val snapshot = SnapshotBuilder.from(SyntheticDataSeeder.generate(Persona.B, Scenario.STABLE_INCOME))
    private val view = ConsolidatedViewBuilder.from(snapshot)

    @Test
    fun streamTotalsMatchAccounts() {
        val savings = snapshot.accounts.filter { it.type == AccountType.SAVINGS }.sumOf { it.latestBalance ?: 0.0 }
        val emis = snapshot.liabilities.filter { it.kind != "CARD" }.sumOf { it.emi ?: 0.0 }
        val sip = snapshot.investments.sumOf { it.monthlyAmount }
        assertEquals(savings, view.liquidCash, 0.5)
        assertEquals(45_200.0, view.liquidCash, 0.5)
        assertEquals(emis, view.monthlyEmis, 0.5)
        assertEquals(26_700.0, view.monthlyEmis, 0.5)
        assertEquals(sip, view.monthlySip, 0.5)
        assertEquals(10_000.0, view.monthlySip, 0.5)
        assertTrue(view.avgDiscretionary > 0.0)
    }

    @Test
    fun cardDuesAreNotSubtractedFromCash() {
        assertTrue(view.cardDues >= 0.0)
        assertEquals(45_200.0, view.liquidCash, 0.5)
    }

    @Test
    fun sipIsExcludedFromDti() {
        val score = HealthScore.of(snapshot)
        val income = snapshot.declared.typicalIncome!!
        val emis = 18_500.0 + 8_200.0
        val cardMin = snapshot.liabilities.first { it.kind == "CARD" }.minDue ?: 0.0
        assertEquals((emis + cardMin) / income, score.dti, 1e-6)
        assertTrue(score.dti < (emis + 10_000.0) / income)
    }
}
