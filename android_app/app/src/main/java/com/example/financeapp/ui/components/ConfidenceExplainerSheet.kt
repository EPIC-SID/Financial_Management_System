package com.example.financeapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.financeapp.ui.theme.NumberXL
import com.example.financeapp.ui.theme.Spacing
import com.example.financeapp.ui.viewmodel.ConfidenceUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfidenceExplainerSheet(confidence: ConfidenceUi, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text("${confidence.label} confidence", style = MaterialTheme.typography.headlineMedium)
            Text(confidence.interval, style = NumberXL)
            Text(confidence.reading, style = MaterialTheme.typography.bodyLarge)
            Text(confidence.improvement, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Low: limited or stale inputs.\nMedium: useful inputs with some gaps.\nHigh: current, confirmed income and obligations.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
