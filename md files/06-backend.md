# 06 - Backend (Engines, Data, API, Security)

**Related docs:** [01 Product design](01-product-design.md) · [04 Pain points](04-pain-points.md) · [08 Tech stack](08-tech-stack.md) · [09 Overall plan](09-overall-plan.md)

"Backend" here means everything that is not UI: the on-device analytical engines (pure Kotlin), the local database, the ingestion pipeline, and the thin cloud services (Supabase and a FastAPI proxy for Gemini). The cloud side is deliberately small.

---

## 1. Architecture overview

```
┌────────────────────────── Android app (Kotlin) ──────────────────────────┐
│  UI (Compose)  ->  ViewModels  ->  FinancialHealthRepository (StateFlow)  │
│                                          │                                │
│   Ingestion (SMS / CSV / Manual / Synthetic)                              │
│        │                                                                  │
│        v                                                                  │
│   Room (encrypted)  ->  Ledger  ->  Detectors  ->  FinancialSnapshot      │
│                                          │                                │
│   Pure-Kotlin engines (domain/engine, no android.* imports):              │
│     ForecastEngine · PlannerEngine · RiskSimulator · ImpactSimulator      │
│     ConflictResolver · Evaluator · EvidenceExporter                       │
│                                          │                                │
│   Copilot orchestrator (domain/ai): intent -> engine -> narrate -> verify │
└───────────────────────────┬───────────────────────────────────────────────┘
                            │ HTTPS + Supabase JWT (aggregates only)
              ┌─────────────┴──────────────┐
              │   FastAPI proxy            │──> Gemini API
              │   (keys, cache, limits)    │
              └─────────────┬──────────────┘
                            │
                      Supabase (Auth, profile, consents, optional synced aggregates)
```

**Design decisions**
1. **Engines run on the device.** Raw data never has to leave the phone, the demo works offline, and `./gradlew test` verifies the math in seconds without an emulator.
2. **The language model never calculates.** It maps a question to a structured call and narrates engine results. A verifier checks the narration.
3. **Synthetic data first.** Required for held-out evaluation and a reproducible demo; the real SMS path uses the same schema.
4. **Stateless proxy.** The FastAPI service stores nothing raw and exists to protect the Gemini key and rate limits.

## 2. Package layout

```
com.example.financeapp
├── domain
│   ├── model        PlanningModels.kt, ForecastModels.kt, AccountModels.kt, FinancialSnapshot.kt, CopilotModels.kt
│   ├── engine       ForecastEngine.kt, BaselineForecasts.kt, PlannerEngine.kt, ConflictResolver.kt,
│   │                RiskSimulator.kt, ImpactSimulator.kt, Evaluator.kt, EvidenceExporter.kt,
│   │                HealthScore.kt, VerdictEngine.kt, ConsolidatedView.kt
│   ├── detect       RecurringDetector.kt, IncomeDetector.kt, LiabilityExtractor.kt, InvestmentDetector.kt
│   └── ai           CopilotOrchestrator.kt, IntentRouter.kt, NumberVerifier.kt, LocalAdvisor.kt
├── data
│   ├── local        Room entities, DAOs, AppDatabase.kt
│   ├── ingest       SmsReader.kt, SmsParser.kt, CsvImporter.kt, SyntheticDataSeeder.kt, OtpFilter.kt
│   ├── remote       BackendClient.kt (proxy), SupabaseClient.kt
│   └── FinancialHealthRepository.kt
└── ui                (see 02-pages.md and 05-frontend-ui-style.md)
```

Rule: `domain/engine`, `domain/detect` and `domain/model` contain no `android.*` imports.

## 3. Data model

### 3.1 Domain classes (plain Kotlin)

