# 02 - Pages (Screen Inventory)

Every screen, sheet and dialog in the app: purpose, entry points, data, components, actions and states. Visual tokens and component specs are in [05 Frontend UI style](05-frontend-ui-style.md). Step-by-step journeys are in [07 UI/UX flow](07-ui-ux-flow.md).

---

## 1. Site map

```
App
├── Onboarding (first run only)
│   ├── O1 Welcome
│   ├── O2 Choose data source
│   ├── O3 SMS consent (only if SMS chosen)
│   ├── O4 About your money (3 steps)
│   ├── O5 Your safety net
│   └── O6 Building your picture
│
├── Main (bottom navigation, 4 tabs)
│   ├── Cockpit ........................ M1
│   │     └── Accounts overview sheet .. M10
│   ├── Plan ........................... M2
│   │     ├── What-if sheet ............ M3
│   │     ├── Conflict resolution ...... M4
│   │     └── Model report ............. M7
│   ├── Copilot ........................ M5
│   └── Activity ....................... M6
│         ├── Transactions tab
│         ├── Repeating payments tab
│         ├── Transaction detail sheet . M6a
│         ├── Obligation detail sheet .. M6b
│         └── Add entry sheet .......... M9
│
├── Shared sheets
│   ├── Score explainer ................ M8a
│   └── Confidence explainer ........... M8b
│
└── Settings
    ├── S1 Settings
    ├── S1a Edit financial inputs (income, cushion, loans, goals)
    ├── S2 Privacy center
    └── S3 Demo mode (hidden, "Judge mode")
```

## 2. Inventory table

| ID | Screen | Type | Route | ViewModel | Primary data | Owner track |
|---|---|---|---|---|---|---|
| O1 | Welcome | Screen | `onboarding/welcome` | `OnboardingViewModel` | none | UI |
| O2 | Choose data source | Screen | `onboarding/source` | `OnboardingViewModel` | source choice | UI |
| O3 | SMS consent | Screen | `onboarding/sms` | `OnboardingViewModel` | permission state | UI |
| O4 | About your money | Screen (3 steps) | `onboarding/profile` | `OnboardingViewModel` | income type, fixed payments, age range | UI |
| O5 | Your safety net | Screen | `onboarding/safety` | `OnboardingViewModel` | cushion, goal, loans | UI |
| O6 | Building your picture | Screen | `onboarding/building` | `IngestViewModel` | ingest progress, completeness | UI + Data |
| M1 | Cockpit | Tab | `cockpit` | `CockpitViewModel` | verdict, score, timeline, metrics | UI |
| M2 | Plan and forecast | Tab | `plan` | `PlanViewModel` | forecast, plans, period table | UI |
| M3 | What-if | Sheet | `plan/whatif` | `WhatIfViewModel` | simulator deltas | Copilot |
| M4 | Conflict resolution | Screen | `plan/conflict` | `PlanViewModel` | conflict, relaxations | UI |
| M5 | Copilot | Tab | `copilot` | `CopilotViewModel` | chat messages, tiered answers | UI + Copilot |
| M6 | Activity | Tab | `activity` | `ActivityViewModel` | transactions, obligations | UI |
| M6a | Transaction detail | Sheet | `activity/tx/{id}` | `ActivityViewModel` | one transaction | UI |
| M6b | Obligation detail | Sheet | `activity/ob/{id}` | `ActivityViewModel` | one recurring item | UI |
| M7 | Model report | Screen | `plan/report` | `ReportViewModel` | evaluation results | UI + Math |
| M8a | Score explainer | Sheet | `explain/score` | `CockpitViewModel` | score components | UI |
| M8b | Confidence explainer | Sheet | `explain/confidence` | `PlanViewModel` | interval width, history length | UI |
| M9 | Add entry | Sheet | `activity/add` | `ActivityViewModel` | manual transaction | UI |
| M10 | Accounts overview | Sheet | `cockpit/accounts` | `CockpitViewModel` | accounts, card dues, loans, SIPs | UI |
| S1 | Settings | Screen | `settings` | `SettingsViewModel` | preferences | UI |
| S1a | Edit financial inputs | Screen | `settings/inputs` | `SettingsViewModel` | declared inputs | UI |
| S2 | Privacy center | Screen | `settings/privacy` | `PrivacyViewModel` | consents, transmission log | UI |
| S3 | Demo mode | Screen | `settings/demo` | `DemoViewModel` | scenarios, injectors | Data |

