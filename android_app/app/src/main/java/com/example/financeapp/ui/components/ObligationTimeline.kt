package com.example.financeapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.financeapp.ui.theme.GuidanceYellow
import com.example.financeapp.ui.theme.Ink
import com.example.financeapp.ui.theme.NumberCompact
import com.example.financeapp.ui.theme.Spacing
import com.example.financeapp.ui.viewmodel.ObligationUi

@Composable
fun ObligationTimeline(items: List<ObligationUi>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text("Next 30 days", style = MaterialTheme.typography.titleLarge)
        if (items.isEmpty()) Text("No obligations yet", style = MaterialTheme.typography.bodyMedium)
        items.forEach { item ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Canvas(Modifier.size(16.dp)) { drawCircle(if (item.isToday) GuidanceYellow else Color.Transparent); drawCircle(Ink, radius = 4.dp.toPx()) }
                Column(Modifier.weight(1f)) {
                    Text(item.label, style = MaterialTheme.typography.bodyMedium)
                    Text(item.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(item.amount, style = NumberCompact)
            }
        }
    }
}
