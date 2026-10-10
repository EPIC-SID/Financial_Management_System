package com.example.financeapp.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthScoreTest {
    private val known = HealthScoreInputs(
        cash = 600.0,
        monthlyObligations = 100.0,
        monthlyDebtService = 0.0,
        monthlyIncome = 1_000.0,
        minimumReserve = 600.0,
        netCashFlowStdDev = 0.0,
        meanIncome = 1_000.0
    )

    @Test fun formulasReachExactComponentBoundaries() {
        val result = HealthScore.calculate(known)
        assertTrue(result.isKnown)
        assertEquals(100, result.score)
        assertEquals(listOf(25.0, 25.0, 25.0, 25.0), result.components.map { it.score })
    }

    @Test fun statusThresholdsAreExact() {
        assertEquals(HealthScoreBand.RISK, HealthScore.bandFor(49))
        assertEquals(HealthScoreBand.WATCH, HealthScore.bandFor(50))
        assertEquals(HealthScoreBand.WATCH, HealthScore.bandFor(75))
        assertEquals(HealthScoreBand.SAFE, HealthScore.bandFor(76))
    }

    @Test fun zeroOrMissingRequiredInputsAreUnknown() {
        val zero = HealthScore.calculate(known.copy(monthlyObligations = 0.0))
        val missing = HealthScore.calculate(known.copy(monthlyIncome = null))
        for (result in listOf(zero, missing)) {
            assertFalse(result.isKnown)
            assertEquals(0, result.score)
            assertEquals(4, result.components.size)
        }
    }

    @Test fun invalidNumbersNeverProduceNaN() {
        val result = HealthScore.calculate(known.copy(meanIncome = Double.NaN))
        assertFalse(result.isKnown)
        assertEquals(0, result.score)
        assertTrue(result.components.none { it.score.isNaN() })
    }

    @Test fun componentExplanationsCarryInputsAndSingleLineGuidance() {
        val result = HealthScore.calculate(known)
        result.components.forEach { component ->
            assertTrue(component.inputs.isNotEmpty())
            assertTrue(component.reading.isNotBlank())
            assertFalse(component.reading.contains('\n'))
            assertTrue(component.whatWouldRaiseThis.isNotBlank())
            assertFalse(component.whatWouldRaiseThis.contains('\n'))
        }
    }

    @Test fun calculationIsDeterministic() {
        val first = HealthScore.calculate(known)
        repeat(100) { assertEquals(first, HealthScore.calculate(known)) }
    }
}