The app shell (navigation graph, bottom bar) is wired by the Integration Lead only. See [09 Overall plan](09-overall-plan.md), ownership rules.

## 3. Global rules

- **Bottom navigation:** Cockpit, Plan, Copilot, Activity. Hidden on onboarding, inside sheets, and while the keyboard is open in Copilot.
- **Top bar:** screen title, a data-source chip ("Your data" or "Demo data"), overflow menu with Settings.
- **Back behaviour:** system back dismisses sheets first, then pops the screen. Back from a tab root returns to Cockpit, then exits.
- **Data source chip:** always visible so judges and users know whether data is synthetic.
- **Persistent states on every screen:** loading skeleton, empty, error with retry, offline notice where network matters (Copilot only).
- **Number formatting:** Indian lakh grouping everywhere (see [05](05-frontend-ui-style.md)).

## 4. Onboarding pages

### O1 Welcome
- **Purpose:** state the promise and the privacy stance.
- **Content:** headline "Know where you stand before the month ends." Three privacy lines: stays on your phone, never asks for bank passwords, delete anytime.
- **Actions:** Primary "Get started". Tertiary "I already have an account".
- **States:** none.

### O2 Choose data source
- **Purpose:** pick how data comes in.
- **Content:** three selectable cards: Read bank SMS (Android, recommended), Upload a bank statement (CSV), Try a sample profile (persona picker: Early earner, Mid-career with EMIs, 40s with heavy commitments).
- **Actions:** Primary "Continue" (disabled until a card is chosen).
- **Edge cases:** SMS card hidden on non-Android; sample profile is the default fallback.

### O3 SMS consent
- **Purpose:** explain exactly what is read before the system permission dialog.
- **Content:** "What we read" (bank alerts: amount, date, merchant) vs "What we never read" (personal chats, OTPs, contacts).
- **Actions:** Primary "Allow SMS access" (system dialog). Tertiary "Not now" (back to O2).
- **States:** permission denied (calm explanation, offer CSV or sample), permanently denied (link to system settings).

### O4 About your money
- **Purpose:** place the user on the two axes that drive defaults: income variability and obligation load.
- **Steps:**
  1. How does your money arrive? Fixed salary / Varies month to month / A mix.
  2. What payments are fixed? Multi-select chips: Rent, Home loan, Vehicle EMI, Credit card, Gadget EMI, Insurance, Subscriptions, Mutual fund SIP, None.
  3. Age range (optional): 18-25, 26-38, 39-50, Prefer not to say. Note: "Only used to tune wording."
- **Actions:** Next / Back; progress bar.

### O5 Your safety net
- **Purpose:** collect the planner's hard constraints.
- **Content:** currency field for the minimum cash cushion (chips: 1 month of essentials, 2 months, custom); optional goal (name, amount, by when); expandable loan details per selected EMI (outstanding, rate, EMI, months left); card limit and minimum due for a selected credit card; SIP amount and debit day for a selected SIP.
- **Helper:** "Skip anything you don't know. We'll show how sure we are."
- **Actions:** Primary "Build my picture".

### O6 Building your picture
- **Purpose:** show ingestion progress and set honest expectations.
- **Content:** checklist animating through reading messages (live count), finding repeat payments, estimating income, checking upcoming dues. Ends with a completeness summary such as "1 bank account, 1 card, 2 loans, 1 SIP, 3 repeat payments and 1 income source found. Missing: car loan rate."
- **Actions:** Primary "See my picture".
- **States:** failure (nothing found) offers CSV or sample profile.

