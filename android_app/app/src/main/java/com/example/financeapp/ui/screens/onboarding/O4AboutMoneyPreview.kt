package com.example.financeapp.ui.screens.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.financeapp.ui.theme.FinanceAppTheme
import com.example.financeapp.ui.viewmodel.OnboardingFixtures

@Preview(name = "O4 About money · 360 · 200%", widthDp = 360, heightDp = 800, fontScale = 2f, showBackground = true)
@Composable
private fun O4AboutMoneyPreview() {
    FinanceAppTheme { OnboardingScreen(OnboardingFixtures.aboutMoney()) }
}
