package com.example.financeapp.data

import com.example.financeapp.data.ingest.Persona
import com.example.financeapp.data.ingest.Scenario
import com.example.financeapp.domain.model.Direction
import com.example.financeapp.domain.model.Source
import com.example.financeapp.domain.model.Txn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FinancialHealthRepositoryTest {

    @Test
    fun switchScenarioEmitsNewSnapshotScoreAndVerdict() {
        val repo = FinancialHealthRepository(Persona.B, Scenario.STABLE_INCOME)
        val before = repo.state.value
        assertEquals(25_000.0, before.snapshot.declared.minReserve, 0.0)
        repo.switchScenario(Scenario.INFEASIBLE_TARGET)
        val after = repo.state.value
        assertEquals(Scenario.INFEASIBLE_TARGET.label, "Infeasible")
        assertEquals(40_000.0, after.snapshot.declared.minReserve, 0.0)
        assertNotNull(after.snapshot.declared.goal)
        assertNotEquals(before.score.total, after.score.total)
        assertTrue(after.verdict.summary.isNotBlank())
    }

    @Test
    fun injectTransactionUpdatesCashAndRecomputes() {
        val repo = FinancialHealthRepository()
        val before = repo.state.value
        val bank = before.snapshot.accounts.first().id
        repo.injectTransaction(
            Txn("inj-1", LocalDate.of(2026, 10, 1), 15_000.0, Direction.DEBIT, "HEALTH", "hash", bank, false, null, Source.MANUAL),
        )
        val after = repo.state.value
        assertEquals(before.snapshot.cash - 15_000.0, after.snapshot.cash, 0.5)
        assertEquals(before.view.liquidCash - 15_000.0, after.view.liquidCash, 0.5)
    }

    @Test
    fun updateDeclaredRetriggersScoreAndVerdict() {
        val repo = FinancialHealthRepository()
        val before = repo.state.value
        repo.updateDeclared(before.snapshot.declared.copy(minReserve = 80_000.0))
        val after = repo.state.value
        assertEquals(80_000.0, after.snapshot.declared.minReserve, 0.0)
        assertTrue(after.score.buffer < before.score.buffer)
        assertTrue(after.verdict.summary.isNotBlank())
    }

    @Test
    fun sparsePersonaHasLowerCompleteness() {
        val full = FinancialHealthRepository(Persona.B, Scenario.STABLE_INCOME).state.value
        val sparse = FinancialHealthRepository(Persona.B_SPARSE, Scenario.STABLE_INCOME).state.value
        assertTrue(sparse.snapshot.completeness < full.snapshot.completeness)
        assertTrue(sparse.snapshot.missing.any { it.contains("rate") || it.contains("limit") || it.contains("minimum") })
    }
}
