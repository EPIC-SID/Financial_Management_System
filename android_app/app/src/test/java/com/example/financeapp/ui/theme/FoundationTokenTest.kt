package com.example.financeapp.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class FoundationTokenTest {
    @Test fun radiiAndButtonSizesMatchThePhaseOneContract() {
        assertEquals(12f, Radius.field.value)
        assertEquals(16f, Radius.button.value)
        assertEquals(20f, Radius.card.value)
        assertEquals(40f, Spacing.compactButtonHeight.value)
        assertEquals(48f, Spacing.defaultButtonHeight.value)
        assertEquals(56f, Spacing.largeButtonHeight.value)
    }

    @Test fun heroMoneyUsesInterAndTabularFigures() {
        assertEquals(Inter, NumberHero.fontFamily)
        assertEquals("tnum, lnum", NumberHero.fontFeatureSettings)
    }
}
