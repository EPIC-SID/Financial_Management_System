package com.example.financeapp.domain.engine

import org.junit.Assert.*
import org.junit.Test

class ConflictResolverTest {
    private val resolver = ConflictResolver()
    private val simulator = ImpactSimulator()

    private fun inputs(period: CashFlowPeriod, cash: Double = 45200.0, reserve: Double = 40000.0,
                       count: Int = 24): SimulationInputs {
        val path = List(count) { period }
        return SimulationInputs(cash, reserve, path, listOf(path))
    }

    @Test
    fun scenarioDComputesConflictAndFourHonestRelaxations() {
        val data = inputs(CashFlowPeriod(55000.0, 24500.0, 26700.0, 6400.0, 10000.0))
        val conflict = resolver.resolve(data, SavingsGoal(100000.0, 6))!!
        assertEquals(1, conflict.firstViolatedPeriod)
        assertEquals(100000.0, conflict.shortfall, 0.01)
        assertEquals(4, conflict.relaxations.size)
        assertTrue(conflict.relaxations.all { it.riskAfter in 0.0..1.0 })
        assertTrue(conflict.relaxations.all { !it.feasible })
        assertEquals(1.0, conflict.relaxations.first().riskAfter, 0.0)
        assertTrue(conflict.relaxations.last().notRecommended)
    }

    @Test
    fun bisectionFindsEachFeasibleBoundary() {
        val data = inputs(CashFlowPeriod(55000.0, 20000.0, 10000.0, sip = 15000.0), cash = 80000.0)
        val goal = SavingsGoal(120000.0, 6)
        val conflict = resolver.resolve(data, goal)!!
        val options = conflict.relaxations.associateBy { it.kind }
        assertEquals(100000.0, conflict.maximumAchievable, 0.02)
        assertEquals(5, conflict.firstViolatedPeriod)
        assertEquals(8.0, options.getValue(RelaxationKind.EXTEND_TIMELINE).value, 0.0)
        assertEquals(20000.0, options.getValue(RelaxationKind.REDUCE_RESERVE).value, 100.0)
        assertEquals(20000.0 / 6, options.getValue(RelaxationKind.REDUCE_SIP).delta, 100.0)
        assertTrue(options.values.all { it.feasible && it.riskAfter == 0.0 })
        val reserve = options.getValue(RelaxationKind.REDUCE_RESERVE).value
        assertNotNull(resolver.resolve(data.copy(minReserve = reserve + 101), goal))
        val cut = options.getValue(RelaxationKind.REDUCE_SIP).delta
        val smallerCutPath = data.periods.map { it.copy(sip = it.sip - cut + 101) }
        assertNotNull(resolver.resolve(data.copy(periods = smallerCutPath), goal))
    }

    @Test
    fun feasibleGoalReturnsNoConflictAndAbsentSipHasThreeOptions() {
        val data = inputs(CashFlowPeriod(55000.0, 20000.0, 10000.0), cash = 80000.0)
        assertNull(resolver.resolve(data, SavingsGoal(100000.0, 6)))
        val conflict = resolver.resolve(data, SavingsGoal(300000.0, 6))!!
        assertEquals(3, conflict.relaxations.size)
        assertTrue(conflict.relaxations.none { it.kind == RelaxationKind.REDUCE_SIP })
    }

    @Test
    fun extensionFindsEarlierSolutionEvenWhenLaterObligationsMakeItImpossible() {
        val normal = CashFlowPeriod(10000.0, 0.0, 0.0)
        val path = List(24) { if (it == 8) normal.copy(essentials = 500000.0) else normal }
        val data = SimulationInputs(0.0, 0.0, path, listOf(path))
        val option = resolver.resolve(data, SavingsGoal(70000.0, 6))!!.relaxations[1]
        assertTrue(option.feasible)
        assertEquals(7.0, option.value, 0.0)
    }

    @Test
    fun relaxationRiskCountsBreachingPathsRatherThanUsingP50AsProbability() {
        val data = inputs(CashFlowPeriod(55000.0, 20000.0, 10000.0, sip = 15000.0), cash = 80000.0)
        val poor = data.periods.map { it.copy(income = 20000.0) }
        val conflict = resolver.resolve(data.copy(sampledPaths = listOf(data.periods, poor)), SavingsGoal(120000.0, 6))!!
        assertTrue(conflict.relaxations.all { it.riskAfter == 0.5 })
    }

