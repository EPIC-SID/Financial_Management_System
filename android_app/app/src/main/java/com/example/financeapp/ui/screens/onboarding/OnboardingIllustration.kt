package com.example.financeapp.ui.screens.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.financeapp.R
import com.example.financeapp.ui.theme.GuidanceYellow
import com.example.financeapp.ui.theme.Radius
import com.example.financeapp.ui.theme.Spacing
import com.example.financeapp.ui.theme.SummaryGreen
import com.example.financeapp.ui.viewmodel.OnboardingPage
import com.example.financeapp.ui.viewmodel.OnboardingUiState

@Composable
internal fun OnboardingIllustration(state: OnboardingUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(160.dp),
        shape = RoundedCornerShape(Radius.card),
        color = illustrationTint(state.page)
    ) {
        Image(
            painter = painterResource(illustrationFor(state.page)),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(Spacing.sm)
        )
    }
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

private fun illustrationTint(page: OnboardingPage): Color =
    if (page == OnboardingPage.SAFETY_NET) GuidanceYellow.copy(alpha = .22f) else SummaryGreen
