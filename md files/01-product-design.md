# 01 - Product Design

**Product:** RupAI Financial Health Copilot
**Track:** HackMatrix 5.0, FIN-02 Financial Health Copilot: From Transactions to Action (PCCOE Pune)
**Platform:** Native Android (Kotlin, Jetpack Compose)
**Related docs:** [03 Target audience](03-target-audience.md) · [04 Pain points](04-pain-points.md) · [02 Pages](02-pages.md) · [07 UX flow](07-ui-ux-flow.md) · [09 Overall plan](09-overall-plan.md)

> **Version 2.** Merges the FIN-02 multi-account addendum (formerly `new changes.md`) into the full set. Bank accounts, credit cards, loans, investments (SIPs) and payment histories are now one consolidated picture, described in section 2.1 and built in [06 Backend](06-backend.md), section 7.4.

---

## 1. One-line product definition

RupAI Financial Health Copilot turns a person's scattered bank accounts, credit cards, loans, SIPs and payment histories into **a clear verdict on the next few months, a constrained plan, and the expected impact of every recommended action**, while showing how sure it is.

## 2. The problem in plain words

People in India pay through UPI, cards, EMIs and loans across many apps. No single tool tells them whether they will be okay on the 18th, which obligation hits when, or what to do about it. Existing trackers describe the past. They do not plan under uncertain cash flow. Full evidence and sources are in [04 Pain points](04-pain-points.md).

### 2.1 The five financial streams

FIN-02 asks for one understanding across bank accounts, credit cards, loans, investments and payment histories. The product treats these as five streams that feed one snapshot.

| # | Stream | Where it comes from | Role in the product | Kept on the device |
|---|---|---|---|---|
| 1 | Bank accounts | Bank SMS (credit and debit alerts with balance), CSV, declared | Current liquid cash, the starting point of every forecast | Institution, last four digits, latest balance, balance date |
| 2 | Credit cards | Card SMS (spend alerts, statement generated, due reminders), CSV, declared | Short-term revolving liability and the payment cliff on the due date | Card name, limit, unbilled amount, total and minimum due, due date |
| 3 | Loans and EMIs | EMI debit SMS, CSV, declared | Fixed debt service, debt-to-income, interest saved by prepayment | Loan name, outstanding, rate, EMI, months left, due day |
| 4 | Investments | SIP debits seen in bank messages, declared | Committed savings: how much leaves each month for SIPs. Not portfolio value | Platform or fund house, monthly amount, debit day |
| 5 | Payment histories | The full transaction stream (food, rent, travel, utilities, shopping) | Discretionary spend and its variance, which drives the Monte Carlo ranges | Category, amount, date, hashed counterparty |

Rules that keep the consolidation honest:
- **One ledger, no double counting.** A card-bill payment is a transfer. Spending is analysed when the card is used. Cash is reduced when the bill is paid: once, on its due date.
- **SIP is committed savings.** It is not an expense and not debt service. It is paid by default, excluded from debt-to-income, and can be paused in the what-if simulator.
- **Investments means SIP debits only.** Messages cannot reveal portfolio value, so the product never shows one and gives no investment advice.
- **Institution names and account digits stay on the phone.** The proxy and Gemini receive per-stream totals only.

## 3. Product vision and goals

**Vision:** a calm, honest financial co-pilot that answers "will I be okay, and what should I do?" with numbers the user can trust.

| Goal | What it means in the product |
|---|---|
| Clarity | One consolidated view across bank accounts, cards, loans, SIPs and payment history (the five streams) |
| Foresight | A probabilistic forecast and a 30-day obligations timeline, not just history |
| Action | Two or more compared plans with period-by-period balances and trade-offs |
| Honesty | Every statement is labelled Observed, Predicted or Recommended, with confidence tied to interval width |
| Trust | Raw bank messages never leave the phone; no bank credentials ever collected |
| Adaptability | Plans recompute when income, expenses or debts change, and conflicts are named with a proposed relaxation |

**Non-goals (v1):** investment advice, portfolio valuation (only SIP debits are observed), tax filing, bill payment or money movement, multi-user or household budgeting, iOS, regional-language UI.