```kotlin
enum class Direction { DEBIT, CREDIT }
enum class Source { SMS, CSV, MANUAL, SYNTHETIC }
enum class PeriodType { MONTHLY, WEEKLY }
enum class AccountType { SAVINGS, CARD, LOAN, INVESTMENT, WALLET }

data class Txn(
    val id: String,                // smsId, csv row hash, or uuid
    val date: java.time.LocalDate,
    val amount: Double,            // always positive; sign comes from direction
    val direction: Direction,
    val category: String,
    val counterpartyHash: String,  // salted hash; never the raw VPA or name
    val accountId: String?,
    val isTransfer: Boolean,       // own-account transfer or card-bill payment
    val recurringId: String?,
    val source: Source
)

data class Liability(
    val id: String, val kind: String,        // HOME_LOAN, VEHICLE_LOAN, CARD, GADGET_EMI ...
    val outstanding: Double?, val annualRate: Double?,
    val emi: Double?, val monthsLeft: Int?, val dueDay: Int?,
    val creditLimit: Double?, val minDue: Double?, val totalDue: Double?,
    val unbilled: Double?, val accountId: String?   // card: spend since the last statement
)

data class Account(
    val id: String, val type: AccountType,
    val institution: String,        // shown on device only, never sent to the proxy or Gemini
    val last4: String?,             // shown as "•• 4091" on device only
    val latestBalance: Double?, val balanceAsOf: java.time.LocalDate?
)

data class Investment(
    val id: String, val platform: String,   // on device only
    val monthlyAmount: Double, val debitDay: Int,
    val assetType: String,                  // MUTUAL_FUND, STOCKS, PPF
    val accountId: String?, val confidence: Double
)

data class RecurringItem(
    val id: String, val label: String, val amount: Double,
    val intervalDays: Int, val nextDue: java.time.LocalDate,
    val confidence: Double, val kind: String   // RENT, EMI, CARD_BILL, SUBSCRIPTION, UTILITY, INSURANCE, OTHER
)

data class DeclaredInputs(
    val incomeType: String,            // FIXED, VARIABLE, MIXED
    val typicalIncome: Double?,
    val minReserve: Double,            // hard planner constraint
    val goal: Goal?,
    val ageRange: String?              // tone only
)
data class Goal(val name: String, val targetAmount: Double, val byPeriodIndex: Int)

data class FinancialSnapshot(
    val asOf: java.time.LocalDate,
    val cash: Double,
    val accounts: List<Account>,
    val liabilities: List<Liability>,
    val investments: List<Investment>,
    val recurring: List<RecurringItem>,
    val history: List<MonthlyAggregate>,   // income, essentials, variable, per month
    val declared: DeclaredInputs,
    val completeness: Double,              // 0..1, see section 9
    val missing: List<String>              // e.g. ["loan balance"]
)

/** The five streams in one object. Built by ConsolidatedViewBuilder from a FinancialSnapshot; derived, never stored. */
data class ConsolidatedView(
    val liquidCash: Double,              // stream 1: sum of latest SAVINGS balances
    val cardDues: Double,                // stream 2: sum of total due, or unbilled when no statement yet
    val monthlyEmis: Double,             // stream 3: sum of loan EMIs
    val monthlySip: Double,              // stream 4: sum of detected or declared SIPs
    val avgDiscretionary: Double,        // stream 5: trailing 6-month mean of discretionary spend
    val staleAccountIds: List<String>    // balances older than 7 days
)

data class MonthlyAggregate(
    val month: java.time.YearMonth, val income: Double,
    val essentials: Double, val debtService: Double, val discretionary: Double,
    val investments: Double
)

data class ProbabilisticForecast(
    val periodIndex: Int, val metric: String,        // "income", "expenses", "balance"
    val p10: Double, val p50: Double, val p90: Double,
    val method: String, val horizon: String, val intervalLevel: Double   // 0.80
)

data class PlanningPeriod(
    val index: Int, val label: String,
    val opening: Double, val income: Double, val essentials: Double,
    val debtService: Double, val discretionary: Double, val sip: Double,
    val prepay: Double, val saved: Double, val ending: Double,
    val reserve: Double, val constraintOk: Boolean
)

data class ActionSequence(
    val id: String, val name: String,
    val periods: List<PlanningPeriod>,
    val terminalNetPosition: Double,       // cash minus outstanding debt at horizon end
    val investedTotal: Double,            // SIP contributions over the horizon, reported separately
    val interestPaid: Double, val interestSavedVsStatusQuo: Double,
    val shortfallRisk: Double,             // P(min balance < reserve) from Monte Carlo
    val tradeOff: String
)

data class GoalConflict(
    val goal: String, val shortfall: Double,
    val violatedConstraint: String, val firstViolatedPeriod: Int,
    val relaxations: List<Relaxation>
)
data class Relaxation(val kind: String, val description: String, val delta: Double, val riskAfter: Double)
```

### 3.2 Room tables (on device, encrypted)

| Table | Key columns |
|---|---|
| `transactions` | `id` (PK), `date`, `amount`, `direction`, `category`, `counterpartyHash`, `accountId`, `isTransfer`, `recurringId`, `source`, `isCategorizedByAi`, `description` (masked), `createdAt` |
| `accounts` | `id`, `type` (SAVINGS, CARD, LOAN, INVESTMENT, WALLET), `institution`, `last4`, `senderId`, `latestBalance`, `balanceAsOf` |
| `liabilities` | fields of `Liability` |
| `investments` | fields of `Investment`, `confirmedByUser`, `dismissed` |
| `recurring_items` | fields of `RecurringItem`, `confirmedByUser`, `dismissed` |
| `declared_inputs` | single row |
| `plan_runs` | `id`, `createdAt`, `scenario`, `inputsHash`, `resultJson` |
| `forecast_runs` | `id`, `createdAt`, `method`, `seed`, `resultJson` |
| `transmission_log` | `time`, `endpoint`, `dataType`, `bytes` (feeds the privacy center) |

Migration note: existing `TransactionEntity` (smsId primary key) gains the new columns through a Room migration. During the hackathon, destructive fallback is acceptable on debug builds only.

### 3.3 Supabase (cloud)

| Table | Purpose |
|---|---|
| `user_profiles` (existing) | name, email, optional phone, optional age range |
| `consents` | `user_id`, `sms_access`, `cloud_sync`, `ai_assistance`, `updated_at` |
| `synced_aggregates` (optional, opt-in) | monthly aggregates only |

Row-level security on every table: a user can read and write only their own rows. The service-role key lives only on the server.

## 4. Ingestion pipeline

```
SMS inbox / broadcast ─┐
CSV upload ────────────┼─> normalise -> filter -> parse -> hash counterparty -> categorise -> ledger rules -> Room
Manual entry ──────────┤
Synthetic seeder ──────┘
```

