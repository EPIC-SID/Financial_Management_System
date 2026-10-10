package com.example.financeapp.data.ingest

import com.example.financeapp.domain.model.Account
import com.example.financeapp.domain.model.AccountType
import com.example.financeapp.domain.model.DeclaredInputs
import com.example.financeapp.domain.model.Direction
import com.example.financeapp.domain.model.Goal
import com.example.financeapp.domain.model.Investment
import com.example.financeapp.domain.model.Liability
import com.example.financeapp.domain.model.Source
import com.example.financeapp.domain.model.Txn
import java.security.MessageDigest
import java.time.LocalDate
import java.time.YearMonth
import java.util.Random
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToLong

data class SeedWindow(val name: String, val months: IntRange, val seed: Long)

data class Seeds(val train: Long = 101L, val validation: Long = 202L, val test: Long = 303L)

/** A "statement generated" alert. Not money movement, so it is not a [Txn]. */
data class CardStatement(
    val cardAccountId: String,
    val statementDate: LocalDate,
    val totalDue: Double,
    val minDue: Double?,
    val dueDate: LocalDate,
)

data class SyntheticDataset(
    val persona: Persona,
    val scenario: Scenario,
    /** First day of the test window (month 25). The live snapshot uses only data before this date. */
    val demoDate: LocalDate,
    val windows: List<SeedWindow>,
    /** All 30 months, raw (no recurring labels), sorted by date. */
    val transactions: List<Txn>,
    val cardStatements: List<CardStatement>,
    val openingBalances: Map<String, Double>,
    /** Ground truth as of [demoDate]. */
    val accounts: List<Account>,
    val liabilities: List<Liability>,
    val investments: List<Investment>,
    val declared: DeclaredInputs,
    /** Declared synthetic assumptions, for the evidence README. */
    val assumptions: List<String>,
)

/**
 * Deterministic synthetic histories (06-backend.md section 12).
 * Months 1-18 train, 19-24 validation, 25-30 test; each window draws from its own seeded stream,
 * and scenario overlays touch only the test window, so months 1-24 are identical across scenarios.
 */
object SyntheticDataSeeder {

    const val TOTAL_MONTHS = 30
    val TRAIN = 1..18
    val VALIDATION = 19..24
    val TEST = 25..30
    val DEMO_DATE: LocalDate = LocalDate.of(2026, 10, 1)
    private val MONTH_25: YearMonth = YearMonth.from(DEMO_DATE)

    const val REDUCED_INCOME = 55_000.0
    const val INCOME_DROP_MONTH = 26
    const val EMERGENCY_AMOUNT = 25_000.0
    const val EMERGENCY_MONTH = 27
    const val INFEASIBLE_RESERVE = 40_000.0
    const val INFEASIBLE_GOAL = 1_00_000.0
    const val HIGH_UNCERTAINTY_VOLATILITY = 0.40

    const val DATE_JITTER_DAYS = 2
    const val FESTIVAL_UPLIFT = 0.20
    val FESTIVAL_MONTHS = setOf(10, 11)
    const val SHOCK_RATE_PER_MONTH = 0.15
    const val SHOCK_MEDIAN = 8_000.0
    private const val SHOCK_LOG_SIGMA = 0.6
    private val SHOCK_MERCHANTS = listOf("City Hospital" to "HEALTH", "Car Service Centre" to "MAINTENANCE", "Home Repairs" to "MAINTENANCE")

    fun calendarMonth(month: Int): YearMonth = MONTH_25.plusMonths((month - 25).toLong())