## 4. Core concept: the Verdict, Plan, Impact loop

```
Data in (SMS / CSV / sample) -> Ledger and obligations -> Forecast (with range)
        -> Plans compared under constraints -> Recommended action with impact
        -> User changes something -> Everything recomputes and the change is explained
```

1. **Verdict.** A one-line answer on the Cockpit: "You're fine until 24 Oct. Tight on 25 Oct."
2. **Plan.** At least two action sequences over 6 monthly or 8 weekly periods, honouring income, essential expenses, debt obligations and a declared minimum cash reserve.
3. **Impact.** Every recommendation shows before and after: runway, shortfall chance, interest saved.
4. **Replan.** When something changes, the plan updates. If goals and constraints cannot all be met, the conflict is stated with numbers and a relaxation is proposed.

## 5. The trust model (what makes this different)

Every insight carries one of three labels. This is the product's signature and a direct hackathon requirement.

| Label | Meaning | Source | Must show |
|---|---|---|---|
| **Observed** | Something that happened in the data | Ledger, parsed messages | Source and period |
| **Predicted** | A model output about the future | Forecast and Monte Carlo engine | Range (80% interval), horizon, method, confidence label derived from interval width |
| **Recommended** | A suggested action | Planner and simulator | Expected impact and the trade-off |

Rules:
- Numbers are always computed by deterministic engines. The language model only interprets questions and narrates results; it never calculates.
- Confidence is never an unexplained percentage. It is derived from interval width relative to the median and the length of history, and it opens an explainer.
- A **data-completeness meter** shows what the system does not know (for example "loan balance missing") and how adding it changes the forecast.

## 6. Feature set

### 6.1 Must-have (judged requirements)

| # | Feature | Maps to |
|---|---|---|
| F1 | Consolidated financial-health view across the five streams (ledger, obligations, income, debt, SIPs) | PS outcome 1 |
| F2 | Recurring-payment, debt, income and SIP detection | PS outcome 2 |
| F3 | Probabilistic cash-flow forecast, P10/P50/P90, declared horizon and method, calibrated intervals | Addition 2 |
| F4 | Cash-shortfall risk per action sequence, with an explanation of how risk changes the chosen plan | Addition 2 |
| F5 | Constrained multi-period planner, at least two compared sequences, balances and trade-offs per period | Addition 1 |
| F6 | Replan on income or expense change; conflict detection with an explicit relaxation proposal | Addition 1 |
| F7 | Conversational Q&A (spending, savings, debt, upcoming pressure, affordability) with Observed / Predicted / Recommended sections | PS outcomes 3-5 |
| F8 | Recommendation impact display (what-if simulator with before and after) | PS outcome 4 |
| F9 | Recomputation visible when new transactions, income or debt are added | PS outcome 6 |
| F10 | Evaluation harness: forecast vs historical-average and recurring-payment baselines on held-out periods; MAE, RMSE, 80% interval coverage and width | Addition 2 |
| F11 | Evidence pack export: period-level plans, constraints, forecasts, actual or simulated outcomes, baseline comparison, train-validation-test timeline | Required evidence |
| F12 | Five demonstration scenarios (stable income, income reduction, unexpected essential expense, infeasible target, uncertainty changes the preferred plan) | Required demonstration |
| F13 | Consolidated accounts view: bank cash, card dues, loan EMIs, SIPs and payment history in one place, with per-account detail | PS outcome 1, FIN-02 summary |

### 6.2 Should-have

- Onboarding with three profile questions and a declared cushion, goals and loan details.
- Health score (0-100) that is fully explainable: four components, each out of 25.
- Data-completeness meter and confidence explainer.
- Privacy center: consent toggles, "what left your phone" log, export and delete.
- Real SMS ingestion path (Android) in addition to synthetic data.
- Card, loan and SIP message parsers (statement due, EMI debit, SIP debit) for a few known bank senders.

### 6.3 Could-have (post-hackathon)

CSV and statement import polish, weekly digest notifications, biometric lock, Hindi and regional languages, Account Aggregator integration (requires registration; out of scope now), receipt OCR.

## 7. How the product relates to existing RupAI

