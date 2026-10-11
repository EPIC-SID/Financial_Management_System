package com.example.financeapp.data.planning

import com.example.financeapp.domain.ai.LocalAdvisor
import com.example.financeapp.domain.engine.*
import com.example.financeapp.domain.freelancer.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Test

class ExpenseReplanTest {
    private fun posted(amount: Long = 800_000, kind: FinancialEventKind = FinancialEventKind.EXPENSE,
        funding: FundingSource = FundingSource.BANK): DecisionRequest {
        val base = decisionRequest()
        val event = FinancialEvent("posted", base.asOf, amount, kind, funding, ExpenseClass.ESSENTIAL, "HEALTH")
        return base.copy(events = base.events + event,
            cashAnchor = base.cashAnchor!!.copy(amountPaise = 10_000_000 - if (funding == FundingSource.BANK) amount else 0),
            expense = ExpenseScenario.Posted(ExpenseReview("posted", expectedPayment = false)))
    }

    @Test fun postedExpenseUsesUpdatedCashExactlyOnceAndRetryIsIdempotent() = runBlocking {
        val pipeline = offlinePipeline()
        val before = pipeline.evaluate(decisionRequest()) as DecisionOutcome.Ready
        val request = posted()
        val after = pipeline.evaluate(request) as DecisionOutcome.Ready
        val retry = pipeline.evaluate(request.copy(events = request.events + request.events.last())) as DecisionOutcome.Ready
        assertEquals(92000.0, after.inputs.cash, 0.0)
        assertEquals(before.bundle.forecastP50 - 8000.0, after.bundle.forecastP50, 0.000001)
        assertEquals(after.bundle, retry.bundle)
        assertNull(after.impact)
        val postedImpact = after.postedImpact!!
        assertEquals(100000.0, postedImpact.cashWithoutExpenseAtAnchor, 0.0)
        assertEquals(92000.0, postedImpact.cashAtAnchor, 0.0)
    }

    @Test fun hypotheticalExpenseUsesExistingSimulatorOnceWithoutChangingCashOrHistory() = runBlocking {
        val change = WhatIfChange(oneOffExpense = 15000.0, expensePeriod = 2)
        val result = offlinePipeline().evaluate(decisionRequest().copy(expense = ExpenseScenario.Hypothetical(change))) as DecisionOutcome.Ready
        assertEquals(100000.0, result.inputs.cash, 0.0)
        assertEquals(ImpactSimulator().simulate(result.inputs, change), result.impact)
        assertNull(result.postedImpact)
        assertEquals(0L, result.history.months.last().cashEssentialsPaise)
    }

    @Test fun detectorReceivesOnlyStrictPriorHistoryAndExpectedPaymentMetadata() = runBlocking {
        val request = posted(1_500_000).let { it.copy(expense = ExpenseScenario.Posted(ExpenseReview("posted", true, "rent-local"))) }
        var calls = 0
        val pipeline = DecisionPipeline(narrate = { i, b, _ -> LocalAdvisor().advise(i, b) },
            detect = { candidate, prior, review ->
                calls++
                assertTrue(review.expectedPayment)
                assertEquals("rent-local", review.localMerchantKey)
                assertTrue(prior.all { it.occurredAt < candidate.occurredAt })
                assertTrue(prior.all { it.kind == FinancialEventKind.EXPENSE })
                assertEquals(prior.size, prior.distinctBy { it.id }.size)
                ExpenseAssessment(candidate.id, ExpenseStatus.NORMAL, "EXPECTED_PAYMENT")
            })
        val ready = pipeline.evaluate(request) as DecisionOutcome.Ready
        assertEquals(1, calls)
        assertEquals(ExpenseStatus.NORMAL, ready.expenseAssessment!!.status)
        assertTrue(ready.alerts.isEmpty())
    }

    @Test fun unusualnessAndReserveSeverityAreSeparateAndAlertsAreDeduplicatedPerCollector() = runBlocking {
        val request = posted().let { it.copy(cashAnchor = it.cashAnchor!!.copy(amountPaise = 3_500_000)) }
        val pipeline = DecisionPipeline(narrate = { i, b, _ -> LocalAdvisor().advise(i, b) },
            detect = { event, _, _ -> ExpenseAssessment(event.id, ExpenseStatus.NORMAL, "EXPECTED_PAYMENT") })
        val firstDelivered = kotlinx.coroutines.CompletableDeferred<Unit>()
        val source = flow { emit(request); firstDelivered.await(); emit(request.copy(revision = 2)) }
        val results = pipeline.observe(source).onEach { firstDelivered.complete(Unit) }.toList()
        val ready = results.filterIsInstance<DecisionOutcome.Ready>()
        assertEquals(2, ready.size)
        assertFalse(ready.first().alerts.single().unusual)
        assertTrue(ready.first().alerts.single().reservePressure)
        assertTrue(ready.last().alerts.isEmpty())
    }