1. **Allow-list senders.** Process only known bank, card and wallet sender IDs (for example `HDFCBK`, `SBIINB`, `ICICIB`, `AXISBK`, `PAYTMB`, `PHNEPE`, `GPAY`). Ignore everything else.
2. **OTP and secrets filter.** Drop any message matching OTP patterns ("OTP", "one time password", "do not share", "valid for N minutes") before parsing. Never store or log them.
3. **Parse.** Extract amount, direction, date, counterparty text, account last 4, available balance (when shown). Card messages: total due, minimum due, due date, limit, outstanding. Loan messages: EMI amount, due date, outstanding. Card spend alerts and "statement generated" alerts. Investment debits: SIP amount, debit day and platform or fund-house counterparty (matched against an allow-list; also found by the recurring detector).
4. **Hash counterparty.** `counterpartyHash = HMAC-SHA256(deviceSecret, normalisedCounterparty)` truncated to 16 hex characters. `deviceSecret` is generated once and held in the Android Keystore. This replaces the old `***@***` masking: hashes are private on display yet stable for matching recurring and peer-to-peer payments.
5. **Categorise.** Existing four layers: VPA resolver, local brand regex, Google Places, Gemini fallback via the proxy. Only the merchant string and category cross the network, never the raw message.
6. **Ledger rules** (section 5).
7. **Dedupe.** Deterministic id per source (`smsId`; CSV row hash of date, amount, narration; uuid for manual). Conflict strategy: ignore.
8. **CSV importer.** Column mapping dialog (date, narration, debit, credit, balance); parsed fully on device.
9. **Synthetic seeder.** Section 12.

## 5. Ledger rules

| Rule | Behaviour |
|---|---|
| Own-account transfers | Mark `isTransfer = true`; excluded from income and expense |
| Credit-card bill payment | Mark as transfer to the card liability. Two views: the **spending view** counts a purchase when the card is used; the **cash view** reduces bank cash once, when the bill is paid on its due date. Card-use transactions never reduce bank cash, and the bill is never counted as spending |
| Refunds and reversals | Net against the original transaction when matched by counterparty and amount within 30 days; otherwise a credit |
| Cashback | Credit categorised "Rewards"; excluded from income |
| Peer-to-peer | Tag as rent, split, loan or other when the user confirms; repeated equal payments to one hash become rent candidates |
| Balance anchor | Latest balance from messages sets `cash`; if absent, `cash` comes from declared inputs, and completeness drops |
| Multiple bank accounts | `cash` is the sum of the latest balances across SAVINGS accounts; a balance older than 7 days is flagged stale; transfers between own accounts are excluded |
| SIP and investment debits | Category "Investments"; not spending and not debt service; counted as committed savings in the cash view; excluded from DTI and from essentials; reported as `investments` in `MonthlyAggregate` |

## 6. Recurring, income and liability detection

### 6.1 Recurring detector
For each `counterpartyHash` (or category for utilities) with at least 3 debits:
1. Sort by date; compute gaps in days.
2. Candidate interval = the median gap, snapped to {7, 14, 30, 90, 365} when within ±3 days (30-day snap allows month-end drift).
3. Amount check: coefficient of variation of amounts ≤ 0.15 for fixed items; utilities and cards allowed up to 0.5 and tagged variable.
4. `confidence = 0.5·regularity + 0.3·amountStability + 0.2·min(1, occurrences/6)`, where regularity = share of gaps within ±3 days of the interval.
5. `nextDue` = last date + interval (use the day-of-month for monthly items).
6. Keep items with confidence ≥ 0.6; show lower ones as "possible" for the user to confirm.
7. Label by category and keywords (EMI, rent, insurance, subscription).

### 6.2 Income detector
- Credits above a threshold (default ₹2,000) that are not transfers, refunds or cashback.
- Salary: monthly credits from one counterparty within a ±5-day window and amount CV ≤ 0.1. Report typical amount and expected day.
- Irregular: all other recurring or sporadic credits; estimate monthly income distribution (mean, standard deviation, share of months with zero).
- Declared income type overrides the detected type, and any conflict is shown in the completeness card.

### 6.3 Liability extractor
Builds `Liability` rows from card statements, EMI debits, and declared loans. When outstanding or rate is missing, the engine still plans (using EMI and tenure when known) but reports the gap in `missing`.

### 6.4 Investment (SIP) detector
Investments are observed only as debits. The product never sees a portfolio value.
1. Candidate debits: narration or counterparty matches an allow-list of platforms and fund houses (for example Groww, Zerodha Coin, AMC names) or carries mandate keywords (NACH, ECS, SIP).
2. Run the recurring detector (6.1) on the candidates. A monthly interval with amount CV ≤ 0.05 is a SIP; the confidence formula is unchanged.
3. Output an `Investment` row: platform label, monthly amount, debit day, asset type (MUTUAL_FUND by default; STOCKS or PPF only when the user declares them).
4. One-off investment debits are categorised "Investments" but not marked recurring.
5. A declared SIP amount overrides a detected one; a conflict shows in the completeness card.

## 7. Snapshot, verdict and health score

### 7.1 Completeness
`completeness` is the weighted share of known inputs: cash anchor (0.25), income (0.25), recurring obligations (0.2), liability details (0.2; EMI, rate, outstanding and months left for loans, limit and minimum due for cards), months of history ≥ 3 (0.1). `missing` lists the unknown inputs in plain words. Each missing item shows how it would narrow the forecast.

### 7.2 Verdict engine (next 30 days)
Daily simulation over 30 days using dated recurring items (rent, EMIs, card due dates, SIP debits), expected income dates (with ±2 day jitter) and variable spend spread across days with bootstrapped noise. Output: first day the P10 balance falls below the reserve ("tight on"), and the day before it ("fine until"), plus the shortfall chance at that date. If no day breaches, the verdict reads "You're fine for the next 30 days" with the minimum P10 balance and its date.

