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
    var startDestination by remember { mutableStateOf(Screen.Login.route) }
    var isCheckingAuth by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val sessionStatus = SupabaseClient.auth.sessionStatus.value
        if (sessionStatus is SessionStatus.Authenticated) {
            startDestination = Screen.Home.route
        }
        isCheckingAuth = false
    }

    // Monitor session status — redirect to login on token expiry or sign out if user was in-app
    val sessionStatus by SupabaseClient.auth.sessionStatus.collectAsState()
    LaunchedEffect(sessionStatus) {
        if (!isCheckingAuth && sessionStatus is SessionStatus.NotAuthenticated) {
            val currentRoute = navController.currentDestination?.route
            if (currentRoute != null && currentRoute != Screen.Login.route && currentRoute != Screen.SignUp.route) {
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Home.route) { inclusive = true }
                }
            }
        }
    }

    if (isCheckingAuth) {
        // Show a loading spinner while checking auth state
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BronzePrimary)
        }
        return
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
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
                    navController.navigate(Screen.Home.route) {
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
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                },
                onNavigateToTransactions = {
                    navController.navigate(Screen.Transactions.route)
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true } // Clear entire backstack on sign out
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
    }
}
