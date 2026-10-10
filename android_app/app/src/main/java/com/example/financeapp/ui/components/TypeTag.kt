package com.example.financeapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.financeapp.ui.theme.BronzeContainerDark
import com.example.financeapp.ui.theme.BronzeContainerLight
import com.example.financeapp.ui.theme.BronzeTextLight
import com.example.financeapp.ui.theme.FinanceAppTheme
import com.example.financeapp.ui.theme.LabelS
import com.example.financeapp.ui.theme.ObservedContainerDark
import com.example.financeapp.ui.theme.ObservedContainerLight
import com.example.financeapp.ui.theme.ObservedTeal
import com.example.financeapp.ui.theme.PillRadius
import com.example.financeapp.ui.theme.PredictedBlue
import com.example.financeapp.ui.theme.PredictedContainerDark
import com.example.financeapp.ui.theme.PredictedContainerLight
import com.example.financeapp.ui.theme.Spacing

enum class StatementType(val label: String, val icon: ImageVector) {
    FACT("Observed", Icons.Filled.CheckCircle),
    PREDICTION("Predicted", Icons.Filled.QueryStats),
    RECOMMENDATION("Recommended", Icons.Filled.ArrowForward),
}

@Composable
fun TypeTag(type: StatementType, modifier: Modifier = Modifier, dark: Boolean = false) {
    val (fg, bg) = when (type) {
        StatementType.FACT -> ObservedTeal to if (dark) ObservedContainerDark else ObservedContainerLight
        StatementType.PREDICTION -> PredictedBlue to if (dark) PredictedContainerDark else PredictedContainerLight
        StatementType.RECOMMENDATION ->
            (if (dark) Color(0xFFF0C494) else BronzeTextLight) to if (dark) BronzeContainerDark else BronzeContainerLight
    }
    Row(
        modifier = modifier
            .height(24.dp)
            .background(bg, PillRadius)
            .padding(horizontal = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
    ) {
        Icon(type.icon, contentDescription = type.label, tint = fg, modifier = Modifier.size(14.dp))
        Text(type.label, style = LabelS, color = fg)
    }
}

@Preview(showBackground = true, name = "TypeTag light")
@Composable
private fun TypeTagLightPreview() {
    FinanceAppTheme(darkTheme = false) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), modifier = Modifier.padding(Spacing.md)) {
            StatementType.entries.forEach { TypeTag(it) }
        }
    }
}

@Preview(showBackground = true, name = "TypeTag dark", backgroundColor = 0xFF14110D)
@Composable
private fun TypeTagDarkPreview() {
    FinanceAppTheme(darkTheme = true) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), modifier = Modifier.padding(Spacing.md)) {
            StatementType.entries.forEach { TypeTag(it, dark = true) }
        }
    }
}
