package com.example.financeapp.data.planning

import com.example.financeapp.data.TransactionDao
import com.example.financeapp.data.TransactionEntity
import com.example.financeapp.data.remote.BackendClient
import com.example.financeapp.domain.ai.LocalAdvisor
import com.example.financeapp.domain.engine.*
import com.example.financeapp.domain.freelancer.*
import com.example.financeapp.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/** Complete, resolved local snapshot. Cash already includes every posted event through asOf. */
data class DecisionRequest(
    val revision: Long,
    val events: List<FinancialEvent>,
    val historyStart: YearMonth,
    val asOf: Instant,
    val cashAnchor: CashAnchor?,
    val commitments: List<MonthlyCommitments>,
    val minReservePaise: Long,
    val cardDuesPaise: Long,
    val loans: List<SimulationLoan> = emptyList(),
    val seed: Int = 42,
    val riskThreshold: Double = 0.15,
    val languageCode: String = "en",
    val goal: SavingsGoal? = null,
    val expense: ExpenseScenario? = null,
    val delay: OneMonthReceiptDelay? = null
)

data class ExpenseReview(val eventId: String, val expectedPayment: Boolean, val localMerchantKey: String = "")

sealed class ExpenseScenario {
    data class Posted(val review: ExpenseReview) : ExpenseScenario()
    data class Hypothetical(val change: WhatIfChange) : ExpenseScenario()
}

// Integration ports, not a second anomaly detector or a replacement for Roshan's domain types.
enum class ExpenseStatus { NORMAL, UNUSUAL, INSUFFICIENT_HISTORY, NOT_APPLICABLE, UNAVAILABLE }
data class ExpenseAssessment(val eventId: String, val status: ExpenseStatus, val reasonCode: String)
data class PostedExpenseImpact(val cashWithoutExpenseAtAnchor: Double, val cashAtAnchor: Double,
    val shortfallIncrease: Double)
data class ExpenseAlert(val id: String, val unusual: Boolean, val reservePressure: Boolean)
enum class SpeechOutcome { SPOKEN, UNAVAILABLE, REVIEW_REQUIRED }
enum class PipelineIssue {
    UNRESOLVED_LEDGER, INVALID_INPUT, HISTORY_NOT_READY, FORECAST_NOT_READY,
    UNSUPPORTED_BUNDLE, LANGUAGE_UNAVAILABLE, DEPENDENCY_FAILURE, ALERT_CAPACITY_REACHED
}

sealed class DecisionOutcome {
    data class Unavailable(val revision: Long?, val issue: PipelineIssue,
        val historyIssues: Set<HistoryIssue> = emptySet(), val forecastIssue: ForecastIssue? = null) : DecisionOutcome()
    data class Ready(
        val revision: Long, val history: NormalizedFinancialHistory, val forecast: FreelancerProjection,
        val inputs: SimulationInputs, val decision: FreelancerDecision, val bundle: ResultBundle,
        val narration: VerifiedNarration, val conflict: GoalConflict?, val impact: ImpactResult?,
        val expenseAssessment: ExpenseAssessment?, val postedImpact: PostedExpenseImpact?,
        val alerts: List<ExpenseAlert>, val limitations: List<String>
    ) : DecisionOutcome()
}

