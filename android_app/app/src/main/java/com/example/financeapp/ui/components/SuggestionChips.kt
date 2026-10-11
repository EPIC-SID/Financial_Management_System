package com.example.financeapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.financeapp.ui.theme.CanvasWhite
import com.example.financeapp.ui.theme.Ink
import com.example.financeapp.ui.theme.Spacing

@Composable
fun SuggestionChips(suggestions: List<String>, onSuggestion: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        suggestions.forEach { suggestion ->
            AssistChip(
                onClick = { onSuggestion(suggestion) },
                label = { Text(suggestion, style = MaterialTheme.typography.labelLarge) },
                modifier = Modifier.heightIn(min = Spacing.minimumTouchTarget),
                border = AssistChipDefaults.assistChipBorder(borderColor = Ink, borderWidth = 1.dp),
                colors = AssistChipDefaults.assistChipColors(containerColor = CanvasWhite, labelColor = Ink)
            )
        }
    }
}
