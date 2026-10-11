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
import com.example.financeapp.ui.theme.CanvasWhite
import com.example.financeapp.ui.theme.Ink
import com.example.financeapp.ui.theme.MutedInk
import com.example.financeapp.ui.theme.NumberCompact
import com.example.financeapp.ui.theme.Radius
import com.example.financeapp.ui.theme.Spacing
import com.example.financeapp.ui.theme.SurfaceNeutral
import com.example.financeapp.ui.util.formatInr
import com.example.financeapp.ui.util.formatRiskPercent
import com.example.financeapp.ui.viewmodel.PlanKind
import com.example.financeapp.ui.viewmodel.PlanSummaryUi

@Composable
fun PlanCard(plan: PlanKind, summary: PlanSummaryUi, modifier: Modifier = Modifier) {
    val dashed = plan == PlanKind.STATUS_QUO
    val fill = when (plan) { PlanKind.PLAN_A -> ActionGreen; PlanKind.PLAN_B -> MaterialTheme.colorScheme.surface; PlanKind.STATUS_QUO -> SurfaceNeutral }
    val dashedBorder = if (dashed) Modifier.drawBehind {
        drawRoundRect(MutedInk, cornerRadius = CornerRadius(Radius.card.toPx()), style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
    } else Modifier
    Surface(
        modifier.then(dashedBorder),
        shape = RoundedCornerShape(Radius.card),
        color = fill,
        contentColor = if (plan == PlanKind.PLAN_A) CanvasWhite else Ink,
        border = if (dashed) null else BorderStroke(1.dp, Ink)
    ) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text(plan.label, style = MaterialTheme.typography.titleLarge)
            PlanMetric("Cash after 6 months", formatInr(summary.endingReserve))
            PlanMetric("Interest saved", formatInr(summary.interestSaved))
            PlanMetric("Risk of low cash", formatRiskPercent(summary.shortfallRisk))
            if (summary.pathsEvaluated > 0) Text(
                "${summary.breachedPaths} of ${summary.pathsEvaluated} simulations fell below the safety balance at least once in six months. This is not the chance of reaching your goal.",
                style = MaterialTheme.typography.bodySmall)
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