    @Test fun transfersAndSettlementsDoNotInvokeAnomalyDetectorOrOneOffSimulator() = runBlocking {
        val pipeline = DecisionPipeline(narrate = { i, b, _ -> LocalAdvisor().advise(i, b) },
            detect = { _, _, _ -> error("Not an expense") })
        for (kind in listOf(FinancialEventKind.INTERNAL_TRANSFER, FinancialEventKind.CARD_SETTLEMENT)) {
            val ready = pipeline.evaluate(posted(kind = kind)) as DecisionOutcome.Ready
            assertEquals(ExpenseStatus.NOT_APPLICABLE, ready.expenseAssessment!!.status)
            assertNull(ready.postedImpact)
            assertNull(ready.impact)
            assertTrue(ready.alerts.isEmpty())
        }
    }

    @Test fun cardConsumptionDoesNotReduceBankCashAndMissingDetectorIsExplicit() = runBlocking {
        val ready = offlinePipeline().evaluate(posted(funding = FundingSource.CREDIT_CARD)) as DecisionOutcome.Ready
        assertEquals(100000.0, ready.inputs.cash, 0.0)
        assertEquals(ExpenseStatus.UNAVAILABLE, ready.expenseAssessment!!.status)
        assertEquals(0.0, ready.postedImpact!!.shortfallIncrease, 0.0)
        assertTrue(ready.alerts.isEmpty())
    }

    @Test fun missingFutureOrConflictingPostedEventIsRejected() = runBlocking {
        val request = posted()
        for (bad in listOf(request.copy(events = decisionRequest().events),
            request.copy(events = request.events + request.events.last().copy(amountPaise = 1)),
            request.copy(events = request.events.dropLast(1) + request.events.last().copy(occurredAt = request.asOf.plusSeconds(1))))) {
            assertTrue(offlinePipeline().evaluate(bad) is DecisionOutcome.Unavailable)
        }
    }

    @Test fun simulatorRunsOnceForHypotheticalAndNeverForPostedExpense() = runBlocking {
        var calls = 0
        val pipeline = DecisionPipeline(narrate = { i, b, _ -> LocalAdvisor().advise(i, b) },
            simulate = { inputs, change -> calls++; ImpactSimulator().simulate(inputs, change) })
        pipeline.evaluate(posted())
        assertEquals(0, calls)
        pipeline.evaluate(decisionRequest().copy(expense = ExpenseScenario.Hypothetical(WhatIfChange(oneOffExpense = 8000.0))))
        assertEquals(1, calls)
    }

    @Test fun unusualExpenseCanAlertWithoutReservePressureAndDetectorErrorsStayExplicit() = runBlocking {
        for (bad in listOf(false, true)) {
            val pipeline = DecisionPipeline(narrate = { i, b, _ -> LocalAdvisor().advise(i, b) },
                detect = { event, _, _ ->
                    if (bad) error("private detector detail")
                    ExpenseAssessment(event.id, ExpenseStatus.UNUSUAL, "CATEGORY_SPIKE")
                })
            val ready = pipeline.evaluate(posted()) as DecisionOutcome.Ready
            if (bad) {
                assertEquals(ExpenseStatus.UNAVAILABLE, ready.expenseAssessment!!.status)
                assertTrue(ready.alerts.isEmpty())
            } else {
                assertTrue(ready.alerts.single().unusual)
                assertFalse(ready.alerts.single().reservePressure)
            }
        }
    }

    @Test fun overflowingHypotheticalArithmeticIsRejectedInsteadOfExposedToUi() = runBlocking {
        val request = decisionRequest().copy(expense = ExpenseScenario.Hypothetical(WhatIfChange(incomeChange = Double.MAX_VALUE)))
        assertTrue(offlinePipeline().evaluate(request) is DecisionOutcome.Unavailable)
    }

    @Test fun unresolvedPlanOrGoalCannotBeReadAsAnUnqualifiedRecommendation() = runBlocking {
        var calls = 0
        val pipeline = DecisionPipeline(narrate = { i, b, _ -> LocalAdvisor().advise(i, b) },
            speak = { _, _ -> calls++; true })
        val lowCash = posted().let { it.copy(cashAnchor = it.cashAnchor!!.copy(amountPaise = 3_500_000)) }
        for (request in listOf(lowCash, decisionRequest().copy(goal = SavingsGoal(1_000_000.0, 6)))) {
            val ready = pipeline.evaluate(request) as DecisionOutcome.Ready
            assertEquals(SpeechOutcome.REVIEW_REQUIRED, pipeline.readAloud(ready.narration))
            assertTrue(ready.narration.requiresReview)
            assertTrue(ready.narration.response.recommended.single().text.contains("review"))
        }
        assertEquals(0, calls)
    }
}
