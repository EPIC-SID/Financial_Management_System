package com.example.financeapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.financeapp.ui.icons.Icons
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.financeapp.ui.components.AccountsRow
import com.example.financeapp.ui.components.AccountsSheet
import com.example.financeapp.ui.components.AskPill
import com.example.financeapp.ui.components.BrandImageBackground
import com.example.financeapp.ui.components.CockpitQuickActions
import com.example.financeapp.ui.components.CompletenessCard
import com.example.financeapp.ui.components.ConfidenceExplainerSheet
import com.example.financeapp.ui.components.HealthGauge
import com.example.financeapp.ui.components.Next30DaysSummary
import com.example.financeapp.ui.components.QuickActionTile
import com.example.financeapp.ui.components.ScoreExplainerSheet
import com.example.financeapp.ui.components.StatusPill
import com.example.financeapp.ui.components.VerdictCard
import com.example.financeapp.ui.theme.ActionGreen
import com.example.financeapp.ui.theme.FinanceAppTheme
import com.example.financeapp.ui.theme.Ink
import com.example.financeapp.ui.theme.Radius
import com.example.financeapp.ui.theme.Spacing
import com.example.financeapp.ui.theme.SurfaceNeutral
import com.example.financeapp.ui.theme.WatchWash
import com.example.financeapp.ui.viewmodel.CockpitContentState
import com.example.financeapp.ui.viewmodel.CockpitFixtures
import com.example.financeapp.ui.viewmodel.CockpitUiState
import com.example.financeapp.ui.viewmodel.CockpitViewModel

@Composable
fun CockpitRoute(
    viewModel: CockpitViewModel = viewModel(),
    onAction: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    CockpitScreen(state, onScenarioSelected = viewModel::selectScenario, onAction = onAction)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CockpitScreen(
    state: CockpitUiState,
    onScenarioSelected: (String) -> Unit = {},
    onAction: (String) -> Unit = {},
    showHeader: Boolean = true
) {
    var menuOpen by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf<String?>(null) }
    BrandImageBackground(com.example.financeapp.R.drawable.bg_white, Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = if (showHeader) androidx.compose.material3.ScaffoldDefaults.contentWindowInsets else androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = { if (showHeader) {
            TopAppBar(
                title = { Text("Overview") },
                actions = {
                    Surface(shape = RoundedCornerShape(Radius.tag), color = SurfaceNeutral) {
                        Text(state.dataSource, Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xxs), style = MaterialTheme.typography.labelSmall)
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Outlined.MoreVert, "Choose synthetic scenario") }
                        DropdownMenu(menuOpen, { menuOpen = false }) {
                            com.example.financeapp.data.ingest.Scenario.entries.forEach { scenario ->
                                DropdownMenuItem({ Text(scenario.label) }, onClick = { menuOpen = false; onScenarioSelected(scenario.name) })
                            }
                            DropdownMenuItem({ Text("Freelancer demo") }, onClick = { menuOpen = false; onScenarioSelected("freelancer") })
                        }
                    }
                }
            )
        } }
    ) { inner ->
        if (state.contentState == CockpitContentState.SKELETON) CockpitSkeleton(Modifier.padding(inner)) else {
            LazyColumn(
                Modifier.fillMaxSize().padding(inner),
                contentPadding = PaddingValues(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                        Text("Your money at a glance", style = MaterialTheme.typography.headlineSmall)
                    }
                }
                item { VerdictCard(state.verdict, state.verdictReading, state.confidence.label,
                    { sheet = "confidence" }, { onAction("plan") }, heading = "Estimated bank balance after 6 months") }
                item {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        CockpitQuickActions.forEach { action -> QuickActionTile(action, { if (action.label == "Accounts") sheet = "accounts" else onAction(action.label) }) }
                    }
                }
                item { HealthGauge(state.healthScore, state.healthStatus, state.healthComponents, { sheet = "score" }) }
                item { AccountsRow(state.accounts, { sheet = "accounts" }) }
                item { Next30DaysSummary(state.obligations, state.metrics) }
                state.alert?.let { alert -> item { AlertBanner(alert) } }
                state.missingData?.let { reason -> item {
                    CompletenessCard(state.completeness, reason, state.contentState in listOf(CockpitContentState.NO_DATA, CockpitContentState.LOW_COMPLETENESS), { onAction("add data") })
                } }
                item { AskPill(onClick = { onAction("copilot") }) }
            }
        }
    }
    }
    when (sheet) {
        "accounts" -> AccountsSheet(state.accountDetails, state.cashAfterCardBills, { sheet = null }, { onAction("edit accounts") })
        "score" -> ScoreExplainerSheet(state.healthComponents) { sheet = null }
        "confidence" -> ConfidenceExplainerSheet(state.confidence) { sheet = null }
    }
}

@Composable
private fun AlertBanner(text: String) {
    Surface(shape = MaterialTheme.shapes.large, color = WatchWash) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            StatusPill(com.example.financeapp.ui.components.HealthStatus.WATCH)
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun CockpitSkeleton(modifier: Modifier = Modifier) {
    Column(modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        listOf(44.dp, 240.dp, 88.dp, 320.dp, 100.dp).forEach { height ->
            Surface(Modifier.fillMaxWidth().height(height), shape = MaterialTheme.shapes.large, color = SurfaceNeutral) {}
        }
    }
}

private class CockpitPreviewProvider : CollectionPreviewParameterProvider<CockpitUiState>(CockpitFixtures.all)

@Preview(name = "Cockpit · 360 · 100%", widthDp = 360, heightDp = 800, fontScale = 1f, showBackground = true)
@Preview(name = "Cockpit · 412 · 100%", widthDp = 412, heightDp = 900, fontScale = 1f, showBackground = true)
@Preview(name = "Cockpit · 360 · 130%", widthDp = 360, heightDp = 800, fontScale = 1.3f, showBackground = true)
@Preview(name = "Cockpit · 412 · 130%", widthDp = 412, heightDp = 900, fontScale = 1.3f, showBackground = true)
@Preview(name = "Cockpit · 360 · 200%", widthDp = 360, heightDp = 800, fontScale = 2f, showBackground = true)
@Preview(name = "Cockpit · 412 · 200%", widthDp = 412, heightDp = 900, fontScale = 2f, showBackground = true)
@Composable
private fun CockpitPreview(@PreviewParameter(CockpitPreviewProvider::class) state: CockpitUiState) {
    FinanceAppTheme { CockpitScreen(state) }
}
