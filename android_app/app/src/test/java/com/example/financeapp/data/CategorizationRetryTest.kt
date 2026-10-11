package com.example.financeapp.data

import org.junit.Assert.*
import org.junit.Test

class CategorizationRetryTest {

    @Test
    fun shouldEnqueueRetry_whenZeroNewSmsButPendingRowsExist_returnsTrue() {
        // Core requirement of Phase 2B: retries pending uncategorized rows even when sync finds 0 new SMS
        val shouldEnqueue = CategorizationRetryPolicy.shouldEnqueueRetry(
            newMiscCount = 0,
            pendingMiscCount = 4
        )
        assertTrue("Must enqueue background worker to recover pending rows", shouldEnqueue)
    }

    @Test
    fun shouldEnqueueRetry_whenZeroNewSmsAndZeroPending_returnsFalse() {
        val shouldEnqueue = CategorizationRetryPolicy.shouldEnqueueRetry(
            newMiscCount = 0,
            pendingMiscCount = 0
        )
        assertFalse("Must not enqueue worker when there is nothing to categorize", shouldEnqueue)
    }

    @Test
    fun shouldEnqueueRetry_whenNewMiscRowsExist_returnsTrue() {
        val shouldEnqueue = CategorizationRetryPolicy.shouldEnqueueRetry(
            newMiscCount = 3,
            pendingMiscCount = 0
        )
        assertTrue("Must enqueue worker when newly inserted rows need categorization", shouldEnqueue)
    }

    @Test
    fun isEligibleForRetry_onlyUnprocessedMiscellaneousRowsAreEligible() {
        // Pending MISCELLANEOUS not yet processed by AI -> eligible
        assertTrue(CategorizationRetryPolicy.isEligibleForRetry("MISCELLANEOUS", isCategorizedByAi = false))
        assertTrue(CategorizationRetryPolicy.isEligibleForRetry("miscellaneous", isCategorizedByAi = false))

        // Already AI-processed -> not eligible
        assertFalse(CategorizationRetryPolicy.isEligibleForRetry("MISCELLANEOUS", isCategorizedByAi = true))

        // Deterministically classified (FOOD, HEALTH, TRAVEL) -> not eligible
        assertFalse(CategorizationRetryPolicy.isEligibleForRetry("FOOD", isCategorizedByAi = false))
        assertFalse(CategorizationRetryPolicy.isEligibleForRetry("HEALTH", isCategorizedByAi = true))
    }

    @Test
    fun buildSyncMessage_reportsCorrectUserFeedback() {
        // Zero new SMS, pending retry
        val retryMsg = CategorizationRetryPolicy.buildSyncMessage(
            newEntitiesCount = 0,
            newMiscCount = 0,
            pendingMiscCount = 5
        )
        assertTrue(retryMsg.contains("Retrying 5 pending categorization(s)"))

        // Zero new SMS, zero pending
        val upToDateMsg = CategorizationRetryPolicy.buildSyncMessage(
            newEntitiesCount = 0,
            newMiscCount = 0,
            pendingMiscCount = 0
        )
        assertEquals("Already up to date.", upToDateMsg)

        // New SMS synced with MISC count
        val newMiscMsg = CategorizationRetryPolicy.buildSyncMessage(
            newEntitiesCount = 10,
            newMiscCount = 3,
            pendingMiscCount = 3
        )
        assertTrue(newMiscMsg.contains("Synced 10 transaction(s). Categorizing 3 in background…"))

        // New SMS synced, all resolved at Layer 1+2
        val resolvedMsg = CategorizationRetryPolicy.buildSyncMessage(
            newEntitiesCount = 4,
            newMiscCount = 0,
            pendingMiscCount = 0
        )
        assertEquals("Synced 4 transaction(s).", resolvedMsg)
    }
}

