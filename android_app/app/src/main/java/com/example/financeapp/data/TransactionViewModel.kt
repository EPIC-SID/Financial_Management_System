package com.example.financeapp.data

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.SmsParser.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Single source of truth for transaction data across the entire app.
 * Both HomeScreen and TransactionsScreen observe [transactions].
 *
 * Design:
 * - [transactions] is a StateFlow backed by Room's Flow — auto-updates on any DB change,
 *   including when WorkManager silently updates categories in the background.
 * - [syncSms] performs the fast (Layer 1+2) sync inline, then enqueues the
 *   AiCategorizationWorker for Layer 3+4 processing in the background.
 */
class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val dao = db.transactionDao()

    private val _isSyncing = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val _syncMessage = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage

    /**
     * Live list of all transactions from Room DB, newest first.
     * Automatically emits whenever any row is inserted or updated (e.g. AI categorization).
     */
    val transactions: StateFlow<List<TransactionEntity>> =
        dao.getAllTransactions()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    /**
     * Preloads categorizer caches if needed.
     */
    fun loadCaches(context: android.content.Context) {
        // BackendCategorizerClient handles local SharedPreferences caching automatically
    }

    /**
     * Main sync function.
     *
     * Step 1 (fast, ~50ms even for 150 SMS):
     *   - Read SMS from inbox since [startTimeMs]
     *   - Run Layer 1 (VPA) + Layer 2 (Regex) categorization synchronously
     *   - Bulk insert into Room (IGNORE on duplicate smsId)
     *   - UI updates immediately via Room Flow
     *
     * Step 2 (background):
     *   - Enqueue AiCategorizationWorker for MISCELLANEOUS rows
     *   - Worker runs Layer 3 (Places) + Layer 4 (Gemini) off the main thread
     *   - Room Flow pushes category updates to UI silently
     */
    fun syncSms(startTimeMs: Long) {
        if (_isSyncing.value) return  // Prevent concurrent syncs

        viewModelScope.launch(Dispatchers.IO) {
            _isSyncing.value = true
            _syncMessage.value = null

            try {
                val context = getApplication<Application>()

                // Read SMS inbox
                val rawSmsList = SmsReader.read(context, startTimeMs)

                // Get existing smsIds for dedup check
                val existingIds = dao.getAllSmsIds().toHashSet()

                // Filter new SMS only, then apply Layer 1+2 parsing
                val newEntities = rawSmsList
                    .filter { it.id !in existingIds }
                    .mapNotNull { raw ->
                        SmsParser.parse(raw.body, raw.id, raw.date)
                            ?.let { txnData ->
                                // Layer 1: VPA check
                                val vpaCategory = VpaResolver.resolveFromSms(raw.body)
                                val finalTxn = if (vpaCategory != null && txnData.category == "MISCELLANEOUS") {
                                    txnData.copy(category = vpaCategory)
                                } else {
                                    txnData
                                }
                                finalTxn.toEntity()
                            }
                    }

                if (newEntities.isNotEmpty()) {
                    dao.insertAll(newEntities)
                    Log.d("TransactionViewModel", "Inserted ${newEntities.size} new transactions")
                }

                // Only check AI backlog if we actually inserted new transactions this sync.
                // Checking all-time pendingAiCount on every sync caused the "Categorizing 283..."
                // toast to fire on every period-change button press, even with 0 new rows.
                val newMiscCount = newEntities.count { it.category == "MISCELLANEOUS" && !it.isCategorizedByAi }

                _syncMessage.value = when {
                    newEntities.isEmpty()   -> "Already up to date."
                    newMiscCount > 0        -> "Synced ${newEntities.size} transaction(s). Categorizing $newMiscCount in background…"
                    else                    -> "Synced ${newEntities.size} transaction(s)."
                }

                // Enqueue AI worker only when there are newly inserted rows that need categorization.
                // The worker itself uses ExistingWorkPolicy.KEEP so it won't run if already active.
                if (newMiscCount > 0) {
                    AiCategorizationWorker.enqueue(context)
                }

            } catch (e: Exception) {
                Log.e("TransactionViewModel", "Sync failed", e)
                _syncMessage.value = "Sync failed: ${e.localizedMessage}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    /** Clears the one-shot sync message after it has been shown. */
    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    /**
     * Resets the local Room database and clears all SharedPreferences caches.
     */
    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            dao.deleteAll()
            context.getSharedPreferences("GeminiCategorizerCache", android.content.Context.MODE_PRIVATE).edit().clear().apply()
            context.getSharedPreferences("PlacesCategorizerCache", android.content.Context.MODE_PRIVATE).edit().clear().apply()
            context.getSharedPreferences("RupAiPrefs", android.content.Context.MODE_PRIVATE).edit().clear().apply()
            _syncMessage.value = "Local database & cache reset complete."
        }
    }
}
