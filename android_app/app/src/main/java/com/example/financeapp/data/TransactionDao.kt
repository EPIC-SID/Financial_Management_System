package com.example.financeapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    /**
     * Live stream of all transactions ordered newest-first.
     * Room automatically notifies collectors whenever the table changes.
     */
    @Query("SELECT * FROM transactions ORDER BY transactionDate DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    /**
     * Insert a batch of transactions. IGNORE = if smsId already exists, skip silently.
     * This is the core deduplication mechanism — same SMS never stored twice.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    /**
     * Returns only transactions that are MISCELLANEOUS AND have not yet been
     * processed by the AI pipeline (layers 3+4). Prevents re-calling APIs.
     */
    @Query("SELECT * FROM transactions WHERE category = 'MISCELLANEOUS' AND isCategorizedByAi = 0")
    suspend fun getMiscellaneousNotAiProcessed(): List<TransactionEntity>

    /**
     * Called by AiCategorizationWorker after processing a transaction.
     * Marks it as done so it is never sent to an AI API again.
     */
    @Query("UPDATE transactions SET category = :category, isCategorizedByAi = 1 WHERE smsId = :smsId")
    suspend fun updateCategoryAndMarkAiProcessed(smsId: String, category: String)

    /**
     * Returns the set of all known smsIds in the DB.
     * Used for fast client-side dedup check before parsing a batch of SMS.
     */
    @Query("SELECT smsId FROM transactions")
    suspend fun getAllSmsIds(): List<String>

    /** Total number of transactions stored locally. */
    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getCount(): Int

    /** Reset isCategorizedByAi flag for all MISCELLANEOUS transactions so AI can re-process them. */
    @Query("UPDATE transactions SET isCategorizedByAi = 0 WHERE category = 'MISCELLANEOUS'")
    suspend fun resetAiCategorizationFlags()

    /** Delete all local transactions (reset DB). */
    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}
