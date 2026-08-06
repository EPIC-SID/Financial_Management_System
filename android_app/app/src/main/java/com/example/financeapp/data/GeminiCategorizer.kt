package com.example.financeapp.data

import android.content.Context

/**
 * Legacy GeminiCategorizer — now delegates to BackendCategorizerClient.
 * Client-side direct Gemini calls have been migrated to the FastAPI backend
 * for security, shared caching, and quota protection.
 */
object GeminiCategorizer {
    fun loadCache(context: Context) {
        // Handled automatically by BackendCategorizerClient
    }

    fun isInCache(merchantName: String): Boolean {
        return false
    }

    suspend fun categorize(context: Context, merchantName: String, smsSnippet: String): String {
        return BackendCategorizerClient.categorize(context, merchantName, smsSnippet)
    }

    suspend fun recategorize(context: Context, merchantName: String, smsSnippet: String): String {
        return BackendCategorizerClient.categorize(context, merchantName, smsSnippet)
    }
}
