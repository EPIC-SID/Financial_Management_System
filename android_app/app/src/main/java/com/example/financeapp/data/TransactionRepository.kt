package com.example.financeapp.data

import android.util.Log
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order

object TransactionRepository {

    private const val TAG = "TransactionRepository"

    /**
     * Fetches all transactions for the current user from Supabase.
     * Returns [Resource.Success] with the list, or [Resource.Error] on failure.
     */
    suspend fun getTransactions(): Resource<List<TransactionData>> {
        val userId = SupabaseClient.auth.currentUserOrNull()?.id
            ?: return Resource.Error("User not authenticated")
        return try {
            val data = SupabaseClient.client.postgrest["transactions"]
                .select {
                    filter { eq("user_id", userId) }
                    order(column = "transaction_date", order = Order.DESCENDING)
                }
                .decodeList<TransactionData>()
            Resource.Success(data)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch transactions", e)
            Resource.Error("Failed to load transactions: ${e.localizedMessage}", e)
        }
    }

    suspend fun insertTransactions(transactions: List<TransactionData>): Resource<Int> {
        val userId = SupabaseClient.auth.currentUserOrNull()?.id
            ?: return Resource.Error("User not authenticated")
        if (transactions.isEmpty()) return Resource.Success(0)

        // Deduplicate within the current batch to prevent Postgres from throwing multiple target conflicts
        val uniqueTransactions = transactions.distinctBy { it.smsId }
        val txnsWithUser = uniqueTransactions.map { it.copy(userId = userId) }

        return try {
            SupabaseClient.client.postgrest["transactions"]
                .upsert(txnsWithUser, onConflict = "sms_id", ignoreDuplicates = true)
            Resource.Success(txnsWithUser.size)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert transactions", e)
            Resource.Error("Failed to sync transactions: ${e.localizedMessage}", e)
        }
    }
}
