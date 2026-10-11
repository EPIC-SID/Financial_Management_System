package com.example.financeapp.ui.navigation

import com.example.financeapp.data.ingest.Scenario
import com.example.financeapp.ui.viewmodel.AppDataMode

data class NavigationChoice(val route: String, val label: String)

fun navigationChoices(mode: AppDataMode): List<NavigationChoice> = buildList {
    if (mode == AppDataMode.DEMO) {
        add(NavigationChoice(Screen.Cockpit.route, "Overview"))
        add(NavigationChoice(Screen.Plan.route, "Plans"))
        add(NavigationChoice(Screen.Copilot.route, "Ask AI"))
    }
    add(NavigationChoice(Screen.Transactions.route, "Activity"))
    add(NavigationChoice(Screen.Profile.route, "Profile"))
}

fun destinationFor(mode: AppDataMode, route: String): String =
    if (mode == AppDataMode.LIVE && route in listOf(Screen.Cockpit.route, Screen.Plan.route,
        Screen.Copilot.route, Screen.Conflict.route, Screen.Home.route, Screen.AiInsights.route)) Screen.Transactions.route else route

fun scenarioTitle(scenario: Scenario): String = when (scenario) {
    Scenario.STABLE_INCOME -> "Regular income"
    Scenario.INCOME_REDUCTION -> "Income drops"
    Scenario.UNEXPECTED_EXPENSE -> "Unexpected expense"
    Scenario.INFEASIBLE_TARGET -> "Goal needs a change"
    Scenario.HIGH_UNCERTAINTY -> "Income varies"
}
