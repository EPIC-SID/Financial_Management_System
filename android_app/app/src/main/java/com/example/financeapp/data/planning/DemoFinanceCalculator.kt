package com.example.financeapp.data.planning

import com.example.financeapp.data.ingest.*
import com.example.financeapp.domain.engine.*
import com.example.financeapp.domain.freelancer.*
import com.example.financeapp.domain.model.AccountType
import com.example.financeapp.domain.model.Direction
import com.example.financeapp.domain.model.ResultBundle
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.roundToLong
import kotlin.random.Random

data class DemoFinanceResult(
    val dataset: SyntheticDataset,
    val history: NormalizedFinancialHistory,
    val inputs: SimulationInputs,
    val decision: FreelancerDecision,
    val bundle: ResultBundle?,
    val goal: SavingsGoal?,
    val conflict: GoalConflict?,
    val cardDues: Double
)

/** Demo-only boundary: no Room/TransactionEntity dependency and no writes to the live ledger. */
class DemoFinanceCalculator {
    fun calculate(scenario: Scenario, persona: Persona = Persona.B, relaxation: Relaxation? = null): DemoFinanceResult {
        val dataset = SyntheticDataSeeder.generate(persona, scenario)
        val profile = SyntheticPersonas.profile(persona)
        val asOf = dataset.demoDate.atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant()
        val cash = dataset.accounts.single { it.type == AccountType.SAVINGS }.latestBalance!!
        val past = dataset.transactions.filter { it.date < dataset.demoDate }
        val events = past.filterNot { it.accountId == profile.card.id && it.direction == Direction.CREDIT }.map { tx ->
            val kind = when {
                tx.category == "CARD_BILL" -> FinancialEventKind.CARD_SETTLEMENT
                tx.isTransfer -> FinancialEventKind.INTERNAL_TRANSFER
                tx.category == "INCOME" && tx.direction == Direction.CREDIT -> FinancialEventKind.INCOME
                tx.category == "EMI" -> FinancialEventKind.DEBT_PAYMENT
                tx.category == "INVESTMENTS" -> FinancialEventKind.SIP
                tx.direction == Direction.CREDIT -> FinancialEventKind.REFUND
                else -> FinancialEventKind.EXPENSE
            }
            FinancialEvent(tx.id, tx.date.atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant(), paise(tx.amount), kind,
                if (tx.accountId == profile.card.id) FundingSource.CREDIT_CARD else FundingSource.BANK,
                if (tx.category in setOf("RENT", "UTILITIES", "INSURANCE", "EDUCATION", "HEALTH", "MAINTENANCE"))
                    ExpenseClass.ESSENTIAL else ExpenseClass.DISCRETIONARY, tx.category)
        }
        val history = (FinancialHistoryNormalizer().normalize(events, SyntheticDataSeeder.calendarMonth(1), asOf,
            CashAnchor(paise(cash), asOf)) as HistoryNormalization.Ready).history
        val forecast = (FreelancerForecaster().forecast(history, 42) as FreelancerForecast.Ready).forecast
        val loans = dataset.liabilities.filter { it.kind != "CARD" }.map {
            require(it.annualRate != null && it.outstanding != null && it.emi != null) { "Incomplete demo loan" }
            SimulationLoan(it.outstanding, it.annualRate, it.emi)
        }
        val recentCardBills = past.filter { it.accountId == profile.bank.id && it.category == "CARD_BILL" }
            .groupBy { YearMonth.from(it.date) }
        val cardBill = history.months.takeLast(6).map { month ->
            recentCardBills[month.month].orEmpty().sumOf { it.amount }
        }.average()
        val cardDues = dataset.liabilities.filter { it.kind == "CARD" }.sumOf { it.totalDue ?: it.unbilled ?: 0.0 }
        // Future calendar obligations are declared assumptions, never read from generated test outcomes.
        val commitments = List(6) { index ->
            val month = YearMonth.from(dataset.demoDate).plusMonths(index.toLong()).monthValue
            val essentials = profile.bills.filter { it.calendarMonths == null || month in it.calendarMonths }.sumOf { it.amount }
            MonthlyCommitments(paise(essentials), paise(loans.sumOf { it.emi } + cardBill),
                paise(dataset.investments.sumOf { it.monthlyAmount }))
        }
        var inputs = ForecastPathAdapter().adapt(forecast, commitments, paise(dataset.declared.minReserve), loans)
        val random = Random(73)
        fun declaredPath(path: List<CashFlowPeriod>, sampled: Boolean) = path.mapIndexed { index, p ->
            val income = when {
                scenario == Scenario.INFEASIBLE_TARGET -> SyntheticDataSeeder.REDUCED_INCOME
                scenario == Scenario.INCOME_REDUCTION && index >= 1 -> SyntheticDataSeeder.REDUCED_INCOME
                scenario == Scenario.HIGH_UNCERTAINTY -> profile.income.base * if (sampled) 0.6 + random.nextDouble() * 0.8 else 1.0
                profile.income.kind == IncomeKind.FIXED -> profile.income.base
                else -> p.income
            }
            p.copy(income = income, essentials = p.essentials +
                if (scenario == Scenario.UNEXPECTED_EXPENSE && index == 2) SyntheticDataSeeder.EMERGENCY_AMOUNT else 0.0)
        }
        inputs = inputs.copy(periods = declaredPath(inputs.periods, false),
            sampledPaths = inputs.sampledPaths.map { declaredPath(it, true) })
        var goal = dataset.declared.goal?.let { SavingsGoal(it.targetAmount, it.byPeriodIndex) }
        if (relaxation != null) {
            require(goal != null && relaxation.feasible)
            // Accept only an option computed for this exact scenario, not arbitrary UI supplied amounts.
            require(ConflictResolver().resolve(inputs, goal)?.relaxations?.contains(relaxation) == true)
            when (relaxation.kind) {
                RelaxationKind.LOWER_TARGET -> goal = goal.copy(targetAmount = relaxation.value)
                RelaxationKind.EXTEND_TIMELINE -> goal = goal.copy(byPeriod = relaxation.value.toInt())
                RelaxationKind.REDUCE_RESERVE -> inputs = inputs.copy(minReserve = relaxation.value)
                RelaxationKind.REDUCE_SIP -> {
                    fun cut(path: List<CashFlowPeriod>) = path.map { it.copy(sip = (it.sip - relaxation.delta).coerceAtLeast(0.0)) }
                    inputs = inputs.copy(periods = cut(inputs.periods), sampledPaths = inputs.sampledPaths.map(::cut))
                }
            }
        }
        val decision = FreelancerDecisionAdapter().compare(inputs)
        val conflict = goal?.let { ConflictResolver().resolve(inputs, it) }
        val bundle = try { ForecastResultAdapter().adapt(history, inputs, decision, paise(cardDues)) }
            catch (_: IllegalArgumentException) { null }
        return DemoFinanceResult(dataset, history, inputs, decision, bundle, goal, conflict, cardDues)
    }

    private fun paise(value: Double): Long {
        require(value.isFinite() && value in 0.0..1e12)
        return (value * 100).roundToLong()
    }
}
