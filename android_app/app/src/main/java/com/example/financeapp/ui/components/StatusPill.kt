package com.example.financeapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.financeapp.ui.theme.FinanceAppTheme
import com.example.financeapp.ui.theme.LabelS
import com.example.financeapp.ui.theme.PillRadius
import com.example.financeapp.ui.theme.RiskContainer
import com.example.financeapp.ui.theme.RiskText
import com.example.financeapp.ui.theme.SafeContainer
import com.example.financeapp.ui.theme.SafeText
import com.example.financeapp.ui.theme.Spacing
import com.example.financeapp.ui.theme.WatchContainer
import com.example.financeapp.ui.theme.WatchText

enum class HealthStatus(val label: String, val icon: ImageVector) {
    SAFE("Safe", Icons.Filled.CheckCircle),
    WATCH("Watch", Icons.Filled.Warning),
    RISK("Risk", Icons.Filled.Error),
}

@Composable
fun StatusPill(status: HealthStatus, modifier: Modifier = Modifier) {
    val (fg, bg) = when (status) {
        HealthStatus.SAFE -> SafeText to SafeContainer
        HealthStatus.WATCH -> WatchText to WatchContainer
        HealthStatus.RISK -> RiskText to RiskContainer
    }
    Row(
        modifier = modifier
            .height(24.dp)
            .background(bg, PillRadius)
            .padding(horizontal = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
    ) {
        Icon(status.icon, contentDescription = status.label, tint = fg, modifier = Modifier.size(14.dp))
        Text(status.label, style = LabelS, color = fg)
    }
}

@Preview(showBackground = true, name = "StatusPill light")
@Composable
private fun StatusPillLightPreview() {
    FinanceAppTheme(darkTheme = false) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), modifier = Modifier.padding(Spacing.md)) {
            HealthStatus.entries.forEach { StatusPill(it) }
        }
    }
}

@Preview(showBackground = true, name = "StatusPill dark", backgroundColor = 0xFF14110D)
@Composable
private fun StatusPillDarkPreview() {
    FinanceAppTheme(darkTheme = true) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), modifier = Modifier.padding(Spacing.md)) {
            HealthStatus.entries.forEach { StatusPill(it) }
        }
    }
}
