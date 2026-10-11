package com.example.financeapp.domain.freelancer

import com.example.financeapp.domain.engine.ImpactSimulator
import com.example.financeapp.domain.engine.SimulationLoan
import com.example.financeapp.domain.engine.WhatIfChange
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.YearMonth

class ForecastPathAdapterTest {
    private val anchor = CashAnchor(5_500_001, Instant.parse("2026-10-11T12:00:00Z"))
    private val data = NormalizedFinancialHistory(List(6) { i ->
        FinancialHistoryMonth(YearMonth.of(2026, 4).plusMonths(i.toLong()),
            incomePaise = 5_500_001, cashDiscretionaryPaise = 123_456,
            cashEssentialsPaise = 99_999_999, cashDebtServicePaise = 99_999_999, cashSipPaise = 99_999_999)
    }, anchor)
    private val forecast = (FreelancerForecaster().forecast(data, 42) as FreelancerForecast.Ready).forecast
    private val commitments = List(6) { i -> MonthlyCommitments(2_000_001, 500_000 + i.toLong(), 100_001) }
    private val adapter = ForecastPathAdapter()

    @Test fun convertsPaiseOnceAndPreservesDeclaredCommitmentsAcrossAllPaths() {
        val loan = SimulationLoan(100_000.0, 0.12, 5_000.0)
        val inputs = adapter.adapt(forecast, commitments, 4_000_001, listOf(loan))
        assertEquals(55_000.01, inputs.cash, 0.000001)
        assertEquals(40_000.01, inputs.minReserve, 0.000001)
        assertEquals(listOf(loan), inputs.loans)
        assertEquals(2_000, inputs.sampledPaths.size)
        (listOf(inputs.periods) + inputs.sampledPaths).forEach { path ->
            path.forEachIndexed { i, period ->
                assertEquals(55_000.01, period.income, 0.000001)
                assertEquals(20_000.01, period.essentials, 0.000001)
                assertEquals(commitments[i].debtServicePaise / 100.0, period.debtService, 0.000001)
                assertEquals(1_000.01, period.sip, 0.000001)
                assertEquals(1_234.56, period.discretionary, 0.000001)
            }
        }
    }

    @Test fun adaptedInputsRunInExistingSimulatorWithoutSamplingAgain() {
        val inputs = adapter.adapt(forecast, commitments, 4_000_000)
        val result = ImpactSimulator().simulate(inputs, WhatIfChange())
        assertEquals(6, result.periods.size)
        assertEquals(0.0, result.shortfallRiskDelta, 0.0)
        assertEquals(0.0, result.runwayDeltaMonths, 0.0)
        assertEquals(inputs, adapter.adapt(forecast, commitments, 4_000_000))
    }

    @Test fun rejectsMissingCommitmentsInvalidMoneyAndDebtBelowEmi() {
        assertThrows(IllegalArgumentException::class.java) { adapter.adapt(forecast, commitments.take(5), 0) }
        assertThrows(IllegalArgumentException::class.java) { adapter.adapt(forecast, commitments, -1) }
        assertThrows(IllegalArgumentException::class.java) { adapter.adapt(forecast, commitments, Long.MAX_VALUE) }
        assertThrows(IllegalArgumentException::class.java) { MonthlyCommitments(-1, 0, 0) }
        assertThrows(IllegalArgumentException::class.java) { MonthlyCommitments(0, Long.MAX_VALUE, 0) }
        assertThrows(IllegalArgumentException::class.java) {
            adapter.adapt(forecast, commitments, 0, listOf(SimulationLoan(100_000.0, 0.12, 6_000.0)))
        }
    }

    @Test fun delayDoesNotChangeDebtEssentialsOrSip() {
        val delayed = (FreelancerForecaster().forecast(data, 42, OneMonthReceiptDelay(1))
            as FreelancerForecast.Ready).forecast
        val inputs = adapter.adapt(delayed, commitments, 0)
        assertEquals(0.0, inputs.sampledPaths[0][0].income, 0.0)
        assertEquals(110_000.02, inputs.sampledPaths[0][1].income, 0.000001)
        inputs.sampledPaths.flatten().forEach {
            assertEquals(20_000.01, it.essentials, 0.000001)
            assertEquals(1_000.01, it.sip, 0.000001)
        }
    }
}