### 7.3 Health score (proposal, deterministic, 0-100)
Sum of four components, each 0 to 25. All inputs come from the snapshot.

| Component | Formula | Meaning |
|---|---|---|
| Cash runway | `25·clamp((cash / monthlyObligations) / 6, 0, 1)` | Months of essentials plus debt service covered by cash; 6 months scores full |
| Debt load | `25·(1 − clamp(DTI / 0.5, 0, 1))` | `DTI = monthly debt service / monthly income`; 50% or more scores zero |
| Buffer | `25·clamp(cash / minReserve, 0, 1)` | Cash against the declared cushion |
| Stability | `25·(1 − clamp((σ_net / meanIncome) / 0.3, 0, 1))` | Variability of monthly net cash flow relative to income |

The score explainer shows each component, its inputs and what would raise it. Thresholds: Risk below 50, Watch 50-75, Safe above 75.

Stream notes: `monthlyObligations` in the runway formula is essentials plus debt service (EMIs and the card bill). DTI counts EMIs and card minimum payments only. SIP is excluded from both because it is voluntary and pausable; it appears separately as the committed savings rate (`monthlySip / monthlyIncome`), shown on the accounts sheet and not scored.

### 7.4 Consolidated view (the five streams)
`ConsolidatedViewBuilder` is a pure function from `FinancialSnapshot` to `ConsolidatedView`. The Cockpit accounts row, the accounts sheet (M10) and the Copilot Observed section all read it, so the three always agree.

| Field | Stream | Definition |
|---|---|---|
| `liquidCash` | Bank accounts | Sum of the latest balances of SAVINGS accounts (7.1 balance anchor) |
| `cardDues` | Credit cards | Sum of `totalDue` over cards with a statement; for a card with no statement yet, its `unbilled` amount |
| `monthlyEmis` | Loans | Sum of `emi` over loans |
| `monthlySip` | Investments | Sum of `monthlyAmount` over detected or declared SIPs |
| `avgDiscretionary` | Payment histories | Trailing 6-month mean of `discretionary` |
| `staleAccountIds` | Bank accounts | Accounts whose balance is older than 7 days |

Rules:
- Card dues are displayed next to cash, never subtracted from it in the pill. The sheet shows "Cash after card bills" as a separate Observed line. The forecast takes the card bill out of cash once, on its due date (ledger rules, section 5).
- A stale balance lowers completeness and widens the forecast, and the pill carries a Watch icon.
- Every number the Copilot quotes about the streams comes from this object (13.5).

Example of an Observed section built from it (the values come from the bundle at run time): "Bank cash ₹X. Card dues ₹Y. Loan EMIs ₹Z a month. SIP ₹W a month."

## 8. Forecast engine (Addition 2)

### 8.1 What is forecast
For each future period t (6 monthly or 8 weekly) and each simulated path k:
```
Income_t(k)      salaried: detected salary with timing jitter and bonus months
                 irregular: bootstrap from historical monthly income (block bootstrap, block length 2)
Recurring_t      dated, near-deterministic amounts from recurring items; SIP debits are dated, fixed amounts; the card bill is one outflow on its due date equal to the statement estimate (unbilled amount plus projected card spend, bootstrapped), and card-use transactions do not touch cash; variable recurring (utilities) bootstrapped
Variable_t(k)    point forecast + bootstrapped residual (per category group)
Shock_t(k)       optional one-off essential expense (off by default; the demo injects a known one)
NetFlow_t(k)   = Income_t(k) − Recurring_t − Variable_t(k) − Shock_t(k)
Balance_t(k)   = Balance_{t−1}(k) + NetFlow_t(k) − (plan actions)
```

### 8.2 Declared method (state this in the UI and the evidence pack)
`"Component bootstrap, 2,000 seeded paths, conformally calibrated 80% intervals"`
- **Point forecasts:** exponentially weighted mean (α = 0.4) per component over the training window.
- **Residuals:** historical forecast errors per component, resampled with replacement.
- **Quantiles:** P10, P50, P90 per period from the simulated distribution. The central 80% interval is [P10, P90].
- **Horizon:** 6 monthly periods, or 8 weekly periods.

### 8.3 Calibration (uses the validation window)
Raw bootstrap intervals are usually too narrow with short history. Calibrate with a scaling factor `s` learned on the validation window:
1. At each origin in the validation window, forecast the next h steps.
2. For every (origin, step) compute `r = |actual − P50| / halfWidth`, where `halfWidth = (P90 − P10) / 2`.
3. Let `s` be the 80th percentile of all `r` values (use a finite-sample correction: the ⌈(n+1)·0.8⌉-th smallest of n values).
4. Replace each interval by `P50 ± s·halfWidth`. Where `s < 1` keep the raw width (do not shrink below the bootstrap).
5. Store `s` and `n` with the forecast run. Report both.

Add an inflation of 10% per missing key input (cash anchor, income, loan balance) to reflect incomplete information, and show it as a reason on the confidence explainer.

### 8.4 Confidence label
`relativeWidth = (P90 − P10) / max(|P50|, 1)`. High: under 0.25 and at least 6 months of history. Low: over 0.60 or under 3 months of history. Otherwise Medium. Starting values; tune on the backtest.

### 8.5 Shortfall risk
```
shortfallRisk(plan) = (# paths with min_t Balance_t(k) < reserve) / (# paths)
```
Also reported per period: `P(Balance_t < reserve)`, and expected shortfall (mean of `reserve − Balance_t` over breaching paths).

