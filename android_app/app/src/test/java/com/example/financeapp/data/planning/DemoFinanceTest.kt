package com.example.financeapp.data.planning

import com.example.financeapp.data.ingest.Persona
import com.example.financeapp.data.ingest.Scenario
import com.example.financeapp.domain.engine.RelaxationKind
import com.example.financeapp.domain.freelancer.FreelancerPlanId
import com.example.financeapp.domain.model.CopilotIntent
import com.example.financeapp.domain.ai.NumberVerifier
import com.example.financeapp.domain.ai.localization.LocalizedAdvisor
import com.example.financeapp.ui.viewmodel.PlanKind
import com.example.financeapp.ui.viewmodel.planUi
import com.example.financeapp.ui.util.formatRiskPercent
import org.junit.Assert.*
import org.junit.Test

class DemoFinanceTest {
    private val calculator = DemoFinanceCalculator()

    @Test fun scenarioSimulationEvidence() {
        Scenario.entries.forEach { scenario ->
            val result = calculator.calculate(scenario)
            result.decision.plans.forEach { plan ->
                assertEquals(2000, plan.pathsEvaluated)
                println("SIMULATION $scenario ${plan.id}: risk=${plan.shortfallRisk}, paths=${plan.pathsEvaluated}, " +
                    "income=${plan.periods.map { it.income }}, endings=${plan.periods.map { it.ending }}")
            }
            if (scenario == Scenario.INCOME_REDUCTION || scenario == Scenario.INFEASIBLE_TARGET) {
                assertTrue("$scenario must expose reserve breaches", result.decision.plans.all { it.shortfallRisk > 0.0 })
            }
        }
    }

    @Test fun uiExposesSampleCountsAndEachPeriodsActualForecast() {
        val result = calculator.calculate(Scenario.HIGH_UNCERTAINTY)
        val ui = result.planUi(PlanKind.PLAN_B, true)
        val assessment = result.decision.plans.single { it.id == FreelancerPlanId.PLAN_B }
        assertEquals(2000, ui.summary.pathsEvaluated)
        assertEquals(692, ui.summary.breachedPaths)
        assertEquals("34.6%", formatRiskPercent(ui.summary.shortfallRisk))
        ui.rows.forEachIndexed { i, row ->
            assertEquals(ui.forecast[i], row.forecast)
            assertEquals(assessment.cashBands[i].p50, row.forecast!!.p50, 0.0)
            assertEquals(assessment.periodShortfallRisk[i], row.forecast!!.shortfallChance, 0.0)
            assertEquals(assessment.periods[i].ending, row.endingBalance, 0.0)
        }
        assertTrue(ui.rows.map { it.endingBalance }.distinct().size > 1)
        assertTrue(ui.rows.any { it.forecast!!.p50 != it.endingBalance })
        val emergency = calculator.calculate(Scenario.UNEXPECTED_EXPENSE)
        assertEquals(FreelancerPlanId.PLAN_B, emergency.decision.recommended)
        assertEquals(1.0, emergency.decision.plans.single { it.id == FreelancerPlanId.PLAN_A }.shortfallRisk, 0.0)
        assertEquals(0.0, emergency.decision.plans.single { it.id == FreelancerPlanId.PLAN_B }.shortfallRisk, 0.0)
    }

    @Test fun riskFormattingNeverHidesNonzeroBreachProbability() {
        assertEquals("0%", formatRiskPercent(0.0))
        assertEquals("<0.1%", formatRiskPercent(1.0 / 2000))
        assertEquals("0.1%", formatRiskPercent(2.0 / 2000))
        assertEquals("100%", formatRiskPercent(1.0))
    }

