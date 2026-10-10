package com.example.financeapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.financeapp.ui.components.ConflictBanner
import com.example.financeapp.ui.components.ForecastFanChart
import com.example.financeapp.ui.components.PeriodTable
import com.example.financeapp.ui.components.PlanCard
import com.example.financeapp.ui.components.PlanSwitch
import com.example.financeapp.ui.components.RupAiButton
import com.example.financeapp.ui.components.RupAiButtonVariant
import com.example.financeapp.ui.components.StatementType
import com.example.financeapp.ui.components.TypeTag
import com.example.financeapp.ui.theme.FinanceAppTheme
import com.example.financeapp.ui.theme.GuidanceYellow
import com.example.financeapp.ui.theme.Ink
import com.example.financeapp.ui.theme.Spacing
import com.example.financeapp.ui.theme.SurfaceNeutral
import com.example.financeapp.ui.viewmodel.PlanContentState
import com.example.financeapp.ui.viewmodel.PlanFixtures
import com.example.financeapp.ui.viewmodel.PlanKind
import com.example.financeapp.ui.viewmodel.PlanPeriod
import com.example.financeapp.ui.viewmodel.PlanUiState
import com.example.financeapp.ui.viewmodel.PlanViewModel
import kotlinx.coroutines.launch

@Composable
fun PlanRoute(viewModel: PlanViewModel = viewModel(), onTryWhatIf: () -> Unit = {}, onResolveConflict: () -> Unit = {}) {
    val state by viewModel.uiState.collectAsState()
    PlanScreen(state, viewModel::selectPeriod, viewModel::selectPlan, viewModel::setCompareMode, onTryWhatIf, onResolveConflict)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    state: PlanUiState,
    onPeriodSelected: (PlanPeriod) -> Unit = {},
    onPlanSelected: (PlanKind) -> Unit = {},
    onCompareChanged: (Boolean) -> Unit = {},
    onTryWhatIf: () -> Unit = {},
    onResolveConflict: () -> Unit = {}
) {
    var showTable by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    Scaffold(topBar = { TopAppBar(title = { Text("Plan") }) }, snackbarHost = { SnackbarHost(snackbar) }) { inner ->
        if (state.contentState == PlanContentState.COMPUTING) {
            PlanSkeleton(Modifier.padding(inner))
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(inner),
                contentPadding = PaddingValues(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text("Period", style = MaterialTheme.typography.labelLarge)
                        PlanSwitch(PlanPeriod.entries, state.period, PlanPeriod::label, onPeriodSelected)
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text("Approach", style = MaterialTheme.typography.labelLarge)
                        PlanSwitch(PlanKind.entries, state.plan, PlanKind::label, onPlanSelected)
                    }
                }
                state.confidenceNote?.let { note -> item { LowConfidenceNote(note) } }
                item { ForecastFanChart(state.forecast, showTable, { showTable = it }) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        TextButton(onClick = { showTable = !showTable }) { Text(if (showTable) "Hide period details" else "Show period details") }
                        if (showTable) PeriodTable(state.rows)
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Compare mode", style = MaterialTheme.typography.titleMedium)
                        Switch(checked = state.compareMode, onCheckedChange = onCompareChanged)
                    }
                }
                if (state.compareMode) {
                    item { PlanCard(PlanKind.PLAN_A, PlanFixtures.state(state.period, PlanKind.PLAN_A).summary) }
                    item { PlanCard(PlanKind.PLAN_B, PlanFixtures.state(state.period, PlanKind.PLAN_B).summary) }
                } else item { PlanCard(state.plan, state.summary) }
                item { WhyThisPlan(state.summary.why) }
                state.conflict?.let { conflict -> item { ConflictBanner(conflict.statement, onResolveConflict) } }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        RupAiButton("Apply plan", { scope.launch { snackbar.showSnackbar("Plan applied") } })
                        RupAiButton("Try what-if", onTryWhatIf, variant = RupAiButtonVariant.OUTLINE)
                    }
                }
            }
        }
    }
}

@Composable
private fun WhyThisPlan(text: String) {
    Surface(shape = MaterialTheme.shapes.large, color = GuidanceYellow.copy(alpha = .22f), contentColor = Ink) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            TypeTag(StatementType.RECOMMENDATION)
            Text("Why this plan", style = MaterialTheme.typography.titleMedium)
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun LowConfidenceNote(text: String) {
    Surface(shape = MaterialTheme.shapes.large, color = SurfaceNeutral, border = BorderStroke(1.dp, Ink)) {
        Text(text, Modifier.padding(Spacing.md), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PlanSkeleton(modifier: Modifier = Modifier) {
    Column(modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        listOf(52.dp, 52.dp, 290.dp, 180.dp, 96.dp).forEach { height ->
            Surface(Modifier.fillMaxWidth().height(height), shape = MaterialTheme.shapes.large, color = SurfaceNeutral) {}
        }
    }
}

private class PlanPreviewProvider : CollectionPreviewParameterProvider<PlanUiState>(PlanFixtures.all)

@Preview(name = "Plan states · 360 · 100%", widthDp = 360, heightDp = 800, fontScale = 1f, showBackground = true)
@Preview(name = "Plan states · 412 · 100%", widthDp = 412, heightDp = 900, fontScale = 1f, showBackground = true)
@Preview(name = "Plan states · 360 · 200%", widthDp = 360, heightDp = 800, fontScale = 2f, showBackground = true)
@Preview(name = "Plan states · 412 · 200%", widthDp = 412, heightDp = 900, fontScale = 2f, showBackground = true)
@Composable
private fun PlanPreview(@PreviewParameter(PlanPreviewProvider::class) state: PlanUiState) {
    FinanceAppTheme { PlanScreen(state) }
}
