package com.example.financeapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.example.financeapp.ui.theme.FinanceAppTheme
import com.example.financeapp.ui.theme.Ink
import com.example.financeapp.ui.theme.Radius
import com.example.financeapp.ui.theme.Spacing
import com.example.financeapp.ui.theme.SummaryGreen
import com.example.financeapp.ui.theme.SurfaceNeutral

enum class StatementType { FACT, PREDICTION, RECOMMENDATION }

@Composable
fun TypeTag(type: StatementType, modifier: Modifier = Modifier) {
    val (glyph, label) = when (type) {
        StatementType.FACT -> "✓" to "Observed"
        StatementType.PREDICTION -> "≈" to "Predicted"
        StatementType.RECOMMENDATION -> "→" to "Suggested"
    }
    Surface(
        modifier = modifier.semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(Radius.tag),
        color = if (type == StatementType.FACT) SummaryGreen else SurfaceNeutral,
        contentColor = Ink
    ) {
        Row(
            modifier = Modifier.heightIn(min = 24.dp).padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)
        ) {
            Text(glyph, modifier = Modifier.clearAndSetSemantics {}, style = MaterialTheme.typography.labelMedium, fontSize = 16.sp)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

class StatementTypePreviewProvider : CollectionPreviewParameterProvider<StatementType>(StatementType.values().toList())

@Preview(name = "Statement tags · every type", showBackground = true)
@Preview(name = "Statement tags · 200%", fontScale = 2f, showBackground = true)
@Composable
private fun TypeTagPreview(@PreviewParameter(StatementTypePreviewProvider::class) type: StatementType) {
    FinanceAppTheme { TypeTag(type, Modifier.padding(Spacing.md)) }
}