## 5. Main app pages

### M1 Cockpit
- **Purpose:** answer "am I okay, and what is coming?"
- **Layout, top to bottom:**
  1. Verdict card: status icon, one-line verdict, range, confidence chip, Primary "See plan".
  2. Health score card: gauge with four component bars (Cash runway, Debt load, Buffer, Spending stability), link "How this is calculated" (M8a).
  3. Consolidated accounts row directly below the health score card: four pills (Cash, Card dues, Loan EMIs, SIP), each with an icon, label and amount. One Observed tag sits on the row. Tapping a pill opens the accounts overview (M10). A pill with no data reads "Add" and opens S1a.
  4. Obligations timeline for the next 30 days with "See all" (Activity > Repeating payments).
  5. 2x2 metric cards: Discretionary runway, Debt pressure, Essentials share, Shortfall risk. Each carries a statement-type tag.
  6. Alert banner when risk crosses the threshold.
  7. Data-completeness card when completeness is below 80%.
- **Actions:** extended FAB "Add" (M9).
- **States:** skeleton, no data ("Choose a data source"), low completeness, alert active.

### M2 Plan and forecast
- **Purpose:** compare ways forward under uncertainty.
- **Layout:**
  1. Period switch (6 months / 8 weeks) and plan switch (Plan A: pay debt faster, Plan B: build buffer first, Status quo).
  2. Fan chart card (P10-P90 band, P50 line, reserve line, "today" marker) with table toggle.
  3. Plan card: ending reserve, interest saved, shortfall risk, trade-off line.
  4. "Why this plan" line stating the risk rule.
  5. Expandable period table: month, income, essentials, debt, SIP, discretionary, ending balance.
  6. Conflict banner when the goal is infeasible.
- **Actions:** Primary "Apply plan", Secondary "Try what-if", icon button "Model report", segmented Compare mode (A and B side by side).
- **States:** computing (skeleton), infeasible (banner), thin history (Low confidence note).

### M3 What-if sheet
- **Purpose:** show the effect of an action before committing.
- **Controls:** sliders for discretionary cut (₹0 to ₹10,000), extra loan prepayment (₹0 to ₹15,000), income change (-50% to +50%), SIP change (pause up to the current amount); one-off expense field.
- **Outputs:** live before and after chips for runway, shortfall risk, interest saved.
- **Actions:** Primary "Apply to plan", Tertiary "Reset". Opens prefilled when launched from a Copilot recommendation.

### M4 Conflict resolution
- **Purpose:** make an infeasible goal understandable and fixable.
- **Content:** a computed statement such as "Your goal of ₹1,00,000 breaks your ₹40,000 cushion in Month 3 by ₹18,200." Option cards: lower the target by ₹X, extend by N months, reduce the cushion by ₹Y (flagged "Not recommended" if below the safe level), and, when an SIP exists, reduce or pause the SIP by ₹Z. Each shows its effect on shortfall risk.
- **Actions:** Primary "Apply proposed relaxation", Secondary "Keep my target and show what breaks".

### M5 Copilot
- **Purpose:** natural-language questions about spending, savings, debt, upcoming pressure and affordability.
- **Content:** suggestion chips; message list; assistant answers as a three-section card (Observed, Predicted, Recommended) with a footer "Numbers calculated on your device".
- **Actions inside an answer:** "Simulate this" (opens M3 prefilled), "Add to plan".
- **States:** thinking (skeleton), AI unavailable (on-device answer with notice), error with retry, long answer collapsed after six lines.

### M6 Activity
- **Tab 1, Transactions:** search, filter chips (All, Income, Food, Travel, Shopping, Bills, Health, Education, Entertainment, Investments, Repeating) and an account selector (All accounts or one), rows grouped by date, FAB "Add".
- **Tab 2, Repeating payments:** header "₹X committed in the next 30 days"; groups This week / Later this month / Next month; each row shows name, amount, due date and detection confidence.
- **States:** empty ("Add data to see your transactions"), filtered empty.

