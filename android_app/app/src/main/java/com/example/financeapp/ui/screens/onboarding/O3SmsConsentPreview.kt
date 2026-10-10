package com.example.financeapp.ui.screens.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.financeapp.ui.theme.FinanceAppTheme
import com.example.financeapp.ui.viewmodel.OnboardingFixtures
import com.example.financeapp.ui.viewmodel.SmsPermissionState

@Preview(name = "O3 SMS consent · 360 · 200%", widthDp = 360, heightDp = 800, fontScale = 2f, showBackground = true)
@Composable
private fun O3SmsConsentPreview() {
    FinanceAppTheme { OnboardingScreen(OnboardingFixtures.sms()) }
}

@Preview(name = "O3 Denied · 360 · 200%", widthDp = 360, heightDp = 800, fontScale = 2f, showBackground = true)
@Composable
private fun O3DeniedPreview() {
    FinanceAppTheme { OnboardingScreen(OnboardingFixtures.sms(SmsPermissionState.DENIED)) }
}

@Preview(name = "O3 Permanently denied · 360 · 200%", widthDp = 360, heightDp = 800, fontScale = 2f, showBackground = true)
@Composable
private fun O3PermanentlyDeniedPreview() {
    FinanceAppTheme { OnboardingScreen(OnboardingFixtures.sms(SmsPermissionState.PERMANENTLY_DENIED)) }
}
