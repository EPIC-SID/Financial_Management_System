package com.example.financeapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.financeapp.domain.engine.HealthScoreComponent
import com.example.financeapp.ui.theme.ActionGreen
import com.example.financeapp.ui.theme.Ink
import com.example.financeapp.ui.theme.NumberHero
import com.example.financeapp.ui.theme.Spacing

@Composable
fun HealthGauge(score: Int, status: HealthStatus, components: List<HealthScoreComponent>, onExplain: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier, shape = MaterialTheme.shapes.large, border = androidx.compose.foundation.BorderStroke(1.dp, Ink)) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Money health", style = MaterialTheme.typography.titleLarge)
                StatusPill(status)
            }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(180.dp).semantics { contentDescription = "Money health score $score out of 100" }) {
                    val stroke = 15.dp.toPx(); val pad = stroke / 2
                    drawArc(Ink.copy(alpha = .12f), 150f, 240f, false, Offset(pad, pad), Size(size.width - stroke, size.height - stroke), style = Stroke(stroke, cap = StrokeCap.Round))
                    drawArc(ActionGreen, 150f, 240f * score / 100f, false, Offset(pad, pad), Size(size.width - stroke, size.height - stroke), style = Stroke(stroke, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(score.toString(), style = NumberHero)
                    Text("of 100", style = MaterialTheme.typography.bodyMedium)
                }
            }
            components.forEach {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(it.label, style = MaterialTheme.typography.bodyMedium)
                        Text("${it.score}/25", style = MaterialTheme.typography.labelMedium)
                    }
                    LinearProgressIndicator(progress = (it.score / 25.0).toFloat(), modifier = Modifier.fillMaxWidth().height(8.dp), color = ActionGreen)
                }
            }
            TextButton(onClick = onExplain) { Text("How this is calculated") }
        }
    }
}
