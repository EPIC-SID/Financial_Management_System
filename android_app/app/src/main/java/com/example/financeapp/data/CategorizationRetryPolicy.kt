package com.example.financeapp.data

/**
 * CategorizationRetryPolicy — Pure-Kotlin policy deciding when background categorization
 * should be enqueued or retried, and formatting user-facing status feedback.
 */
object CategorizationRetryPolicy {

    /**
     * Decides whether AiCategorizationWorker should be enqueued.
     * Guarantees pending uncategorized rows are retried even when sync finds 0 new SMS.
     */
    fun shouldEnqueueRetry(newMiscCount: Int, pendingMiscCount: Int): Boolean {
        return newMiscCount > 0 || pendingMiscCount > 0
    }

    /**
     * Determines whether a given transaction entity is eligible for AI categorization retry.
     */
    fun isEligibleForRetry(category: String, isCategorizedByAi: Boolean): Boolean {
        return category.uppercase() == "MISCELLANEOUS" && !isCategorizedByAi
    }

    /**
     * Generates a clear status message describing the sync and retry operation.
     */
    fun buildSyncMessage(newEntitiesCount: Int, newMiscCount: Int, pendingMiscCount: Int): String {
        return when {
            newEntitiesCount == 0 && pendingMiscCount > 0 ->
                "Up to date with SMS. Retrying $pendingMiscCount pending categorization(s) in background…"
            newEntitiesCount == 0 ->
                "Already up to date."
            newMiscCount > 0 ->
                "Synced $newEntitiesCount transaction(s). Categorizing $newMiscCount in background…"
            else ->
                "Synced $newEntitiesCount transaction(s)."
        }
    }
}

