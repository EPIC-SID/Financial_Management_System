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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.financeapp.ui.theme.ActionGreen
import com.example.financeapp.ui.theme.Ink
import com.example.financeapp.ui.theme.MutedInk
import com.example.financeapp.ui.theme.NumberCompact
import com.example.financeapp.ui.theme.Radius
import com.example.financeapp.ui.theme.Spacing
import com.example.financeapp.ui.theme.SurfaceNeutral
import com.example.financeapp.ui.util.formatInr
import com.example.financeapp.ui.util.formatPercent
import com.example.financeapp.ui.viewmodel.PlanKind
import com.example.financeapp.ui.viewmodel.PlanSummaryUi

@Composable
fun PlanCard(plan: PlanKind, summary: PlanSummaryUi, modifier: Modifier = Modifier) {
    val largeText = LocalDensity.current.fontScale >= 1.5f
    val dashed = plan == PlanKind.STATUS_QUO
    val fill = when (plan) { PlanKind.PLAN_A -> ActionGreen; PlanKind.PLAN_B -> MaterialTheme.colorScheme.surface; PlanKind.STATUS_QUO -> SurfaceNeutral }
    val dashedBorder = if (dashed) Modifier.drawBehind {
        drawRoundRect(MutedInk, cornerRadius = CornerRadius(Radius.card.toPx()), style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
    } else Modifier
    Surface(
        modifier.then(dashedBorder),
        shape = RoundedCornerShape(Radius.card),
        color = fill,
        contentColor = Ink,
        border = if (dashed) null else BorderStroke(1.dp, Ink)
    ) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text(plan.label, style = MaterialTheme.typography.titleLarge)
            if (largeText) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    PlanMetric("Ending reserve", formatInr(summary.endingReserve))
                    PlanMetric("Interest saved", formatInr(summary.interestSaved))
                    PlanMetric("Shortfall risk", formatPercent(summary.shortfallRisk))
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    PlanMetric("Ending reserve", formatInr(summary.endingReserve))
                    PlanMetric("Interest saved", formatInr(summary.interestSaved))
                    PlanMetric("Shortfall risk", formatPercent(summary.shortfallRisk))
                }
            }
            Text(summary.tradeOff, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PlanMetric(label: String, value: String) {
    Column(Modifier.padding(end = Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, style = NumberCompact)
    }
}
