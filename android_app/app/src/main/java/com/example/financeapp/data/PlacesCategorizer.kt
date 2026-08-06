package com.example.financeapp.data

import android.content.Context

/**
 * Legacy PlacesCategorizer — now handled server-side by the FastAPI backend pipeline.
 */
object PlacesCategorizer {
    fun loadCache(context: Context) {
        // Handled automatically by BackendCategorizerClient
    }

    suspend fun categorize(context: Context, merchantName: String): String? {
        // Backend handles Places API + Gemini pipeline
        return null
    }
}
