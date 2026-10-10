package com.example.financeapp.data.remote

import com.example.financeapp.BuildConfig
import com.example.financeapp.data.SupabaseClient
import com.example.financeapp.domain.ai.IntentRouter
import com.example.financeapp.domain.ai.LocalAdvisor
import com.example.financeapp.domain.ai.NumberVerifier
import com.example.financeapp.domain.model.CopilotIntent
import com.example.financeapp.domain.model.ResponseSource
import com.example.financeapp.domain.model.ResultBundle
import com.example.financeapp.domain.model.StructuredResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.net.HttpURLConnection
import java.net.URL

class BackendClient(
    private val transport: suspend (String, JsonObject) -> JsonObject = { path, body -> post(path, body) },
    private val localAdvisor: LocalAdvisor = LocalAdvisor(),
    private val verifier: NumberVerifier = NumberVerifier()
) {
    suspend fun interpret(question: String): CopilotIntent {
        val localIntent = IntentRouter().route(question)
        return try {
            // Raw questions can contain PII; only the local allow-listed intent leaves the device.
            val response = transport("/api/v1/copilot/interpret", buildJsonObject { put("intent", localIntent.name) })
            CopilotIntent.valueOf(response.getValue("intent").jsonPrimitive.content)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            localIntent
        }
    }

    suspend fun answer(question: String, resultProvider: (CopilotIntent) -> ResultBundle): StructuredResponse {
        val intent = interpret(question)
        return narrate(intent, resultProvider(intent))
    }

    suspend fun narrate(intent: CopilotIntent, bundle: ResultBundle): StructuredResponse {
        val local = localAdvisor.advise(intent, bundle)
        return try {
            val horizon = Regex("([1-9]|1[0-9]|2[0-4]) months?").matchEntire(bundle.horizon)
                ?: return local
            if (bundle.recommendedPlanId !in setOf("Plan A", "Plan B", "Status quo")) return local
            val payload = buildJsonObject {
                put("intent", intent.name)
                put("bundle", buildJsonObject {
                    put("liquidRunwayMonths", bundle.liquidRunwayMonths)
                    put("projectedShortfallRisk", bundle.projectedShortfallRisk)
                    put("recommendedPlanId", bundle.recommendedPlanId)
                    put("savingsRate", bundle.savingsRate)
                    put("bankCash", bundle.bankCash)
                    put("cardDues", bundle.cardDues)
                    put("monthlyLoanEmis", bundle.monthlyLoanEmis)
                    put("monthlySip", bundle.monthlySip)
                    put("avgDiscretionary", bundle.avgDiscretionary)
                    put("forecastP10", bundle.forecastP10)
                    put("forecastP50", bundle.forecastP50)
                    put("forecastP90", bundle.forecastP90)
                    put("horizonMonths", horizon.groupValues[1].toInt())
                    put("confidenceLabel", bundle.confidenceLabel.name)
                })
            }
            val response = transport("/api/v1/copilot/narrate", payload)
            fun text(section: String): String = response.getValue(section).jsonArray.single()
                .jsonObject.getValue("text").jsonPrimitive.also { require(it.isString && it.content.length <= 2000) }.content
            // The model may rewrite text; engine metadata remains authoritative.
            verifier.verifyOrFallback(local.copy(
                observed = listOf(local.observed.single().copy(text = text("observed"))),
                predicted = listOf(local.predicted.single().copy(text = text("predicted"))),
                recommended = listOf(local.recommended.single().copy(text = text("recommended"))),
                source = ResponseSource.REMOTE
            ), intent, bundle)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            local
        }
    }

    companion object {
        private suspend fun post(path: String, body: JsonObject): JsonObject = withContext(Dispatchers.IO) {
            val url = URL(BuildConfig.BACKEND_URL.trimEnd('/') + path)
            require(url.protocol == "https" || (BuildConfig.DEBUG && url.protocol == "http" &&
                url.host in setOf("10.0.2.2", "127.0.0.1", "localhost")))
            val token = SupabaseClient.auth.currentAccessTokenOrNull()
            require(!token.isNullOrBlank())
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.apply {
                    requestMethod = "POST"
                    instanceFollowRedirects = false
                    connectTimeout = 5000
                    readTimeout = 5000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                }
                val bytes = body.toString().toByteArray(Charsets.UTF_8)
                require(bytes.size <= 32768)
                connection.setFixedLengthStreamingMode(bytes.size)
                connection.outputStream.use { it.write(bytes) }
                check(connection.responseCode == 200)
                val response = connection.inputStream.use { input ->
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(4096)
                    while (true) {
                        val read = input.read(buffer, 0, minOf(buffer.size, 32769 - output.size()))
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        require(output.size() <= 32768)
                    }
                    output.toByteArray()
                }
                require(response.size <= 32768)
                Json.parseToJsonElement(response.toString(Charsets.UTF_8)).jsonObject
            } finally {
                connection.disconnect()
            }
        }
    }
}
