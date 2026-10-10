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
import com.example.financeapp.ui.util.formatInrCompact
import com.example.financeapp.ui.viewmodel.PeriodRowUi

private val tableHeaders = listOf("Period", "Income", "Essentials", "Debt", "SIP", "Discretionary", "Ending balance")

@Composable
fun PeriodTable(rows: List<PeriodRowUi>, modifier: Modifier = Modifier) {
    Surface(modifier, shape = MaterialTheme.shapes.large, border = BorderStroke(1.dp, Ink)) {
        Column(Modifier.horizontalScroll(rememberScrollState()).padding(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            TableRow(tableHeaders, header = true)
            rows.forEach { row ->
                TableRow(listOf(row.period, formatInrCompact(row.income), formatInrCompact(row.essentials), formatInrCompact(row.debt), formatInrCompact(row.sip), formatInrCompact(row.discretionary), formatInrCompact(row.endingBalance)))
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
