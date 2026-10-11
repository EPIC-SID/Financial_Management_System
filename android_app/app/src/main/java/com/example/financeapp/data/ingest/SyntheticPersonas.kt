package com.example.financeapp.data.ingest

/** Synthetic personas from 06-backend.md sections 12.2 and 12.2a. All values are synthetic assumptions. */
enum class Persona(val ageRange: String) { A("18-24"), B("25-34"), C("45-54"), B_SPARSE("25-34") }

/** Demo scenarios from 06-backend.md section 12.3; defined on persona B. */
enum class Scenario(val label: String) {
    STABLE_INCOME("Stable"),
    INCOME_REDUCTION("Income -30%"),
    UNEXPECTED_EXPENSE("Emergency ₹25k"),
    INFEASIBLE_TARGET("Infeasible"),
    HIGH_UNCERTAINTY("High Uncertainty"),
}

enum class IncomeKind { FIXED, AR1 }

data class IncomeProfile(
    val employer: String,
    val kind: IncomeKind,
    val base: Double,
    val payDay: Int,
    val bonusCalendarMonths: Set<Int> = emptySet(),
    val bonusAmount: Double = 0.0,
    /** AR(1) on log income: stationary sigma and persistence. */
    val logSigma: Double = 0.0,
    val phi: Double = 0.0,
    /** Chance a month's payment lands in the next month. */
    val slipChance: Double = 0.0,
)

data class BankProfile(val id: String, val institution: String, val last4: String, val balanceAtDemo: Double)

data class CardProfile(
    val id: String,
    val issuer: String,
    val last4: String,
    val creditLimit: Double?,
    val statementDay: Int,
    val dueDay: Int,
    /** Null when the minimum due is not known (B-sparse). */
    val minDueShare: Double?,
)

/** Exactly one of [outstandingAtDemo] / [monthsLeftAtDemo] is given; the seeder derives the other. */
data class LoanProfile(
    val id: String,
    val kind: String,
    val lender: String,
    val emi: Double,
    val dueDay: Int,
    val annualRate: Double,
    val rateKnown: Boolean = true,
    val outstandingAtDemo: Double? = null,
    val monthsLeftAtDemo: Int? = null,
    val firstMonth: Int = 1,
)

data class SipProfile(val id: String, val platform: String, val amount: Double, val debitDay: Int, val firstMonth: Int = 1)

data class BillProfile(
    val merchant: String,
    val category: String,
    val amount: Double,
    val day: Int,
    val amountCv: Double = 0.0,
    /** Null = every month; otherwise calendar months (1-12) for annual or semi-annual bills. */
    val calendarMonths: Set<Int>? = null,
)

data class VariableCategory(
    val category: String,
    val merchants: List<String>,
    val monthlyMean: Double,
    val logSigma: Double,
    val txnsPerMonth: Int,
    val onCard: Boolean,
)

data class PersonaProfile(
    val persona: Persona,
    val income: IncomeProfile,
    val bank: BankProfile,
    val card: CardProfile,
    val loans: List<LoanProfile>,
    val sip: SipProfile?,
    val bills: List<BillProfile>,
    val variable: List<VariableCategory>,
    /** Fixed monthly transfer to the person's own deposit during history, so balances stay realistic. */
    val monthlySweep: Double,
    val defaultReserve: Double,
)

object SyntheticPersonas {

    private val B = PersonaProfile(
        persona = Persona.B,
        income = IncomeProfile("Acme Tech Salary", IncomeKind.FIXED, 85_000.0, payDay = 1,
            bonusCalendarMonths = setOf(3), bonusAmount = 15_000.0),
        bank = BankProfile("B-bank", "HDFC Bank", "4091", balanceAtDemo = 45_200.0),
        card = CardProfile("B-card", "SBI SimplyCLICK", "8821", creditLimit = 1_50_000.0,
            statementDay = 5, dueDay = 25, minDueShare = 0.05),
        loans = listOf(
            LoanProfile("B-home", "HOME_LOAN", "HDFC Home Loan", 18_500.0, dueDay = 10, annualRate = 0.085,
                outstandingAtDemo = 18_50_000.0),
            LoanProfile("B-car", "VEHICLE_LOAN", "ICICI Car Loan", 8_200.0, dueDay = 15, annualRate = 0.095,
                monthsLeftAtDemo = 24),
        ),
        sip = SipProfile("B-sip", "Groww Mutual Fund", 10_000.0, debitDay = 5),
        bills = listOf(
            BillProfile("Landlord Rent", "RENT", 22_000.0, day = 1),
            BillProfile("Electricity & Utilities", "UTILITIES", 2_500.0, day = 20, amountCv = 0.15),
        ),
        variable = listOf(
            VariableCategory("FOOD", listOf("Swiggy", "Zomato"), 4_000.0, 0.25, 6, onCard = true),
            VariableCategory("GROCERIES", listOf("BigBasket", "DMart"), 3_500.0, 0.20, 3, onCard = true),
            VariableCategory("SHOPPING", listOf("Amazon", "Myntra"), 4_500.0, 0.35, 2, onCard = true),
            VariableCategory("TRAVEL", listOf("Uber", "IRCTC"), 2_500.0, 0.30, 4, onCard = true),
            VariableCategory("ENTERTAINMENT", listOf("BookMyShow", "Netflix"), 1_500.0, 0.25, 2, onCard = true),
            VariableCategory("FOOD", listOf("Local Kirana", "Chai Point"), 600.0, 0.30, 4, onCard = false),
        ),
        monthlySweep = 6_000.0,
        defaultReserve = 25_000.0,
    )

