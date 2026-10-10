package com.example.financeapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.financeapp.ui.theme.Spacing

@Composable
fun AskPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, modifier = modifier.fillMaxWidth().heightIn(min = Spacing.minimumTouchTarget), shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null)
            Text("Ask Copilot", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
            Icon(Icons.Outlined.ChevronRight, contentDescription = null)
        }
    }
}
