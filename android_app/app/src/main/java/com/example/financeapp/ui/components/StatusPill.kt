package com.example.financeapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.financeapp.ui.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.example.financeapp.ui.theme.FinanceAppTheme
import com.example.financeapp.ui.theme.Ink
import com.example.financeapp.ui.theme.Radius
import com.example.financeapp.ui.theme.ErrorInk
import com.example.financeapp.ui.theme.ErrorPink
import com.example.financeapp.ui.theme.Spacing
import com.example.financeapp.ui.theme.SummaryGreen
import com.example.financeapp.ui.theme.WatchInk
import com.example.financeapp.ui.theme.WatchWash

enum class HealthStatus(val label: String) {
    SAFE("Safe"),
    WATCH("Watch"),
    RISK("Risk"),
}

@Composable
fun StatusPill(status: HealthStatus, modifier: Modifier = Modifier) {
    val (label, icon) = when (status) {
        HealthStatus.SAFE -> "Safe" to Icons.Outlined.CheckCircle
        HealthStatus.WATCH -> "Watch" to Icons.Outlined.WarningAmber
        HealthStatus.RISK -> "Risk" to Icons.Outlined.ErrorOutline
    }
    val foreground = when (status) {
        HealthStatus.SAFE -> Ink
        HealthStatus.WATCH -> WatchInk
        HealthStatus.RISK -> ErrorInk
    }
    val background = when (status) {
        HealthStatus.SAFE -> SummaryGreen
        HealthStatus.WATCH -> WatchWash
        HealthStatus.RISK -> ErrorPink
    }
    Surface(
        modifier = modifier.semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(Radius.tag),
        color = background,
        contentColor = foreground
    ) {
        Row(
            modifier = Modifier.heightIn(min = 24.dp).padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

class HealthStatusPreviewProvider : CollectionPreviewParameterProvider<HealthStatus>(HealthStatus.values().toList())

@Preview(name = "Status pills · every status", showBackground = true)
@Preview(name = "Status pills · 200%", fontScale = 2f, showBackground = true)
@Composable
private fun StatusPillPreview(@PreviewParameter(HealthStatusPreviewProvider::class) status: HealthStatus) {
    FinanceAppTheme { StatusPill(status, Modifier.padding(Spacing.md)) }
}