    fun generate(
        persona: Persona,
        scenario: Scenario = Scenario.STABLE_INCOME,
        seeds: Seeds = Seeds(),
        shocks: Boolean = true,
    ): SyntheticDataset {
        require(scenario == Scenario.STABLE_INCOME || persona == Persona.B || persona == Persona.B_SPARSE) {
            "Scenarios are defined on persona B (06-backend.md 12.3)"
        }
        val p = SyntheticPersonas.profile(persona)
        val streams = mapOf(
            TRAIN to Random(seeds.train * 1_000 + persona.ordinal),
            VALIDATION to Random(seeds.validation * 1_000 + persona.ordinal),
            TEST to Random(seeds.test * 1_000 + persona.ordinal),
        )
        val volatility = Random(seeds.test * 1_000 + persona.ordinal + 777)

        val txns = mutableListOf<Txn>()
        var logIncomeDev = 0.0
        var slippedIncome: Pair<Double, Int>? = null

        for (m in 1..TOTAL_MONTHS) {
            val rng = streams.entries.first { m in it.key }.value
            val ym = calendarMonth(m)
            var n = 0
            fun add(day: Int, merchant: String, amount: Double, dir: Direction, category: String, accountId: String, isTransfer: Boolean = false) {
                txns += Txn(
                    id = "syn-${persona.name}-$m-${n++}",
                    date = ym.atDay(day.coerceIn(1, ym.lengthOfMonth())),
                    amount = round(amount), direction = dir, category = category,
                    counterpartyHash = hash(merchant), accountId = accountId,
                    isTransfer = isTransfer, recurringId = null, source = Source.SYNTHETIC,
                )
            }
            fun jitter(day: Int) = day + rng.nextInt(2 * DATE_JITTER_DAYS + 1) - DATE_JITTER_DAYS

            // Income
            var income = when (p.income.kind) {
                IncomeKind.FIXED -> p.income.base + if (ym.monthValue in p.income.bonusCalendarMonths) p.income.bonusAmount else 0.0
                IncomeKind.AR1 -> {
                    val innovationSd = p.income.logSigma * kotlin.math.sqrt(1 - p.income.phi * p.income.phi)
                    logIncomeDev = p.income.phi * logIncomeDev + innovationSd * rng.nextGaussian()
                    p.income.base * exp(logIncomeDev - p.income.logSigma * p.income.logSigma / 2)
                }
            }
            if (m in TEST) income = when (scenario) {
                Scenario.INCOME_REDUCTION -> if (m >= INCOME_DROP_MONTH) REDUCED_INCOME else income
                Scenario.INFEASIBLE_TARGET -> REDUCED_INCOME
                Scenario.HIGH_UNCERTAINTY -> p.income.base * (1 + HIGH_UNCERTAINTY_VOLATILITY * (2 * volatility.nextDouble() - 1))
                else -> income
            }
            val payDay = jitter(p.income.payDay)
            slippedIncome?.let { (amount, _) -> add(2 + rng.nextInt(4), p.income.employer, amount, Direction.CREDIT, "INCOME", p.bank.id) }
            slippedIncome = null
            if (rng.nextDouble() < p.income.slipChance) slippedIncome = income to m
            else add(payDay, p.income.employer, income, Direction.CREDIT, "INCOME", p.bank.id)

            // Fixed bills
            for (b in p.bills) {
                if (b.calendarMonths != null && ym.monthValue !in b.calendarMonths) continue
                val amount = b.amount * (1 + b.amountCv * rng.nextGaussian()).coerceAtLeast(0.3)
                add(jitter(b.day), b.merchant, amount, Direction.DEBIT, b.category, p.bank.id)
            }

            // Loan EMIs
            for (loan in p.loans) {
                if (m < loan.firstMonth || m > lastEmiMonth(loan)) continue
                add(jitter(loan.dueDay), loan.lender, loan.emi, Direction.DEBIT, "EMI", p.bank.id)
            }

            // SIP
            p.sip?.let { if (m >= it.firstMonth) add(jitter(it.debitDay), it.platform, it.amount, Direction.DEBIT, "INVESTMENTS", p.bank.id) }

            // Variable spend: lognormal monthly total per category, festival uplift, split across the month
            val uplift = if (ym.monthValue in FESTIVAL_MONTHS) 1 + FESTIVAL_UPLIFT else 1.0
            for (v in p.variable) {
                val total = v.monthlyMean * uplift * exp(v.logSigma * rng.nextGaussian() - v.logSigma * v.logSigma / 2)
                val weights = DoubleArray(v.txnsPerMonth) { 0.5 + rng.nextDouble() }
                val sum = weights.sum()
                weights.forEach { w ->
                    add(1 + rng.nextInt(ym.lengthOfMonth()), v.merchants[rng.nextInt(v.merchants.size)], total * w / sum,
                        Direction.DEBIT, v.category, if (v.onCard) p.card.id else p.bank.id)
                }
            }

            // Optional one-off essential shocks (Poisson)
            if (shocks) repeat(poisson(rng, SHOCK_RATE_PER_MONTH)) {
                val (merchant, category) = SHOCK_MERCHANTS[rng.nextInt(SHOCK_MERCHANTS.size)]
                val amount = SHOCK_MEDIAN * exp(SHOCK_LOG_SIGMA * rng.nextGaussian())
                add(1 + rng.nextInt(ym.lengthOfMonth()), merchant, amount, Direction.DEBIT, category, p.bank.id)
            }

            if (scenario == Scenario.UNEXPECTED_EXPENSE && m == EMERGENCY_MONTH) {
                add(12, "Apollo Hospital", EMERGENCY_AMOUNT, Direction.DEBIT, "HEALTH", p.bank.id)
            }

            // History only: move surplus to the person's own deposit
            if (m < TEST.first && p.monthlySweep > 0) {
                add(28, "Own Fixed Deposit", p.monthlySweep, Direction.DEBIT, "TRANSFER", p.bank.id, isTransfer = true)
            }
        }

        val statements = cardCycle(p, txns)
        val all = txns.sortedWith(compareBy<Txn> { it.date }.thenBy { it.id })

        // Back-solve the opening balance so the bank balance on the demo date matches the profile.
        val historyNet = all.filter { it.accountId == p.bank.id && it.date < DEMO_DATE }.sumOf(::signed)
        val opening = p.bank.balanceAtDemo - historyNet

        return SyntheticDataset(
            persona = persona,
            scenario = scenario,
            demoDate = DEMO_DATE,
            windows = listOf(
                SeedWindow("train", TRAIN, seeds.train),
                SeedWindow("validation", VALIDATION, seeds.validation),
                SeedWindow("test", TEST, seeds.test),
            ),
            transactions = all,
            cardStatements = statements,
            openingBalances = mapOf(p.bank.id to opening),
            accounts = accounts(p, opening + historyNet),
            liabilities = liabilities(p, all, statements),
            investments = listOfNotNull(p.sip?.let {
                Investment(it.id, it.platform, it.amount, it.debitDay, "MUTUAL_FUND", p.bank.id, confidence = 1.0)
            }),
            declared = declared(p, scenario),
            assumptions = assumptions(p, scenario, seeds, shocks),
        )
    }

