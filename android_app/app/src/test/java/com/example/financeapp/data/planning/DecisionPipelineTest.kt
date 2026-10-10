package com.example.financeapp.data.planning

import com.example.financeapp.domain.ai.LocalAdvisor
import com.example.financeapp.domain.engine.*
import com.example.financeapp.domain.freelancer.*
import com.example.financeapp.domain.model.*
import com.example.financeapp.data.TransactionDao
import com.example.financeapp.data.TransactionEntity
import com.example.financeapp.data.remote.BackendClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.YearMonth
import kotlinx.serialization.json.*

internal fun decisionRequest(revision: Long = 1): DecisionRequest {
    val now = Instant.parse("2026-07-01T00:00:00Z")
    val events = (1..6).flatMap { month ->
        val at = Instant.parse("2026-${month.toString().padStart(2, '0')}-10T00:00:00Z")
        listOf(FinancialEvent("income-$month", at, 6_000_000, FinancialEventKind.INCOME, FundingSource.BANK),
            FinancialEvent("spend-$month", at, 500_000, FinancialEventKind.EXPENSE,
                FundingSource.BANK, ExpenseClass.DISCRETIONARY, "SHOPPING"))
    }
    return DecisionRequest(revision, events, YearMonth.of(2026, 1), now, CashAnchor(10_000_000, now),
        List(6) { MonthlyCommitments(3_000_000, 0, 0) }, 4_000_000, 0)
}

internal fun offlinePipeline() = DecisionPipeline(narrate = { intent, bundle, _ -> LocalAdvisor().advise(intent, bundle) })

class DecisionPipelineTest {
    @Test fun runsRealNormalizerForecastAndDecisionWithSelectedTerminalBankCash() = runBlocking {
        val result = offlinePipeline().evaluate(decisionRequest()) as DecisionOutcome.Ready
        val plan = result.decision.plans.single { it.id == result.decision.recommended }
        assertEquals(2000, plan.pathsEvaluated)
        assertEquals(plan.terminalCash.p10, result.bundle.forecastP10, 0.0)
        assertEquals(plan.terminalCash.p50, result.bundle.forecastP50, 0.0)
        assertEquals(plan.terminalCash.p90, result.bundle.forecastP90, 0.0)
        assertEquals(plan.shortfallRisk, result.bundle.projectedShortfallRisk, 0.0)
        assertEquals("Plan A", result.bundle.recommendedPlanId)
        assertEquals("6 months", result.bundle.horizon)
        assertEquals(100000.0, result.inputs.cash, 0.0)
        assertEquals(ResponseSource.LOCAL, result.narration.provenance)
        assertEquals("en-IN", result.narration.speechLocale)
    }

    @Test fun coldStartUnknownLedgerAndMissingBalanceAreExplicitlyUnavailable() = runBlocking {
        val base = decisionRequest()
        for (request in listOf(base.copy(historyStart = YearMonth.of(2026, 6)),
            base.copy(cashAnchor = null),
            base.copy(events = base.events + base.events.first().copy(id = "unknown", kind = FinancialEventKind.UNKNOWN)))) {
            assertTrue(offlinePipeline().evaluate(request) is DecisionOutcome.Unavailable)
        }
    }

    @Test fun invalidInputsNeverReachNarration() = runBlocking {
        val pipeline = DecisionPipeline(narrate = { _, _, _ -> error("Must not narrate") })
        val base = decisionRequest()
        for (request in listOf(base.copy(revision = -1), base.copy(commitments = emptyList()),
            base.copy(cardDuesPaise = -1), base.copy(minReservePaise = -1),
            base.copy(riskThreshold = Double.NaN), base.copy(languageCode = "fr"),
            base.copy(goal = SavingsGoal(100.0, 7)))) {
            assertTrue(pipeline.evaluate(request) is DecisionOutcome.Unavailable)
        }
    }

    @Test fun bundleAdapterRejectsUnsupportedRangesRatherThanClamping() = runBlocking {
        val ready = offlinePipeline().evaluate(decisionRequest()) as DecisionOutcome.Ready
        val adapter = ForecastResultAdapter()
        val chosen = ready.decision.plans.single { it.id == ready.decision.recommended }
        fun decision(plan: FreelancerPlanAssessment) = ready.decision.copy(
            plans = ready.decision.plans.map { if (it.id == plan.id) plan else it })
        for (bad in listOf(chosen.copy(shortfallRisk = -0.01),
            chosen.copy(terminalCash = ForecastBand(-1e13, 0.0, 1e13)),
            chosen.copy(periods = chosen.periods.map { it.copy(income = 0.0) }))) {
            try {
                adapter.adapt(ready.history, ready.inputs, decision(bad), 0)
                fail("Unsupported values must not be clamped")
            } catch (_: IllegalArgumentException) { }
        }
    }

