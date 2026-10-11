package com.example.financeapp.domain.ai

import com.example.financeapp.data.remote.BackendClient
import com.example.financeapp.domain.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

class LocalizedNarrationTest {
    private val bundle = ResultBundle(3.4, .12, "Plan B", .18, 45200.0, 14300.0,
        26700.0, 10000.0, 6400.0, -38000.0, 52000.0, 69000.0, "6 months", ConfidenceLabel.Medium)
    private val intent = CopilotIntent.ACCOUNT_OVERVIEW
    private val codes = listOf("en", "hi-Latn", "hi-Deva", "mr-Deva")
    // Contract fixtures only; real offline translations remain owned by the localization track.
    private fun phrase(code: String) = (when (code) {
        "en" -> "Cash INR 45200"
        "hi-Latn" -> "Aapka cash INR 45200 hai"
        "hi-Deva" -> "\u0906\u092a\u0915\u0940 \u0930\u093e\u0936\u093f INR 45200"
        else -> "\u0924\u0941\u092e\u091a\u0940 \u0930\u0915\u094d\u0915\u092e INR 45200"
    }) + "; 12%; 6 months; INR -38000, INR 52000, INR 69000"

    private fun fallback(code: String, b: ResultBundle = bundle): StructuredResponse {
        val local = LocalAdvisor().advise(intent, b)
        return local.copy(
            observed = listOf(local.observed.single().copy(text = phrase(code))),
            predicted = listOf(local.predicted.single().copy(text = phrase(code))),
            recommended = listOf(local.recommended.single().copy(text = phrase(code), tradeOff = phrase(code))))
    }

    private fun response(code: String, text: String = phrase(code)): JsonObject = buildJsonObject {
        put("languageCode", code)
        put("intent", intent.name)
        put("source", "REMOTE")
        for (section in listOf("observed", "predicted", "recommended")) {
            put(section, buildJsonArray { add(buildJsonObject {
                put("text", text)
                if (section == "predicted") {
                    put("range", buildJsonObject { put("p10", -38000.0); put("p50", 52000.0); put("p90", 69000.0) })
                    put("confidenceLabel", "Medium")
                    put("horizon", "6 months")
                }
            }) })
        }
    }

    @Test fun allLanguageCodesUseRequestedTextBeforeVerification() = runBlocking {
        for (code in codes) {
            val client = BackendClient(transport = { _, body ->
                assertEquals(code, body.getValue("languageCode").jsonPrimitive.content)
                assertEquals(setOf("intent", "bundle", "languageCode"), body.keys)
                response(code)
            }, localizedAdvisor = { _, _, lang -> fallback(lang) })
            val answer = client.narrate(intent, bundle, code)
            assertEquals(ResponseSource.REMOTE, answer.source)
            assertEquals(phrase(code), answer.observed.single().text)
            assertEquals(fallback(code).predicted.single().range, answer.predicted.single().range)
            assertEquals(fallback(code).recommended.single().tradeOff, answer.recommended.single().tradeOff)
        }
    }

    @Test fun failuresReturnEntireRequestedLanguageFallback() = runBlocking {
        for (code in codes) {
            val failures: List<suspend (String, JsonObject) -> JsonObject> = listOf(
                { _, _ -> throw SocketTimeoutException() },
                { _, _ -> throw IOException("offline") },
                { _, _ -> error("HTTP 503") },
                { _, _ -> buildJsonObject {} },
                { _, _ -> response(code, "Cash INR 99999") },
                { _, _ -> response(code, " ") },
                { _, _ -> response(code, "x".repeat(2001)) },
                { _, _ -> response("wrong-language") },
                { _, _ -> JsonObject(response(code) + ("intent" to JsonPrimitive("WHAT_IF_QUERY"))) },
                { _, _ -> JsonObject(response(code) + ("source" to JsonPrimitive("LOCAL"))) },
                { _, _ -> JsonObject(response(code) + ("predicted" to buildJsonArray {
                    add(buildJsonObject { put("text", phrase(code)); put("range", buildJsonObject { put("p50", 99999) }) })
                })) }
            )
            for (transport in failures) {
                val client = BackendClient(transport = transport, localizedAdvisor = { _, _, lang -> fallback(lang) })
                assertEquals(fallback(code), client.narrate(intent, bundle, code))
            }
        }
    }

    @Test fun unsupportedLanguageAndMissingRegionalFallbackFailBeforeNetwork() = runBlocking {
        val client = BackendClient(transport = { _, _ -> error("Network must not run") })
        for (code in listOf("fr", "HI", "", "hi-Deva")) {
            try {
                client.narrate(intent, bundle, code)
                fail("Language configuration must fail explicitly: $code")
            } catch (_: IllegalArgumentException) { }
        }
    }

    @Test fun oldEnglishResponsesStillWorkWithoutLanguageEcho() = runBlocking {
        val client = BackendClient(transport = { _, body ->
            assertEquals("en", body.getValue("languageCode").jsonPrimitive.content)
            JsonObject(response("en") - "languageCode")
        })
        assertEquals(ResponseSource.REMOTE, client.narrate(intent, bundle).source)
    }

