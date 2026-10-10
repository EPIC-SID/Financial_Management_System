package com.example.financeapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.financeapp.ui.theme.CanvasWhite
import com.example.financeapp.ui.theme.GuidanceYellow
import com.example.financeapp.ui.theme.Ink
import com.example.financeapp.ui.theme.MutedInk
import com.example.financeapp.ui.theme.Radius
import com.example.financeapp.ui.theme.Spacing
import com.example.financeapp.ui.theme.SummaryGreen
import com.example.financeapp.ui.viewmodel.CopilotAnswerUi

@Composable
fun ThreeSectionAnswerCard(
    answer: CopilotAnswerUi,
    onSimulate: () -> Unit,
    onAddToPlan: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Surface(modifier, shape = RoundedCornerShape(Radius.card), color = CanvasWhite, border = BorderStroke(1.dp, Ink)) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            AnswerSection(StatementType.FACT, CanvasWhite) {
                Text(answer.observed, style = MaterialTheme.typography.bodyMedium)
                Text(answer.observedSource, style = MaterialTheme.typography.labelSmall, color = MutedInk)
            }
            AnswerSection(StatementType.PREDICTION, SummaryGreen) {
                Text(answer.predicted, style = MaterialTheme.typography.bodyMedium)
                Text(answer.predictedRange, style = MaterialTheme.typography.titleMedium)
                Text("${answer.predictedHorizon} · ${answer.confidence}", style = MaterialTheme.typography.labelMedium)
            }
            AnswerSection(StatementType.RECOMMENDATION, GuidanceYellow.copy(alpha = .22f)) {
                Text(
                    answer.recommended,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (expanded) Int.MAX_VALUE else 6,
                    overflow = TextOverflow.Ellipsis
                )
                answer.options.forEach { option ->
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                        Text("→ ${option.action}", style = MaterialTheme.typography.titleSmall)
                        Text("Impact: ${option.impact}", style = MaterialTheme.typography.bodySmall)
                        Text("Trade-off: ${option.tradeOff}", style = MaterialTheme.typography.bodySmall, color = MutedInk)
                    }
                }
                if (answer.isLong) TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Show less" else "Show more") }
            }
            Text("Numbers calculated on your device", style = MaterialTheme.typography.labelSmall, color = MutedInk)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                RupAiButton("Simulate this", onSimulate, Modifier.weight(1f), variant = RupAiButtonVariant.OUTLINE)
                RupAiButton("Add to plan", onAddToPlan, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AnswerSection(type: StatementType, color: androidx.compose.ui.graphics.Color, content: @Composable () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        color = color,
        shape = RoundedCornerShape(Radius.card)
    ) {
        Column(Modifier.padding(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            TypeTag(type)
            content()
        }
    }
}
