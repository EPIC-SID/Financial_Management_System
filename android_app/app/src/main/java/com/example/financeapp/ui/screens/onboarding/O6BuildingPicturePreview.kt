package com.example.financeapp.ui.screens.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.financeapp.ui.theme.FinanceAppTheme
import com.example.financeapp.ui.viewmodel.OnboardingFixtures

@Preview(name = "O6 Building · 360 · 200%", widthDp = 360, heightDp = 800, fontScale = 2f, showBackground = true)
@Composable
private fun O6BuildingPreview() {
    FinanceAppTheme { OnboardingScreen(OnboardingFixtures.building()) }
}

@Preview(name = "O6 Complete · 360 · 200%", widthDp = 360, heightDp = 800, fontScale = 2f, showBackground = true)
@Composable
private fun O6CompletePreview() {
    FinanceAppTheme { OnboardingScreen(OnboardingFixtures.complete()) }
}

@Preview(name = "O6 Failed · 360 · 200%", widthDp = 360, heightDp = 800, fontScale = 2f, showBackground = true)
@Composable
private fun O6FailedPreview() {
    FinanceAppTheme { OnboardingScreen(OnboardingFixtures.failed()) }
}
