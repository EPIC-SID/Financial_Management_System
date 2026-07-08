package com.example.financeapp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.financeapp.ui.screens.HomeScreen
import com.example.financeapp.ui.screens.LoginScreen
import com.example.financeapp.ui.screens.ProfileScreen
import com.example.financeapp.ui.screens.EditProfileScreen
import com.example.financeapp.ui.screens.SignUpScreen
import com.example.financeapp.ui.screens.TransactionsScreen
import com.example.financeapp.ui.screens.AiReviewScreen
import com.example.financeapp.ui.theme.BronzePrimary
import io.github.jan.supabase.gotrue.SessionStatus

@Composable
fun FinanceAppNavGraph() {
    val navController = rememberNavController()

    // Check if the user is already signed in
    var startDestination by remember { mutableStateOf("login") }
    var isCheckingAuth by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val sessionStatus = SupabaseClient.auth.sessionStatus.value
        if (sessionStatus is SessionStatus.Authenticated) {
            startDestination = "home"
        }
        isCheckingAuth = false
    }

    if (isCheckingAuth) {
        // Show a loading spinner while checking auth state
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BronzePrimary)
        }
        return
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate("signup")
                }
            )
        }
        composable("signup") {
            SignUpScreen(
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateBackToLogin = {
                    navController.popBackStack()
                }
            )
        }
        composable("home") {
            HomeScreen(
                onNavigateToTransactions = {
                    navController.navigate("transactions")
                },
                onNavigateToProfile = {
                    navController.navigate("profile")
                }
            )
        }
        composable("transactions") {
            TransactionsScreen(
                onNavigateBack = {
                    navController.popBackStack() // Or navigate("home")
                },
                onNavigateToProfile = {
                    navController.navigate("profile")
                }
            )
        }
        composable("profile") {
            ProfileScreen(
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = false }
                    }
                },
                onNavigateToTransactions = {
                    navController.navigate("transactions")
                },
                onNavigateToLogin = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true } // Clear entire backstack on sign out
                    }
                },
                onNavigateToEditProfile = {
                    navController.navigate("edit_profile")
                }
            )
        }
        composable("edit_profile") {
            EditProfileScreen(
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
