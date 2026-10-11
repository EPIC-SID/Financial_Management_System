package com.example.financeapp.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.financeapp.ui.theme.Spacing
import com.example.financeapp.ui.viewmodel.OnboardingDataSource

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PermissionRecoveryActions(onAlternativeSelected: (OnboardingDataSource) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        AssistChip(
            onClick = { onAlternativeSelected(OnboardingDataSource.SAMPLE) },
            label = { Text("Use sample") }
        )
    }
}
