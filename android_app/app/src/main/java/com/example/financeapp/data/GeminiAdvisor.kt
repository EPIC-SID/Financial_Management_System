package com.example.financeapp.data

import com.example.financeapp.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import android.util.Log

/**
 * Uses Gemini AI to generate personalized financial advice based on real spending data.
 */
object GeminiAdvisor {

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    /**
     * Generates 3 financial suggestions based on income and transaction history.
     */
    suspend fun getSuggestions(
        income: Double,
        expenses: List<TransactionEntity>
    ): List<AiSuggestion> = withContext(Dispatchers.IO) {
        if (BuildConfig.GEMINI_API_KEY.isBlank() || expenses.isEmpty()) {
            return@withContext emptyList()
        }

        val expenseSummary = expenses
            .filter { it.amount < 0 }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { kotlin.math.abs(it.amount) } }

        val prompt = """
            You are a helpful financial advisor. Based on the following monthly data:
            Income: ₹${income}
            Spending by Category: ${expenseSummary.map { "${it.key}: ₹${it.value}" }.joinToString(", ")}
            
            Provide exactly 3 actionable financial suggestions. 
            Format your response as a JSON array of objects with keys: "title", "description", and "type".
            "type" can be "SAVINGS", "WARNING", or "ADVICE".
            Keep descriptions short (max 15 words).
            Respond ONLY with the JSON array.
        """.trimIndent()

        try {
            val response = generativeModel.generateContent(prompt)
            val text = response.text ?: return@withContext emptyList()
            
            // Extract JSON array
            val jsonStart = text.indexOf("[")
            val jsonEnd = text.lastIndexOf("]") + 1
            if (jsonStart == -1 || jsonEnd == 0) return@withContext emptyList()
            
            val jsonArray = JSONArray(text.substring(jsonStart, jsonEnd))
            val list = mutableListOf<AiSuggestion>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(AiSuggestion(
                    title = obj.optString("title", "Insight"),
                    description = obj.optString("description", ""),
                    type = obj.optString("type", "ADVICE")
                ))
            }
            list
        } catch (e: Exception) {
            Log.e("GeminiAdvisor", "AI generation failed", e)
            emptyList()
        }
    }
}

data class AiSuggestion(
    val title: String,
    val description: String,
    val type: String
)
