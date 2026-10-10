package com.example.financeapp.domain.engine

import com.example.financeapp.data.SyntheticDataSeeder
import com.example.financeapp.domain.model.ActionType
import com.example.financeapp.domain.model.Scenario
import com.example.financeapp.domain.model.StrategyType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlannerTest {

    @Test
    fun testPlanComparisonPlanAvsPlanB() {
        val snapshot = SyntheticDataSeeder.generate(Scenario.STABLE_INCOME).snapshot
        val result = PlannerEngine.evaluatePlans(snapshot, riskThresholdTheta = 0.15)

        assertNotNull(result.planA)
        assertNotNull(result.planB)
        assertNotNull(result.statusQuo)

        assertEquals(StrategyType.AGGRESSIVE_DEBT_PAYOFF, result.planA.strategyType)
        assertEquals(StrategyType.BALANCED_RESERVE, result.planB.strategyType)

        assertEquals(6, result.planA.periodBreakdowns.size)
        assertEquals(6, result.planB.periodBreakdowns.size)

        // Trade-off 1: Plan A saves more interest than Plan B or Status Quo
        println("Interest Saved Plan A: ₹${result.planA.interestSavedVsStatusQuo}")
        println("Interest Saved Plan B: ₹${result.planB.interestSavedVsStatusQuo}")
        assertTrue(
            "Plan A should save more or equal interest than Plan B",
            result.planA.interestSavedVsStatusQuo >= result.planB.interestSavedVsStatusQuo
        )

        // Trade-off 2: Plan B holds a higher terminal liquid cash balance
        println("Terminal Balance Plan A: ₹${result.planA.terminalBalance}")
        println("Terminal Balance Plan B: ₹${result.planB.terminalBalance}")
        assertTrue(
            "Plan B should build a higher liquid reserve than Plan A",
            result.planB.terminalBalance >= result.planA.terminalBalance
        )

        // Trade-off 3: Plan B has equal or lower shortfall risk
        println("Shortfall Risk Plan A: ${result.planA.shortfallRisk * 100}%")
        println("Shortfall Risk Plan B: ${result.planB.shortfallRisk * 100}%")
        assertTrue(
            "Plan B should have <= shortfall risk compared to Plan A",
            result.planB.shortfallRisk <= result.planA.shortfallRisk + 0.05 // allowance for stochastic noise
        )
    }

    @Test
    fun testHardConstraintsRespected() {
        val snapshot = SyntheticDataSeeder.generate(Scenario.STABLE_INCOME).snapshot
        val result = PlannerEngine.evaluatePlans(snapshot)

        // Hard constraint: Discretionary cut cannot exceed 60% of baseline
        result.planA.periodBreakdowns.forEach { breakdown ->
            val cutPercent = breakdown.discretionaryCutAmount / (breakdown.discretionarySpend + breakdown.discretionaryCutAmount)
            assertTrue("Discretionary cut must be <= 60%", cutPercent <= 0.601)
            assertTrue("Ending balance should be positive", breakdown.endingBalance > 0.0)
            assertTrue("Regular debt service was paid", breakdown.regularDebtService > 0.0)
        }
    }

    @Test
    fun testDebtAmortisationReducesOutstandingPrincipal() {
        val snapshot = SyntheticDataSeeder.generate(Scenario.STABLE_INCOME).snapshot
        val result = PlannerEngine.evaluatePlans(snapshot)

        val initialDebt = snapshot.debts.sumOf { it.outstandingPrincipal }
        val terminalDebtA = result.planA.terminalDebt
        val terminalDebtStatusQuo = result.statusQuo.terminalDebt

        println("Initial Debt: ₹$initialDebt")
        println("Status Quo Terminal Debt: ₹$terminalDebtStatusQuo")
        println("Plan A Terminal Debt: ₹$terminalDebtA")

        assertTrue("Debt must decrease over time", terminalDebtStatusQuo < initialDebt)
        assertTrue("Plan A accelerates debt payoff vs Status Quo", terminalDebtA <= terminalDebtStatusQuo)
        assertTrue("Status Quo interest paid is higher than Plan A", result.statusQuo.totalInterestPaid > result.planA.totalInterestPaid)
    }

    @Test
    fun testDynamicPlanSelectionRationale() {
        val stableSnapshot = SyntheticDataSeeder.generate(Scenario.STABLE_INCOME).snapshot
        val stableResult = PlannerEngine.evaluatePlans(stableSnapshot, riskThresholdTheta = 0.15)

        // Under stable income, verify a non-empty rationale string explaining the decision
        println("Stable Selection: ${stableResult.recommendedPlan.name}")
        println("Rationale: ${stableResult.whyThisPlan}")
        assertTrue("Rationale must mention risk or interest", stableResult.whyThisPlan.contains("risk") || stableResult.whyThisPlan.contains("interest"))

        // Under high uncertainty, verify the engine warns or selects the liquidity-preserving plan
        val uncertainSnapshot = SyntheticDataSeeder.generate(Scenario.HIGH_UNCERTAINTY).snapshot
        val uncertainResult = PlannerEngine.evaluatePlans(uncertainSnapshot, riskThresholdTheta = 0.10)

        println("High Uncertainty Selection: ${uncertainResult.recommendedPlan.name}")
        println("High Uncertainty Rationale: ${uncertainResult.whyThisPlan}")

        // Under tight risk threshold (10%) and high uncertainty, Plan B must be selected over Plan A
        assertEquals("Plan B (Balanced Reserve Buffer)", uncertainResult.recommendedPlan.name)
        assertTrue(
            "Rationale explains why Plan B was chosen over Plan A",
            uncertainResult.whyThisPlan.contains("Plan B")
        )
    }

    @Test
    fun testActionSequenceModelIntegration() {
        val snapshot = SyntheticDataSeeder.generate(Scenario.STABLE_INCOME).snapshot
        val result = PlannerEngine.evaluatePlans(snapshot)

        val seqA = result.planA.actionSequence
        assertEquals(StrategyType.AGGRESSIVE_DEBT_PAYOFF, seqA.strategyType)
        assertEquals(6, seqA.projectedBalances.size)
        assertTrue("Trade-off summary must be non-empty", seqA.tradeOffSummary.isNotBlank())

        // Verify action items contain valid action types
        val allActionTypes = seqA.actionsPerPeriod.values.flatten().map { it.actionType }.toSet()
        assertTrue("Should contain CUT_DISCRETIONARY", allActionTypes.contains(ActionType.CUT_DISCRETIONARY))
        assertTrue("Should contain PREPAY_DEBT", allActionTypes.contains(ActionType.PREPAY_DEBT))
    }
}