RupAI today is an Android expense tracker: SMS capture, a four-layer categorisation pipeline, a dashboard and a Gemini advisor. It is backward-looking.

| Capability | Existing RupAI | This product adds |
|---|---|---|
| SMS ingestion, dedupe, categorisation, privacy masking | Yes | Reused; masking replaced by salted hashed counterparty IDs for matching |
| Dashboard of income vs expense | Yes ("income minus expenses") | Liability- and transfer-aware ledger |
| Accounts, cards, loans, SIPs | Single static income field; no liabilities | `Account`, `Liability`, `Investment` and a consolidated five-stream view |
| Recurring detection | Todo only | Recurring and liability detector |
| Forecast | No | Calibrated probabilistic forecast and Monte Carlo risk |
| Planning | No | Constrained planner, replan, conflict relaxation |
| Advice | One generic Gemini card | Tagged, engine-backed, impact-quantified recommendations |
| Evidence | No | Reproducible evaluation and export |

## 8. Experience pillars

1. **Verdict first, evidence second.** Open with the answer; details are one tap away.
2. **Ranges, not false precision.** Show a likely range and how wide it is.
3. **Calm, never shaming.** "Tight on 25 Oct" instead of "you overspent".
4. **One primary action per screen.**
5. **Demo tools stay hidden** from real users (Demo mode in settings).

Visual and interaction detail is in [05 Frontend UI style](05-frontend-ui-style.md) and [07 UI/UX flow](07-ui-ux-flow.md).

## 9. Data and privacy stance

- Ingestion: on-device SMS parsing (Android), CSV or statement upload, synthetic personas, and manual entry. Account Aggregator is out of scope.
- The backend (or Gemini) receives only normalised rows or aggregates: amount, date, category, salted hashed counterparty ID. Never raw SMS text, VPAs, account numbers, phone numbers or credentials.
- Institution names and the last four digits of an account or card are shown on the phone only. Copilot prompts carry per-stream totals, not account details.
- Never collected: net-banking credentials, PINs, CVVs, OTPs, full account or card numbers.
- Declared by the user (because messages cannot tell us): income type and typical amount, minimum cash cushion, loan details, card limit, SIP amount, goals, optional age range (tone only).
- Full detail in [06 Backend](06-backend.md), section "Security and privacy".

## 10. Success criteria

**For the hackathon**
- All requirements in F1-F12 demonstrable on a phone and verifiable from a terminal.
- The five scenarios each produce a clearly different, explainable result.
- Reproducible evaluation: judges can run one Gradle command and see real metrics and an exported evidence pack.

**For the product (after the hackathon)**
- A new user reaches a first verdict in under three minutes.
- Forecast interval coverage near the declared level (80%) on held-out data.
- Users act on at least one recommendation per month.

## 11. Risks and mitigations

| Risk | Mitigation |
|---|---|
| Thin history gives wide, unhelpful intervals | Show wide ranges honestly, flag Low confidence, ask for declared inputs |
| LLM invents numbers | Engines compute; a verifier checks every number in narrated text against engine output; local deterministic fallback |
| Overfitting the demo | Held-out periods generated with separate seeds; rolling-origin backtests across personas |
| Real SMS formats vary by bank | Start with known bank senders; keep CSV and synthetic paths as fallbacks |
| Multi-account scope grows (parsers for every stream) | Synthetic and sample profiles first; real parsers only for card, EMI and SIP templates of two or three banks; see the cut order in [09](09-overall-plan.md) |
| Scope too big for the time | Clear priority order in [09 Overall plan](09-overall-plan.md); cut list included |
| Privacy claims overstated | Only claim what the architecture does; privacy center shows the actual log |

## 12. Open decisions

1. Final health-score formula and thresholds (proposal in [06 Backend](06-backend.md)).
2. Confidence label thresholds (starting values to be tuned on the backtest).
3. Default shortfall-risk threshold for choosing a plan (proposal: 10-15%, tuned by cohort).
4. Whether a later version lets the user declare a portfolio value. Not in v1: only SIP debits are observed.
5. Persona B variable-spend calibration so the baseline monthly surplus stays positive with the SIP (see [06](06-backend.md), section 12.2a).
