package com.example.financeapp.ui.viewmodel

import org.junit.Assert.assertNotEquals
import org.junit.Test

class CockpitViewModelTest {
    @Test
    fun scenarioSwitchUpdatesVerdictAndScore() {
        val viewModel = CockpitViewModel()
        val initial = viewModel.uiState.value

        viewModel.selectScenario("pressure")

        assertNotEquals(initial.verdict, viewModel.uiState.value.verdict)
        assertNotEquals(initial.healthScore, viewModel.uiState.value.healthScore)
    }
}
