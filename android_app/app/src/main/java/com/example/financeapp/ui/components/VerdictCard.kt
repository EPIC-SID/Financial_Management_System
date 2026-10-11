package com.example.financeapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.example.financeapp.R
import com.example.financeapp.ui.theme.CanvasWhite
import com.example.financeapp.ui.theme.NumberHero
import com.example.financeapp.ui.theme.Radius
import com.example.financeapp.ui.theme.Spacing

@Composable
fun VerdictCard(
    verdict: String,
    reading: String,
    confidence: String,
    onConfidenceClick: () -> Unit,
    onSeePlan: () -> Unit,
    modifier: Modifier = Modifier,
    heading: String = "Month-end cushion"
) {
    Surface(modifier, shape = RoundedCornerShape(Radius.card), color = Color.Transparent, contentColor = CanvasWhite) {
        BrandImageBackground(R.drawable.bg_green) {
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
            RupAiButton("See plan", onSeePlan)
        }
        }
    }
}
