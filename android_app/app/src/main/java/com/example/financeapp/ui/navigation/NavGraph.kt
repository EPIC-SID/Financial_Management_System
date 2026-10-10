package com.example.financeapp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.financeapp.data.SupabaseClient
import com.example.financeapp.ui.screens.*
import com.example.financeapp.ui.theme.ActionGreen
import com.example.financeapp.ui.viewmodel.PlanFixtures
import io.github.jan.supabase.gotrue.SessionStatus

@Composable
fun FinanceAppNavGraph() {
    val navController = rememberNavController()

    // Default start destination directly to Cockpit for Hackathon demonstration
    val startDestination = Screen.Cockpit.route

    NavHost(navController = navController, startDestination = startDestination) {
        // Core Hackathon Stages 3A–3C Screens
        composable(Screen.Cockpit.route) {
            CockpitRoute(
                onAction = { action ->
                    when (action) {
                        "cockpit" -> { /* Already on cockpit */ }
                        "plan" -> navController.navigate(Screen.Plan.route)
                        "copilot" -> navController.navigate(Screen.Copilot.route)
                        "activity" -> navController.navigate(Screen.Transactions.route)
                        "profile" -> navController.navigate(Screen.Profile.route)
                    }
                }
            )
        }

        composable(Screen.Plan.route) {
            PlanRoute(
                onResolveConflict = {
                    navController.navigate(Screen.Conflict.route)
                }
            )
        }

        composable(Screen.Copilot.route) {
            CopilotRoute()
        }

        composable(Screen.Conflict.route) {
            val conflict = PlanFixtures.infeasible().conflict ?: error("No conflict")
            ConflictScreen(
                conflict = conflict,
                onKeepTarget = { navController.popBackStack() },
                onApply = { navController.popBackStack() }
            )
        }

        // Supporting & Authentication Flows
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Cockpit.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate(Screen.SignUp.route)
                }
            )
        }

        composable(Screen.SignUp.route) {
            SignUpScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Cockpit.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateBackToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToTransactions = {
                    navController.navigate(Screen.Transactions.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToAiInsights = {
                    navController.navigate(Screen.AiInsights.route)
                }
            )
        }

        composable(Screen.Transactions.route) {
            TransactionsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Cockpit.route) {
                        popUpTo(Screen.Cockpit.route) { inclusive = false }
                    }
                },
                onNavigateToTransactions = {
                    navController.navigate(Screen.Transactions.route)
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToEditProfile = {
                    navController.navigate(Screen.EditProfile.route)
                }
            )
        }

        composable(Screen.EditProfile.route) {
            EditProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.AiReview.route) {
            AiReviewScreen(
                onDismiss = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.AiInsights.route) {
            AiInsightsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
