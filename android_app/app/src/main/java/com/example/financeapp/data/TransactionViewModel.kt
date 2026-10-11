package com.example.financeapp.data

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.SmsParser.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
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
                val rawSmsList = SmsReader.read(context, startTimeMs, filterByBankSenders = true)

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

                val newMiscCount = newEntities.count { it.category == "MISCELLANEOUS" && !it.isCategorizedByAi }
                val pendingMiscCount = dao.getMiscellaneousNotAiProcessed().size

                _syncMessage.value = CategorizationRetryPolicy.buildSyncMessage(
                    newEntitiesCount = newEntities.size,
                    newMiscCount = newMiscCount,
                    pendingMiscCount = pendingMiscCount
                )

                // Enqueue AI worker if newly inserted rows need categorization OR if pending rows need recovery
                if (CategorizationRetryPolicy.shouldEnqueueRetry(newMiscCount, pendingMiscCount)) {
                    AiCategorizationWorker.enqueue(context)
                }

            } catch (e: Exception) {
                Log.e("TransactionViewModel", "SMS sync unavailable")
                _syncMessage.value = "Could not sync SMS. Check SMS permission and try again."
            } finally {
                _isSyncing.value = false
            }
        }
    }

    /**
     * Retries categorization for any pending MISCELLANEOUS rows that have not yet been AI-processed.
     * Can be called independently of SMS sync.
     */
    fun retryPendingCategorization() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val pending = dao.getMiscellaneousNotAiProcessed()
            if (pending.isNotEmpty()) {
                Log.d("TransactionViewModel", "Retrying categorization for ${pending.size} pending transactions")
                AiCategorizationWorker.enqueue(context)
                _syncMessage.value = "Retrying categorization for ${pending.size} transaction(s) in background…"
            } else {
                _syncMessage.value = "No pending categorizations to retry."
            }
        }
    }

    /** Clears the one-shot sync message after it has been shown. */
    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    /**
     * Manually updates the category for a merchant.
     * Saves the preference to local cache so all future SMS from this merchant are automatically categorized,
     * and updates all existing transactions in Room DB matching this merchant.
     */
    fun updateCategoryForMerchant(merchant: String, newCategory: String) {
        require(CategorizationPolicy.VALID_CATEGORIES.contains(newCategory))
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            try {
                dao.updateCategoryForMerchant(merchant, newCategory)
                BackendCategorizerClient.saveToCache(context, merchant, newCategory)
                _syncMessage.value = "Merchant category saved on this phone."
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _syncMessage.value = "Could not save the category. Try again." }
        }
    }

    fun recheckLocalCategories() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var resolved = 0
                dao.getUnclassifiedDebits().forEach { row ->
                    CategorizationPolicy.knownMerchantCategory(row.merchant)?.let { category ->
                        dao.repairLocalCategory(row.smsId, category)
                        resolved++
                    }
                }
                _syncMessage.value = "Rechecked locally: $resolved categories resolved."
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _syncMessage.value = "Local category recheck unavailable. Try again." }
        }
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