/** Freelancer-only, opt-in orchestration. It never reads SMS, mutates Room, applies plans or auto-speaks. */
class DecisionPipeline(
    private val localizedAdvisor: ((CopilotIntent, ResultBundle, String) -> StructuredResponse)? = null,
    private val narrate: suspend (CopilotIntent, ResultBundle, String) -> StructuredResponse =
        { intent, bundle, language -> BackendClient(localizedAdvisor = localizedAdvisor).narrate(intent, bundle, language) },
    private val detect: ((FinancialEvent, List<FinancialEvent>, ExpenseReview) -> ExpenseAssessment)? = null,
    private val speak: (suspend (String, String) -> Boolean)? = null,
    private val simulate: (SimulationInputs, WhatIfChange) -> ImpactResult = ImpactSimulator()::simulate,
    private val resolve: (SimulationInputs, SavingsGoal) -> GoalConflict? = ConflictResolver()::resolve
) {
    /** Existing Room stream, explicit upstream resolution: the current entity lacks funding/event types. */
    fun observe(dao: TransactionDao, snapshot: (List<TransactionEntity>) -> DecisionRequest?): Flow<DecisionOutcome> =
        observe(dao.getAllTransactions().map {
            try { snapshot(it) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { null }
        }).catch { emit(DecisionOutcome.Unavailable(null, PipelineIssue.DEPENDENCY_FAILURE)) }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(snapshots: Flow<DecisionRequest?>): Flow<DecisionOutcome> = flow {
        val deliveredAlerts = HashSet<String>()
        snapshots.mapLatest { request ->
            if (request == null) DecisionOutcome.Unavailable(null, PipelineIssue.UNRESOLVED_LEDGER)
            else evaluate(request)
        }.buffer(0).collect { result ->
            if (result is DecisionOutcome.Ready) {
                val fresh = result.alerts.filter { it.id !in deliveredAlerts }
                if (deliveredAlerts.size + fresh.size > 100_000) {
                    emit(DecisionOutcome.Unavailable(result.revision, PipelineIssue.ALERT_CAPACITY_REACHED))
                } else {
                    deliveredAlerts.addAll(fresh.map { it.id })
                    emit(result.copy(alerts = fresh))
                }
            } else emit(result)
        }
    }

    suspend fun evaluate(request: DecisionRequest): DecisionOutcome = withContext(Dispatchers.Default) {
        fun unavailable(issue: PipelineIssue) = DecisionOutcome.Unavailable(request.revision, issue)
        if (request.languageCode !in setOf("en", "hi-Latn", "hi-Deva", "mr-Deva") ||
            (request.languageCode != "en" && localizedAdvisor == null)) return@withContext unavailable(PipelineIssue.LANGUAGE_UNAVAILABLE)
        try {
            require(request.revision >= 0 && request.commitments.size == 6 && request.loans.size <= 100)
            require(request.minReservePaise in 0..100_000_000_000_000L && request.cardDuesPaise in 0..100_000_000_000_000L)
            require(request.riskThreshold.isFinite() && request.riskThreshold in 0.0..1.0)
            require(request.goal == null || request.goal.byPeriod <= 6)
            val normalized = FinancialHistoryNormalizer().normalize(request.events, request.historyStart,
                request.asOf, request.cashAnchor)
            if (normalized is HistoryNormalization.Unavailable) return@withContext DecisionOutcome.Unavailable(
                request.revision, PipelineIssue.HISTORY_NOT_READY, normalized.issues)
            val history = (normalized as HistoryNormalization.Ready).history
            currentCoroutineContext().ensureActive()
            val forecastResult = FreelancerForecaster().forecast(history, request.seed, request.delay)
            if (forecastResult is FreelancerForecast.Unavailable) return@withContext DecisionOutcome.Unavailable(
                request.revision, PipelineIssue.FORECAST_NOT_READY, forecastIssue = forecastResult.issue)
            val forecast = (forecastResult as FreelancerForecast.Ready).forecast
            val inputs = ForecastPathAdapter().adapt(forecast, request.commitments, request.minReservePaise, request.loans)
            currentCoroutineContext().ensureActive()
            val decision = FreelancerDecisionAdapter().compare(inputs, request.riskThreshold)
            val bundle = try { ForecastResultAdapter().adapt(history, inputs, decision, request.cardDuesPaise) }
                catch (_: IllegalArgumentException) { return@withContext unavailable(PipelineIssue.UNSUPPORTED_BUNDLE) }
            currentCoroutineContext().ensureActive()
            val conflict = request.goal?.let { resolve(inputs, it) }
            var assessment: ExpenseAssessment? = null
            var postedImpact: PostedExpenseImpact? = null
            var impact: ImpactResult? = null
            when (val expense = request.expense) {
                is ExpenseScenario.Posted -> {
                    val review = expense.review
                    require(review.localMerchantKey.length <= 128)
                    val events = request.events.distinctBy { it.id }
                    val candidate = events.single { it.id == review.eventId }
                    val start = request.historyStart.atDay(1).atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant()
                    require(candidate.occurredAt in start..request.asOf)
                    if (candidate.kind != FinancialEventKind.EXPENSE) {
                        assessment = ExpenseAssessment(candidate.id, ExpenseStatus.NOT_APPLICABLE, "NOT_EXPENSE")
                    } else {
                        assessment = try {
                            detect?.invoke(candidate, events.filter { it.kind == FinancialEventKind.EXPENSE &&
                                it.occurredAt >= start && it.occurredAt < candidate.occurredAt }, review)
                                ?.also { require(it.eventId == candidate.id && it.reasonCode.matches(Regex("[A-Z_]{1,64}"))) }
                                ?: ExpenseAssessment(candidate.id, ExpenseStatus.UNAVAILABLE, "DETECTOR_NOT_INSTALLED")
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { ExpenseAssessment(candidate.id, ExpenseStatus.UNAVAILABLE, "DETECTOR_FAILED") }
                        val debit = if (candidate.funding == FundingSource.BANK) candidate.amountPaise / 100.0 else 0.0
                        val without = inputs.cash + debit
                        require(without.isFinite())
                        postedImpact = PostedExpenseImpact(without, inputs.cash,
                            (inputs.minReserve - inputs.cash).coerceAtLeast(0.0) -
                                (inputs.minReserve - without).coerceAtLeast(0.0))
                    }
                }
                is ExpenseScenario.Hypothetical -> impact = simulate(inputs, expense.change).also {
                    require(listOf(it.runwayDeltaMonths, it.shortfallRiskDelta, it.interestSaved).all { value -> value.isFinite() })
                    require(it.shortfallRiskDelta in -1.0..1.0 && it.periods.size == inputs.periods.size)
                    require(it.periods.withIndex().all { (index, period) ->
                        period.index == index + 1 && period.opening.isFinite() && period.ending.isFinite() })
                }
                null -> Unit
            }
            val alerts = if (assessment?.status == ExpenseStatus.UNUSUAL || (postedImpact?.shortfallIncrease ?: 0.0) > 0.0)
                listOf(ExpenseAlert("expense:${assessment!!.eventId}", assessment.status == ExpenseStatus.UNUSUAL,
                    (postedImpact?.shortfallIncrease ?: 0.0) > 0.0)) else emptyList()
            val intent = CopilotIntent.PLAN_COMPARISON
            val requiresReview = decision.noPlanMeetsLimits || conflict != null
            val fallback = try {
                var local = localizedAdvisor?.invoke(intent, bundle, request.languageCode) ?: LocalAdvisor().advise(intent, bundle)
                if (requiresReview && request.languageCode == "en") local = local.copy(
                    recommended = local.recommended.map { it.copy(
                        text = "Recommended: review the computed conflict and risk warnings before choosing a plan.",
                        tradeOff = "This answer does not authorize a financial action.") })
                VerifiedNarration.verify(local, intent, bundle, request.languageCode, local)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { return@withContext unavailable(PipelineIssue.LANGUAGE_UNAVAILABLE) }
            currentCoroutineContext().ensureActive()
            val answer = if (requiresReview) fallback.response else try { narrate(intent, bundle, request.languageCode) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { fallback.response }
            currentCoroutineContext().ensureActive()
            val narration = VerifiedNarration.verify(answer, intent, bundle, request.languageCode, fallback.response, requiresReview)
            DecisionOutcome.Ready(request.revision, history, forecast, inputs, decision, bundle, narration,
                conflict, impact, assessment, postedImpact, alerts, forecast.limitations + listOf(
                    "Freelancer-only opt-in pipeline; Room events require explicit upstream resolution.",
                    "Low confidence denotes uncalibrated forecast evidence, not measured language accuracy.",
                    "Goal conflicts use the existing fixed-obligation/60% discretionary-cut model, not the selected loan policy.",
                    "What-if impact compares status quo paths; it does not apply or replace the selected plan.",
                    "Posted impact is an immediate cash-anchor counterfactual, not a historical balance reconstruction.",
                    "Anomaly, localization and speech ports require teammate adapters; device/language review pending."
                ))
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: IllegalArgumentException) { unavailable(PipelineIssue.INVALID_INPUT) }
        catch (_: Exception) { unavailable(PipelineIssue.DEPENDENCY_FAILURE) }
    }

    /** User-triggered only; the service must report unsupported/missing locale data as false. */
    suspend fun readAloud(narration: VerifiedNarration): SpeechOutcome {
        if (narration.requiresReview) return SpeechOutcome.REVIEW_REQUIRED
        val player = speak ?: return SpeechOutcome.UNAVAILABLE
        return try {
            if (player(narration.speechText, narration.speechLocale)) SpeechOutcome.SPOKEN else SpeechOutcome.UNAVAILABLE
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { SpeechOutcome.UNAVAILABLE }
    }
}
