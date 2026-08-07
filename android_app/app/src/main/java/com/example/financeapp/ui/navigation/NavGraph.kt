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
import com.example.financeapp.ui.screens.AiInsightsScreen
import com.example.financeapp.ui.theme.BronzePrimary
import io.github.jan.supabase.gotrue.SessionStatus

@Composable
fun FinanceAppNavGraph() {
    val navController = rememberNavController()

    // Track auth state — wait for Supabase to finish restoring session from SharedPreferences
    var startDestination by remember { mutableStateOf(Screen.Login.route) }
    var isCheckingAuth by remember { mutableStateOf(true) }

    // Collect the session status flow to properly wait for LoadingFromStorage to resolve.
    // On cold start: status goes LoadingFromStorage → Authenticated (if saved) or NotAuthenticated (if no token).
    // We must NOT check .value immediately — it may still be LoadingFromStorage at that point.
    val sessionStatus by SupabaseClient.auth.sessionStatus.collectAsState()

    LaunchedEffect(sessionStatus) {
        when (sessionStatus) {
            is SessionStatus.Authenticated -> {
                if (isCheckingAuth) {
                    // First resolution on launch — user has a valid saved session, go straight to Home
                    startDestination = Screen.Home.route
                    isCheckingAuth = false
                }
                // If already past auth check, do nothing (user is already on Home)
            }
            is SessionStatus.NotAuthenticated -> {
                if (isCheckingAuth) {
                    // No saved session found — show Login
                    isCheckingAuth = false
                } else {
                    // Session expired or user signed out mid-session — redirect to Login
                    val currentRoute = navController.currentDestination?.route
                    if (currentRoute != null && currentRoute != Screen.Login.route && currentRoute != Screen.SignUp.route) {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                }
            }
            is SessionStatus.LoadingFromStorage -> {
                // Still restoring session from SharedPreferences — keep spinner visible
            }
            else -> {
                // NetworkError or other transient states — keep spinner, will resolve shortly
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
        composable(Screen.AiInsights.route) {
            AiInsightsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
