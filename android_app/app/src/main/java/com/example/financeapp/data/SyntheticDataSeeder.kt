package com.example.financeapp.data

import com.example.financeapp.domain.model.CashReserveConstraint
import com.example.financeapp.domain.model.Debt
import com.example.financeapp.domain.model.FinancialSnapshot
import com.example.financeapp.domain.model.PlanningPeriod
import com.example.financeapp.domain.model.RecurringObligation
import com.example.financeapp.domain.model.SavingsGoal
import com.example.financeapp.domain.model.Scenario
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Random

/**
 * In-memory, deterministic generator for the 5 demo scenarios.
 *
 * Timeline (relative to [ANCHOR] = Month 0):
 * - Train: Month -6 to -2, Validation: Month -1  -> [FinancialSnapshot.recentTransactions]
 * - Planning horizon: Month +1 to +6             -> [FinancialSnapshot.planningPeriods] (expected values)
 *   and [ScenarioData.futureActuals] (one realised outcome, ground truth for backtests)
 */
object SyntheticDataSeeder {

    const val HISTORY_MONTHS = 6
    const val HORIZON_MONTHS = 6
    const val DEFAULT_SEED = 42L

    /** Fixed so every run produces identical dates. */
    val ANCHOR: YearMonth = YearMonth.of(2026, 10)

    const val OPENING_BALANCE = 40_000.0
    const val DEFAULT_MIN_RESERVE = 25_000.0
    const val STABLE_INCOME = 85_000.0
    const val REDUCED_INCOME = 55_000.0
    /** Tuned so ~40%+ of seeded futures breach the reserve (spec: shortfall risk > 35%). */
    const val HIGH_UNCERTAINTY_BASE_INCOME = 70_000.0
    const val INCOME_VOLATILITY = 0.40
    const val MEDICAL_EMERGENCY = 25_000.0
    const val MEDICAL_EMERGENCY_MONTH = 3
    const val INCOME_DROP_MONTH = 2

    val RECURRING = listOf(
        RecurringObligation("Rent", 22_000.0, 1, "BILLS"),
        RecurringObligation("HDFC Home Loan EMI", 18_500.0, 10, "BILLS"),
        RecurringObligation("Car Loan EMI", 8_200.0, 15, "BILLS"),
        RecurringObligation("Electricity & Utilities", 2_500.0, 20, "BILLS"),
    )

    val DEBTS = listOf(
        Debt("HDFC Home Loan", 18_00_000.0, 0.085, 18_500.0),
        Debt("Car Loan", 3_50_000.0, 0.095, 8_200.0),
    )

    private val ESSENTIAL_FIXED = RECURRING.filterNot { it.name.endsWith("EMI") }.sumOf { it.amount }
    private val DEBT_FIXED = RECURRING.filter { it.name.endsWith("EMI") }.sumOf { it.amount }

    /** category, merchant, count per month, mean amount, std dev. */
    private data class Discretionary(val category: String, val merchant: String, val count: Int, val mean: Double, val sd: Double)

    private val DISCRETIONARY = listOf(
        Discretionary("FOOD", "Swiggy", 8, 900.0, 350.0),
        Discretionary("SHOPPING", "Amazon", 2, 2_500.0, 1_200.0),
        Discretionary("TRAVEL", "Uber", 1, 2_000.0, 900.0),
    )

    private val ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    data class ScenarioData(
        val snapshot: FinancialSnapshot,
        /** Month +1 to +6 realised transactions; never shown to the engines being evaluated. */
        val futureActuals: List<TransactionEntity>,
        val reserve: CashReserveConstraint,
    )

