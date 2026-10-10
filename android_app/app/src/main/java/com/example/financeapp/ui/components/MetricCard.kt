package com.example.financeapp.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.financeapp.ui.theme.BodyM
import com.example.financeapp.ui.theme.CardRadius
import com.example.financeapp.ui.theme.FinanceAppTheme
import com.example.financeapp.ui.theme.LabelS
import com.example.financeapp.ui.theme.NumberXL
import com.example.financeapp.ui.theme.Spacing
import com.example.financeapp.ui.theme.Title
import com.example.financeapp.ui.util.formatInr
import com.example.financeapp.ui.util.formatInrCompact

@Composable
fun MetricCard(
    title: String,
    value: Double,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    delta: Double? = null,
    type: StatementType = StatementType.FACT,
    compact: Boolean = false,
) {
    Surface(
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outline, CardRadius),
        shape = CardRadius,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = Title, color = MaterialTheme.colorScheme.onSurface)
                TypeTag(type)
            }
            Text(
                if (compact) formatInrCompact(value) else formatInr(value),
                style = NumberXL,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                subtitle?.let {
                    Text(it, style = BodyM, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                delta?.let { StatusPill(if (it < 0) HealthStatus.RISK else HealthStatus.SAFE) }
                delta?.let {
                    Text(
                        "${if (it >= 0) "+" else ""}${formatInrCompact(it)}",
                        style = LabelS,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "MetricCard light")
@Composable
private fun MetricCardLightPreview() {
    FinanceAppTheme(darkTheme = false) {
        MetricCard(
            title = "Bank cash",
            value = 45_200.0,
            subtitle = "HDFC savings",
            delta = 2_400.0,
            modifier = Modifier.padding(Spacing.md),
        )
    }
}

@Preview(showBackground = true, name = "MetricCard dark", backgroundColor = 0xFF14110D)
@Composable
private fun MetricCardDarkPreview() {
    FinanceAppTheme(darkTheme = true) {
        MetricCard(
            title = "Shortfall risk",
            value = 0.0,
            subtitle = "Next 6 months",
            type = StatementType.PREDICTION,
            modifier = Modifier.padding(Spacing.md),
        )
    }
}
