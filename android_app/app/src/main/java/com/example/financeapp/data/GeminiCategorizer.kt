package com.example.financeapp.data

import android.content.Context
import android.util.Log
import com.example.financeapp.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel

object GeminiCategorizer {
    private const val TAG = "GeminiCategorizer"
    private const val PREFS_NAME = "GeminiCategorizerCache"

    private val validCategories = setOf(
        "FOOD", "TRAVEL", "SHOPPING", "BILLS", "ENTERTAINMENT",
        "INVESTMENTS", "HEALTH", "EDUCATION", "MISCELLANEOUS"
    )

    // In-memory cache: merchant name -> category
    private val memoryCache = HashMap<String, String>()

    // Lazy-initialized GenerativeModel (only created on first AI call)
    private val model by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash-latest",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    /**
     * Loads the persistent cache from SharedPreferences into memory.
     * Call this once on app start or before the first sync.
     */
    fun loadCache(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.all.forEach { (key, value) ->
            if (value is String) memoryCache[key] = value
        }
        Log.d(TAG, "Cache loaded with ${memoryCache.size} entries")
    }

    /**
     * Categorizes a merchant using a cached result or a Gemini API call.
     * Falls back to "MISCELLANEOUS" if the API call fails.
     */
    suspend fun categorize(context: Context, merchantName: String, smsSnippet: String): String {
        // 1. Check in-memory cache first (fastest)
        val normalizedKey = merchantName.trim().lowercase()
        memoryCache[normalizedKey]?.let { cached ->
            Log.d(TAG, "Cache HIT for '$merchantName' -> $cached")
            return cached
        }

        // 2. No cache — call Gemini API
        Log.d(TAG, "Cache MISS for '$merchantName', calling Gemini API...")
        val category = try {
            val prompt = """
                You are a financial transaction categorizer for Indian users.
                Given a merchant name and a brief transaction description, reply with ONLY one word from this exact list:
                FOOD, TRAVEL, SHOPPING, BILLS, ENTERTAINMENT, INVESTMENTS, HEALTH, EDUCATION, MISCELLANEOUS

                Rules:
                - Person's name (e.g. "Mr Ramdas", "Atharva Anil") -> MISCELLANEOUS
                - Food stall, dhaba, sweet shop, bakery, canteen, vending machine -> FOOD
                - Transport, cab, fuel, parking, metro, train, flight -> TRAVEL
                - Hospital, clinic, pharmacy, doctor, medicine -> HEALTH
                - School, college, tuition, online course, library -> EDUCATION
                - If uncertain -> MISCELLANEOUS
                - Reply with ONLY the single category word. No punctuation. No explanation.

                Merchant: "$merchantName"
                Description: "${smsSnippet.take(100)}"
                Category:
            """.trimIndent()

            val response = model.generateContent(prompt)
            val rawReply = response.text?.trim()?.uppercase() ?: "MISCELLANEOUS"

            // Validate the response is one of our known categories
            if (rawReply in validCategories) rawReply else "MISCELLANEOUS"
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API call failed for '$merchantName': ${e.message}")
            "MISCELLANEOUS"
        }

        // 3. Save to memory cache and SharedPreferences
        memoryCache[normalizedKey] = category
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(normalizedKey, category).apply()

        Log.d(TAG, "Gemini categorized '$merchantName' -> $category")
        return category
    }

    /**
     * Forces a fresh Gemini API call for a merchant, bypassing the cache.
     * Used during re-categorization of already-stored MISCELLANEOUS transactions.
     */
    suspend fun recategorize(context: Context, merchantName: String, smsSnippet: String): String {
        val normalizedKey = merchantName.trim().lowercase()
        Log.d(TAG, "Forced re-categorization for '$merchantName'...")
        val category = try {
            val prompt = """
                You are a financial transaction categorizer for Indian users.
                Given a merchant name and a brief transaction description, reply with ONLY one word from this exact list:
                FOOD, TRAVEL, SHOPPING, BILLS, ENTERTAINMENT, INVESTMENTS, HEALTH, EDUCATION, MISCELLANEOUS

                Rules:
                - Person's name (e.g. "Mr Ramdas", "Atharva Anil") -> MISCELLANEOUS
                - Food stall, dhaba, sweet shop, bakery, canteen, vending machine -> FOOD
                - Transport, cab, fuel, parking, metro, train, flight -> TRAVEL
                - Hospital, clinic, pharmacy, doctor, medicine -> HEALTH
                - School, college, tuition, online course, library -> EDUCATION
                - If uncertain -> MISCELLANEOUS
                - Reply with ONLY the single category word. No punctuation. No explanation.

                Merchant: "$merchantName"
                Description: "${smsSnippet.take(100)}"
                Category:
            """.trimIndent()
            val response = model.generateContent(prompt)
            val rawReply = response.text?.trim()?.uppercase() ?: "MISCELLANEOUS"
            if (rawReply in validCategories) rawReply else "MISCELLANEOUS"
        } catch (e: Exception) {
            Log.e(TAG, "Gemini recategorize failed for '$merchantName': ${e.message}")
            "MISCELLANEOUS"
        }
        // Update cache with fresh result
        memoryCache[normalizedKey] = category
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(normalizedKey, category).apply()
        Log.d(TAG, "Recategorized '$merchantName' -> $category")
        return category
    }
}
