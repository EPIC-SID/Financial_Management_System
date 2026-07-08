package com.example.financeapp.data

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order

object TransactionRepository {
    
    /**
     * Fetches all transactions for the current user from Supabase.
     */
    suspend fun getTransactions(): List<TransactionData> {
        val userId = SupabaseClient.auth.currentUserOrNull()?.id ?: return emptyList()
        return try {
            SupabaseClient.client.postgrest["transactions"]
                .select {
                    filter { eq("user_id", userId) }
                    order(column = "transaction_date", order = Order.DESCENDING)
                }
                .decodeList<TransactionData>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Inserts list of transaction records into Supabase.
     * Uses upsert to skip duplicate records matching the unique `sms_id` constraint.
     */
    suspend fun insertTransactions(transactions: List<TransactionData>) {
        val userId = SupabaseClient.auth.currentUserOrNull()?.id ?: return
        if (transactions.isEmpty()) return
        
        val txnsWithUser = transactions.map { it.copy(userId = userId) }
        
        try {
            SupabaseClient.client.postgrest["transactions"]
                .upsert(txnsWithUser)
        } catch (e: Exception) {
            // Log or ignore errors from duplicate keys if they fail to upsert
        }
    }
}
