package com.example.financeapp.domain.freelancer

import com.example.financeapp.domain.engine.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.Instant
import java.time.YearMonth

class FreelancerDecisionTest {
    private val adapter = FreelancerDecisionAdapter()
    private val periods = List(6) { CashFlowPeriod(60.0, 30.0, 10.0) }
    private val loan = SimulationLoan(200.0, 0.12, 10.0)
    private fun inputs(paths: List<List<CashFlowPeriod>> = listOf(periods)) =
        SimulationInputs(100.0, 40.0, periods, paths, listOf(loan))

    @Test fun uncertaintyChangesChoiceWithoutChangingPointForecastOrPolicies() {
        val certain = adapter.compare(inputs())
        val shock = periods.mapIndexed { i, p -> if (i == 1) p.copy(income = 0.0) else p }
        val uncertain = adapter.compare(inputs(List(2_000) { if (it % 2 == 0) periods else shock }))
        assertEquals(FreelancerPlanId.PLAN_A, certain.recommended)
        assertEquals(FreelancerPlanId.PLAN_B, uncertain.recommended)
        assertEquals(certain.plans.map { it.periods }, uncertain.plans.map { it.periods })
        assertEquals(0.5, uncertain.plans[0].shortfallRisk, 0.0)
        assertEquals(0.0, uncertain.plans[1].shortfallRisk, 0.0)
        assertTrue(uncertain.plans.all { it.pathsEvaluated == 2_000 })
        assertFalse(uncertain.noPlanMeetsLimits)
    }

    @Test fun statusQuoMatchesExistingSimulatorAndAccountsForSipAndDebtOnce() {
        val p = periods.map { it.copy(discretionary = 5.0, sip = 3.0, debtService = 12.0) }
        val data = inputs(listOf(p)).copy(periods = p)
        val plan = adapter.compare(data).plans.last()
        val existing = ImpactSimulator().simulate(data, WhatIfChange())
        assertEquals(existing.periods.map { it.ending }, plan.periods.map { it.ending })
        assertEquals(18.0, plan.investedTotal, 0.000001)
        assertEquals(0.0, plan.interestSavedVsStatusQuo, 0.0)
        assertEquals(FreelancerPlanId.STATUS_QUO, plan.id)
        assertEquals(12.0, plan.periods.first().regularDebtService, 0.000001)
    }

    @Test fun initialBreachCountsEvenIfAllMonthlyBalancesRecover() {
        val recovery = periods.map { it.copy(income = 100.0) }
        val result = adapter.compare(inputs(listOf(recovery)).copy(cash = 10.0, periods = recovery))
        assertTrue(result.plans.all { it.shortfallRisk == 1.0 && !it.deterministicFeasible })
        assertTrue(result.plans.all { plan -> plan.periods.all { it.ending >= 40.0 } })
        assertTrue(result.plans.all { plan -> plan.periodShortfallRisk.all { it == 0.0 } })
        assertTrue(result.noPlanMeetsLimits)
        assertTrue(result.reason.contains("No plan"))
    }

    @Test fun preservesObligationsAndCapsCutsAndPrepayments() {
        val p = List(6) { CashFlowPeriod(600.0, 30.0, 10.0, 20.0, 5.0) }
        val result = adapter.compare(inputs(listOf(p)).copy(periods = p))
        result.plans.forEach { plan ->
            plan.periods.forEach {
                assertEquals(30.0, it.essentials, 0.0)
                assertEquals(5.0, it.sip, 0.0)
                assertTrue(it.discretionary in 14.0..20.0)
                assertTrue(it.extraPrepayment >= 0.0)
                assertTrue(it.remainingDebt >= 0.0)
                assertTrue(it.ending >= 40.0)
            }
            assertEquals(30.0, plan.investedTotal, 0.0)
        }
        assertEquals(0.0, result.plans.first().periods.last().remainingDebt, 0.0)
        assertTrue(result.plans.first().interestSavedVsStatusQuo > 0.0)
    }

    @Test fun loanPayoffAndNegativeAmortizationMatchExistingStatusQuo() {
        for (loans in listOf(listOf(SimulationLoan(2.0, 0.12, 10.0)),
            listOf(SimulationLoan(1_000.0, 0.24, 10.0)))) {
            val data = inputs().copy(loans = loans)
            val plan = adapter.compare(data).plans.last()
            assertEquals(ImpactSimulator().simulate(data, WhatIfChange()).periods.map { it.ending },
                plan.periods.map { it.ending })
            if (loans[0].outstanding == 1_000.0) assertTrue(plan.periods.last().remainingDebt > 1_000.0)
            else assertEquals(0.0, plan.periods.last().remainingDebt, 0.0)
        }
    }

    @Test fun prepaymentUsesHighestInterestLoanFirst() {
        val data = SimulationInputs(100.0, 40.0, listOf(CashFlowPeriod(0.0, 0.0, 0.0)),
            listOf(listOf(CashFlowPeriod(0.0, 0.0, 0.0))),
            listOf(SimulationLoan(100.0, 0.0, 0.0), SimulationLoan(100.0, 0.12, 0.0)))
        val first = adapter.compare(data).plans.first().periods.single()
        assertEquals(listOf(100.0, 53.0), first.loanPrincipals)
        assertEquals(48.0, first.extraPrepayment, 0.0)
        assertEquals(52.0, first.ending, 0.0)
    }

    @Test fun riskCountsPathsNotPeriodsAndReportsPeriodRiskSeparately() {
        val bad = List(6) { CashFlowPeriod(0.0, 30.0, 10.0) }
        val result = adapter.compare(inputs(listOf(periods, bad)))
        result.plans.forEach {
            assertEquals(0.5, it.shortfallRisk, 0.0)
            assertEquals(0.5, it.periodShortfallRisk.last(), 0.0)
            assertTrue(it.expectedWorstShortfall > 0.0)
        }
        assertTrue(result.noPlanMeetsLimits)
    }