    @Test
    fun whatIfExpenseIncomeAndSipReturnExpectedDeltas() {
        val data = inputs(CashFlowPeriod(55000.0, 20000.0, 10000.0, sip = 10000.0), cash = 80000.0, count = 6)
        val identity = simulator.simulate(data, WhatIfChange())
        assertEquals(0.0, identity.runwayDeltaMonths, 0.0)
        assertEquals(0.0, identity.shortfallRiskDelta, 0.0)
        val expense = simulator.simulate(data, WhatIfChange(oneOffExpense = 100000.0, expensePeriod = 3))
        assertEquals(-100000.0 / 30000, expense.runwayDeltaMonths, 1e-9)
        assertEquals(1.0, expense.shortfallRiskDelta, 0.0)
        assertEquals(6, expense.periods.size)
        val cut = simulator.simulate(data, WhatIfChange(incomeChange = -0.5))
        assertTrue(cut.runwayDeltaMonths < 0)
        val pause = simulator.simulate(data, WhatIfChange(sipReduction = 10000.0))
        assertEquals(2.0, pause.runwayDeltaMonths, 1e-9)
        assertEquals(0.0, pause.interestSaved, 0.0)
    }

    @Test
    fun prepaymentAmortizesDebtWithoutDoubleCountingEmi() {
        val data = inputs(CashFlowPeriod(55000.0, 20000.0, 10000.0), cash = 100000.0, count = 2)
            .copy(loans = listOf(SimulationLoan(100000.0, 0.12, 10000.0)))
        val result = simulator.simulate(data, WhatIfChange(extraPrepayment = 20000.0))
        assertEquals(200.0, result.interestSaved, 1e-9)
        assertEquals(105000.0, result.periods.first().ending, 1e-9)
        assertEquals(130000.0, result.periods.last().ending, 1e-9)
    }

    @Test
    fun openingReserveBreachCannotBeHiddenByLaterIncome() {
        val data = inputs(CashFlowPeriod(55000.0, 20000.0, 10000.0), cash = 30000.0)
        val conflict = resolver.resolve(data, SavingsGoal(10000.0, 6))!!
        assertEquals(0, conflict.firstViolatedPeriod)
        assertFalse(conflict.relaxations.first().feasible)
        val reserve = conflict.relaxations.first { it.kind == RelaxationKind.REDUCE_RESERVE }
        assertTrue(reserve.feasible)
        assertEquals(30000.0, reserve.value, 100.0)
    }

    @Test
    fun fullPrepaymentStopsFutureInterestAndCapsPaymentsAtRemainingDebt() {
        val data = inputs(CashFlowPeriod(0.0, 0.0, 1000.0), cash = 10000.0, reserve = 0.0, count = 2)
            .copy(loans = listOf(SimulationLoan(1000.0, 0.12, 1000.0)))
        val result = simulator.simulate(data, WhatIfChange(extraPrepayment = 1000.0))
        assertEquals(0.1, result.interestSaved, 1e-9)
        assertEquals(8990.0, result.periods.last().ending, 1e-9)
    }

    @Test
    fun identicalDrawsMakeRiskDeltasDeterministicAndFast() {
        val base = inputs(CashFlowPeriod(55000.0, 20000.0, 10000.0, sip = 10000.0), count = 6)
        val paths = List(2000) { i -> base.periods.map { it.copy(income = 25000.0 + i * 20) } }
        val data = base.copy(sampledPaths = paths)
        val change = WhatIfChange(oneOffExpense = 25000.0, expensePeriod = 3)
        repeat(3) { simulator.simulate(data, change) }
        val start = System.nanoTime()
        val result = simulator.simulate(data, change)
        val elapsedMs = (System.nanoTime() - start) / 1000000.0
        println("ImpactSimulator: 2000 paths x 6 periods in $elapsedMs ms")
        assertTrue("Simulation took $elapsedMs ms", elapsedMs < 200)
        assertEquals(result, simulator.simulate(data, change))
        assertTrue(result.shortfallRiskDelta > 0.0)
    }

    @Test
    fun rejectsInvalidMoneyPathsAndActions() {
        assertThrows(IllegalArgumentException::class.java) { CashFlowPeriod(Double.NaN, 0.0, 0.0) }
        assertThrows(IllegalArgumentException::class.java) { SavingsGoal(-1.0, 6) }
        assertThrows(IllegalArgumentException::class.java) { WhatIfChange(discretionaryCut = 0.7) }
        val data = inputs(CashFlowPeriod(10000.0, 5000.0, 0.0), count = 6)
        assertThrows(IllegalArgumentException::class.java) { data.copy(sampledPaths = emptyList()) }
        assertThrows(IllegalArgumentException::class.java) { resolver.resolve(data, SavingsGoal(1000.0, 7)) }
        assertThrows(IllegalArgumentException::class.java) { simulator.simulate(data, WhatIfChange(extraPrepayment = 1.0)) }
    }
}
