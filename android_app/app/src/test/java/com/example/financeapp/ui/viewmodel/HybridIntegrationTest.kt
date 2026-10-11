package com.example.financeapp.ui.viewmodel

import com.example.financeapp.data.ingest.Scenario
import com.example.financeapp.data.planning.DemoFinanceCalculator
import com.example.financeapp.data.remote.BackendClient
import com.example.financeapp.domain.ai.localization.LocalizedAdvisor
import com.example.financeapp.domain.model.CopilotIntent
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class HybridIntegrationTest {
    private val result by lazy { DemoFinanceCalculator().calculate(Scenario.STABLE_INCOME) }

    @Test fun screensMapComputedValuesAndNeverUsePreviewBalances() {
        val cockpit = result.cockpitUi()
        val plan = result.planUi(PlanKind.PLAN_A, true)
        assertEquals("Synthetic planning", cockpit.dataSource)
        assertTrue(cockpit.accounts.first().amount!!.contains("45,200"))
        assertEquals(6, plan.forecast.size)
        assertEquals(listOf(PlanPeriod.SIX_MONTHS), plan.availablePeriods)
        assertEquals(3, plan.comparisons.size)
        val assessment = result.decision.plans.first()
        assertEquals(assessment.cashBands[0].p10, plan.forecast[0].p10, 0.0)
        assertEquals(assessment.periods[0].ending, plan.rows[0].endingBalance, 0.0)
        assertEquals(assessment.shortfallRisk, plan.summary.shortfallRisk, 0.0)
    }

    @Test fun conflictDisablesApplyAndPreservesOptionFeasibility() {
        val demo = DemoFinanceCalculator().calculate(Scenario.INFEASIBLE_TARGET)
        val ui = demo.planUi(PlanKind.PLAN_A, false)
        assertFalse(ui.canApply)
        assertNotNull(ui.conflict)
        assertEquals(demo.conflict!!.relaxations.map { it.feasible }, ui.conflict!!.options.map { it.feasible })
    }

    @Test fun offlineCopilotShowsVerifiedDemoFactsNotTheFixture() = runBlocking {
        val client = BackendClient(transport = { _, _ -> error("offline") }, localizedAdvisor = LocalizedAdvisor::adviseBundle)
        val vm = CopilotViewModel({ result }, client, this)
        vm.submit("cash overview")
        yield()
        assertEquals(CopilotContentState.OFFLINE, vm.uiState.value.contentState)
        val answer = vm.uiState.value.messages.last().answer!!
        assertTrue(answer.observed.contains("45200"))
        assertFalse(answer.observed.contains("92,400"))
        assertTrue(answer.observedSource.contains("Synthetic"))
    }

    @Test fun inventedBackendNumbersFallBackAndRawQuestionNeverLeavesDevice() = runBlocking {
        var payload: JsonObject? = null
        val client = BackendClient(transport = { _, body ->
            payload = body
            buildJsonObject {
                put("observed", buildJsonArray { add(buildJsonObject { put("text", "Cash INR 999999999999") }) })
                put("predicted", buildJsonArray { add(buildJsonObject { put("text", "Risk 99%") }) })
                put("recommended", buildJsonArray { add(buildJsonObject { put("text", "Spend INR 888888888") }) })
            }
        }, localizedAdvisor = LocalizedAdvisor::adviseBundle)
        val vm = CopilotViewModel({ result }, client, this)
        vm.submit("cash overview account 123456789 person@example.com")
        yield()
        assertNotNull(payload)
        assertFalse(payload.toString().contains("123456789"))
        assertFalse(payload.toString().contains("person@example.com"))
        assertEquals(CopilotContentState.OFFLINE, vm.uiState.value.contentState)
        assertFalse(vm.uiState.value.messages.last().answer!!.observed.contains("999999999999"))
    }

    @Test fun languageChangeClearsOldAnswerAndReturnsRequestedScript() = runBlocking {
        val vm = CopilotViewModel({ result }, BackendClient(transport = { _, _ -> error("offline") },
            localizedAdvisor = LocalizedAdvisor::adviseBundle), this)
        vm.submit("cash")
        yield()
        vm.selectLanguage("hi-Deva")
        assertEquals(CopilotContentState.EMPTY, vm.uiState.value.contentState)
        vm.submit("cash")
        yield()
        assertTrue(vm.uiState.value.messages.last().answer!!.observed.contains("नकद"))
    }

    @Test fun missingFactsNeverCallBackendAndQuestionIsBounded() = runBlocking {
        var calls = 0
        val vm = CopilotViewModel({ null }, BackendClient(transport = { _, _ -> calls++; error("unexpected") }), this)
        vm.updateDraft("x".repeat(2000))
        assertEquals(1000, vm.uiState.value.draft.length)
        vm.submit("cash")
        assertEquals(CopilotContentState.ERROR, vm.uiState.value.contentState)
        assertEquals(0, calls)
    }

    @Test fun staleResponseCannotOverwriteNewScenario() = runBlocking {
        var snapshot = result
        val pending = CompletableDeferred<JsonObject>()
        val vm = CopilotViewModel({ snapshot }, BackendClient(transport = { _, _ -> pending.await() },
            localizedAdvisor = LocalizedAdvisor::adviseBundle), this)
        vm.submit("cash")
        yield()
        snapshot = DemoFinanceCalculator().calculate(Scenario.INCOME_REDUCTION)
        vm.invalidate()
        pending.complete(buildJsonObject { })
        yield()
        assertEquals(CopilotContentState.EMPTY, vm.uiState.value.contentState)
        assertTrue(vm.uiState.value.messages.isEmpty())
    }

    @Test fun unresolvedConflictBypassesRemoteRecommendation() = runBlocking {
        val demo = DemoFinanceCalculator().calculate(Scenario.INFEASIBLE_TARGET)
        var calls = 0
        val vm = CopilotViewModel({ demo }, BackendClient(transport = { _, _ -> calls++; error("unexpected") }), this)
        vm.submit("compare plans")
        yield()
        assertEquals(0, calls)
        if (demo.bundle != null) {
            assertEquals(CopilotContentState.OFFLINE, vm.uiState.value.contentState)
            assertTrue(vm.uiState.value.messages.last().answer!!.recommended.contains("Review"))
        } else assertEquals(CopilotContentState.ERROR, vm.uiState.value.contentState)
    }
}
