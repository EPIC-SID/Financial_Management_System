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
    private const val MAX_BATCH_CHUNK_SIZE = 25
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 10_000

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
        val cached = getPrefs(context).getString(key, null)
        return cached != null && CategorizationPolicy.isCachableCategory(cached)
    }

    fun getFromCache(context: Context, merchantName: String): String? {
        val key = merchantName.trim().lowercase()
        val cached = getPrefs(context).getString(key, null)
        if (cached != null && !CategorizationPolicy.isCachableCategory(cached)) {
            // Self-heal: purge poisoned or transient cache entries
            getPrefs(context).edit().remove(key).apply()
            return null
        }
        return cached
    }

    fun saveToCache(context: Context, merchantName: String, category: String) {
        val key = merchantName.trim().lowercase()
        if (!CategorizationPolicy.isCachableCategory(category)) {
            Log.d(TAG, "Refusing to cache transient fallback category")
            return
        }
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
        val cleanMerchant = RemoteCategorizationPolicy.merchant(merchantName) ?: return@withContext "MISCELLANEOUS"

        // Check local disk cache
        getFromCache(context, cleanMerchant)?.let { cached ->
            Log.d(TAG, "Local category cache hit")
            return@withContext cached
        }

        val baseUrl = BuildConfig.BACKEND_URL.trimEnd('/')
        if (!context.getSharedPreferences(RemoteCategorizationPolicy.PREFS, Context.MODE_PRIVATE)
                .getBoolean(RemoteCategorizationPolicy.ENABLED, false) ||
            !RemoteCategorizationPolicy.isAllowedUrl(baseUrl, BuildConfig.DEBUG)) return@withContext "MISCELLANEOUS"
        val url = URL("$baseUrl/api/v1/categorize")
        val token = getJwtToken()
        if (token.isNullOrBlank()) return@withContext "MISCELLANEOUS"

        var connection: HttpURLConnection? = null
        try {
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                instanceFollowRedirects = false
                setRequestProperty("Content-Type", "application/json")
                if (!token.isNullOrEmpty()) {
                    setRequestProperty("Authorization", "Bearer $token")
                }
                doOutput = true
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
            }
            connection = conn

            val payload = JSONObject().apply {
                put("merchant", cleanMerchant)
                put("sms_snippet", "")
            }.toString()

            conn.outputStream.use { os ->
                os.write(payload.toByteArray(Charsets.UTF_8))
            }

            val statusCode = conn.responseCode
            if (statusCode == 200) {
                val responseText = boundedResponse(conn)
                val json = JSONObject(responseText)
                val category = json.optString("category", "MISCELLANEOUS")

                // Cache locally only if not a transient fallback
                saveToCache(context, cleanMerchant, category)
                Log.d(TAG, "Backend category resolved")
                return@withContext if (CategorizationPolicy.isCachableCategory(category)) category else "MISCELLANEOUS"
            } else {
                Log.e(TAG, "Backend returned HTTP $statusCode")
                return@withContext "MISCELLANEOUS"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Backend categorization unavailable")
            return@withContext "MISCELLANEOUS"
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Categorize a batch of transactions in bounded chunked HTTP requests (POST /api/v1/categorize/batch).
     * Returns a map of merchant name -> category.
     */
    suspend fun categorizeBatch(
        context: Context,
        items: List<Pair<String, String>> // List of (merchant, smsSnippet)
    ): Map<String, String> = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext emptyMap()
        if (!context.getSharedPreferences(RemoteCategorizationPolicy.PREFS, Context.MODE_PRIVATE)
                .getBoolean(RemoteCategorizationPolicy.ENABLED, false) ||
            !RemoteCategorizationPolicy.isAllowedUrl(BuildConfig.BACKEND_URL, BuildConfig.DEBUG)) return@withContext emptyMap()

        val results = mutableMapOf<String, String>()
        val uncachedItems = mutableListOf<Pair<String, String>>()

        // Filter out locally cached items
        for ((merchant, _) in items) {
            if (RemoteCategorizationPolicy.merchant(merchant) == null) continue
            val cached = getFromCache(context, merchant)
            if (cached != null) {
                results[merchant] = cached
            } else {
                uncachedItems.add(Pair(merchant, ""))
            }
        }

        if (uncachedItems.isEmpty()) return@withContext results

        val baseUrl = BuildConfig.BACKEND_URL.trimEnd('/')
        val url = URL("$baseUrl/api/v1/categorize/batch")
        val token = getJwtToken()
        if (token.isNullOrBlank()) return@withContext results

        Log.i(TAG, "Initiating batch categorization request to $url (${uncachedItems.size} merchants in chunks of $MAX_BATCH_CHUNK_SIZE)")

        val chunks = uncachedItems.chunked(MAX_BATCH_CHUNK_SIZE)

        for (chunk in chunks) {
            if (!context.getSharedPreferences(RemoteCategorizationPolicy.PREFS, Context.MODE_PRIVATE)
                    .getBoolean(RemoteCategorizationPolicy.ENABLED, false)) break
            var connection: HttpURLConnection? = null
            try {
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    instanceFollowRedirects = false
                    setRequestProperty("Content-Type", "application/json")
                    if (!token.isNullOrEmpty()) {
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                    doOutput = true
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                }
                connection = conn

                val jsonArray = JSONArray()
                for ((merchant, _) in chunk) {
                    jsonArray.put(JSONObject().apply {
                        put("merchant", merchant)
                        put("sms_snippet", "")
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
                    val responseText = boundedResponse(conn)
                    val json = JSONObject(responseText)
                    val resultsArray = json.optJSONArray("results") ?: JSONArray()

                    for (i in 0 until resultsArray.length()) {
                        val obj = resultsArray.getJSONObject(i)
                        val merchant = obj.optString("merchant")
                        val category = obj.optString("category", "MISCELLANEOUS")
                        if (chunk.any { it.first == merchant } && CategorizationPolicy.isCachableCategory(category)) {
                            results[merchant] = category
                            saveToCache(context, merchant, category)
                        }
                    }
                } else {
                    Log.e(TAG, "Batch categorize chunk HTTP $statusCode")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Batch categorization unavailable")
            } finally {
                connection?.disconnect()
            }
        }

        return@withContext results
    }

    private fun boundedResponse(connection: HttpURLConnection): String = connection.inputStream.use { input ->
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        while (true) {
            val count = input.read(buffer, 0, minOf(buffer.size, 32769 - output.size()))
            if (count < 0) break
            output.write(buffer, 0, count)
            require(output.size() <= 32768) { "Response too large" }
        }
        output.toString("UTF-8")
    }
}
