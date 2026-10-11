package com.example.financeapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.financeapp.ui.theme.Ink
import com.example.financeapp.ui.theme.Spacing
import com.example.financeapp.ui.util.formatInr
import com.example.financeapp.ui.util.formatRiskPercent
import com.example.financeapp.ui.viewmodel.PeriodRowUi

private val tableHeaders = listOf("Month", "Income", "Essential costs", "Debt payments", "Monthly investing", "Other spending", "Planned balance", "Low estimate", "Typical estimate", "High estimate", "Risk of low cash")

@Composable
fun PeriodTable(rows: List<PeriodRowUi>, modifier: Modifier = Modifier) {
    Surface(modifier, shape = MaterialTheme.shapes.large, border = BorderStroke(1.dp, Ink)) {
        Column(Modifier.horizontalScroll(rememberScrollState()).padding(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            TableRow(tableHeaders, header = true)
            rows.forEach { row ->
                val forecast = row.forecast
                TableRow(listOf(row.period, formatInr(row.income), formatInr(row.essentials), formatInr(row.debt),
                    formatInr(row.sip), formatInr(row.discretionary), formatInr(row.endingBalance),
                    forecast?.let { formatInr(it.p10) } ?: "-", forecast?.let { formatInr(it.p50) } ?: "-",
                    forecast?.let { formatInr(it.p90) } ?: "-", forecast?.let { formatRiskPercent(it.shortfallChance) } ?: "-"))
            }
        }
    }
}

@Composable
private fun TableRow(values: List<String>, header: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        values.forEachIndexed { index, value ->
            Text(value, Modifier.width(if (index == 0) 92.dp else 108.dp), style = if (header) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium)
        }
    }
}
