package com.example.financeapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.financeapp.ui.theme.Ink
import com.example.financeapp.ui.theme.MutedInk
import com.example.financeapp.ui.theme.NumberCompact
import com.example.financeapp.ui.theme.Spacing
import com.example.financeapp.ui.viewmodel.MetricUi
import com.example.financeapp.ui.viewmodel.ObligationUi

@Composable
fun Next30DaysSummary(
    items: List<ObligationUi>,
    metrics: List<MetricUi>,
    modifier: Modifier = Modifier
) {
    Surface(modifier, shape = MaterialTheme.shapes.large, border = BorderStroke(1.dp, Ink)) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text("Next 30 days", style = MaterialTheme.typography.titleLarge)
            if (items.isEmpty()) {
                Text("No upcoming payments", style = MaterialTheme.typography.bodyMedium, color = MutedInk)
            } else {
                items.take(3).forEach { item ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(item.label, style = MaterialTheme.typography.bodyMedium)
                            Text(item.date, style = MaterialTheme.typography.bodySmall, color = MutedInk)
                        }
                        Text(item.amount, style = NumberCompact)
                    }
                }
            }
            if (metrics.isNotEmpty()) {
                Divider(color = Ink.copy(alpha = .12f))
                metrics.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        row.forEach { metric ->
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                                Text(metric.title, style = MaterialTheme.typography.bodySmall, color = MutedInk)
                                Text(metric.value, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        if (row.size == 1) Column(Modifier.weight(1f)) {}
                    }
                }
            }
        }
    }
}