    private val A = PersonaProfile(
        persona = Persona.A,
        income = IncomeProfile("Freelance Clients", IncomeKind.AR1, 30_000.0, payDay = 7,
            logSigma = 0.2, phi = 0.3, slipChance = 0.2),
        bank = BankProfile("A-bank", "Kotak Bank", "1187", balanceAtDemo = 14_000.0),
        card = CardProfile("A-card", "Axis Neo", "5530", creditLimit = 20_000.0,
            statementDay = 12, dueDay = 2, minDueShare = 0.05),
        loans = listOf(
            LoanProfile("A-phone", "GADGET_EMI", "Bajaj Phone EMI", 2_300.0, dueDay = 7, annualRate = 0.0,
                monthsLeftAtDemo = 9, firstMonth = 16),
        ),
        sip = null,
        bills = listOf(
            BillProfile("PG Rent", "RENT", 8_000.0, day = 5),
            BillProfile("Jio Recharge", "UTILITIES", 600.0, day = 18, amountCv = 0.1),
        ),
        variable = listOf(
            VariableCategory("FOOD", listOf("Swiggy", "Canteen"), 4_500.0, 0.30, 8, onCard = false),
            VariableCategory("GROCERIES", listOf("Local Kirana"), 3_000.0, 0.25, 4, onCard = false),
            VariableCategory("TRAVEL", listOf("Rapido", "Metro Card"), 1_800.0, 0.30, 6, onCard = false),
            VariableCategory("SHOPPING", listOf("Amazon"), 2_500.0, 0.40, 1, onCard = true),
            VariableCategory("ENTERTAINMENT", listOf("Spotify", "BookMyShow"), 800.0, 0.30, 2, onCard = true),
        ),
        monthlySweep = 1_500.0,
        defaultReserve = 8_000.0,
    )

    private val C = PersonaProfile(
        persona = Persona.C,
        income = IncomeProfile("Mega Corp Salary", IncomeKind.FIXED, 1_40_000.0, payDay = 1),
        bank = BankProfile("C-bank", "SBI", "2290", balanceAtDemo = 2_10_000.0),
        card = CardProfile("C-card", "HDFC Regalia", "7714", creditLimit = 3_00_000.0,
            statementDay = 8, dueDay = 28, minDueShare = 0.05),
        loans = listOf(
            LoanProfile("C-home", "HOME_LOAN", "SBI Home Loan", 45_000.0, dueDay = 10, annualRate = 0.0875,
                outstandingAtDemo = 42_00_000.0),
        ),
        sip = SipProfile("C-sip", "Zerodha Coin", 15_000.0, debitDay = 5),
        bills = listOf(
            BillProfile("Parents Medical", "HEALTH", 5_000.0, day = 8, amountCv = 0.1),
            BillProfile("Electricity & Utilities", "UTILITIES", 4_000.0, day = 20, amountCv = 0.15),
            BillProfile("School Fees", "EDUCATION", 45_000.0, day = 15, calendarMonths = setOf(4, 10)),
            BillProfile("LIC Premium", "INSURANCE", 28_000.0, day = 20, calendarMonths = setOf(1)),
        ),
        variable = listOf(
            VariableCategory("GROCERIES", listOf("BigBasket", "Nature's Basket"), 9_000.0, 0.20, 4, onCard = true),
            VariableCategory("FOOD", listOf("Swiggy", "Restaurants"), 7_000.0, 0.25, 6, onCard = true),
            VariableCategory("SHOPPING", listOf("Amazon", "Croma"), 8_000.0, 0.40, 2, onCard = true),
            VariableCategory("TRAVEL", listOf("Indian Oil", "MakeMyTrip"), 6_000.0, 0.30, 4, onCard = true),
            VariableCategory("ENTERTAINMENT", listOf("PVR", "Hotstar"), 2_000.0, 0.25, 2, onCard = false),
            VariableCategory("FOOD", listOf("Local Kirana"), 3_000.0, 0.25, 4, onCard = false),
        ),
        monthlySweep = 20_000.0,
        defaultReserve = 50_000.0,
    )

    /** Same as B but missing the car loan rate, card limit and minimum due, and only two months of SIP history. */
    private val B_SPARSE = B.copy(
        persona = Persona.B_SPARSE,
        bank = B.bank.copy(id = "BS-bank"),
        card = B.card.copy(id = "BS-card", creditLimit = null, minDueShare = null),
        loans = listOf(
            B.loans[0].copy(id = "BS-home"),
            B.loans[1].copy(id = "BS-car", rateKnown = false),
        ),
        sip = B.sip!!.copy(id = "BS-sip", firstMonth = 23),
    )

    fun profile(persona: Persona): PersonaProfile = when (persona) {
        Persona.A -> A
        Persona.B -> B
        Persona.C -> C
        Persona.B_SPARSE -> B_SPARSE
    }
}
