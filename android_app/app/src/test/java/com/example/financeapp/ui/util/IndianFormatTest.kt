package com.example.financeapp.ui.util

import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class IndianFormatTest {
    @Test fun currencyUsesIndianGrouping() {
        val examples = listOf(
            0.0 to "₹0", 999.0 to "₹999", 1000.0 to "₹1,000",
            10000.0 to "₹10,000", 100000.0 to "₹1,00,000",
            150000.0 to "₹1,50,000", 10000000.0 to "₹1,00,00,000",
            -4200.0 to "−₹4,200"
        )
        examples.forEach { (input, expected) -> assertEquals(expected, formatInr(input)) }
    }

    @Test fun roundingIsSymmetricAndSuppressesNegativeZero() {
        assertEquals("₹1,000", formatInr(999.5))
        assertEquals("−₹1,000", formatInr(-999.5))
        assertEquals("₹0", formatInr(-0.0))
        assertEquals("₹0", formatInr(-0.49))
        assertEquals("−₹1", formatInr(-0.5))
    }

    @Test fun compactUsesLakhAndCroreUnits() {
        val examples = listOf(
            999.0 to "₹999", 1000.0 to "₹1K", 25000.0 to "₹25K",
            150000.0 to "₹1.5L", 23000000.0 to "₹2.3Cr",
            -4200.0 to "−₹4.2K", -0.49 to "₹0"
        )
        examples.forEach { (input, expected) -> assertEquals(expected, formatInrCompact(input)) }
    }

    @Test fun compactRoundingPromotesAtUnitBoundaries() {
        assertEquals("₹1K", formatInrCompact(999.5))
        assertEquals("₹1L", formatInrCompact(99950.0))
        assertEquals("₹1Cr", formatInrCompact(9995000.0))
        assertEquals("−₹1L", formatInrCompact(-99950.0))
        assertEquals("₹99.9K", formatInrCompact(99949.0))
    }

    @Test fun largeFiniteAmountsDoNotOverflowLong() {
        assertEquals("₹10,00,00,00,00,00,00,00,00,000", formatInr(1e20))
        assertEquals("₹10000000000000Cr", formatInrCompact(1e20))
        assertFalse(formatInr(Double.MAX_VALUE).contains("E"))
    }

    @Test fun percentagesAcceptRatiosAndRoundHalfUp() {
        assertEquals("38%", formatPercent(0.38))
        assertEquals("39%", formatPercent(0.385))
        assertEquals("−39%", formatPercent(-0.385))
        assertEquals("0%", formatPercent(-0.001))
        assertEquals("125%", formatPercent(1.25))
    }

    @Test fun nonFiniteNumbersAreRejected() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).forEach { value ->
            assertThrows(IllegalArgumentException::class.java) { formatInr(value) }
            assertThrows(IllegalArgumentException::class.java) { formatInrCompact(value) }
            assertThrows(IllegalArgumentException::class.java) { formatPercent(value) }
        }
    }

    @Test fun dateAddsYearOnlyOutsideCurrentYear() {
        val today = LocalDate.of(2026, 10, 11)
        assertEquals("18 Oct", formatDate(LocalDate.of(2026, 10, 18), today))
        assertEquals("18 Oct 2025", formatDate(LocalDate.of(2025, 10, 18), today))
        assertEquals("1 Jan 2027", formatDate(LocalDate.of(2027, 1, 1), today))
    }

    @Test fun periodsAreOneBasedAndCrossTheYearBoundary() {
        val firstMonth = YearMonth.of(2026, 10)
        assertEquals("Month 1 (Oct)", formatPeriod(1, firstMonth))
        assertEquals("Month 3 (Dec)", formatPeriod(3, firstMonth))
        assertEquals("Month 4 (Jan)", formatPeriod(4, firstMonth))
        assertThrows(IllegalArgumentException::class.java) { formatPeriod(0, firstMonth) }
    }

    @Test fun formattingDoesNotDependOnDeviceLocale() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("₹1.5L", formatInrCompact(150000.0))
            assertEquals("₹1,50,000", formatInr(150000.0))
            assertEquals("18 Oct", formatDate(LocalDate.of(2026, 10, 18), LocalDate.of(2026, 1, 1)))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test fun accountMaskExposesOnlyLastFourDigits() {
        assertEquals("•• 4091", formatAccountDigits("123456784091"))
        assertEquals("•• 0042", formatAccountDigits("0042"))
        listOf("", "123", "12ab4091", "1234 4091", "１２３４").forEach {
            val failure = assertThrows(IllegalArgumentException::class.java) { formatAccountDigits(it) }
            if (it.isNotEmpty()) assertFalse(failure.message.orEmpty().contains(it))
        }
    }
}