    /** Signed effect on the account: credit positive, debit negative. */
    fun signed(t: Txn): Double = if (t.direction == Direction.CREDIT) t.amount else -t.amount

    /** Statement on [CardProfile.statementDay] covers card spend since the previous statement; paid in full on the due date. */
    private fun cardCycle(p: PersonaProfile, txns: MutableList<Txn>): List<CardStatement> {
        val cardSpend = txns.filter { it.accountId == p.card.id && it.direction == Direction.DEBIT }
        val statements = mutableListOf<CardStatement>()
        var from = calendarMonth(1).atDay(1)
        for (m in 1..TOTAL_MONTHS) {
            val ym = calendarMonth(m)
            val statementDate = ym.atDay(p.card.statementDay)
            val due = cardSpend.filter { it.date >= from && it.date < statementDate }.sumOf { it.amount }
            from = statementDate
            if (due <= 0) continue
            val dueDate = if (p.card.dueDay > p.card.statementDay) ym.atDay(p.card.dueDay) else ym.plusMonths(1).atDay(p.card.dueDay)
            val minDue = p.card.minDueShare?.let { round(due * it) }
            statements += CardStatement(p.card.id, statementDate, round(due), minDue, dueDate)
            if (YearMonth.from(dueDate) > calendarMonth(TOTAL_MONTHS)) continue
            val id = "syn-${p.persona.name}-$m-cardbill"
            txns += Txn("$id-bank", dueDate, round(due), Direction.DEBIT, "CARD_BILL", hash(p.card.issuer), p.bank.id, true, null, Source.SYNTHETIC)
            txns += Txn("$id-card", dueDate, round(due), Direction.CREDIT, "CARD_BILL", hash(p.bank.institution), p.card.id, true, null, Source.SYNTHETIC)
        }
        return statements
    }