    @Test fun stableDemoUsesOnlyPastHistoryAndComputedPlans() {
        val result = calculator.calculate(Scenario.STABLE_INCOME)
        assertEquals(24, result.history.months.size)
        assertTrue(result.history.months.all { it.month.isBefore(java.time.YearMonth.from(result.dataset.demoDate)) })
        assertEquals(3, result.decision.plans.size)
        result.decision.plans.forEach { plan ->
            assertEquals(2000, plan.pathsEvaluated)
            assertEquals(6, plan.cashBands.size)
            assertEquals(plan.terminalCash, plan.cashBands.last())
            assertTrue(plan.cashBands.all { it.p10 <= it.p50 && it.p50 <= it.p90 })
        }
        assertEquals(45200.0, result.inputs.cash, 0.01)
        assertNotNull(result.bundle)
    }

    @Test fun cardConsumptionIsNotDebitedAgainAsDiscretionaryCash() {
        val result = calculator.calculate(Scenario.STABLE_INCOME)
        assertTrue(result.history.months.last().discretionaryConsumptionPaise > result.history.months.last().cashDiscretionaryPaise)
        assertTrue(result.inputs.periods.first().discretionary < 2000.0)
        assertTrue(result.inputs.periods.first().debtService > result.inputs.loans.sumOf { it.emi })
    }

    @Test fun scenariosChangeComputedInputsWithoutUsingFutureTransactions() {
        val stable = calculator.calculate(Scenario.STABLE_INCOME)
        val drop = calculator.calculate(Scenario.INCOME_REDUCTION)
        val emergency = calculator.calculate(Scenario.UNEXPECTED_EXPENSE)
        val volatile = calculator.calculate(Scenario.HIGH_UNCERTAINTY)
        assertEquals(stable.history, drop.history)
        assertEquals(55000.0, drop.inputs.periods[1].income, 0.01)
        assertEquals(stable.inputs.periods[2].essentials + 25000.0, emergency.inputs.periods[2].essentials, 0.01)
        assertTrue(volatile.inputs.sampledPaths.map { it[0].income }.distinct().size > 1)
        assertEquals(stable.inputs.periods[0].income, volatile.inputs.periods[0].income, 0.01)
    }

    @Test fun annualBillsOnlyAppearInDeclaredCalendarMonths() {
        val result = calculator.calculate(Scenario.STABLE_INCOME, Persona.C)
        assertEquals(54000.0, result.inputs.periods[0].essentials, 0.01)
        assertEquals(9000.0, result.inputs.periods[1].essentials, 0.01)
        assertEquals(37000.0, result.inputs.periods[3].essentials, 0.01)
    }

    @Test fun conflictUsesEngineAndRejectsImpossibleRelaxation() {
        val result = calculator.calculate(Scenario.INFEASIBLE_TARGET)
        assertNotNull(result.conflict)
        assertEquals(RelaxationKind.values().toSet(), result.conflict!!.relaxations.map { it.kind }.toSet())
        val impossible = result.conflict!!.relaxations.firstOrNull { !it.feasible }
        if (impossible != null) {
            try { calculator.calculate(Scenario.INFEASIBLE_TARGET, relaxation = impossible); fail("Must reject infeasible option") }
            catch (_: IllegalArgumentException) { }
        }
    }

    @Test fun localizedBundleTemplatesPassActualVerifierForEveryIntent() {
        val bundle = calculator.calculate(Scenario.STABLE_INCOME).bundle!!
        for (language in listOf("en", "hi-Latn", "hi-Deva", "mr-Deva")) {
            for (intent in CopilotIntent.values()) {
                val response = LocalizedAdvisor.adviseBundle(intent, bundle, language)
                assertEquals(response, NumberVerifier().verifyOrFallback(response, intent, bundle, response))
                assertEquals(bundle.forecastP50, response.predicted.single().range.p50, 0.0)
            }
        }
    }

    @Test fun selectedPlanMetadataMatchesActualTerminalQuantiles() {
        val result = calculator.calculate(Scenario.STABLE_INCOME)
        val plan = result.decision.plans.single { it.id == result.decision.recommended }
        assertEquals(plan.terminalCash.p10, result.bundle!!.forecastP10, 0.0)
        assertEquals(plan.shortfallRisk, result.bundle!!.projectedShortfallRisk, 0.0)
        assertEquals(6, result.decision.plans.single { it.id == FreelancerPlanId.STATUS_QUO }.periods.size)
    }
}