    @Test fun rejectsInvalidThresholdsAndUnboundedPathCounts() {
        for (theta in listOf(-0.1, 1.1, Double.NaN)) {
            assertThrows(IllegalArgumentException::class.java) { adapter.compare(inputs(), theta) }
        }
        assertThrows(IllegalArgumentException::class.java) { adapter.compare(inputs(List(10_001) { periods })) }
    }

    @Test fun realForecastPathsReachPlannerWithoutResamplingOrChangingCommitments() {
        val history = NormalizedFinancialHistory(List(6) { i ->
            FinancialHistoryMonth(YearMonth.of(2026, 4).plusMonths(i.toLong()), incomePaise = 6_000)
        }, CashAnchor(10_000, Instant.parse("2026-10-01T00:00:00Z")))
        fun compare(delay: OneMonthReceiptDelay?): FreelancerDecision {
            val projection = (FreelancerForecaster().forecast(history, 42, delay) as FreelancerForecast.Ready).forecast
            val inputs = ForecastPathAdapter().adapt(projection,
                List(6) { MonthlyCommitments(3_000, 1_000, 0) }, 4_000, listOf(loan))
            return adapter.compare(inputs)
        }
        assertEquals(FreelancerPlanId.PLAN_A, compare(null).recommended)
        val delayed = compare(OneMonthReceiptDelay(2))
        assertEquals(FreelancerPlanId.PLAN_B, delayed.recommended)
        assertEquals(1.0, delayed.plans[0].shortfallRisk, 0.0)
        assertEquals(0.0, delayed.plans[1].shortfallRisk, 0.0)
        assertTrue(delayed.plans.all { it.pathsEvaluated == 2_000 })
    }

    @Test fun rejectsAggregateOverflowInsteadOfReturningInfiniteObjectives() {
        val p = listOf(CashFlowPeriod(0.0, 0.0, 0.0))
        val data = SimulationInputs(100.0, 40.0, p, listOf(p), List(100) { SimulationLoan(1e307, 0.0, 0.0) })
        assertThrows(IllegalArgumentException::class.java) { adapter.compare(data) }
    }

    @Test fun savesUncertaintyDecisionAndPeriodTablesAsComputedEvidence() {
        val point = periods.map { it.copy(income = it.income * 1_000, essentials = it.essentials * 1_000,
            debtService = it.debtService * 1_000) }
        val shock = point.mapIndexed { i, p -> if (i == 1) p.copy(income = 0.0) else p }
        val base = SimulationInputs(100_000.0, 40_000.0, point, List(2_000) { point },
            listOf(SimulationLoan(200_000.0, 0.12, 10_000.0)))
        fun pathJson(path: List<CashFlowPeriod>) = buildJsonArray { path.forEach { add(buildJsonObject {
            put("income", it.income); put("essentials", it.essentials); put("debtService", it.debtService)
            put("discretionary", it.discretionary); put("sip", it.sip)
        }) } }
        val json = buildJsonObject {
            put("data", "Synthetic policy demonstration; not a model accuracy benchmark")
            put("units", "INR; annualRate and shortfallRisk are fractions")
            put("cash", base.cash); put("reserve", base.minReserve); put("riskThreshold", 0.15)
            put("paths", 2_000); put("pointPath", pathJson(point)); put("shockPath", pathJson(shock))
            put("sampling", "Certain: 2000 point paths. Uncertain: 1000 point and 1000 shock paths, alternating.")
            putJsonArray("loans") { base.loans.forEach { add(buildJsonObject {
                put("outstanding", it.outstanding); put("annualRate", it.annualRate); put("emi", it.emi)
            }) } }
            putJsonArray("runs") {
                for (uncertain in listOf(false, true)) {
                    val result = adapter.compare(if (uncertain) base.copy(sampledPaths =
                        List(2_000) { if (it % 2 == 0) point else shock }) else base)
                    add(buildJsonObject {
                        put("uncertain", uncertain); put("recommended", result.recommended.name)
                        put("noPlanMeetsLimits", result.noPlanMeetsLimits); put("reason", result.reason)
                        putJsonArray("plans") { result.plans.forEach { plan -> add(buildJsonObject {
                            put("id", plan.id.name); put("risk", plan.shortfallRisk)
                            put("deterministicFeasible", plan.deterministicFeasible)
                            put("netPosition", plan.terminalNetPosition); put("interestSaved", plan.interestSavedVsStatusQuo)
                            put("investedTotal", plan.investedTotal); put("expectedWorstShortfall", plan.expectedWorstShortfall)
                            put("terminalCashP10", plan.terminalCash.p10); put("terminalCashP50", plan.terminalCash.p50)
                            put("terminalCashP90", plan.terminalCash.p90)
                            putJsonArray("periodRisks") { plan.periodShortfallRisk.forEach { add(it) } }
                            putJsonArray("periods") { plan.periods.forEach { p -> add(buildJsonObject {
                                put("index", p.index); put("opening", p.opening); put("income", p.income)
                                put("essentials", p.essentials); put("debtService", p.regularDebtService)
                                put("discretionary", p.discretionary); put("sip", p.sip)
                                put("extraPrepayment", p.extraPrepayment); put("interest", p.interest)
                                put("ending", p.ending); put("remainingDebt", p.remainingDebt)
                            }) } }
                        }) } }
                    })
                }
            }
        }
        val file = File("build/reports/freelancer/phase3a-decisions.json")
        requireNotNull(file.parentFile).mkdirs()
        file.writeText(Json { prettyPrint = true }.encodeToString(JsonObject.serializer(), json))
        assertTrue(file.isFile)
    }
}