### M6a Transaction detail (sheet)
Merchant, amount, date, category dropdown with "Apply to all from this merchant", "Treat as repeating payment" switch, masked message snippet (stored on device only).

### M6b Obligation detail (sheet)
Name, amount, interval, next due date, detection confidence, evidence (the last matching payments). Actions: Confirm, Edit, Dismiss.

### M7 Model report
- **Purpose:** evidence for judges and for trust.
- **Content:** headline "Forecast vs actual on held-out months"; metrics table for our model, historical-average baseline and recurring-payment baseline (MAE, RMSE, 80% interval coverage, mean interval width); train / validation / test timeline bar; accordion of synthetic-data assumptions.
- **Actions:** Primary "Export evidence pack" (CSV, JSON, PDF), Secondary "Re-run backtest".
- **Rule:** every value is computed from the run. A "Synthetic data" label shows when the source is generated.

### M8a Score explainer and M8b Confidence explainer (sheets)
Plain-language breakdown of how the score or confidence label was derived, with the actual inputs and a "What would improve this" line.

### M9 Add entry (sheet)
Amount (currency field), direction, date, category, note, "Repeats?" with interval. Saves to the ledger and triggers recomputation.

### M10 Accounts overview (sheet)
- **Purpose:** show where the money sits and what each pill on the Cockpit counts.
- **Entry:** tap any pill in the accounts row (M1), or "See all accounts" under the row.
- **Content:** four groups that match the five streams (payment history lives in Activity):
  - Bank accounts: institution, last four digits as "•• 4091", balance and "as of" date. A Watch pill appears when the balance is older than 7 days.
  - Credit cards: limit, unbilled amount, total and minimum due, due date, a utilisation bar.
  - Loans: outstanding, rate, EMI, months left, due day.
  - SIPs: platform, monthly amount, debit day.
  - Footer line: "Cash after card bills" with its type tag.
- **Actions:** per row "Edit details" (opens S1a); Tertiary "Add an account, loan or SIP".
- **States:** empty group ("No SIP found. Add one if you invest monthly."), stale balance, missing detail ("Rate missing. Add it to narrow the forecast").
- **Tags:** each group header carries the Observed tag; values the user typed show "You entered this".
- **Privacy:** institution names and digits are shown here and never sent off the phone.

## 6. Settings pages

### S1 Settings
Profile, financial inputs (S1a), notifications, privacy center (S2), Judge mode switch, about and version.

### S1a Edit financial inputs
Income type and typical amount, minimum cushion, loans (outstanding, rate, EMI, months left), credit cards (limit, minimum due), SIPs (amount, debit day), goals. Saving triggers recomputation and a "What changed" summary.

### S2 Privacy center
- Toggles: SMS access, cloud sync, AI assistance, each with one plain-language line.
- "What left your phone" log: data type and time only.
- "Export my data" (secondary), "Delete my data" (destructive with type-to-confirm).
- Top statement: "Your raw messages never leave this phone." Only claim what the architecture does.

### S3 Demo mode (hidden)
Enabled by the Judge mode switch. While on, the top bar shows "Demo data". Panel: five scenario chips (Stable, Income -30%, Emergency ₹25K, Infeasible target, High uncertainty), injector buttons (surprise bill, bonus income, new recurring payment, pause SIP), seed display, Reset.

## 7. Notifications (system)

Three types only: due soon (three days before an EMI, card due or SIP debit), risk crossed (shortfall chance above 30%), weekly digest. Calm wording, one action each, quiet hours respected, each switchable in Settings.

## 8. State matrix (every data screen)

| State | Treatment |
|---|---|
| Loading | Skeleton blocks matching the final layout |
| Empty | Icon, one-line explanation, one action |
| Error | Plain explanation of what happened and Retry. No apology language |
| Offline (Copilot) | On-device answer plus a notice |
| Low confidence | Low chip, wider band, "add X to narrow this" action |
| Demo | "Demo data" chip, synthetic label on reports |
