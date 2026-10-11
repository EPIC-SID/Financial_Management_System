package com.example.financeapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.financeapp.ui.theme.ActionGreen
import com.example.financeapp.ui.theme.GuidanceYellow
import com.example.financeapp.ui.theme.Ink
import com.example.financeapp.ui.theme.Spacing
import com.example.financeapp.ui.util.formatInrCompact
import com.example.financeapp.ui.util.formatRiskPercent
import com.example.financeapp.ui.util.formatInr
import com.example.financeapp.ui.viewmodel.ForecastPointUi
import kotlin.math.roundToInt

@Composable
fun ForecastFanChart(points: List<ForecastPointUi>, showTable: Boolean, onToggleTable: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    require(points.size >= 2) { "Forecast chart needs at least two periods" }
    var targetProgress by remember(points) { mutableFloatStateOf(0f) }
    var selectedIndex by remember(points) { mutableIntStateOf(0) }
    val progress by animateFloatAsState(targetProgress, tween(400), label = "forecast-band")
    LaunchedEffect(points) { targetProgress = 1f }
    val summary = remember(points) { talkBackSummary(points) }

    Surface(modifier, shape = MaterialTheme.shapes.large, border = androidx.compose.foundation.BorderStroke(1.dp, Ink)) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Estimated cash", style = MaterialTheme.typography.titleMedium)
                    Text("Middle 80% of simulated results", style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Table", style = MaterialTheme.typography.labelMedium)
                    Switch(checked = showTable, onCheckedChange = onToggleTable)
                }
            }
            Canvas(
                Modifier.fillMaxWidth().height(230.dp)
                    .semantics { contentDescription = summary }
                    .pointerInput(points) {
                        detectTapGestures { offset -> selectedIndex = chartIndex(offset.x, size.width, points.size) }
                    }
                    .pointerInput(points) {
                        detectDragGestures { change, _ ->
                            selectedIndex = chartIndex(change.position.x, size.width, points.size)
                            change.consume()
                        }
                    }
            ) {
                val left = 10.dp.toPx(); val right = size.width - 10.dp.toPx(); val top = 12.dp.toPx(); val bottom = size.height - 18.dp.toPx()
                val minValue = (points.minOf { minOf(it.p10, it.reserve) } * .9).toFloat()
                val maxValue = (points.maxOf { it.p90 } * 1.05).toFloat()
                fun x(index: Int) = left + (right - left) * index / (points.lastIndex.toFloat())
                fun y(value: Double) = bottom - (bottom - top) * ((value.toFloat() - minValue) / (maxValue - minValue))
                val upper = Path(); val band = Path(); val median = Path()
                points.forEachIndexed { index, point ->
                    if (index == 0) { upper.moveTo(x(index), y(point.p90)); median.moveTo(x(index), y(point.p50)) }
                    else { upper.lineTo(x(index), y(point.p90)); median.lineTo(x(index), y(point.p50)) }
                }
                band.addPath(upper)
                points.indices.reversed().forEach { index -> band.lineTo(x(index), y(points[index].p10)) }
                band.close()
                clipRect(right = left + (right - left) * progress) {
                    drawPath(band, ActionGreen.copy(alpha = .15f))
                    drawPath(median, Ink, style = Stroke(2.dp.toPx()))
                }
                val reserveY = y(points.first().reserve)
                drawLine(Ink, Offset(left, reserveY), Offset(right, reserveY), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
                drawCircle(GuidanceYellow, 7.dp.toPx(), Offset(left, bottom))
                drawCircle(Ink, 7.dp.toPx(), Offset(left, bottom), style = Stroke(1.dp.toPx()))
                val scrubX = x(selectedIndex); val selected = points[selectedIndex]
                drawLine(Ink.copy(alpha = .35f), Offset(scrubX, top), Offset(scrubX, bottom), 1.dp.toPx())
                drawCircle(Ink, 5.dp.toPx(), Offset(scrubX, y(selected.p50)))
            }
            val selected = points[selectedIndex]
            Surface(shape = RoundedCornerShape(com.example.financeapp.ui.theme.Radius.field), color = MaterialTheme.colorScheme.surfaceVariant) {
                Text(
                    "${selected.period}\nLow: ${formatInr(selected.p10)}\nTypical: ${formatInr(selected.p50)}\nHigh: ${formatInr(selected.p90)}\nChance of falling below the safety balance this month: ${formatRiskPercent(selected.shortfallChance)}",
                    Modifier.padding(Spacing.sm), style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun chartIndex(x: Float, width: Int, count: Int): Int = ((x / width.coerceAtLeast(1)) * (count - 1)).roundToInt().coerceIn(0, count - 1)

private fun talkBackSummary(points: List<ForecastPointUi>): String {
    val last = points.last()
    val safePeriods = points.count { it.p10 >= it.reserve }
    return "Estimate through ${last.period}. Typical ending balance ${formatInrCompact(last.p50)}. The low estimate is above the safety balance in $safePeriods of ${points.size} months. Risk of falling below the safety balance in the last month: ${formatRiskPercent(last.shortfallChance)}."
}