    @Test fun providerFailureAndUnverifiedResponseUseWholeLocalTemplate() = runBlocking {
        for (fail in listOf(true, false)) {
            val result = DecisionPipeline(narrate = { intent, bundle, _ ->
                if (fail) throw java.io.IOException("private provider detail")
                val local = LocalAdvisor().advise(intent, bundle)
                local.copy(source = ResponseSource.REMOTE,
                    observed = listOf(local.observed.single().copy(text = "Cash INR 99999")))
            }).evaluate(decisionRequest()) as DecisionOutcome.Ready
            assertEquals(LocalAdvisor().advise(CopilotIntent.PLAN_COMPARISON, result.bundle), result.narration.response)
            assertEquals(ResponseSource.LOCAL, result.narration.provenance)
        }
    }

    @Test fun regionalModeRequiresRealProviderButAcceptsInjectedVerifiedTemplate() = runBlocking {
        val request = decisionRequest().copy(languageCode = "hi-Deva")
        assertEquals(PipelineIssue.LANGUAGE_UNAVAILABLE,
            (offlinePipeline().evaluate(request) as DecisionOutcome.Unavailable).issue)
        val provider: (CopilotIntent, ResultBundle, String) -> StructuredResponse = { intent, bundle, _ ->
            val local = LocalAdvisor().advise(intent, bundle)
            local.copy(observed = local.observed.map { it.copy(text = "\u092f\u094b\u091c\u0928\u093e Plan A") },
                predicted = local.predicted.map { it.copy(text = "\u091c\u094b\u0916\u093f\u092e 0%") },
                recommended = local.recommended.map { it.copy(text = "\u091c\u093e\u0902\u091a", tradeOff = "\u091c\u093e\u0902\u091a") })
        }
        val result = DecisionPipeline(localizedAdvisor = provider,
            narrate = { _, _, _ -> throw java.io.IOException() }).evaluate(request) as DecisionOutcome.Ready
        assertEquals("hi-Deva", result.narration.languageCode)
        assertEquals("hi-IN", result.narration.speechLocale)
        assertEquals(provider(CopilotIntent.PLAN_COMPARISON, result.bundle, "hi-Deva"), result.narration.response)
    }

    @Test fun explicitSpeechOnlyReceivesVerifiedTextAndUnsupportedSpeechIsReported() = runBlocking {
        var calls = 0
        val pipeline = DecisionPipeline(narrate = { intent, bundle, _ -> LocalAdvisor().advise(intent, bundle) },
            speak = { text, locale -> calls++; assertTrue(text.contains("Predicted:")); assertEquals("en-IN", locale); true })
        val ready = pipeline.evaluate(decisionRequest()) as DecisionOutcome.Ready
        assertEquals(0, calls)
        assertEquals(SpeechOutcome.SPOKEN, pipeline.readAloud(ready.narration))
        assertEquals(1, calls)
        assertEquals(SpeechOutcome.UNAVAILABLE, offlinePipeline().readAloud(ready.narration))
    }

