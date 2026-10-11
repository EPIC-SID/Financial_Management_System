# New Changes & Additions: FIN-02 Consolidated Financial Accounts Integration
## Tackling Bank Accounts, Credit Cards, Loans, Investments & Payment Histories

> **Reference:** Core Problem Statement FIN-02 ("Individuals often have bank accounts, credit cards, loans, investments, and payment histories but no unified understanding of their overall financial health.")  
> **Purpose:** Documenting the multi-account integration layer to be layered onto `HACKATHON_PLAN_Main.md`.

---

## 1. Architectural Mapping Across the 5 Financial Streams

| Financial Stream | Ingestion & Extraction Source | Role in System | Key Data Fields |
|---|---|---|---|
| **1. Bank Accounts** | SMS alerts: `A/c *4091 credited/debited` | Determines **Current Liquid Runway** & immediate spending capacity | `institution`, `accountNumberMasked`, `liquidBalance`, `lastSyncDate` |
| **2. Credit Cards** | SMS alerts: `Spent on Card *8821`, Bill generated alerts | Tracks **Short-Term Revolving Liabilities** & upcoming payment cliffs | `cardName`, `unbilledBalance`, `creditLimit`, `paymentDueDate`, `minimumDue` |
| **3. Loans & EMIs** | SMS alerts: `Loan A/c EMI Rs. X debited` | Tracks **Fixed Term Debt Obligations** & Debt-to-Income (DTI) ratio | `loanName`, `totalPrincipalOutstanding`, `monthlyEmi`, `interestRate`, `dueDayOfMonth` |
| **4. Investments** | SMS alerts: `SIP installment Rs. X debited to Groww/Zerodha` | Measures **Savings Behaviour & Wealth Accumulation Rate** | `portfolioName`, `monthlySipAmount`, `assetType` (Mutual Fund / Stocks / PPF), `debitDay` |
| **5. Payment Histories** | Full historical SMS transaction stream (`Swiggy, Uber, Jio, Kirana`) | Feeds **Discretionary Spend Variance** into 500-run Monte Carlo engine | `merchant`, `amount`, `category`, `date`, `isDiscretionary` |

---

## 2. Phase-by-Phase Integration Specs

### Addition to Phase 1A (Domain Data Models)
File: `app/src/main/java/com/example/financeapp/domain/model/PlanningModels.kt`

```kotlin
enum class AccountType {
    BANK_ACCOUNT,
    CREDIT_CARD,
    LOAN,
    INVESTMENT
}

data class FinancialAccount(
    val id: String,
    val name: String,
    val type: AccountType,
    val balanceOrOutstanding: Double,
    val monthlyObligation: Double,
    val institution: String,
    val accountNumberMasked: String
)
```

---

### Addition to Phase 1B (Synthetic Seeder Multi-Account Profiles)
File: `app/src/main/java/com/example/financeapp/data/SyntheticDataSeeder.kt`

Populate realistic multi-account Indian financial histories:
1. **Bank Account:** HDFC Salary & Savings A/c (Liquid Cash: ₹45,200).
2. **Credit Card:** SBI SimplyCLICK Card (Unbilled Dues: ₹14,300, Statement Due on 25th of month).
3. **Loans & EMIs:**
   - HDFC Home Loan (Monthly EMI: ₹18,500, Principal: ₹18,50,000, 8.5% interest).
   - ICICI Car Loan (Monthly EMI: ₹8,200, Remaining Tenure: 24 months).
4. **Investments:** Zerodha / Groww Mutual Fund SIP (Monthly SIP: ₹10,000 auto-debited on 5th).
5. **Payment Histories:** 6 months of historical debit/credit transactions with realistic noise across Swiggy, Zomato, Rent, Uber, Groceries, and Utilities.

---

### Addition to Phase 2C (Unified Reactive Financial Snapshot)
File: `app/src/main/java/com/example/financeapp/domain/model/FinancialSnapshot.kt`

```kotlin
data class FinancialSnapshot(
    val liquidBankBalance: Double,
    val creditCardDues: Double,
    val totalMonthlyDebtEmis: Double,
    val monthlyInvestments: Double,
    val averageMonthlyDiscretionary: Double,
    val accounts: List<FinancialAccount>,
    val recentTransactions: List<TransactionItem>,
    val healthScore: Int,
    val minCashReserve: Double = 25000.0
)
```

---

### Addition to Phase 3A (UI Consolidated Accounts Bar)
File: `app/src/main/java/com/example/financeapp/ui/screens/FinancialHealthScreen.kt`

Add a dedicated **Consolidated Accounts Row** directly below the Speedometer gauge:
* 🏦 **Liquid Cash Pill**: ₹45,200 (HDFC Savings)
* 💳 **Credit Card Pill**: ₹14,300 (SBI SimplyCLICK)
* 🏠 **Loan EMIs Pill**: ₹26,700/mo (Home + Car)
* 📈 **SIP Investments Pill**: ₹10,000/mo (Groww MF)

---

### Addition to Phase 3C (Gemini Copilot Context Injection)
File: `app/src/main/java/com/example/financeapp/data/GeminiCopilotEngine.kt`

When formulating answers, Gemini Copilot explicitly references the 5 streams in the **[FACT]** lane:
* *"Observed liquid bank balance: ₹45,200 | Credit card dues: ₹14,300 | Active loan obligations: ₹26,700/mo | Monthly SIP: ₹10,000."*
