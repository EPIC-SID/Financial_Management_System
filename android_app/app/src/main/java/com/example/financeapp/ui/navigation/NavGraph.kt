package com.example.financeapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.financeapp.ui.screens.HomeScreen
import com.example.financeapp.ui.screens.OnboardingScreen
import com.example.financeapp.ui.screens.TransactionsScreen
import com.example.financeapp.ui.screens.AiReviewScreen

@Composable
fun FinanceAppNavGraph() {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = "onboarding") {
        composable("onboarding") {
            OnboardingScreen(
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            HomeScreen(
                onNavigateToTransactions = {
                    navController.navigate("transactions")
                }
            )
        }
        composable("transactions") {
            TransactionsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable("ai_review") {
            AiReviewScreen(
                onDismiss = {
                    navController.popBackStack()
                }
            )
        }
    }
}
