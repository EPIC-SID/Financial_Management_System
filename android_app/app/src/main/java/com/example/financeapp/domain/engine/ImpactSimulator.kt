package com.example.financeapp.domain.engine

data class CashFlowPeriod(
    val income: Double,
    val essentials: Double,
    val debtService: Double,
    val discretionary: Double = 0.0,
    val sip: Double = 0.0
) {
    init {
        require(listOf(income, essentials, debtService, discretionary, sip).all { it.isFinite() && it >= 0 })
    }
}

data class SimulationLoan(val outstanding: Double, val annualRate: Double, val emi: Double) {
    init {
        require(listOf(outstanding, annualRate, emi).all { it.isFinite() && it >= 0 })
    }
}

/** Monthly P50 and sampled paths. Debt service includes loan EMIs; annual rates are fractions. */
data class SimulationInputs(
    val cash: Double,
    val minReserve: Double,
    val periods: List<CashFlowPeriod>,
    val sampledPaths: List<List<CashFlowPeriod>>,
    val loans: List<SimulationLoan> = emptyList()
) {
    init {
        require(cash.isFinite() && cash >= 0 && minReserve.isFinite() && minReserve >= 0)
        require(periods.isNotEmpty() && periods.size <= 24)
        require(sampledPaths.isNotEmpty() && sampledPaths.all { it.size == periods.size })
        require((listOf(periods) + sampledPaths).all { path ->
            path.all { it.debtService >= loans.sumOf { loan -> loan.emi } }
        })
    }
}

data class WhatIfChange(
    val discretionaryCut: Double = 0.0,
    val incomeChange: Double = 0.0,
    val oneOffExpense: Double = 0.0,
    val expensePeriod: Int = 1,
    val sipReduction: Double = 0.0,
    val extraPrepayment: Double = 0.0
) {
    init {
        require(discretionaryCut.isFinite() && discretionaryCut in 0.0..0.6)
        require(incomeChange.isFinite() && incomeChange >= -1.0)
        require(listOf(oneOffExpense, sipReduction, extraPrepayment).all { it.isFinite() && it >= 0 })
        require(expensePeriod >= 1)
    }
}

data class ImpactPeriod(val index: Int, val opening: Double, val ending: Double)

data class ImpactResult(
    val runwayDeltaMonths: Double,
    val shortfallRiskDelta: Double,
    val interestSaved: Double,
    val periods: List<ImpactPeriod>
)

class ImpactSimulator {
    /** Runway compares horizon-end cash using the baseline monthly essentials and debt service. */
    fun simulate(inputs: SimulationInputs, change: WhatIfChange): ImpactResult {
        require(change.expensePeriod <= inputs.periods.size)
        require(change.extraPrepayment <= inputs.loans.sumOf { it.outstanding })
        val baseline = run(inputs, inputs.periods, WhatIfChange())
        val changed = run(inputs, inputs.periods, change)
        var baselineBreaches = 0
        var changedBreaches = 0
        // Reusing supplied draws keeps a slider change from introducing sampling noise.
        for (path in inputs.sampledPaths) {
            if (run(inputs, path, WhatIfChange(), false).breached) baselineBreaches++
            if (run(inputs, path, change, false).breached) changedBreaches++
        }
        val monthlyCost = inputs.periods.map { it.essentials + it.debtService }.average()
        fun runway(cash: Double): Double {
            val available = (cash - inputs.minReserve).coerceAtLeast(0.0)
            return if (monthlyCost == 0.0) {
                if (available == 0.0) 0.0 else Double.POSITIVE_INFINITY
            } else available / monthlyCost
        }
        val before = runway(baseline.ending)
        val after = runway(changed.ending)
        return ImpactResult(
            runwayDeltaMonths = if (before == after) 0.0 else after - before,
            shortfallRiskDelta = (changedBreaches - baselineBreaches).toDouble() / inputs.sampledPaths.size,
            interestSaved = baseline.interest - changed.interest,
            periods = changed.periods
        )
    }

    private data class Run(val ending: Double, val interest: Double, val breached: Boolean, val periods: List<ImpactPeriod>)

    private fun run(inputs: SimulationInputs, path: List<CashFlowPeriod>, change: WhatIfChange, table: Boolean = true): Run {
        val principal = inputs.loans.map { it.outstanding }.toDoubleArray()
        val order = inputs.loans.indices.sortedByDescending { inputs.loans[it].annualRate }
        var cash = inputs.cash
        var interest = 0.0
        var breached = cash < inputs.minReserve
        val periods = if (table) ArrayList<ImpactPeriod>(path.size) else null
        path.forEachIndexed { index, period ->
            val opening = cash
            var loanPayments = 0.0
            inputs.loans.forEachIndexed { i, loan ->
                val accrued = principal[i] * loan.annualRate / 12.0
                interest += accrued
                val payment = minOf(loan.emi, principal[i] + accrued)
                principal[i] = (principal[i] + accrued - payment).coerceAtLeast(0.0)
                loanPayments += payment
            }
            cash += period.income * (1.0 + change.incomeChange) - period.essentials -
                (period.debtService - inputs.loans.sumOf { it.emi } + loanPayments) -
                period.discretionary * (1.0 - change.discretionaryCut) -
                (period.sip - change.sipReduction).coerceAtLeast(0.0)
            if (index == change.expensePeriod - 1) cash -= change.oneOffExpense
            if (index == 0) {
                var remaining = change.extraPrepayment
                for (i in order) {
                    val payment = minOf(remaining, principal[i])
                    principal[i] -= payment
                    remaining -= payment
                    cash -= payment
                }
            }
            breached = breached || cash < inputs.minReserve
            periods?.add(ImpactPeriod(index + 1, opening, cash))
        }
        return Run(cash, interest, breached, periods ?: emptyList())
    }
}
