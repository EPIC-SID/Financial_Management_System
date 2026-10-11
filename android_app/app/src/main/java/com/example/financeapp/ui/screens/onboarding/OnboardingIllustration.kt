package com.example.financeapp.ui.screens.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.financeapp.R
import com.example.financeapp.ui.viewmodel.OnboardingPage
import com.example.financeapp.ui.viewmodel.OnboardingUiState

@Composable
internal fun OnboardingIllustration(state: OnboardingUiState) {
    Image(
        painter = painterResource(illustrationFor(state.page)),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxWidth().height(280.dp)
    )
}

@DrawableRes
private fun illustrationFor(page: OnboardingPage): Int = when (page) {
    OnboardingPage.WELCOME -> R.drawable.ledger_organizer
    OnboardingPage.DATA_SOURCE -> R.drawable.onboarding_choices
    OnboardingPage.SMS_CONSENT -> R.drawable.privacy_control
    OnboardingPage.ABOUT_MONEY -> R.drawable.plan_commitments
    OnboardingPage.SAFETY_NET -> R.drawable.advisor_guidance
    OnboardingPage.BUILDING -> R.drawable.plan_board
}