    fun generate(scenario: Scenario, seed: Long = DEFAULT_SEED): ScenarioData {
        val rng = Random(seed * 31 + scenario.ordinal)
        val history = (-HISTORY_MONTHS..-1).flatMap { monthTransactions(scenario, it, rng) }
        val future = (1..HORIZON_MONTHS).flatMap { monthTransactions(scenario, it, rng) }

        val avgDiscretionary = history
            .filter { tx -> DISCRETIONARY.any { it.category == tx.category } }
            .sumOf { -it.amount } / HISTORY_MONTHS

        val periods = (1..HORIZON_MONTHS).map { m ->
            val ym = ANCHOR.plusMonths(m.toLong())
            PlanningPeriod(
                periodIndex = m,
                label = "M+$m (${ym.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${ym.year})",
                startDate = ym.atDay(1).toString(),
                projectedIncome = expectedIncome(scenario, m),
                essentialExpenses = ESSENTIAL_FIXED + if (hasEmergency(scenario, m)) MEDICAL_EMERGENCY else 0.0,
                debtObligations = DEBT_FIXED,
                projectedDiscretionary = avgDiscretionary,
            )
        }

        val minReserve = if (scenario == Scenario.INFEASIBLE_TARGET) 40_000.0 else DEFAULT_MIN_RESERVE
        val goal = if (scenario == Scenario.INFEASIBLE_TARGET) {
            SavingsGoal("Save ₹1,00,000 in 6 months", 1_00_000.0, HORIZON_MONTHS)
        } else null

        return ScenarioData(
            snapshot = FinancialSnapshot(
                activeScenario = scenario,
                currentBalance = OPENING_BALANCE,
                minReserve = minReserve,
                debts = DEBTS,
                recurringObligations = RECURRING,
                recentTransactions = history,
                planningPeriods = periods,
                savingsGoal = goal,
            ),
            futureActuals = future,
            reserve = CashReserveConstraint(minReserve),
        )
    }

    fun expectedIncome(scenario: Scenario, month: Int): Double = when (scenario) {
        Scenario.STABLE_INCOME, Scenario.UNEXPECTED_EXPENSE -> STABLE_INCOME
        Scenario.INCOME_REDUCTION -> if (month >= INCOME_DROP_MONTH) REDUCED_INCOME else STABLE_INCOME
        Scenario.INFEASIBLE_TARGET -> REDUCED_INCOME
        Scenario.HIGH_UNCERTAINTY -> HIGH_UNCERTAINTY_BASE_INCOME
    }

    private fun hasEmergency(scenario: Scenario, month: Int) =
        scenario == Scenario.UNEXPECTED_EXPENSE && month == MEDICAL_EMERGENCY_MONTH

    private fun monthTransactions(scenario: Scenario, month: Int, rng: Random): List<TransactionEntity> {
        val ym = ANCHOR.plusMonths(month.toLong())
        val txs = mutableListOf<TransactionEntity>()
        fun add(day: Int, merchant: String, amount: Double, category: String) {
            txs += tx(scenario, month, txs.size, ym, day, merchant, amount, category)
        }

        val income = if (scenario == Scenario.HIGH_UNCERTAINTY) {
            HIGH_UNCERTAINTY_BASE_INCOME * (1 + INCOME_VOLATILITY * (2 * rng.nextDouble() - 1))
        } else expectedIncome(scenario, month)
        add(1, "Salary Credit", round(income), "INCOME")

        RECURRING.forEach { add(it.dayOfMonth, it.name, -it.amount, it.category) }

        DISCRETIONARY.forEach { d ->
            repeat(d.count) {
                val amount = (d.mean + d.sd * rng.nextGaussian()).coerceAtLeast(d.mean * 0.2)
                add(1 + rng.nextInt(ym.lengthOfMonth()), d.merchant, -round(amount), d.category)
            }
        }

        if (hasEmergency(scenario, month)) add(12, "Apollo Hospital", -MEDICAL_EMERGENCY, "HEALTH")

        return txs.sortedBy { it.transactionDate }
    }

    private fun tx(
        scenario: Scenario, month: Int, n: Int, ym: YearMonth, day: Int,
        merchant: String, amount: Double, category: String,
    ): TransactionEntity {
        val at = LocalDateTime.of(ym.year, ym.month, day.coerceAtMost(ym.lengthOfMonth()), 10, 0).plusMinutes(n.toLong())
        return TransactionEntity(
            smsId = "syn-${scenario.name}-$month-$n",
            merchant = merchant,
            amount = amount,
            category = category,
            description = "Synthetic ${scenario.label}",
            transactionDate = at.format(ISO),
            isCategorizedByAi = true,
            createdAt = at.toEpochSecond(ZoneOffset.UTC) * 1000,
        )
    }

    private fun round(x: Double) = Math.round(x).toDouble()
}