    private fun accounts(p: PersonaProfile, bankBalance: Double): List<Account> = buildList {
        add(Account(p.bank.id, AccountType.SAVINGS, p.bank.institution, p.bank.last4, bankBalance, DEMO_DATE))
        add(Account(p.card.id, AccountType.CARD, p.card.issuer, p.card.last4, null, null))
        p.loans.forEach { add(Account(it.id, AccountType.LOAN, it.lender, null, null, null)) }
        p.sip?.let { add(Account(it.id, AccountType.INVESTMENT, it.platform, null, null, null)) }
    }

    private fun liabilities(p: PersonaProfile, txns: List<Txn>, statements: List<CardStatement>): List<Liability> {
        val loans = p.loans.map { l ->
            Liability(
                id = l.id, kind = l.kind,
                outstanding = round(outstandingAtDemo(l)),
                annualRate = if (l.rateKnown) l.annualRate else null,
                emi = l.emi, monthsLeft = monthsLeftAtDemo(l), dueDay = l.dueDay,
                creditLimit = null, minDue = null, totalDue = null, unbilled = null, accountId = l.id,
            )
        }
        val last = statements.lastOrNull { it.statementDate < DEMO_DATE }
        val unbilled = txns.filter {
            it.accountId == p.card.id && it.direction == Direction.DEBIT && it.date < DEMO_DATE &&
                (last == null || it.date >= last.statementDate)
        }.sumOf { it.amount }
        val unpaid = if (last != null && last.dueDate >= DEMO_DATE) last.totalDue else 0.0
        val card = Liability(
            id = p.card.id, kind = "CARD", outstanding = round(unpaid + unbilled), annualRate = null,
            emi = null, monthsLeft = null, dueDay = p.card.dueDay, creditLimit = p.card.creditLimit,
            minDue = if (p.card.minDueShare != null && unpaid > 0) last?.minDue else null,
            /** Null when the latest statement is already paid; then dues are the unbilled amount. */
            totalDue = if (unpaid > 0) unpaid else null,
            unbilled = round(unbilled), accountId = p.card.id,
        )
        return loans + card
    }

    private fun declared(p: PersonaProfile, scenario: Scenario) = DeclaredInputs(
        incomeType = if (p.income.kind == IncomeKind.AR1 || scenario == Scenario.HIGH_UNCERTAINTY) "VARIABLE" else "FIXED",
        typicalIncome = if (scenario == Scenario.INFEASIBLE_TARGET) REDUCED_INCOME else p.income.base,
        minReserve = if (scenario == Scenario.INFEASIBLE_TARGET) INFEASIBLE_RESERVE else p.defaultReserve,
        goal = if (scenario == Scenario.INFEASIBLE_TARGET) Goal("Save ₹1,00,000 in 6 months", INFEASIBLE_GOAL, TEST.count()) else null,
        ageRange = p.persona.ageRange,
    )

