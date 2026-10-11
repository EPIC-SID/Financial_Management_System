package com.example.financeapp.ui.screens.onboarding

import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingScreenTest {
    @Test
    fun cushionChoicesMapToStoredDigits() {
        assertEquals("30000", cushionValueFor("₹30,000"))
        assertEquals("60000", cushionValueFor("₹60,000"))
        assertEquals("", cushionValueFor("Decide later"))
    }
}
