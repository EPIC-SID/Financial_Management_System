package com.example.financeapp.data

import com.example.financeapp.data.ingest.Persona
import com.example.financeapp.data.ingest.Scenario
import com.example.financeapp.data.ingest.SyntheticDataSeeder
import com.example.financeapp.data.ingest.SyntheticDataset
import com.example.financeapp.domain.engine.ConsolidatedViewBuilder
import com.example.financeapp.domain.engine.HealthScore
import com.example.financeapp.domain.engine.HealthScoreBreakdown
import com.example.financeapp.domain.engine.Verdict
import com.example.financeapp.domain.engine.VerdictEngine
import com.example.financeapp.domain.model.ConsolidatedView
import com.example.financeapp.domain.model.DeclaredInputs
import com.example.financeapp.domain.model.FinancialSnapshot
import com.example.financeapp.domain.model.Txn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HealthState(
    val snapshot: FinancialSnapshot,
    val view: ConsolidatedView,
    val score: HealthScoreBreakdown,
    val verdict: Verdict,
)

/**
 * In-memory source of truth for Financial Health. Derived score/view/verdict recompute on every change.
 * shortcut: no Room; 2E detectors not wired — snapshot uses seeder ground truth.
 */
class FinancialHealthRepository(
    initialPersona: Persona = Persona.B,
    initialScenario: Scenario = Scenario.STABLE_INCOME,
) {
    private var persona = initialPersona
    private var scenario = initialScenario
    private var extra = emptyList<Txn>()
    private var declaredOverride: DeclaredInputs? = null
    private var dataset: SyntheticDataset = SyntheticDataSeeder.generate(persona, scenario)

    private val _state = MutableStateFlow(compute())
    val state: StateFlow<HealthState> = _state.asStateFlow()

    fun switchScenario(next: Scenario, nextPersona: Persona = persona) {
        persona = nextPersona
        scenario = next
        extra = emptyList()
        declaredOverride = null
        dataset = SyntheticDataSeeder.generate(persona, scenario)
        _state.value = compute()
    }

    fun injectTransaction(tx: Txn) {
        require(tx.amount.isFinite() && tx.amount > 0) { "amount must be a positive number" }
        extra = extra + tx
        _state.value = compute()
    }

    fun updateDeclared(declared: DeclaredInputs) {
        require(declared.minReserve.isFinite() && declared.minReserve >= 0)
        declaredOverride = declared
        _state.value = compute()
    }

    private fun compute(): HealthState {
        val snapshot = SnapshotBuilder.from(dataset, extra, declaredOverride ?: dataset.declared)
        return HealthState(
            snapshot = snapshot,
            view = ConsolidatedViewBuilder.from(snapshot),
            score = HealthScore.of(snapshot),
            verdict = VerdictEngine.run(snapshot),
        )
    }

    companion object {
        val instance by lazy { FinancialHealthRepository() }
    }
}
