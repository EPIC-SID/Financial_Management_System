package com.example.financeapp.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class IndianFormatTest {

    @Test
    fun formatInrUsesLakhGrouping() {
        assertEquals("₹999", formatInr(999.0))
        assertEquals("₹1,000", formatInr(1000.0))
        assertEquals("₹12,345", formatInr(12345.0))
        assertEquals("₹1,50,000", formatInr(150000.0))
        assertEquals("₹12,34,567", formatInr(1234567.0))
    }

    @Test
    fun formatInrUsesTrueMinus() {
        assertEquals("−₹4,200", formatInr(-4200.0))
    }

    @Test
    fun formatInrCompact() {
        assertEquals("₹25K", formatInrCompact(25_000.0))
        assertEquals("₹1.5L", formatInrCompact(1_50_000.0))
        assertEquals("₹2.3Cr", formatInrCompact(2_30_00_000.0))
        assertEquals("−₹4.2K", formatInrCompact(-4_200.0))
    }
}
