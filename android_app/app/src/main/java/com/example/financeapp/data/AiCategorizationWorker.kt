package com.example.financeapp.data

import android.content.Context
import android.util.Log
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Background WorkManager job that runs the expensive AI pipeline (Layers 3+4)
 * for any MISCELLANEOUS transactions that haven't been AI-processed yet.
 *
 * Key guarantees:
 * - Runs as ExistingWorkPolicy.KEEP — only one instance runs at a time, no duplicates.
 * - Only queries rows where isCategorizedByAi = false — already processed rows are skipped.
 * - Both PlacesCategorizer and GeminiCategorizer cache results in SharedPreferences —
 *   the same merchant is NEVER sent to an API more than once, even across restarts.
 * - Room Flow auto-notifies the UI as each row is updated — no manual refresh needed.
 * - Rate-limited to ~4 seconds between Gemini API calls to respect free tier quota (15 req/min).
 */
class AiCategorizationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "AiCategorizationWorker"
        private const val WORK_NAME = "ai_categorization"

        // Free tier allows ~15 requests/min → 1 req per 4 seconds to stay safe
        private const val GEMINI_RATE_LIMIT_MS = 4_000L

        /**
         * Enqueues the worker. If already running or pending, does nothing (KEEP policy).
         */
        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<AiCategorizationWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,  // Don't start a new one if already running
                request
            )
            Log.d(TAG, "Enqueued AI categorization worker")
        }
    }

    override suspend fun doWork(): Result {
        val dao = AppDatabase.getInstance(applicationContext).transactionDao()

        return withContext(Dispatchers.IO) {
            try {
                val miscTransactions = dao.getMiscellaneousNotAiProcessed()
                Log.d(TAG, "Found ${miscTransactions.size} MISCELLANEOUS transactions to process")

                if (miscTransactions.isEmpty()) return@withContext Result.success()

                var geminiCallCount = 0
                var quotaHit = false

                for (txn in miscTransactions) {
                    if (quotaHit) {
                        // Stop processing for this run — will retry on next enqueue
                        Log.w(TAG, "Quota hit — stopping early. Remaining txns will be retried.")
                        break
                    }

                    try {
                        // Check in-memory caches first; only count real API calls toward rate limit
                        val needsGemini = !GeminiCategorizer.isInCache(txn.merchant)

                        val category = resolveCategory(txn)

                        // Apply rate limit only when a real Gemini API call was made
                        if (needsGemini && category != "MISCELLANEOUS") {
                            geminiCallCount++
                            if (geminiCallCount > 1) {
                                Log.d(TAG, "Rate limiting: waiting ${GEMINI_RATE_LIMIT_MS}ms before next Gemini call")
                                delay(GEMINI_RATE_LIMIT_MS)
                            }
                        }

                        // Always mark as AI-processed, even if still MISCELLANEOUS
                        dao.updateCategoryAndMarkAiProcessed(txn.smsId, category)
                        Log.d(TAG, "Updated '${txn.merchant}' -> $category")

                    } catch (e: Exception) {
                        val msg = e.message ?: ""
                        if (msg.contains("Quota", ignoreCase = true) || msg.contains("RESOURCE_EXHAUSTED", ignoreCase = true)) {
                            // Don't mark as processed so it retries on next worker run
                            Log.w(TAG, "Quota exceeded for '${txn.merchant}' — will retry later")
                            quotaHit = true
                        } else {
                            Log.e(TAG, "Failed to process txn ${txn.smsId}: $msg")
                            // Non-quota errors: mark as processed to avoid infinite loop
                            dao.updateCategoryAndMarkAiProcessed(txn.smsId, "MISCELLANEOUS")
                        }
                    }
                }

                Log.d(TAG, "AI categorization run complete. Gemini calls made: $geminiCallCount")
                if (quotaHit) Result.retry() else Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "Worker failed", e)
                Result.retry()  // WorkManager will retry with backoff
            }
        }
    }

    /**
     * Runs Layer 3 (Places API) first, then Layer 4 (Gemini) only if needed.
     * Both respect their SharedPrefs caches — same merchant is never re-queried.
     */
    private suspend fun resolveCategory(txn: TransactionEntity): String {
        // Layer 3: Google Places API
        val placesCategory = PlacesCategorizer.categorize(
            context = applicationContext,
            merchantName = txn.merchant
        )
        if (placesCategory != null) {
            Log.d(TAG, "Layer 3 resolved '${txn.merchant}' -> $placesCategory")
            return placesCategory
        }

        // Layer 4: Gemini LLM fallback
        val geminiCategory = GeminiCategorizer.categorize(
            context = applicationContext,
            merchantName = txn.merchant,
            smsSnippet = txn.description ?: txn.merchant
        )
        Log.d(TAG, "Layer 4 resolved '${txn.merchant}' -> $geminiCategory")
        return geminiCategory
    }
}
