package com.example.financeapp.ui.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingViewModelTest {
    @Test
    fun dataSourceStartsUnselected() {
        val viewModel = OnboardingViewModel()
        viewModel.next()
        assertEquals(null, viewModel.uiState.value.selectedSource)
    }

    @Test
    fun sampleProfileSkipsSmsConsent() {
        val viewModel = OnboardingViewModel()
        viewModel.next()
        viewModel.selectSource(OnboardingDataSource.SAMPLE)
        viewModel.next()
        assertEquals(OnboardingPage.ABOUT_MONEY, viewModel.uiState.value.page)
    }

    @Test
    fun currencyInputUsesIndianGroupingAndDropsNonDigits() {
        assertEquals("₹12,34,567", formatOnboardingCurrency("₹12,34,567"))
        assertTrue(formatOnboardingCurrency("").isEmpty())
    }

    @Test
    fun failedIngestCanReturnToAnotherSource() {
        val viewModel = OnboardingViewModel()
        viewModel.chooseAlternative(OnboardingDataSource.CSV)
        assertEquals(OnboardingPage.DATA_SOURCE, viewModel.uiState.value.page)
        assertEquals(OnboardingDataSource.CSV, viewModel.uiState.value.selectedSource)
    }
}