    @Test fun latestRevisionCancelsOldNarrationEvenIfProviderSwallowsCancellation() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val collected = mutableListOf<DecisionOutcome>()
        val pipeline = DecisionPipeline(narrate = { intent, bundle, _ ->
            if (!started.isCompleted) {
                started.complete(Unit)
                try { awaitCancellation() } catch (_: CancellationException) { }
            }
            LocalAdvisor().advise(intent, bundle)
        })
        val requests = MutableSharedFlow<DecisionRequest?>()
        val job = launch { pipeline.observe(requests).take(1).toList(collected) }
        withTimeout(10000) { requests.subscriptionCount.first { it > 0 } }
        requests.emit(decisionRequest(1))
        withTimeout(10000) { started.await() }
        requests.emit(decisionRequest(2))
        withTimeout(10000) { job.join() }
        assertEquals(2L, (collected.single() as DecisionOutcome.Ready).revision)
    }

    @Test fun nullResolvedSnapshotNeverGuessesIncomeOrFunding() = runBlocking {
        val result = offlinePipeline().observe(flowOf(null)).single() as DecisionOutcome.Unavailable
        assertEquals(PipelineIssue.UNRESOLVED_LEDGER, result.issue)
    }

    @Test fun optionalGoalCallsExistingConflictResolverWithoutChangingPlanEngine() = runBlocking {
        val request = decisionRequest().copy(goal = SavingsGoal(1_000_000.0, 6))
        val result = offlinePipeline().evaluate(request) as DecisionOutcome.Ready
        assertEquals(ConflictResolver().resolve(result.inputs, request.goal!!), result.conflict)
        assertTrue(result.conflict!!.relaxations.isNotEmpty())
    }

    @Test fun existingRoomFlowIsReadOnlyAndMapperErrorsAreExplicit() = runBlocking {
        val row = TransactionEntity("local-id", "Private Merchant", 8000.0, "HEALTH", "Private SMS", "2026-07-01")
        var reads = 0
        val dao = java.lang.reflect.Proxy.newProxyInstance(TransactionDao::class.java.classLoader,
            arrayOf(TransactionDao::class.java)) { _, method, _ ->
            check(method.name == "getAllTransactions") { "Pipeline must never write to Room" }
            reads++
            flowOf(listOf(row))
        } as TransactionDao
        val result = offlinePipeline().observe(dao) { rows ->
            assertEquals(listOf(row), rows)
            decisionRequest()
        }.single() as DecisionOutcome.Ready
        assertEquals(1, reads)
        assertFalse(result.narration.speechText.contains("Private"))
        val failed = offlinePipeline().observe(dao) { error("Private parsing detail") }.single()
        assertEquals(PipelineIssue.UNRESOLVED_LEDGER, (failed as DecisionOutcome.Unavailable).issue)
    }

    @Test fun assembledBackendClientPassesOnlyBundleAndVerifiesModelText() = runBlocking {
        val client = BackendClient(transport = { path, body ->
            assertTrue(path.endsWith("narrate"))
            assertEquals(setOf("intent", "bundle", "languageCode"), body.keys)
            val facts = body.getValue("bundle").jsonObject
            buildJsonObject {
                put("languageCode", "en")
                for (section in listOf("observed", "predicted", "recommended")) {
                    put(section, buildJsonArray { add(buildJsonObject {
                        put("text", "INR ${facts.getValue("bankCash").jsonPrimitive.content}")
                    }) })
                }
            }
        })
        val result = DecisionPipeline(narrate = client::narrate).evaluate(decisionRequest()) as DecisionOutcome.Ready
        assertEquals(ResponseSource.REMOTE, result.narration.provenance)
        assertEquals("INR 100000.0", result.narration.response.observed.single().text)
    }

    @Test fun negativeSavingsAndUndefinedRunwayAreNotForcedIntoApiRanges() = runBlocking {
        val base = decisionRequest()
        for (request in listOf(base.copy(commitments = List(6) { MonthlyCommitments(10_000_000, 0, 0) }),
            base.copy(commitments = List(6) { MonthlyCommitments(0, 0, 0) }))) {
            assertEquals(PipelineIssue.UNSUPPORTED_BUNDLE,
                (offlinePipeline().evaluate(request) as DecisionOutcome.Unavailable).issue)
        }
    }

    @Test fun paymentDelayFlowsThroughExistingForecastWithoutDuplicatingIncome() = runBlocking {
        val before = offlinePipeline().evaluate(decisionRequest()) as DecisionOutcome.Ready
        val after = offlinePipeline().evaluate(decisionRequest().copy(delay = OneMonthReceiptDelay(1))) as DecisionOutcome.Ready
        assertEquals(before.inputs.periods.sumOf { it.income }, after.inputs.periods.sumOf { it.income }, 0.0)
        assertEquals(0.0, after.inputs.periods.first().income, 0.0)
        assertEquals(120000.0, after.inputs.periods[1].income, 0.0)
        assertEquals(before.history, after.history)
        assertEquals(before.inputs.cash, after.inputs.cash, 0.0)
    }

    @Test fun cancellationAndSpeechFailuresAreNotReportedAsSuccessfulPlayback() = runBlocking {
        val ready = offlinePipeline().evaluate(decisionRequest()) as DecisionOutcome.Ready
        for (speech in listOf<suspend (String, String) -> Boolean>({ _, _ -> false }, { _, _ -> error("Device detail") })) {
            assertEquals(SpeechOutcome.UNAVAILABLE, DecisionPipeline(speak = speech).readAloud(ready.narration))
        }
        try {
            DecisionPipeline(speak = { _, _ -> throw CancellationException() }).readAloud(ready.narration)
            fail("Speech cancellation must propagate")
        } catch (_: CancellationException) { }
        try {
            DecisionPipeline(narrate = { _, _, _ -> throw CancellationException() }).evaluate(decisionRequest())
            fail("Narration cancellation must propagate")
        } catch (_: CancellationException) { }
    }
}