    @Test fun regionalResponseRequiresLanguageEcho() = runBlocking {
        val client = BackendClient(transport = { _, _ -> JsonObject(response("hi-Deva") - "languageCode") },
            localizedAdvisor = { _, _, code -> fallback(code) })
        assertEquals(fallback("hi-Deva"), client.narrate(intent, bundle, "hi-Deva"))
    }

    @Test fun malformedMetadataCannotBeHiddenByCorrectText() = runBlocking {
        val changes = listOf(
            "predicted" to buildJsonObject { put("horizon", "99 months") },
            "predicted" to buildJsonObject { put("confidenceLabel", "High") },
            "predicted" to buildJsonObject { put("range", JsonNull) },
            "recommended" to buildJsonObject { put("impact", buildJsonObject {
                put("runwayDeltaMonths", 0.0); put("riskDelta", 0.0); put("interestSaved", 45200.0)
            }) }
        )
        for ((section, changeset) in changes) {
            val client = BackendClient(transport = { _, _ ->
                val body = response("hi-Latn")
                JsonObject(body + (section to buildJsonArray {
                    add(JsonObject(body.getValue(section).jsonArray.single().jsonObject + changeset))
                }))
            }, localizedAdvisor = { _, _, code -> fallback(code) })
            assertEquals(fallback("hi-Latn"), client.narrate(intent, bundle, "hi-Latn"))
        }
    }

    @Test fun localizedAnswerStillSendsOnlyLocallySelectedIntentAndAggregates() = runBlocking {
        val bodies = mutableListOf<JsonObject>()
        val client = BackendClient(transport = { path, body ->
            bodies.add(body)
            if (path.endsWith("interpret")) buildJsonObject { put("intent", intent.name) }
            else response("hi-Latn")
        }, localizedAdvisor = { _, _, code -> fallback(code) })
        assertEquals(ResponseSource.REMOTE,
            client.answer("My account 123456789 at Private Merchant", "hi-Latn") { bundle }.source)
        assertEquals(setOf("intent"), bodies.first().keys)
        assertFalse(bodies.toString().contains("123456789"))
        assertFalse(bodies.toString().contains("Private Merchant"))
    }

    @Test fun invalidInjectedFallbackIsNotTrusted() = runBlocking {
        for (bad in listOf(fallback("hi-Latn").copy(source = ResponseSource.REMOTE),
            fallback("hi-Latn").copy(observed = listOf(ObservedFact("INR 99999", emptyList()))),
            fallback("hi-Latn").copy(predicted = listOf(fallback("hi-Latn").predicted.single().copy(horizon = "99 months"))))) {
            val client = BackendClient(transport = { _, _ -> error("Network must not run") },
                localizedAdvisor = { _, _, _ -> bad })
            try {
                client.narrate(intent, bundle, "hi-Latn")
                fail("Invalid fallback must fail explicitly")
            } catch (_: IllegalArgumentException) { }
        }
    }

    @Test fun verifierRejectsTranslatedScalesNativeDigitsAndOversizedText() {
        val verifier = NumberVerifier()
        for (text in listOf("45200 \u0932\u093e\u0916", "45200 \u0939\u091c\u093e\u0930", "45200 hazaar",
            "12 \u092a\u094d\u0930\u0924\u093f\u0936\u0924", "12 \u091f\u0915\u094d\u0915\u0947", "\u096a\u096b\u0968\u0966\u0966", "x".repeat(2001))) {
            assertFalse(text, verifier.isVerified(text, bundle))
        }
        assertTrue(verifier.isVerified("INR 45200; 12%; 6 months", bundle))
    }

    @Test fun verifierUsesInjectedFallbackForInventedNumbersAndMetadata() {
        val local = fallback("hi-Deva")
        val remote = local.copy(source = ResponseSource.REMOTE,
            recommended = listOf(local.recommended.single().copy(tradeOff = "INR 99999")))
        assertEquals(local, NumberVerifier().verifyOrFallback(remote, intent, bundle, local))
        val bad = local.copy(predicted = listOf(local.predicted.single().copy(range = NumberRange(0.0, 1.0, 2.0))))
        assertEquals(local, NumberVerifier().verifyOrFallback(bad, intent, bundle, local))
    }

    @Test fun cancellationStillPropagatesForRegionalMode() = runBlocking {
        val client = BackendClient(transport = { _, _ -> throw CancellationException() },
            localizedAdvisor = { _, _, code -> fallback(code) })
        try {
            client.narrate(intent, bundle, "mr-Deva")
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
    }

    @Test fun englishFallbackPreservesLargeMoneyAndPaise() = runBlocking {
        val b = bundle.copy(bankCash = 999999999999.99, forecastP50 = 52000.01)
        val local = LocalAdvisor().advise(intent, b)
        assertTrue(local.observed.single().text.contains("999999999999.99"))
        assertTrue(local.predicted.single().text.contains("52000.01"))
        val client = BackendClient(transport = { _, _ -> throw IOException() })
        assertEquals(local, client.narrate(intent, b))
    }
}
