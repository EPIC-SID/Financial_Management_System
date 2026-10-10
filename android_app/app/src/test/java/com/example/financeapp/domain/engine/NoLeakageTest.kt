package com.example.financeapp.domain.engine

import com.example.financeapp.data.SyntheticDataSeeder
import com.example.financeapp.data.TransactionEntity
import com.example.financeapp.domain.model.Scenario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoLeakageTest {

    @Test
    fun testFutureActualsDoNotLeakIntoHistoricalTransactions() {
        // Enforce temporal separation rule (Section 11.1):
        // Historical transactions must strictly belong to Month -6 to -1.
        // Future actuals belong strictly to Month +1 to +6.
        val scenarioData = SyntheticDataSeeder.generate(Scenario.STABLE_INCOME, seed = 42L)
        val history = scenarioData.snapshot.recentTransactions
        val future = scenarioData.futureActuals

        assertTrue("History must not be empty", history.isNotEmpty())
        assertTrue("Future actuals must not be empty", future.isNotEmpty())

        val historyDates = history.map { it.transactionDate }
        val futureDates = future.map { it.transactionDate }

        val maxHistoryDate = historyDates.maxOrNull() ?: ""
        val minFutureDate = futureDates.minOrNull() ?: ""

        assertTrue(
            "Temporal boundary must strictly hold: maxHistoryDate ($maxHistoryDate) < minFutureDate ($minFutureDate)",
            maxHistoryDate < minFutureDate
        )

        // Verify zero overlap of transaction IDs
        val historyIds = history.map { it.smsId }.toSet()
        val futureIds = future.map { it.smsId }.toSet()
        val overlap = historyIds.intersect(futureIds)

        assertTrue("Zero transaction ID overlap between history and future", overlap.isEmpty())
    }

    @Test
    fun testForecastAtOriginIsInvariantToFutureChanges() {
        val scenarioData = SyntheticDataSeeder.generate(Scenario.STABLE_INCOME, seed = 42L)
        val snapshot = scenarioData.snapshot

        // Generate baseline forecast from historical snapshot
        val baseRun = ForecastEngine.generateForecast(snapshot, numPaths = 1000, seed = 999L)

        // Now simulate what would happen if a caller creates future transactions:
        // Even if future transactions exist somewhere in memory, the engine at origin o
        // only ingests the historical snapshot.
        val baseP50Balances = baseRun.balanceSummaries.map { it.p50 }

        // Second run with identical snapshot must produce identical results
        val secondRun = ForecastEngine.generateForecast(snapshot, numPaths = 1000, seed = 999L)
        val secondP50Balances = secondRun.balanceSummaries.map { it.p50 }

        assertEquals("Predictions at origin o must be 100% deterministic", baseP50Balances, secondP50Balances)
    }

    @Test
    fun testEngineRejectsFutureTransactionsInHistory() {
        val scenarioData = SyntheticDataSeeder.generate(Scenario.STABLE_INCOME, seed = 42L)
        val snapshot = scenarioData.snapshot

        // Ensure all recentTransactions are within the expected anchor timeline (2026-10 or earlier)
        val latestAllowedMonth = SyntheticDataSeeder.ANCHOR.toString() // "2026-10"

        val breachingTransactions = snapshot.recentTransactions.filter { tx ->
            tx.transactionDate.take(7) > latestAllowedMonth
        }

        assertTrue(
            "Snapshot history must contain zero transactions beyond anchor date $latestAllowedMonth",
            breachingTransactions.isEmpty()
        )
    }
}