### 8.6 Baselines (for comparison, section 11)
1. **Historical average:** next-period net flow equals the trailing 6-month mean; interval = mean ± 1.28·σ (normal approximation) so the comparison on coverage and width is fair.
2. **Recurring-payment:** salary minus dated recurring items minus last month's variable spend; no variance, so no interval (coverage not applicable).

## 9. Planner (Addition 1)

### 9.1 Inputs and constraints
Inputs: income, essentials and debt service per period (from the forecast, P50 for the deterministic plan, sampled paths for risk), baseline discretionary `V_t`, declared reserve `R`, liabilities, optional goal.

Hard constraints:
1. `Balance_t ≥ R` for every period on the deterministic (P50) path, unless a relaxation is applied.
2. Essentials and debt service (EMIs and the card bill) are paid in full every period. SIP debits are paid by default; only the what-if simulator and the optional SIP relaxation (9.5) reduce them.
3. Discretionary cuts cannot exceed 60% of baseline.

### 9.2 Action sequences as state-dependent policies
Each sequence is a rule that decides, from the current state, how much to cut, prepay and save. Because a policy is a function of the state, the same rule is simulated on every Monte Carlo path.

```
available_t = Balance_{t−1} + Income_t − Essentials_t − DebtService_t − SIP_t − V_t·(1 − c)
surplus_t   = max(0, available_t − m·R)
prepay_t    = min(p·surplus_t, outstandingDebt_t)       // prepay highest-rate loan first
Balance_t   = available_t − prepay_t
```

| Sequence | c (cut) | p (prepay share) | m (buffer multiple) | Intent |
|---|---|---|---|---|
| A: Pay debt faster | 0.30 | 0.8 | 1.0 | Maximise interest saved; accepts a thinner buffer |
| B: Build buffer first | 0.30 | 0.8 | 2.5 | Hold 2.5× the reserve before prepaying; maximises liquidity safety |
| Status quo (baseline) | 0 | 0 | not applicable | No change; reference for "interest saved" and impact |

Parameters are editable in code for tuning. At least two sequences are always compared, plus the status quo. `SIP_t` is the committed SIP debit (zero if none); the three sequences do not change it. SIP contributions are reported as `investedTotal` and are not part of `terminalNetPosition`.

### 9.3 Interest and objective
Monthly rate `r = annualRate / 12`. Each loan amortises by EMI; a prepayment reduces outstanding principal (tenure shortens, EMI unchanged). `interestPaid` is the total interest over the horizon; `interestSavedVsStatusQuo = interestPaid(statusQuo) − interestPaid(plan)`.

Objective on the P50 path: `terminalNetPosition = cash_T − outstandingDebt_T` (higher is better; it already reflects interest saved).

### 9.4 Choosing a plan (how risk changes the recommendation)
```
θ = risk threshold (default by cohort: A 15%, B 12%, C 8%; user-editable)
feasible = { plans with shortfallRisk ≤ θ }
if feasible is not empty: recommend argmax terminalNetPosition over feasible
else: recommend the minimum-risk plan and flag "No plan meets your risk limit"
```
The "Why this plan" line is generated from this rule, for example: "Plan A has the higher payoff but a 38% chance of dipping below your cushion, above your 12% limit. Plan B keeps that to 6%."

### 9.5 Infeasible goals and relaxation (computed, not hardcoded)
1. **Feasibility test:** run the max-saving policy (`c = 0.6`, `p = 0`) on the P50 path. The maximum achievable saved amount by the goal period is `S_max`. If `goal.target > S_max`, the goal is infeasible, with `shortfall = target − S_max`.
2. **First violated period:** the earliest period where `Balance_t < R` under the goal's saving schedule.
3. **Relaxation options**, each re-run through the Monte Carlo for `riskAfter`:
   - **Lower the target** to `S_max` (delta = shortfall).
   - **Extend the timeline:** the smallest `T' > T` (up to 24 periods) at which the target is achievable.
   - **Reduce the reserve:** bisection on `ΔR ∈ [0, R]` (tolerance ₹100) for the smallest reduction that makes the target achievable. If the resulting reserve falls below one month of essentials, flag "Not recommended".
   - **Pause or reduce the SIP** (only when an SIP exists): bisection on the monthly SIP amount (tolerance ₹100) for the smallest reduction that makes the target achievable, re-run through the Monte Carlo for `riskAfter`. It is shown as a trade-off against long-term saving and is never the default.
4. The UI statement is templated from these computed values.

### 9.6 Replan and "what changed"
Any change to the snapshot or declared inputs (new transaction, income change, new loan, edited cushion, scenario switch) recomputes forecast, plans and verdict. The result is diffed against the previous run and summarised: verdict date moved, shortfall risk before and after, whether the recommended plan changed, and which input caused it.

## 10. Impact simulator (what-if)

Inputs: the current snapshot, forecast distribution and an action (discretionary cut, extra prepayment, income change, one-off expense, SIP change). Both the baseline and the action run on the same random draws (common random numbers) to cut noise. Outputs: `runwayDeltaMonths`, `shortfallRiskDelta`, `interestSaved`, plus the period table for the changed path.

`runway = (cash − R) / (avg monthly essentials + debt service)`, in months, floored at 0.

## 11. Evaluation harness

