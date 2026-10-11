package com.example.financeapp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.financeapp.ui.icons.Icons
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.platform.LocalContext
import com.example.financeapp.ui.screens.onboarding.OnboardingRoute
import com.example.financeapp.ui.screens.*
import com.example.financeapp.ui.viewmodel.*
import com.example.financeapp.data.ingest.Persona
import com.example.financeapp.data.ingest.Scenario
import com.example.financeapp.ui.components.WhatIfSheet
import com.example.financeapp.ui.components.AppNavigationShell
import com.example.financeapp.domain.freelancer.FreelancerPlanId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceAppNavGraph() {
    val navController = rememberNavController()
    val hybrid: HybridViewModel = viewModel()
    val state by hybrid.state.collectAsState()
    val copilot: CopilotViewModel = viewModel(factory = remember(hybrid) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CopilotViewModel(snapshotProvider = { hybrid.state.value.result }) as T
        }
    })
    var showWhatIf by remember { mutableStateOf(false) }
    var mode by rememberSaveable { mutableStateOf(AppDataMode.DEMO) }
    LaunchedEffect(state.result) { copilot.invalidate(); showWhatIf = false }
    fun navigate(route: String) {
        val destination = destinationFor(mode, route)
        navController.navigate(destination) { launchSingleTop = true }
    }
    fun replaceLegacy(route: String, target: String) {
        val destination = if (mode == AppDataMode.LIVE && target == Screen.Copilot.route ||
            mode == AppDataMode.LIVE && target == Screen.Cockpit.route) Screen.Transactions.route else target
        navController.navigate(destination) {
            popUpTo(route) { inclusive = true }
            launchSingleTop = true
        }
    }

    val context = LocalContext.current
    val authSession by com.example.financeapp.data.SupabaseClient.auth.sessionStatus.collectAsState()
    val accountOwner = remember(authSession) { com.example.financeapp.data.SupabaseClient.auth.currentUserOrNull()?.id }
    val setupPrefs = remember { context.getSharedPreferences("app_setup", android.content.Context.MODE_PRIVATE) }
    val startDestination = remember { if (setupPrefs.getBoolean("onboarding_complete_v1", false)) Screen.Cockpit.route else Screen.Onboarding.route }
    val entry by navController.currentBackStackEntryAsState()
    val setupOrAuth = entry?.destination?.route in listOf(Screen.Onboarding.route, Screen.Login.route, Screen.SignUp.route)

    AppNavigationShell(visible = !setupOrAuth, mode = mode, route = entry?.destination?.route,
        onNavigate = ::navigate, onBack = { navController.popBackStack() }) { shellPadding ->
    NavHost(navController = navController, startDestination = startDestination, modifier = Modifier.fillMaxSize().padding(shellPadding)) {
        composable(Screen.Onboarding.route) {
            OnboardingRoute(onExistingAccount = { navigate(Screen.Login.route) },
                onCreateAccount = { navigate(Screen.SignUp.route) }, onComplete = { source ->
                setupPrefs.edit().putBoolean("onboarding_complete_v1", true).apply()
                copilot.invalidate()
                hybrid.clearSessionSelections()
                mode = if (source == OnboardingDataSource.SMS) AppDataMode.LIVE else AppDataMode.DEMO
                navController.navigate(if (mode == AppDataMode.LIVE) Screen.Transactions.route else Screen.Cockpit.route) {
                    popUpTo(navController.graph.id)
                    launchSingleTop = true
                }
            })
        }
        // Core Hackathon Stages 3A–3C Screens
        composable(Screen.Cockpit.route) {
            val result = state.result
            if (mode == AppDataMode.LIVE) { LaunchedEffect(Unit) { navigate(Screen.Transactions.route) } }
            else if (result == null) IntegrationStatus(state.loading, state.error, hybrid::retry)
            else CockpitScreen(
                state = result.cockpitUi(),
                showHeader = false,
                onScenarioSelected = { id ->
                    if (id == "freelancer") hybrid.selectScenario(Scenario.STABLE_INCOME, Persona.A)
                    else Scenario.entries.firstOrNull { it.name == id }?.let { hybrid.selectScenario(it) }
                },
                onAction = { action ->
                    when (action) {
                        "cockpit" -> { /* Already on cockpit */ }
                        "plan", "Plan", "What-if", "Try a change" -> { navigate(Screen.Plan.route); if (action in listOf("What-if", "Try a change")) showWhatIf = true }
                        "copilot", "Ask Copilot", "Ask AI" -> navigate(Screen.Copilot.route)
                        "activity", "Add entry", "Transactions" -> navigate(Screen.Transactions.route)
                        "profile" -> navController.navigate(Screen.Profile.route)
                        "add data", "edit accounts" -> navigate(Screen.Transactions.route)
                    }
                }
            )
        }

        composable(Screen.Plan.route) {
            val result = state.result
            if (mode == AppDataMode.LIVE) { LaunchedEffect(Unit) { navigate(Screen.Transactions.route) } }
            else if (result == null) IntegrationStatus(state.loading, state.error, hybrid::retry)
            else PlanScreen(result.planUi(state.plan, state.compare, state.appliedPlan, state.savedSimulation != null),
                onPlanSelected = hybrid::selectPlan, onCompareChanged = hybrid::compare,
                onTryWhatIf = { showWhatIf = true }, onApplyPlan = hybrid::applyPlan,
                onResolveConflict = { navigate(Screen.Conflict.route) }, showHeader = false)
        }

        composable(Screen.Copilot.route) {
            if (mode == AppDataMode.LIVE) { LaunchedEffect(Unit) { navigate(Screen.Transactions.route) } }
            else CopilotRoute(viewModel = copilot, onLaunchWhatIf = { showWhatIf = true },
                onOpenPlan = { navigate(Screen.Plan.route) }, showHeader = false)
        }

        composable(Screen.Conflict.route) {
            val conflict = state.result?.planUi(state.plan, state.compare)?.conflict
            if (conflict == null) IntegrationStatus(false, "No unresolved demo goal conflict.") { navController.popBackStack() }
            else ConflictScreen(
                conflict = conflict,
                showHeader = false,
                onKeepTarget = { navController.popBackStack() },
                onApply = { option ->
                    val index = conflict.options.indexOf(option)
                    if (option.feasible && index >= 0) { hybrid.applyRelaxation(index); navController.popBackStack() }
                }
            )
        }

        // Supporting & Authentication Flows
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(navController.graph.id)
                        launchSingleTop = true
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate(Screen.SignUp.route) { popUpTo(Screen.Login.route) { inclusive = true } }
                },
                onBack = { if (!navController.popBackStack()) navigate(Screen.Onboarding.route) }
            )
        }

        composable(Screen.SignUp.route) {
            SignUpScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(navController.graph.id)
                        launchSingleTop = true
                    }
                },
                onNavigateBackToLogin = {
                    navController.navigate(Screen.Login.route) { popUpTo(Screen.SignUp.route) { inclusive = true } }
                },
                onBack = { if (!navController.popBackStack()) navigate(Screen.Onboarding.route) }
            )
        }

        composable(Screen.Home.route) {
            LaunchedEffect(Unit) { replaceLegacy(Screen.Home.route, Screen.Cockpit.route) }
        }

        composable(Screen.Transactions.route) {
            key(mode) { UnifiedActivityRoute(mode, if (mode == AppDataMode.DEMO) state.result?.dataset else null, showHeader = false) }
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateToHome = {
                    navigate(if (mode == AppDataMode.LIVE) Screen.Transactions.route else Screen.Cockpit.route)
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
                },
                onEditSetup = { navigate(Screen.Onboarding.route) }, showHeader = false
            )
        }

        composable(Screen.EditProfile.route) {
            key(accountOwner) { EditProfileScreen(
                showHeader = false, onNavigateBack = {
                    navController.popBackStack()
                }
            ) }
        }

        composable(Screen.AiReview.route) {
            LaunchedEffect(Unit) { replaceLegacy(Screen.AiReview.route, Screen.Transactions.route) }
        }

        composable(Screen.AiInsights.route) {
            LaunchedEffect(Unit) { replaceLegacy(Screen.AiInsights.route, Screen.Copilot.route) }
        }
    }
    }
    val result = state.result
    if (mode == AppDataMode.DEMO && showWhatIf && result != null) {
        WhatIfSheet(inputs = result.inputs,
            baselineShortfallRisk = result.decision.plans.single { it.id == FreelancerPlanId.STATUS_QUO }.shortfallRisk,
            onDismissRequest = { showWhatIf = false }, initialChange = state.savedSimulation ?: com.example.financeapp.domain.engine.WhatIfChange(),
            applyLabel = "Save simulation (demo only)",
            onApplyToPlan = { change -> hybrid.saveSimulation(change); showWhatIf = false; navigate(Screen.Plan.route) })
    }
}

@Composable
private fun IntegrationStatus(loading: Boolean, message: String?, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (loading) CircularProgressIndicator()
            Text(message ?: "Calculating synthetic demo...", Modifier.padding(16.dp))
            if (!loading) TextButton(onClick = onRetry) { Text("Retry / return") }
        }
    }
}
