package com.example.financeapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.financeapp.ui.theme.Spacing
import com.example.financeapp.ui.theme.WatchInk
import com.example.financeapp.ui.theme.WatchWash

@Composable
fun ConflictBanner(statement: String, onResolve: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onResolve, modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = WatchWash, contentColor = WatchInk) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Icon(Icons.Outlined.WarningAmber, contentDescription = null)
                Text("Goal conflict", style = MaterialTheme.typography.titleMedium)
            }
            Text(statement, style = MaterialTheme.typography.bodyMedium)
            Text("Review options", style = MaterialTheme.typography.labelLarge)
        }
    }
}