### 11.1 Protocol
- **Rolling origin:** for each persona and seed, forecast from every origin in the test window that leaves at least one step ahead; evaluate steps 1 to H (H = 6 months).
- **No leakage:** the forecast at origin `o` may use only data strictly before `o`. A test enforces this (section 14).
- **Models compared:** ours (calibrated bootstrap), historical average, recurring-payment.
- **Targets:** net cash flow per period and cumulative ending balance.

### 11.2 Metrics
| Metric | Definition |
|---|---|
| MAE | mean of `abs(actual − P50)` |
| RMSE | square root of the mean of `(actual − P50)^2` |
| Coverage (80%) | share of actuals within [P10, P90] |
| Mean interval width | mean of `P90 − P10` |
| Pinball loss | at quantiles 0.1, 0.5, 0.9 |

Coverage should be close to 80% overall and be reported by horizon step. Report failures honestly: shocks that the model cannot foresee lower coverage, and the report should show it.

### 11.3 Outputs
A Gradle test (`ModelValidationBenchmarkTest`) prints an ASCII table and writes `benchmark_results.json`. **All numbers come from the run; no hand-typed example values appear anywhere in the app, slides or docs.**

## 12. Synthetic data

### 12.1 Timeline (generation periods)
| Window | Months | Use | Seed stream |
|---|---|---|---|
| Train | 1 to 18 | Fit point forecasters and residual pools | seed A |
| Validation (calibration) | 19 to 24 | Learn the calibration factor `s`; tune thresholds | seed B |
| Test (held out) | 25 to 30 | Evaluation only; generated separately | seed C |

Each window uses its own random stream and is generated after the models are frozen. Scenario shocks (below) apply in the test window.

### 12.2 Personas (illustrative; see [03 Target audience](03-target-audience.md))
| Persona | Income process | Obligations |
|---|---|---|
| A, early earner | Lognormal around ₹30,000 (range about ₹18,000-₹45,000), AR(1) φ = 0.3, 20% chance a payment slips a month | Rent ₹8,000, phone EMI ₹2,300, small card |
| B, mid-career | ₹85,000 fixed, small bonus months | Rent ₹22,000 (1st), home loan EMI ₹18,500 (10th), car EMI ₹8,200 (15th), utilities about ₹2,500 (20th), SIP ₹10,000 (5th), card statement due 25th |
| C, committed | ₹1,40,000 fixed | Home loan EMI ₹45,000, school fees in two annual instalments, annual insurance premium, ₹5,000 monthly parental medical cost, SIP ₹15,000 (5th) |

Variable spend: lognormal per category with a festival-month uplift (about +20% in October and November). Recurring dates jitter by ±2 days. Optional one-off essential shocks follow a Poisson process (λ = 0.15 per month, median ₹8,000).

### 12.2a Account sets (the five streams per persona)
Accounts are generated, not hand-listed. The seeder emits raw SMS-like and CSV-like transactions (card-use alerts, statement-generated alerts, bill payments, EMI debits, SIP debits) and the ingestion pipeline derives accounts and balances from them, so the detectors in section 6 do real work. The demo date is the first day of the test window (month 25). All values are synthetic assumptions.

| Stream | Persona A | Persona B (primary demo) | Persona C |
|---|---|---|---|
| Bank account | Savings account, balance around ₹14,000 | HDFC salary and savings account (•• 4091), balance ₹45,200 on the demo date | Salary account, balance around ₹2,10,000 |
| Credit card | One card, limit ₹20,000 | SBI SimplyCLICK (•• 8821), limit ₹1,50,000, unbilled ₹14,300, statement due on the 25th, minimum due 5% of the statement | One card, limit ₹3,00,000 |
| Loans | Phone EMI ₹2,300, 9 months left, no interest | Home loan: outstanding ₹18,50,000, 8.5%, EMI ₹18,500 due on the 10th. Car loan: EMI ₹8,200 due on the 15th, 24 months left, rate 9.5% (assumed), outstanding derived by the seeder from EMI, rate and tenure | Home loan: EMI ₹45,000, rate 8.75% (assumed), outstanding ₹42,00,000 (assumed) |
| Investments | None | Mutual fund SIP ₹10,000 on the 5th (Groww) | Mutual fund SIP ₹15,000 on the 5th |
| Payment history | 30 months, variable income | 30 months | 30 months with annual fees and premium |

Seeder rules:
- The seeder back-solves each opening balance so the balance on the demo date matches the table.
- **Persona B baseline surplus.** Rent, EMIs, utilities and the SIP total ₹61,200 against ₹85,000 income. Persona B's variable spend (card use included) is calibrated so the baseline monthly surplus is small but positive (design target ₹5,000 to ₹8,000). `SyntheticDataSeederTest` asserts the sign. It is a property of the generator, not a hand-set result.
- **Persona B-sparse** omits the car loan rate, the card limit and minimum due, and has only two months of SIP history, so completeness falls below 80% and journey J7 (add missing information) can be demonstrated.

### 12.3 Demonstration scenarios (match the problem statement)
| Scenario | Definition | What it must show |
|---|---|---|
| A. Stable income | Persona B, ₹85,000 fixed | Baseline plan, narrow band |
| B. Income reduction | Income drops to ₹55,000 from test month 2 | Plan changes; shortfall risk rises; replan |
| C. Unexpected essential expense | ₹25,000 medical bill in test month 3 | Reserve dips; recommendation adapts |
| D. Infeasible target | ₹1,00,000 savings target on ₹55,000 income with a ₹40,000 reserve, full EMIs and the SIP | Computed conflict and relaxation options |
| E. Uncertainty changes the preferred plan | Income volatility ±40%, shortfall risk above 35% for Plan A | Preferred plan flips from A to B because of risk |

