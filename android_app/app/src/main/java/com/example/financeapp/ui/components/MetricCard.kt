package com.example.financeapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.financeapp.ui.theme.Elevation
import com.example.financeapp.ui.theme.FinanceAppTheme
import com.example.financeapp.ui.theme.NumberCompact
import com.example.financeapp.ui.theme.NumberXL
import com.example.financeapp.ui.theme.Spacing
import com.example.financeapp.ui.util.formatInr
import com.example.financeapp.ui.util.formatInrCompact

/** Callers supply formatted engine values, ranges, percentages, or explicit missing-data copy. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MetricCard(
    title: String,
    value: String,
    type: StatementType,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    delta: String? = null,
    status: HealthStatus? = null
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        tonalElevation = Elevation.base,
        shadowElevation = Elevation.base
    ) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Text(title, style = MaterialTheme.typography.bodyMedium)
                TypeTag(type)
            }
            Text(value, style = NumberXL)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (delta != null || status != null) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    delta?.let {
                        Surface(shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.surfaceVariant) {
                            Text(it, Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xxs), style = NumberCompact)
                        }
                    }
                    status?.let { StatusPill(it) }
                }
            }
        }
    }
}

/** Convenience overload taking raw numeric Double. */
@Composable
fun MetricCard(
    title: String,
    value: Double,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    delta: Double? = null,
    type: StatementType = StatementType.FACT,
    compact: Boolean = false,
) {
    MetricCard(
        title = title,
        value = if (compact) formatInrCompact(value) else formatInr(value),
        type = type,
        modifier = modifier,
        subtitle = subtitle,
        delta = delta?.let { "${if (it >= 0) "+" else ""}${formatInrCompact(it)}" },
        status = delta?.let { if (it < 0) HealthStatus.RISK else HealthStatus.SAFE }
    )
}

@Preview(name = "Metric cards · 100%", widthDp = 320, heightDp = 1000, fontScale = 1f, showBackground = true)
@Preview(name = "Metric cards · 130%", widthDp = 320, heightDp = 1400, fontScale = 1.3f, showBackground = true)
@Preview(name = "Metric cards · 200%", widthDp = 320, heightDp = 2000, fontScale = 2f, showBackground = true)
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Phase1CPreview() {
    FinanceAppTheme {
        Surface {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Text("Your money, in view.", style = MaterialTheme.typography.headlineLarge)
                Text("October · Synthetic demo", style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    StatementType.values().forEach { TypeTag(it) }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    HealthStatus.values().forEach { StatusPill(it) }
                }
                MetricCard(
                    title = "Observed cash · ₹45,200",
                    value = "₹45,200",
                    type = StatementType.FACT,
                    modifier = Modifier.fillMaxWidth()
                )
                MetricCard(
                    title = "Month-end cushion",
                    value = "₹18K–₹26K",
                    type = StatementType.PREDICTION,
                    modifier = Modifier.fillMaxWidth(),
                    subtitle = "After upcoming commitments",
                    delta = "Medium confidence"
                )
                MetricCard(
                    title = "A clear next step",
                    value = "Add data",
                    type = StatementType.RECOMMENDATION,
                    modifier = Modifier.fillMaxWidth(),
                    subtitle = "Add data to build your ledger."
                )
                MetricCard(
                    title = "Monthly income / Not added",
                    value = "Not added",
                    type = StatementType.FACT,
                    modifier = Modifier.fillMaxWidth(),
                    subtitle = "Add an income source to enable this estimate.",
                    status = HealthStatus.WATCH
                )
            }
        }
    }
}
