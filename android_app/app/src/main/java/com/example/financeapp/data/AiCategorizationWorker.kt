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
 * - Rate-limited to ~4 seconds between Gemini API calls to respect free tier quota (15 req/min).
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

                // Prepare batch request items: List of (merchant, snippet)
                val batchItems = miscTransactions.map { txn ->
                    Pair(txn.merchant, txn.description ?: txn.merchant)
                }

                // Call backend batch endpoint (handles local cache + backend shared DB cache + Gemini)
                val categoryMap = BackendCategorizerClient.categorizeBatch(
                    context = applicationContext,
                    items = batchItems
                )

                if (categoryMap.isEmpty() && batchItems.isNotEmpty()) {
                    Log.w(TAG, "Backend returned empty category map (network or server error) — will retry later.")
                    return@withContext Result.retry()
                }

                // Update Room database with resolved categories (only for confident non-transient classifications)
                var processedCount = 0
                for (txn in miscTransactions) {
                    val resolvedCategory = categoryMap[txn.merchant]
                    if (resolvedCategory != null && CategorizationPolicy.isCachableCategory(resolvedCategory)) {
                        dao.updateCategoryAndMarkAiProcessed(txn.smsId, resolvedCategory)
                        Log.d(TAG, "Updated '${txn.merchant}' -> $resolvedCategory")
                        processedCount++
                    } else {
                        Log.d(TAG, "Merchant '${txn.merchant}' received unconfirmed/transient category ($resolvedCategory); keeping eligible for retry.")
                    }
                }

                Log.d(TAG, "AI categorization worker run complete. Processed $processedCount transactions.")
                Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "Worker failed", e)
                Result.retry()  // WorkManager will retry with backoff
            }
        }
    }
}