**Check in hour 8.** With rent, EMIs, utilities and the SIP at ₹61,200, an income of ₹55,000 (scenarios B and D) is negative before any variable spend. Those scenarios will breach the reserve quickly and the first lever is the SIP. If the overlay is too harsh for the plans to differ, raise the reduced income (for example to ₹65,000) and record the change in the evidence README.

Scenario overlays act in two places: on the plan inputs in the live demo (snapshot edits), and on the test-window actuals in the evaluation. Document both in the evidence pack.

### 12.4 Real users
Minimum 3 months of history for Medium confidence; fewer months produce Low confidence and a wider band. Users with less history can load a sample profile.

## 13. Copilot (Gemini) contract

### 13.1 Flow
```
question -> IntentRouter (Gemini via proxy, fallback: local rules) -> intent + parameters
         -> engine call -> ResultBundle (typed numbers, tags, periods)
         -> narrate (Gemini via proxy, fallback: template) -> StructuredResponse
         -> NumberVerifier -> UI (or fallback template if verification fails)
```

### 13.2 Intents (tool allow-list)
`spending_summary(period, category?)` · `cash_position()` · `account_overview()` · `upcoming_obligations(days)` · `affordability(amount, when, type, emiMonths?, rate?)` · `simulate_action(type, amount)` (type: cut, prepay, income_change, one_off, sip_change) · `replan(change)` · `compare_plans()` · `explain_period(index)`. Anything else returns a polite "I can help with spending, savings, debt and affordability" reply. The model never receives free-form database access.

### 13.3 Response schema
```json
{
  "observed":    [ { "text": "string", "refs": ["numberKey"] } ],
  "predicted":   [ { "text": "string", "range": {"p10": 0, "p50": 0, "p90": 0},
                     "confidenceLabel": "High|Medium|Low", "horizon": "string" } ],
  "recommended": [ { "text": "string", "impact": {"runwayDeltaMonths": 0, "riskDelta": 0, "interestSaved": 0},
                     "tradeOff": "string" } ]
}
```

### 13.4 Number verifier
Extract every numeric token and currency amount from the narrated text. Each must match a value in the `ResultBundle` (allowing for rounding and formatting such as ₹1,50,000 or ₹1.5L). On any mismatch, discard the model text and render the deterministic template built from the same bundle. Log the event (no content) for debugging.

### 13.5 Prompt-injection and privacy rules
- Prompts contain only aggregates, categories, engine outputs and the user's question. Never raw messages, VPAs, account numbers, phone numbers or merchant names that could identify a person.
- The `ResultBundle` describes the five streams by label and total only (bank cash, card dues, loan EMIs, SIP, average discretionary). Institution names, last-four digits and platform names never enter a prompt.
- Transaction-derived text is untrusted and is excluded from prompts. The user's question is untrusted too: it can only select among allow-listed intents.
- Use an API tier whose terms exclude training on submitted data; verify the current terms before relying on this claim.

### 13.6 Local fallback
`LocalAdvisor` produces the same three-section structure from the `ResultBundle` with templates, so the demo works with no network or quota.

## 14. Proxy API (FastAPI)

Existing endpoints stay. New endpoints are thin.

| Method and path | Purpose | Auth |
|---|---|---|
| `POST /api/v1/categorize` | Single merchant categorisation (existing) | Supabase JWT |
| `POST /api/v1/categorize/batch` | Batch categorisation (existing) | Supabase JWT |
| `POST /api/v1/copilot/interpret` | Question to `{intent, params}` | Supabase JWT |
| `POST /api/v1/copilot/narrate` | `ResultBundle` to `StructuredResponse` | Supabase JWT |
| `GET /api/v1/health` | Liveness | none |

Server rules: verify the JWT on every call; Pydantic validation on all bodies; request size limit 32 KB; per-user rate limit (default 30 requests per minute); shared server-side cache keyed by merchant for categorisation only; logs redact amounts and identifiers; Gemini key only in environment variables; no request body stored.

## 15. Security and privacy

| Layer | Controls |
|---|---|
| Data minimisation | Only normalised rows or aggregates leave the device: amount, date, category, hashed counterparty. Never raw messages, VPAs, account or phone numbers. Institution names and last-four digits stay on the device |
| On device | Encrypted local database (SQLCipher key held in the Android Keystore); tokens in Keystore-backed storage, not plain SharedPreferences; `allowBackup=false`; `FLAG_SECURE` on money screens; optional biometric lock |
| Transport | TLS only; optional certificate pinning |
| Authentication and authorisation | Supabase Auth (PKCE), short-lived JWT, refresh rotation; row-level security; service-role key only on the server |
| Backend | Stateless proxy, validation, rate limits, redacted logs, secrets in environment variables, dependency scanning in CI |
| Storage and retention | Store only plans and forecast results; retention limits; working "Export my data" and "Delete my data" |
| AI layer | Aggregates only; tool allow-list; number verifier; no free-form data access |
| Consent | Separate toggles for SMS access, cloud sync and AI assistance; works fully on device if the user declines sync; transmission log in the privacy center |
| Demo | Run on synthetic personas so no real data is shown |

**Never collected:** net-banking credentials, PINs, CVVs, OTPs, full account or card numbers, contacts, location.

