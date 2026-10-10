package com.example.financeapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.financeapp.ui.theme.Spacing

data class QuickAction(val label: String, val icon: ImageVector)

val CockpitQuickActions = listOf(
    QuickAction("Add entry", Icons.Outlined.AddCircleOutline),
    QuickAction("What-if", Icons.Outlined.Tune),
    QuickAction("Ask Copilot", Icons.Outlined.AutoAwesome),
    QuickAction("Accounts", Icons.Outlined.Payments)
)

@Composable
fun QuickActionTile(action: QuickAction, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, modifier = modifier.widthIn(min = 104.dp).heightIn(min = 80.dp), shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Column(Modifier.padding(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Icon(action.icon, contentDescription = null)
            Text(action.label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
