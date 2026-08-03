package com.example.financeapp.data

import android.content.Context
import android.util.Log
import com.example.financeapp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Layer 3 of the categorization pipeline.
 * Uses the Google Places Text Search API (New) to identify local merchants
 * by name and map their place types to app categories.
 *
 * This is the key to identifying local Indian shops like "Trimurti Store",
 * "Ganesh Medical", or "Vaishnav Dairy" that regex and LLMs can't recognize.
 *
 * Results are cached in SharedPreferences — same merchant is never queried again.
 * Falls back gracefully (returns null) if the API key is missing or call fails.
 */
object PlacesCategorizer {

    private const val TAG = "PlacesCategorizer"
    private const val PREFS_NAME = "PlacesCategorizerCache"
    private const val API_ENDPOINT = "https://places.googleapis.com/v1/places:searchText"

    // Google place type → App category mapping
    private val placeTypeToCategory = mapOf(
        // Food
        "restaurant" to "FOOD",
        "food" to "FOOD",
        "bakery" to "FOOD",
        "cafe" to "FOOD",
        "meal_delivery" to "FOOD",
        "meal_takeaway" to "FOOD",
        "bar" to "FOOD",
        "night_club" to "ENTERTAINMENT",
        // Travel
        "gas_station" to "TRAVEL",
        "transit_station" to "TRAVEL",
        "bus_station" to "TRAVEL",
        "train_station" to "TRAVEL",
        "subway_station" to "TRAVEL",
        "airport" to "TRAVEL",
        "parking" to "TRAVEL",
        "car_rental" to "TRAVEL",
        // Health
        "pharmacy" to "HEALTH",
        "hospital" to "HEALTH",
        "doctor" to "HEALTH",
        "dentist" to "HEALTH",
        "health" to "HEALTH",
        "physiotherapist" to "HEALTH",
        "veterinary_care" to "HEALTH",
        // Shopping
        "store" to "SHOPPING",
        "grocery_or_supermarket" to "SHOPPING",
        "supermarket" to "SHOPPING",
        "convenience_store" to "SHOPPING",
        "shopping_mall" to "SHOPPING",
        "clothing_store" to "SHOPPING",
        "department_store" to "SHOPPING",
        "electronics_store" to "SHOPPING",
        "book_store" to "SHOPPING",
        "hardware_store" to "SHOPPING",
        "furniture_store" to "SHOPPING",
        "shoe_store" to "SHOPPING",
        "jewelry_store" to "SHOPPING",
        // Entertainment
        "movie_theater" to "ENTERTAINMENT",
        "amusement_park" to "ENTERTAINMENT",
        "bowling_alley" to "ENTERTAINMENT",
        "casino" to "ENTERTAINMENT",
        "stadium" to "ENTERTAINMENT",
        // Education
        "school" to "EDUCATION",
        "university" to "EDUCATION",
        "library" to "EDUCATION",
        "book_store" to "EDUCATION",
        // Bills / Finance
        "bank" to "BILLS",
        "atm" to "BILLS",
        "finance" to "BILLS",
        "insurance_agency" to "BILLS",
        "accounting" to "BILLS"
    )

    // In-memory cache for this session
    private val memoryCache = HashMap<String, String?>()

    /**
     * Loads the persistent cache from SharedPreferences into memory.
     * Call once on app start or before first sync.
     */
    fun loadCache(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.all.forEach { (key, value) ->
            if (value is String) memoryCache[key] = value
            else if (value == null) memoryCache[key] = null
        }
        Log.d(TAG, "Cache loaded: ${memoryCache.size} entries")
    }

    /**
     * Categorizes a merchant name using Google Places Text Search API.
     *
     * @return A category string (e.g. "FOOD") or null if no confident match found.
     *         Returns null (not "MISCELLANEOUS") so the pipeline can try Layer 4.
     */
    suspend fun categorize(context: Context, merchantName: String): String? {
        val apiKey = BuildConfig.PLACES_API_KEY
        if (apiKey.isBlank()) {
            Log.w(TAG, "PLACES_API_KEY not set — skipping Layer 3")
            return null
        }

        val normalizedKey = merchantName.trim().lowercase()

        // Check memory cache first
        if (memoryCache.containsKey(normalizedKey)) {
            val cached = memoryCache[normalizedKey]
            Log.d(TAG, "Cache HIT for '$merchantName' -> $cached")
            return cached
        }

        return withContext(Dispatchers.IO) {
            try {
                val result = callPlacesApi(apiKey, merchantName)
                // Save to both caches (null = "no match", also cached to avoid re-calling)
                memoryCache[normalizedKey] = result
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putString(normalizedKey, result ?: "__NULL__").apply()
                Log.d(TAG, "Places API: '$merchantName' -> $result")
                result
            } catch (e: Exception) {
                Log.e(TAG, "Places API call failed for '$merchantName': ${e.message}")
                null
            }
        }
    }

    private fun callPlacesApi(apiKey: String, merchantName: String): String? {
        val url = URL(API_ENDPOINT)
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("X-Goog-Api-Key", apiKey)
            // Only request the 'types' field — minimizes response size and cost
            connection.setRequestProperty("X-Goog-FieldMask", "places.types")
            connection.doOutput = true
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val requestBody = JSONObject().apply {
                put("textQuery", merchantName)
                put("languageCode", "en")
                put("regionCode", "IN")  // Bias results to India
                put("maxResultCount", 1) // Only need the top result
            }.toString()

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody)
                writer.flush()
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "Places API HTTP ${connection.responseCode} for '$merchantName'")
                return null
            }

            val responseText = connection.inputStream.bufferedReader().readText()
            return parsePlacesResponse(responseText)
        } finally {
            connection.disconnect()
        }
    }

    private fun parsePlacesResponse(responseJson: String): String? {
        val root = JSONObject(responseJson)
        val places = root.optJSONArray("places") ?: return null
        if (places.length() == 0) return null

        val firstPlace = places.getJSONObject(0)
        val types = firstPlace.optJSONArray("types") ?: return null

        // Try each type in order; return the first category match
        for (i in 0 until types.length()) {
            val placeType = types.getString(i).lowercase()
            val category = placeTypeToCategory[placeType]
            if (category != null) return category
        }
        return null
    }
}
