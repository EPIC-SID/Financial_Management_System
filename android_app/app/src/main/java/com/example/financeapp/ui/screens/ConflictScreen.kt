package com.example.financeapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.financeapp.ui.components.RupAiButton
import com.example.financeapp.ui.components.RupAiButtonVariant
import com.example.financeapp.ui.theme.ErrorInk
import com.example.financeapp.ui.theme.ErrorPink
import com.example.financeapp.ui.theme.FinanceAppTheme
import com.example.financeapp.ui.theme.Ink
import com.example.financeapp.ui.theme.Spacing
import com.example.financeapp.ui.theme.WatchWash
import com.example.financeapp.ui.util.formatPercent
import com.example.financeapp.ui.viewmodel.ConflictOptionUi
import com.example.financeapp.ui.viewmodel.ConflictUi
import com.example.financeapp.ui.viewmodel.PlanFixtures

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConflictScreen(conflict: ConflictUi, onApply: (ConflictOptionUi) -> Unit = {}, onKeepTarget: () -> Unit = {}) {
    Scaffold(topBar = { TopAppBar(title = { Text("Resolve goal conflict") }) }) { inner ->
        LazyColumn(
            Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            item {
                Surface(shape = MaterialTheme.shapes.large, color = WatchWash) {
                    Text(conflict.statement, Modifier.padding(Spacing.lg), style = MaterialTheme.typography.titleLarge)
                }
            }
            items(conflict.options.size) { index ->
                val option = conflict.options[index]
                ConflictOptionCard(option, { onApply(option) })
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    RupAiButton("Apply proposed relaxation", { onApply(conflict.options.first()) })
                    RupAiButton("Keep my target and show what breaks", onKeepTarget, variant = RupAiButtonVariant.OUTLINE)
                }
            }
        }
    }
}

@Composable
private fun ConflictOptionCard(option: ConflictOptionUi, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, border = BorderStroke(1.dp, Ink)) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(option.title, style = MaterialTheme.typography.titleMedium)
            if (option.notRecommended) {
                Surface(shape = MaterialTheme.shapes.extraSmall, color = ErrorPink, contentColor = ErrorInk) {
                    Text("Not recommended", Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xxs), style = MaterialTheme.typography.labelSmall)
                }
            }
            Text(option.detail, style = MaterialTheme.typography.bodyMedium)
            Text("Shortfall risk after · ${formatPercent(option.shortfallRiskAfter)}", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Preview(name = "Conflict · 360 · 100%", widthDp = 360, heightDp = 800, fontScale = 1f, showBackground = true)
@Preview(name = "Conflict · 412 · 100%", widthDp = 412, heightDp = 900, fontScale = 1f, showBackground = true)
@Preview(name = "Conflict · 360 · 200%", widthDp = 360, heightDp = 800, fontScale = 2f, showBackground = true)
@Preview(name = "Conflict · 412 · 200%", widthDp = 412, heightDp = 900, fontScale = 2f, showBackground = true)
@Composable
private fun ConflictPreview() {
    FinanceAppTheme { ConflictScreen(PlanFixtures.infeasible().conflict!!) }
}
