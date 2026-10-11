package com.example.financeapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.example.financeapp.R
import com.example.financeapp.ui.theme.ActionGreen
import com.example.financeapp.ui.theme.CanvasWhite
import com.example.financeapp.ui.theme.GuidanceYellow
import com.example.financeapp.ui.theme.NumberHero
import com.example.financeapp.ui.theme.Radius
import com.example.financeapp.ui.theme.Spacing

@Composable
fun VerdictCard(
    verdict: String,
    reading: String,
    p10: Double,
    p50: Double,
    p90: Double,
    confidence: String,
    onConfidenceClick: () -> Unit,
    onSeePlan: () -> Unit,
    modifier: Modifier = Modifier,
    heading: String = "Month-end cushion"
) {
    Surface(modifier, shape = RoundedCornerShape(Radius.card), color = ActionGreen, contentColor = CanvasWhite) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Image(
                    painter = painterResource(R.drawable.rupai_logo_mono),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(CanvasWhite),
                    modifier = Modifier.height(24.dp)
                )
                Text(heading, style = MaterialTheme.typography.labelMedium)
            }
            Text(verdict, style = NumberHero.copy(fontWeight = FontWeight.Bold))
            Text(reading, style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                TypeTag(StatementType.PREDICTION)
                Surface(onClick = onConfidenceClick, shape = RoundedCornerShape(Radius.tag), color = MaterialTheme.colorScheme.surface) {
                    Text("$confidence confidence", Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xxs), style = MaterialTheme.typography.labelSmall)
                }
            }
            ForecastBand(p10, p50, p90)
            RupAiButton("See plan", onSeePlan)
        }
    }
}

@Composable
private fun ForecastBand(p10: Double, p50: Double, p90: Double) {
    val fraction = if (p90 > p10) ((p50 - p10) / (p90 - p10)).toFloat().coerceIn(0f, 1f) else .5f
    Canvas(
        Modifier.fillMaxWidth().height(44.dp).semantics {
            contentDescription = "Forecast range, tenth percentile $p10, median $p50, ninetieth percentile $p90"
        }
    ) {
        val y = size.height / 2
        drawLine(CanvasWhite.copy(alpha = .45f), Offset(0f, y), Offset(size.width, y), 8.dp.toPx(), StrokeCap.Round)
        drawLine(CanvasWhite, Offset(size.width * fraction, 5.dp.toPx()), Offset(size.width * fraction, size.height - 5.dp.toPx()), 2.dp.toPx())
        drawCircle(GuidanceYellow, 7.dp.toPx(), Offset(size.width * .28f, y))
    }
}
