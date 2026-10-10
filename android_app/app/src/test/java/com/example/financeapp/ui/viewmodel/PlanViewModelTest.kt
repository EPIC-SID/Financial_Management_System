package com.example.financeapp.ui.viewmodel

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanViewModelTest {
    @Test
    fun switchingPlanUpdatesChartCardAndTableWithin300ms() {
        val viewModel = PlanViewModel()
        val before = viewModel.uiState.value
        val started = System.nanoTime()

        viewModel.selectPlan(PlanKind.PLAN_B)

        val elapsedMs = (System.nanoTime() - started) / 1_000_000
        val after = viewModel.uiState.value
        assertTrue("Fixture switch took ${elapsedMs}ms", elapsedMs < 300)
        assertNotEquals(before.forecast, after.forecast)
        assertNotEquals(before.summary, after.summary)
        assertNotEquals(before.rows, after.rows)
    }

    @Test
    fun infeasibleFixtureIncludesEveryRequiredRelaxation() {
        val titles = PlanFixtures.infeasible().conflict!!.options.map { it.title }
        assertTrue(titles.containsAll(listOf("Lower the target", "Extend the timeline", "Reduce the cushion", "Pause the SIP")))
    }
}
