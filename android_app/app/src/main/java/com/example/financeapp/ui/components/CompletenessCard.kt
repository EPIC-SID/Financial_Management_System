package com.example.financeapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.financeapp.R
import com.example.financeapp.ui.theme.GuidanceYellow
import com.example.financeapp.ui.theme.Radius
import com.example.financeapp.ui.theme.Spacing

@Composable
fun CompletenessCard(percent: Int, reason: String, showIllustration: Boolean, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(Radius.card), color = GuidanceYellow.copy(alpha = .18f)) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            if (showIllustration) Image(painterResource(R.drawable.home_overview), "Coffee, calendar and money planning illustration",
                Modifier.fillMaxWidth().heightIn(max = 180.dp), contentScale = ContentScale.Fit)
            Text("Data completeness · $percent%", style = MaterialTheme.typography.titleLarge)
            Text(reason, style = MaterialTheme.typography.bodyMedium)
            RupAiButton("Add data", onAdd, variant = RupAiButtonVariant.OUTLINE)
        }
    }
}
