package com.example.financeapp.domain.model

enum class PeriodType { MONTHLY, WEEKLY }

enum class ActionType { CUT_DISCRETIONARY, PREPAY_DEBT, BUILD_RESERVE, DEFER_GOAL }

enum class StrategyType { AGGRESSIVE_DEBT_PAYOFF, BALANCED_RESERVE }

enum class Scenario(val label: String) {
    STABLE_INCOME("Stable"),
    INCOME_REDUCTION("Income -30%"),
    UNEXPECTED_EXPENSE("Emergency ₹25k"),
    INFEASIBLE_TARGET("Infeasible"),
    HIGH_UNCERTAINTY("High Uncertainty"),
}

data class PlanningPeriod(
    val periodIndex: Int,
    val label: String,
    val startDate: String,
    val projectedIncome: Double,
    val essentialExpenses: Double,
    val debtObligations: Double,
    val projectedDiscretionary: Double,
)

data class CashReserveConstraint(val minRequiredReserve: Double)

data class Debt(
    val name: String,
    val outstandingPrincipal: Double,
    val annualInterestRate: Double,
    val monthlyEmi: Double,
)

data class SavingsGoal(
    val name: String,
    val targetAmount: Double,
    val deadlinePeriodIndex: Int,
)

data class ActionItem(
    val actionType: ActionType,
    val targetCategory: String,
    val amountDelta: Double,
    val description: String,
)

data class ActionSequence(
    val id: String,
    val name: String,
    val strategyType: StrategyType,
    val actionsPerPeriod: Map<Int, List<ActionItem>>,
    val projectedBalances: List<Double>,
    val terminalReserve: Double,
    val shortfallRisk: Double,
    val tradeOffSummary: String,
)

data class GoalConflict(
    val conflictedGoal: String,
    val shortfallAmount: Double,
    val violatedConstraint: String,
    val proposedRelaxation: String,
)