    private fun assumptions(p: PersonaProfile, scenario: Scenario, seeds: Seeds, shocks: Boolean) = listOf(
        "All data is synthetic. Persona ${p.persona.name}, scenario ${scenario.label}.",
        "Months 1-30 = ${calendarMonth(1)} to ${calendarMonth(TOTAL_MONTHS)}; demo date $DEMO_DATE (month 25).",
        "Windows: train ${TRAIN.first}-${TRAIN.last} (seed ${seeds.train}), validation ${VALIDATION.first}-${VALIDATION.last} (seed ${seeds.validation}), test ${TEST.first}-${TEST.last} (seed ${seeds.test}).",
        "Income: ${p.income.kind} around ₹${p.income.base.roundToLong()}" +
            (if (p.income.kind == IncomeKind.AR1) ", log-sigma ${p.income.logSigma}, phi ${p.income.phi}, slip chance ${p.income.slipChance}" else "") +
            (if (p.income.bonusAmount > 0) ", bonus ₹${p.income.bonusAmount.roundToLong()} in months ${p.income.bonusCalendarMonths}" else "") + ".",
        "Recurring dates jitter by ±$DATE_JITTER_DAYS days; variable spend lognormal with +${(FESTIVAL_UPLIFT * 100).roundToLong()}% in months $FESTIVAL_MONTHS.",
        if (shocks) "One-off shocks: Poisson rate $SHOCK_RATE_PER_MONTH per month, median ₹${SHOCK_MEDIAN.roundToLong()}." else "One-off shocks disabled.",
        "History months transfer ₹${p.monthlySweep.roundToLong()} a month to the person's own deposit (isTransfer).",
        "Card statement on day ${p.card.statementDay}, due day ${p.card.dueDay}, paid in full on the due date.",
        "Opening bank balance back-solved so the balance on the demo date is ₹${p.bank.balanceAtDemo.roundToLong()}.",
        "Default minimum reserve ₹${p.defaultReserve.roundToLong()}.",
        when (scenario) {
            Scenario.STABLE_INCOME -> "No overlay."
            Scenario.INCOME_REDUCTION -> "Income ₹${REDUCED_INCOME.roundToLong()} from month $INCOME_DROP_MONTH."
            Scenario.UNEXPECTED_EXPENSE -> "₹${EMERGENCY_AMOUNT.roundToLong()} medical bill in month $EMERGENCY_MONTH."
            Scenario.INFEASIBLE_TARGET -> "Income ₹${REDUCED_INCOME.roundToLong()} in the test window, reserve ₹${INFEASIBLE_RESERVE.roundToLong()}, goal ₹${INFEASIBLE_GOAL.roundToLong()}."
            Scenario.HIGH_UNCERTAINTY -> "Test-window income uniform within ±${(HIGH_UNCERTAINTY_VOLATILITY * 100).roundToLong()}% of base."
        },
    )

    // Loan maths: months are counted so the EMI of month 25 is the first one still to pay at the demo date.
    private fun monthlyRate(l: LoanProfile) = l.annualRate / 12

    fun monthsLeftAtDemo(l: LoanProfile): Int = l.monthsLeftAtDemo ?: run {
        val p = l.outstandingAtDemo!!
        val r = monthlyRate(l)
        if (r == 0.0) ceil(p / l.emi).toInt() else ceil(-ln(1 - p * r / l.emi) / ln(1 + r)).toInt()
    }

    fun outstandingAtDemo(l: LoanProfile): Double = l.outstandingAtDemo ?: run {
        val n = l.monthsLeftAtDemo!!
        val r = monthlyRate(l)
        if (r == 0.0) l.emi * n else l.emi * (1 - (1 + r).pow(-n)) / r
    }

    private fun lastEmiMonth(l: LoanProfile) = 24 + monthsLeftAtDemo(l)

    private fun poisson(rng: Random, lambda: Double): Int {
        val limit = exp(-lambda)
        var k = 0
        var prod = rng.nextDouble()
        while (prod > limit) { k++; prod *= rng.nextDouble() }
        return k
    }

    // shortcut: SHA-256 of merchant, not HMAC-Keystore; replace when real SMS ingest lands
    private fun hash(counterparty: String): String =
        MessageDigest.getInstance("SHA-256").digest(counterparty.trim().lowercase().toByteArray())
            .joinToString("") { "%02x".format(it) }.take(16)

    private fun round(x: Double) = x.roundToLong().toDouble()
}
