package com.example.financeapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local Room DB entity. smsId is the primary key — guarantees deduplication.
 * The same SMS will never be inserted twice due to OnConflictStrategy.IGNORE in the DAO.
 *
 * isCategorizedByAi: false = not yet processed by AI pipeline (layers 3+4)
 *                    true  = already processed, never sent to API again
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val smsId: String,
    val merchant: String,
    val amount: Double,
    val category: String,
    val description: String?,
    val transactionDate: String,
    val isCategorizedByAi: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
