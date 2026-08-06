package com.example.financeapp.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.financeapp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * BackendCategorizerClient — Client interface for the RupAI FastAPI Backend.
 *
 * Proxies all AI categorization, spending analysis, financial advisor, and recurring transaction
 * requests to the backend server. No Google Places or Gemini API keys are held on the Android client.
 *
 * Endpoints targeted:
 * - POST /api/v1/categorize
 * - POST /api/v1/categorize/batch
 * - POST /api/v1/insights/analyze
 * - POST /api/v1/insights/advisor
 * - POST /api/v1/analytics/recurring
 */
object BackendCategorizerClient {
    private const val TAG = "BackendCategorizer"
    private const val PREFS_NAME = "BackendCategorizerCache"

    private var prefs: SharedPreferences? = null

    private fun getPrefs(context: Context): SharedPreferences {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
        return prefs!!
    }

    private fun getJwtToken(): String? {
        return try {
            SupabaseClient.auth.currentAccessTokenOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve Supabase access token", e)
            null
        }
    }

    fun isInCache(context: Context, merchantName: String): Boolean {
        val key = merchantName.trim().lowercase()
        return getPrefs(context).contains(key)
    }

    fun getFromCache(context: Context, merchantName: String): String? {
        val key = merchantName.trim().lowercase()
        return getPrefs(context).getString(key, null)
    }

    fun saveToCache(context: Context, merchantName: String, category: String) {
        val key = merchantName.trim().lowercase()
        getPrefs(context).edit().putString(key, category).apply()
    }

    /**
     * Categorize a single transaction by merchant name + SMS snippet.
     * Uses local SharedPreferences cache first, then calls POST /api/v1/categorize.
     */
    suspend fun categorize(
        context: Context,
        merchantName: String,
        smsSnippet: String = ""
    ): String = withContext(Dispatchers.IO) {
        val cleanMerchant = merchantName.trim()
        if (cleanMerchant.isEmpty()) return@withContext "MISCELLANEOUS"

        // Check local disk cache
        getFromCache(context, cleanMerchant)?.let { cached ->
            Log.d(TAG, "Local cache HIT for '$cleanMerchant' -> $cached")
            return@withContext cached
        }

        val baseUrl = BuildConfig.BACKEND_URL.trimEnd('/')
        val url = URL("$baseUrl/api/v1/categorize")
        val token = getJwtToken()

        try {
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                if (!token.isNullOrEmpty()) {
                    setRequestProperty("Authorization", "Bearer $token")
                }
                doOutput = true
                connectTimeout = 10_000
                readTimeout = 10_000
            }

            val payload = JSONObject().apply {
                put("merchant", cleanMerchant)
                put("sms_snippet", smsSnippet.take(150))
            }.toString()

            conn.outputStream.use { os ->
                os.write(payload.toByteArray(Charsets.UTF_8))
            }

            val statusCode = conn.responseCode
            if (statusCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val category = json.optString("category", "MISCELLANEOUS")

                // Cache locally
                saveToCache(context, cleanMerchant, category)
                Log.d(TAG, "Backend resolved '$cleanMerchant' -> $category")
                return@withContext category
            } else {
                val errorText = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.e(TAG, "Backend returned HTTP $statusCode: $errorText")
                return@withContext "MISCELLANEOUS"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to call backend categorizer for '$cleanMerchant'", e)
            return@withContext "MISCELLANEOUS"
        }
    }

    /**
     * Categorize a batch of transactions in a single HTTP request (POST /api/v1/categorize/batch).
     * Returns a map of merchant name -> category.
     */
    suspend fun categorizeBatch(
        context: Context,
        items: List<Pair<String, String>> // List of (merchant, smsSnippet)
    ): Map<String, String> = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext emptyMap()

        val results = mutableMapOf<String, String>()
        val uncachedItems = mutableListOf<Pair<String, String>>()

        // Filter out locally cached items
        for ((merchant, snippet) in items) {
            val cached = getFromCache(context, merchant)
            if (cached != null) {
                results[merchant] = cached
            } else {
                uncachedItems.add(Pair(merchant, snippet))
            }
        }

        if (uncachedItems.isEmpty()) return@withContext results

        val baseUrl = BuildConfig.BACKEND_URL.trimEnd('/')
        val url = URL("$baseUrl/api/v1/categorize/batch")
        val token = getJwtToken()

        Log.i(TAG, "Initiating batch categorization request to $url (${uncachedItems.size} merchants)")

        try {
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                if (!token.isNullOrEmpty()) {
                    setRequestProperty("Authorization", "Bearer $token")
                }
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 15_000
            }

            val jsonArray = JSONArray()
            for ((merchant, snippet) in uncachedItems) {
                jsonArray.put(JSONObject().apply {
                    put("merchant", merchant)
                    put("sms_snippet", snippet.take(150))
                })
            }

            val payload = JSONObject().apply {
                put("transactions", jsonArray)
            }.toString()

            conn.outputStream.use { os ->
                os.write(payload.toByteArray(Charsets.UTF_8))
            }

            val statusCode = conn.responseCode
            if (statusCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val resultsArray = json.optJSONArray("results") ?: JSONArray()

                for (i in 0 until resultsArray.length()) {
                    val obj = resultsArray.getJSONObject(i)
                    val merchant = obj.optString("merchant")
                    val category = obj.optString("category", "MISCELLANEOUS")
                    if (merchant.isNotEmpty()) {
                        results[merchant] = category
                        saveToCache(context, merchant, category)
                    }
                }
            } else {
                Log.e(TAG, "Batch categorize HTTP $statusCode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Batch categorize failed", e)
        }

        return@withContext results
    }
}
