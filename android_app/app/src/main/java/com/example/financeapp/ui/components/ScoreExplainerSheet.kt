package com.example.financeapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.financeapp.domain.engine.HealthScoreComponent
import com.example.financeapp.ui.theme.NumberCompact
import com.example.financeapp.ui.theme.Spacing
import com.example.financeapp.ui.util.formatInr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreExplainerSheet(components: List<HealthScoreComponent>, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            Text("How your score is calculated", style = MaterialTheme.typography.headlineMedium)
            components.forEach { component ->
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(component.label, style = MaterialTheme.typography.titleMedium)
                        Text("${component.score.toInt()}/25", style = NumberCompact)
                    }
                    component.inputs.forEach { input ->
                        Text("${input.label}: ${input.value?.let(::formatInr) ?: "Not added"}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(component.reading, style = MaterialTheme.typography.bodyMedium)
                    Text(component.whatWouldRaiseThis, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