| Threat | Mitigation |
|---|---|
| Lost or stolen phone | Encrypted database, biometric lock, `FLAG_SECURE` |
| Server breach | Stateless proxy; hashed IDs; no raw messages or account numbers stored |
| Man in the middle | TLS, optional pinning |
| Cross-user data access | RLS, JWT verification, automated cross-user access tests |
| Model manipulation via transaction text | Untrusted-input handling, intent allow-list, number verifier |
| Credential phishing | Never ask for credentials or OTPs |
| Over-collection | Minimisation, purpose limitation, deletion controls |

Regulatory notes (confirm before any public release, not legal advice): Google Play restricts the SMS permission and requires a permitted use case; India's Digital Personal Data Protection Act brings consent, purpose-limitation and minimisation duties. Account Aggregator integration would be the long-term consent-based route but needs registration and is out of scope now.

## 16. Testing strategy

| Test | Checks | Command |
|---|---|---|
| `IndianFormatTest` | Lakh grouping and compact form | `./gradlew test --tests "*IndianFormatTest"` |
| `SmsParserTest`, `OtpFilterTest` | Parsing, OTP exclusion, hashing stability | `./gradlew test --tests "*SmsParserTest"` |
| `RecurringDetectorTest` | Monthly rent, EMI, jittered utilities, non-recurring noise | `./gradlew test --tests "*RecurringDetectorTest"` |
| `ConsolidatedViewTest` | Each stream total equals the sum of its accounts; a card-use transaction does not reduce bank cash; the card bill appears once, on its due date; SIP is excluded from DTI | `./gradlew test --tests "*ConsolidatedViewTest"` |
| `InvestmentDetectorTest` | Monthly SIP found on personas B and C; a one-off investment debit is not marked recurring; no false positives on variable spend | `./gradlew test --tests "*InvestmentDetectorTest"` |
| `SyntheticDataSeederTest` | Same seed gives identical output; windows do not overlap; account balances reconcile with the ledger; persona B baseline surplus is positive | `./gradlew test --tests "*SyntheticDataSeederTest"` |
| `ForecastEngineTest` | `P10 ≤ P50 ≤ P90`; determinism with a seed; risk between 0 and 1 | `./gradlew test --tests "*ForecastEngineTest"` |
| `NoLeakageTest` | Forecast at origin `o` is identical when all data at or after `o` is removed | `./gradlew test --tests "*NoLeakageTest"` |
| `CalibrationTest` | After calibration, validation coverage is within ±10 points of 80% over at least 200 forecasts | `./gradlew test --tests "*CalibrationTest"` |
| `PlannerTest` | Reserve never violated unless a relaxation is applied; at least two sequences returned; discretionary cut ≤ 60% | `./gradlew test --tests "*PlannerTest"` |
| `ConflictResolverTest` | Scenario D yields a computed conflict and three relaxations (four when an SIP exists) whose `riskAfter` are valid | `./gradlew test --tests "*ConflictResolverTest"` |
| `NumberVerifierTest` | Mismatched number triggers fallback | `./gradlew test --tests "*NumberVerifierTest"` |
| `ModelValidationBenchmarkTest` | Writes `benchmark_results.json` and the evidence pack | `./gradlew test --tests "*ModelValidationBenchmarkTest"` |

## 17. Evidence pack

Produced by `EvidenceExporter` into `build/evidence/` and shareable from the Model report screen.

| File | Contents |
|---|---|
| `README.md` | Method declaration, horizon, interval level, seeds, synthetic assumptions, train / validation / test timeline |
| `periods_plans.csv` | scenario, plan_id, period, opening, income, essentials, debt_service, sip, discretionary, prepay, saved, ending, reserve, constraint_ok |
| `forecasts.csv` | persona, seed, origin, step, metric, p10, p50, p90, actual, in_interval |
| `outcomes.csv` | scenario, plan_id, simulated_min_balance, breached_reserve, terminal_net_position |
| `baselines.csv` | model, MAE, RMSE, coverage_80, mean_width, pinball_loss |
| `conflicts.json` | Computed conflicts and relaxations per scenario |
| `timeline.json` | Windows, months and seed streams |
| `accounts.json` | Synthetic account sets per persona (section 12.2a) |
| `benchmark_results.json` | Aggregate metrics |

This covers the required submission items: period-level plans, constraints, forecasts, actual or simulated outcomes, a baseline comparison and the train-validation-test timeline.

## 18. Performance budgets

| Operation | Budget |
|---|---|
| Forecast, 2,000 paths × 6 periods (JVM) | under 100 ms |
| Three plans plus risk | under 300 ms |
| Verdict (30-day daily simulation) | under 200 ms |
| Consolidated view build | under 20 ms |
| Recompute after a new transaction, end to end | under 500 ms |
| Full evaluation (3 personas × 5 seeds × rolling origins, 10,000 paths) | under 60 s |

## 19. Open items

1. Final constants: EWMA α, bootstrap block length, shortfall-risk threshold per cohort.
2. Whether the recurring detector needs a learned component or whether rules suffice for the demo.
3. Gemini model choice and quota plan for the live demo (local fallback is the safety net).
4. Real SMS formats for each target bank beyond the allow-list, including card, EMI and SIP templates.
5. Persona B variable-spend calibration and the severity of the scenario B and D overlays once the SIP is included (see 12.3).
6. Whether the optional SIP relaxation is shown by default in the conflict screen or only on request.
