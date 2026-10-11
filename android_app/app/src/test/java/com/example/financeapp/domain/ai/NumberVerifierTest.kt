package com.example.financeapp.domain.ai

import com.example.financeapp.data.remote.BackendClient
import com.example.financeapp.domain.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.net.SocketTimeoutException

class NumberVerifierTest {
    private val bundle = ResultBundle(3.4, 0.12, "Plan B", 0.18, 150000.0, 14300.0,
        26700.0, 10000.0, 6400.0, -38000.0, 52000.0, 69000.0, "6 months", ConfidenceLabel.Medium)
    private val intent = CopilotIntent.ACCOUNT_OVERVIEW
    private val advisor = LocalAdvisor()
    private val verifier = NumberVerifier()

    @Test
    fun matchingNumbersAndSupportedFormatsPass() {
        for (text in listOf("Cash Rs. 1,50,000; risk 12%; runway 3.4 months; horizon 6 months",
            "Cash 150,000 or 1.5L or 150K; forecast -38,000; rate 18.0%", "Forecast 52000.0",
            "Cash 1.5 lakh, forecast - 38,000; runway 3.4 liquid months")) {
            assertTrue(text, verifier.isVerified(text, bundle))
        }
        val local = advisor.advise(intent, bundle)
        val remote = local.copy(source = ResponseSource.REMOTE)
        assertEquals(remote, verifier.verifyOrFallback(remote, intent, bundle))
    }

    @Test
    fun inventedNumbersSignsMalformedGroupingAndWrongPercentagesFail() {
        for (text in listOf("Cash 99999", "Forecast 38000", "Risk 0.12%", "Cash 15,00,00",
            "Cash 1.5e5", "Risk 13%", "Cash 150001", "Runway 3.4 million", "Cash - 150000",
            "Cash \u0967\u096b\u0966\u0966\u0966\u0966")) {
            assertFalse(text, verifier.isVerified(text, bundle))
        }
    }

    @Test
    fun roundingUsesDisplayedPrecision() {
        assertTrue(verifier.isVerified("Runway 3 months", bundle))
        assertFalse(verifier.isVerified("Runway 3.5 months", bundle))
        assertTrue(verifier.isVerified("Cash 1.5L", bundle.copy(bankCash = 151200.0)))
        assertFalse(verifier.isVerified("Cash 1.50L", bundle.copy(bankCash = 151200.0)))
    }

    @Test
    fun hallucinatedNumbersAnywhereInResponseReplaceTheWholeAnswer() {
        val local = advisor.advise(intent, bundle)
        val candidates = listOf(
            local.copy(observed = listOf(local.observed.single().copy(text = "Cash is 99999"))),
            local.copy(predicted = listOf(local.predicted.single().copy(text = "Risk is 99%"))),
            local.copy(recommended = listOf(local.recommended.single().copy(tradeOff = "Save 12345"))),
            local.copy(predicted = listOf(local.predicted.single().copy(range = NumberRange(0.0, 999.0, 1000.0)))),
            local.copy(recommended = listOf(local.recommended.single().copy(impact = RecommendationImpact(99.0)))),
            local.copy(observed = emptyList())
        )
        candidates.forEach { assertEquals(local, verifier.verifyOrFallback(it, intent, bundle)) }
    }

    private fun narration(text: String): JsonObject = buildJsonObject {
        for (section in listOf("observed", "predicted", "recommended")) {
            put(section, buildJsonArray { add(buildJsonObject { put("text", text) }) })
        }
    }

    @Test
    fun clientAcceptsVerifiedTextAndNeverSendsRawQuestionsOrArbitraryLabels() = runBlocking {
        val requests = mutableListOf<JsonObject>()
        val client = BackendClient(transport = { path, body ->
            requests.add(body)
            if (path.endsWith("interpret")) buildJsonObject { put("intent", intent.name) }
            else narration("Cash 1.5L and risk 12% over 6 months")
        })
        val answer = client.answer("My account 123456789 at Private Merchant", resultProvider = { bundle })
        assertEquals(ResponseSource.REMOTE, answer.source)
        assertFalse(requests.toString().contains("123456789"))
        assertFalse(requests.toString().contains("Private Merchant"))
        assertEquals(setOf("intent"), requests.first().keys)
        val count = requests.size
        assertEquals(ResponseSource.LOCAL, client.narrate(intent, bundle.copy(recommendedPlanId = "Private Merchant")).source)
        assertEquals(count, requests.size)
    }

    @Test
    fun clientFallsBackOnOfflineTimeoutHttpFailureMalformedOrHallucinatedResponse() = runBlocking {
        val local = advisor.advise(intent, bundle)
        val failures: List<suspend (String, JsonObject) -> JsonObject> = listOf(
            { _, _ -> throw SocketTimeoutException() },
            { _, _ -> throw java.io.IOException("offline") },
            { _, _ -> throw IllegalStateException("HTTP 503") },
            { _, _ -> buildJsonObject {} },
            { _, _ -> narration("Cash is 99999") }
        )
        for (transport in failures) {
            val client = BackendClient(transport)
            assertEquals(local, client.narrate(intent, bundle))
            assertEquals(intent, client.interpret("Account overview"))
        }
    }

    @Test
    fun clientDoesNotSwallowCoroutineCancellation() = runBlocking {
        val client = BackendClient(transport = { _, _ -> throw CancellationException() })
        try {
            client.narrate(intent, bundle)
            fail("Cancellation must propagate")
        } catch (_: CancellationException) {
            // Cancellation belongs to the caller, not the offline fallback.
        }
    }
}
