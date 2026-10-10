package com.example.financeapp.ui.navigation

/**
 * Type-safe navigation route definitions.
 * Replaces hardcoded string routes to prevent typos and enable compile-time safety.
 */
sealed class Screen(val route: String) {
    data object Cockpit : Screen("cockpit")
    data object Plan : Screen("plan")
    data object Copilot : Screen("copilot")
    data object Conflict : Screen("conflict")
    data object Login : Screen("login")
    data object SignUp : Screen("signup")
    data object Home : Screen("home")
    data object Transactions : Screen("transactions")
    data object Profile : Screen("profile")
    data object EditProfile : Screen("edit_profile")
    data object AiReview : Screen("ai_review")
    data object AiInsights : Screen("ai_insights")
}
