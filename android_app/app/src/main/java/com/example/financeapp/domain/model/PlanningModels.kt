package com.example.financeapp.domain.model

enum class PeriodType { MONTHLY, WEEKLY }

data class Goal(val name: String, val targetAmount: Double, val byPeriodIndex: Int)

data class DeclaredInputs(
    /** FIXED, VARIABLE, MIXED */
    val incomeType: String,
    val typicalIncome: Double?,
    /** Hard planner constraint. */
    val minReserve: Double,
    val goal: Goal?,
    /** Tone only. */
    val ageRange: String?,
)

data class PlanningPeriod(
    val index: Int,
    val label: String,
    val opening: Double,
    val income: Double,
    val essentials: Double,
    val debtService: Double,
    val discretionary: Double,
    val sip: Double,
    val prepay: Double,
    val saved: Double,
    val ending: Double,
    val reserve: Double,
    val constraintOk: Boolean,
)

data class ActionSequence(
    val id: String,
    val name: String,
    val periods: List<PlanningPeriod>,
    /** Cash minus outstanding debt at horizon end. */
    val terminalNetPosition: Double,
    /** SIP contributions over the horizon, reported separately. */
    val investedTotal: Double,
    val interestPaid: Double,
    val interestSavedVsStatusQuo: Double,
    /** P(min balance < reserve) from Monte Carlo. */
    val shortfallRisk: Double,
    val tradeOff: String,
)

data class GoalConflict(
    val goal: String,
    val shortfall: Double,
    val violatedConstraint: String,
    val firstViolatedPeriod: Int,
    val relaxations: List<Relaxation>,
)

data class Relaxation(val kind: String, val description: String, val delta: Double, val riskAfter: Double)
