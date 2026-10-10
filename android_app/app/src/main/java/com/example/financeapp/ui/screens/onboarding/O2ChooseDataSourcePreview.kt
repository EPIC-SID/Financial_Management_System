package com.example.financeapp.ui.screens.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.financeapp.ui.theme.FinanceAppTheme
import com.example.financeapp.ui.viewmodel.OnboardingDataSource
import com.example.financeapp.ui.viewmodel.OnboardingFixtures

@Preview(name = "O2 Data source · 360 · 200%", widthDp = 360, heightDp = 800, fontScale = 2f, showBackground = true)
@Composable
private fun O2DataSourcePreview() {
    FinanceAppTheme { OnboardingScreen(OnboardingFixtures.source(OnboardingDataSource.SAMPLE)) }
}

@Preview(name = "O2 Non-Android · 360 · 200%", widthDp = 360, heightDp = 800, fontScale = 2f, showBackground = true)
@Composable
private fun O2NonAndroidPreview() {
    FinanceAppTheme { OnboardingScreen(OnboardingFixtures.source(), showSmsOption = false) }
}
