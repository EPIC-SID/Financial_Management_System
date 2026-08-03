package com.example.financeapp.data

import android.content.Context
import android.util.Log
import androidx.work.*
import kotlinx.coroutines.Dispatchers
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
 */
class AiCategorizationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "AiCategorizationWorker"
        private const val WORK_NAME = "ai_categorization"

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

                for (txn in miscTransactions) {
                    try {
                        val category = resolveCategory(txn)
                        // Always mark as AI-processed, even if still MISCELLANEOUS
                        // This prevents infinite re-processing of truly unresolvable transactions
                        dao.updateCategoryAndMarkAiProcessed(txn.smsId, category)
                        Log.d(TAG, "Updated '${txn.merchant}' -> $category")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to process txn ${txn.smsId}: ${e.message}")
                        // Mark as processed anyway to avoid repeated failures
                        dao.updateCategoryAndMarkAiProcessed(txn.smsId, "MISCELLANEOUS")
                    }
                }

                Log.d(TAG, "AI categorization complete for ${miscTransactions.size} transactions")
                Result.success()
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
