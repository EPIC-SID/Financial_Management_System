package com.example.financeapp.ui.navigation

import com.example.financeapp.data.ingest.Scenario
import com.example.financeapp.ui.viewmodel.AppDataMode
import org.junit.Assert.*
import org.junit.Test

class NavigationChoicesTest {
    @Test fun sampleModeContainsEveryMainScreenOnce() {
        val choices = navigationChoices(AppDataMode.DEMO)
        assertEquals(listOf("Overview", "Plans", "Ask AI", "Activity", "Profile"), choices.map { it.label })
        assertEquals(choices.size, choices.map { it.route }.distinct().size)
    }
    @Test fun liveModeNeverOffersSamplePlanning() {
        assertEquals(listOf(Screen.Transactions.route, Screen.Profile.route), navigationChoices(AppDataMode.LIVE).map { it.route })
        for (route in listOf(Screen.Cockpit.route, Screen.Plan.route, Screen.Copilot.route, Screen.Conflict.route, Screen.Home.route, Screen.AiInsights.route))
            assertEquals(Screen.Transactions.route, destinationFor(AppDataMode.LIVE, route))
    }
    @Test fun modeGuardDoesNotBlockAccountOrOnboardingRoutes() {
        for (mode in AppDataMode.entries) for (route in listOf(Screen.Profile.route, Screen.EditProfile.route, Screen.Login.route, Screen.SignUp.route, Screen.Onboarding.route))
            assertEquals(route, destinationFor(mode, route))
    }
    @Test fun everySampleSituationHasReadableDistinctName() {
        val titles = Scenario.entries.map(::scenarioTitle)
        assertEquals(titles.size, titles.distinct().size)
        assertTrue(titles.none { it.contains('_') || it.contains('%') || it.isBlank() })
    }
}
