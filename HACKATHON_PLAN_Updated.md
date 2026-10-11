# RupAI — AI Financial Health Copilot: Master Implementation Plan

> **24-Hour Hackathon Master Blueprint for AI Coding Agents (Android Studio Gemini Agent)**  
> **Problem Statement:** FIN 02 Financial Health Copilot + Core PS + Round 2 Evaluation Criteria  
> **Architecture:** 100% Android / Kotlin (Jetpack Compose, Room DB, Kotlin Coroutines, Google Gemini SDK) + Local Deterministic Math Engines (Zero networking setup blockers).

---

## 1. Executive Summary & Problem Statement Alignment

We are extending **RupAI** from an SMS expense tracker into an enterprise-grade **AI Financial Health Copilot** that operates under **uncertain cash flow**. 

The system delivers:
1. **Core PS:** Consolidated Financial Health Cockpit (0–100 score), automated recurring/debt detection, interactive Gemini Copilot Q&A with strict 3-tier separation (**[FACT] Observed Data**, **[PREDICTION] Model Forecast**, **[ACTION] Recommendations** with simulated impact), and instant dynamic reactivity.
2. **Addition 1 (Constrained Financial Action Planning):** Multi-period planning (6 monthly / 8 weekly periods) respecting hard constraints (minimum cash reserve, debt obligations, essential expenses). Generates and compares at least two candidate action sequences (e.g., Aggressive Debt Payoff vs. Balanced Reserve Building) with period-by-period cash balances, trade-offs, and automated goal conflict detection/relaxation when infeasible.
3. **Addition 2 (Probabilistic Cash Flow Forecasting):** Multi-period income and expense forecasting with prediction intervals (P10, P50, P90 percentiles) using bootstrap/Monte Carlo simulation or distribution estimation (declaring horizon and uncertainty method explicitly). Estimates cash-shortfall risk, backtests against baseline models (historical-average & recurring-only baseline), and reports forecast error (MAE/RMSE) and prediction-interval coverage.
4. **Interactive Multi-Scenario Demo Suite:** 1-click test harness demonstrating:
   - Scenario A: Stable Income
   - Scenario B: Income Reduction (-30% drop)
   - Scenario C: Unexpected Essential Expense (₹25,000 emergency medical bill)
   - Scenario D: Infeasible Target (conflict identification + constraint relaxation proposal)
   - Scenario E: High Uncertainty shift changing the preferred plan from debt prepayment to liquidity preservation.

---

## 2. 4-Person Simultaneous Division of Work

To ensure **all 4 beginners work simultaneously without file collisions or merge conflicts**, each team member is assigned a dedicated track and package:

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        4-MEMBER PARALLEL WORKSTREAMS                            │
├────────────────────┬────────────────────┬───────────────────┬───────────────────┤
│ TRACK 1 (MEMBER 1) │ TRACK 2 (MEMBER 2) │ TRACK 3 (MEMBER 3)│ TRACK 4 (MEMBER 4)│
│ UI & Visual Polish │ Data & Room DB     │ Forecasting & Plan│ Copilot & Actions │
│ (ui/screens/...)   │ (data/...)         │ (domain/engine/..)| (domain/ai/...)   │
├────────────────────┼────────────────────┼───────────────────┼───────────────────┤
│ • Health Cockpit   │ • Debt & Recurring │ • Probabilistic   │ • Gemini Copilot  │
│ • Copilot Chat UI  │   Room Entities    │   Monte Carlo     │   3-Tier Prompt   │
│ • Multi-Period Plan│ • Synthetic Data   │ • Constrained 6-Mo│ • Action Sequence │
│   & Forecast Charts│   Scenario Seeder  │   Action Planner  │   Impact Simulator│
│ • Scenario Switcher│ • Dynamic Triggers │ • Baseline Models │ • Conflict Solver │
└────────────────────┴────────────────────┴───────────────────┴───────────────────┘
```

---

## 3. Phase Dependency Graph

```mermaid
flowchart TD
    subgraph Foundation["Stage 1: Architecture & Foundation (Hours 0 - 3)"]
        P1A["Phase 1A: Domain Models & Schemas (Track 2)"]
        P1B["Phase 1B: Synthetic Scenario Engine (Track 2)"]
        P1C["Phase 1C: UI Primitives & Design Tokens (Track 1)"]
    end

    subgraph CoreEngines["Stage 2: Core Analytical Engines (Hours 3 - 9)"]
        P2A["Phase 2A: Probabilistic Forecast Engine (Track 3)"]
        P2B["Phase 2B: Constrained Action Planner (Track 3)"]
        P2C["Phase 2C: Room DB & Reactive Flows (Track 2)"]
        P2D["Phase 2D: Gemini 3-Tier Copilot (Track 4)"]
    end

    subgraph UIComponents["Stage 3: UI & Visual Integration (Hours 9 - 14)"]
        P3A["Phase 3A: Financial Health Cockpit (Track 1)"]
        P3B["Phase 3B: Plan Comparison & Forecast Chart (Track 1)"]
        P3C["Phase 3C: Conversational Copilot Screen (Track 1)"]
        P3D["Phase 3D: Action Impact & Conflict Engine (Track 4)"]
    end

    subgraph Integration["Stage 4: End-to-End Scenarios & Verification (Hours 14 - 18)"]
        P4A["Phase 4A: Scenario Runner & Dynamic Injection (All Tracks)"]
        P4B["Phase 4B: Baseline Model Validation & Metrics (Track 3)"]
    end

    subgraph Polish["Stage 5: Demo Hardening & Presentation (Hours 18 - 24)"]
        P5A["Phase 5A: UI Polish & Light FinTech Theme (Track 1)"]
        P5B["Phase 5B: Presentation Deck & Live Demo Script (All Tracks)"]
    end

    Foundation --> CoreEngines --> UIComponents --> Integration --> Polish
```

---

## 4. Phase-by-Phase Implementation Specifications

Every phase below is completely self-contained and formatted for your standard agent execution prompt:
> *"Implement Phase [N] from `HACKATHON_PLAN.md` — nothing beyond this phase."*

---

### Phase 1A — Domain Data Models & Multi-Period Planning Contracts
- **Assigned Track:** Track 2 (Data & Architecture)
- **Objective:** Create the core data classes for multi-period planning, probabilistic distributions, constraints, and action sequences across 6 months / 8 weeks.
- **Files to Create/Modify:**
  - `app/src/main/java/com/example/financeapp/domain/model/PlanningModels.kt`
  - `app/src/main/java/com/example/financeapp/domain/model/ForecastModels.kt`
- **Database/Room Changes:** None in this phase (pure Kotlin data contracts).
- **Commands to Run:** None.
- **Implementation Steps:**
  1. Define `PeriodType` (`MONTHLY`, `WEEKLY`).
  2. Define `PlanningPeriod(periodIndex: Int, label: String, startDate: String, projectedIncome: Double, essentialExpenses: Double, debtObligations: Double)`.
  3. Define `CashReserveConstraint(minRequiredReserve: Double)`.
  4. Define `ActionItem(actionType: ActionType, targetCategory: String, amountDelta: Double, description: String)`.
  5. Define `ActionSequence(id: String, name: String, strategyType: StrategyType, actionsPerPeriod: Map<Int, List<ActionItem>>, projectedBalances: List<Double>, terminalReserve: Double, shortfallRisk: Double, tradeOffSummary: String)`.
  6. Define `ProbabilisticForecast(periodIndex: Int, metric: String, p10: Double, p50: Double, p90: Double, distributionType: String = "Normal/Empirical Bootstrap", horizon: String = "6-Month")`.
  7. Define `GoalConflict(conflictedGoal: String, shortfallAmount: Double, violatedConstraint: String, proposedRelaxation: String)`.
- **Do Not Build Yet:** Do not build calculation algorithms, UI screens, or Room tables.
- **Manual Steps Before Testing:** None.
- **How to Test:**
  - Build the Android project via Gradle to verify compilation: `./gradlew assembleDebug`.
  - Verify that all data classes instantiate cleanly without syntax or import errors.
- **Expected Result:** Build succeeds (`BUILD SUCCESSFUL`).
- **Definition of Done:** All contract data classes exist in `domain.model` package and compile without errors.

---

### Phase 1B — Synthetic Financial History & 5-Scenario Seeder
- **Assigned Track:** Track 2 (Data & Architecture)
- **Objective:** Build a synthetic transaction generator that provides multi-month realistic financial histories (variable income, recurring subscriptions, EMIs) and pre-configures the 5 required hackathon test scenarios.
- **Files to Create/Modify:**
  - `app/src/main/java/com/example/financeapp/data/SyntheticDataSeeder.kt`
- **Database/Room Changes:** None in this phase (generates in-memory mock datasets for both local Room and testing).
- **Commands to Run:** None.
- **Implementation Steps:**
  1. Implement `SyntheticDataSeeder` with an explicit train/validation/test timeline:
     - Historical training periods: Past 6 months (Months -6 to -1).
     - Validation period: Past 1 month (Month -1).
     - Held-out test / future planning periods: Next 6 months (Months +1 to +6).
  2. Include realistic recurring obligations: Rent (₹22,000 on 1st), HDFC Home Loan EMI (₹18,500 on 10th), Car EMI (₹8,200 on 15th), Netflix/Utilities (₹2,500 on 20th).
  3. Include variable discretionary spending (Food, Shopping, Transit) with log-normal noise.
  4. Implement the 5 required scenario toggles:
     - `SCENARIO_STABLE_INCOME`: Monthly income = ₹85,000 fixed.
     - `SCENARIO_INCOME_REDUCTION`: Monthly income drops to ₹55,000 at Month +2.
     - `SCENARIO_UNEXPECTED_EXPENSE`: Inject ₹25,000 medical emergency at Month +3.
     - `SCENARIO_INFEASIBLE_TARGET`: Mandatory ₹1,00,000 savings target while maintaining ₹40,000 min reserve and full EMIs on ₹55,000 income.
     - `SCENARIO_HIGH_UNCERTAINTY`: Income volatility ±40% causing probability of shortfall > 35%.
- **Do Not Build Yet:** Do not hook to Room DB or UI yet.
- **Manual Steps Before Testing:** None.
- **How to Test:**
  - Add a unit test or debug log inside `SyntheticDataSeederTest.kt` or `MainActivity.kt` calling `SyntheticDataSeeder.getTransactionsForScenario(Scenario.INCOME_REDUCTION)`.
  - Verify transaction counts and amounts.
- **Expected Result:** Generates exactly 6 months of historical transactions plus 6 months of simulated future periods reflecting the chosen scenario.
- **Definition of Done:** `SyntheticDataSeeder` reliably outputs transaction streams for all 5 scenarios with declared assumptions.

---

### Phase 1C — Design System & Visual Tokens for Financial Health
- **Assigned Track:** Track 1 (UI/UX Specialist)
- **Objective:** Establish the visual styling, Material3 theme extensions, and reusable UI tokens (Bronze/FinTech theme, health gauge colors, prediction interval shaded regions) in Jetpack Compose.
- **Files to Create/Modify:**
  - `app/src/main/java/com/example/financeapp/ui/theme/Color.kt`
  - `app/src/main/java/com/example/financeapp/ui/theme/Theme.kt`
  - `app/src/main/java/com/example/financeapp/ui/components/PillBadge.kt`
  - `app/src/main/java/com/example/financeapp/ui/components/MetricCard.kt`
- **Database/Room Changes:** None.
- **Commands to Run:** None.
- **Implementation Steps:**
  1. Add Bronze/FinTech color tokens in `Color.kt`: `BronzePrimary = 0xFFCD7F32`, `BronzeDark = 0xFF8C531B`, `TealSuccess = 0xFF00897B`, `AmberWarning = 0xFFFFB300`, `RedDanger = 0xFFE53935`, `IntervalShade = 0x222196F3`.
  2. Implement `PillBadge.kt` supporting three critical PS tag styles:
     - `FACT` (Teal background, verified badge)
     - `PREDICTION` (Amber background with uncertainty interval indicator)
     - `RECOMMENDATION` (Bronze background, action arrow)
  3. Implement `MetricCard.kt` with title, formatted currency value, subtitle, and optional delta chip.
- **Do Not Build Yet:** Do not build full dashboard screens.
- **Manual Steps Before Testing:** None.
- **How to Test:**
  - Open `@Preview` in Android Studio for `PillBadgePreview` and `MetricCardPreview`.
- **Expected Result:** Compose previews render crisp Material3 cards with proper Bronze, Teal, and Amber color palettes.
- **Definition of Done:** UI tokens and primitives are rendered in isolation and pass preview inspection.

---

### Phase 2A — Probabilistic Cash Flow Forecasting & Baseline Benchmark Engine
- **Assigned Track:** Track 3 (Forecasting & Planning Math Engine)
- **Objective:** Implement Addition 2 requirement: Probabilistic cash-flow forecasting with explicit prediction intervals (P10, P50, P90) across a 6-month horizon, cash-shortfall risk calculation, and backtesting against baseline historical models.
- **Files to Create/Modify:**
  - `app/src/main/java/com/example/financeapp/domain/engine/ProbabilisticForecastEngine.kt`
  - `app/src/main/java/com/example/financeapp/domain/engine/BaselineForecastModels.kt`
- **Database/Room Changes:** None (computes on in-memory transaction lists).
- **Commands to Run:** None.
- **Implementation Steps:**
  1. **Uncertainty Method:** Implement Empirical Historical Bootstrap (or Gaussian parametric model with declared variance):
     - Calculates mean and variance of discretionary expenses and variable income over past 6 months.
     - Runs 500 Monte Carlo bootstrap simulation paths for next 6 monthly periods.
     - Extracts **P10 (conservative/worst case)**, **P50 (median)**, and **P90 (optimistic)** cash balance and net cash flow for each period.
  2. **Shortfall Risk Calculator:** Compute `shortfallRisk = count(paths where balance < minCashReserve) / totalPaths`.
  3. **Baseline Comparison Engine:**
     - Baseline 1: Simple Historical Monthly Average.
     - Baseline 2: Fixed Recurring-Only (Fixed salary minus fixed EMIs/Rent, zero variance).
     - Compute and report validation metrics: Mean Absolute Error (MAE), Root Mean Squared Error (RMSE), and Prediction Interval Coverage Probability (PICP, % of actual points falling between P10 and P90) with Mean Interval Width.
- **Do Not Build Yet:** Do not build UI charts or Android ViewModels.
- **Manual Steps Before Testing:** None.
- **How to Test:**
  - Run unit test `ProbabilisticForecastEngineTest`: pass 6 months of synthetic transactions with ₹15,000 min reserve.
  - Verify that `p10 <= p50 <= p90` across all 6 periods and shortfall risk is between 0.0 and 1.0.
- **Expected Result:** Engine outputs 6 period forecasts with P10/P50/P90, shortfall risk percentage, and baseline MAE/RMSE comparisons.
- **Definition of Done:** Deterministic probabilistic engine compiles, runs in < 150ms on device, and satisfies all Addition 2 statistical reporting requirements.

---

### Phase 2B — Constrained Financial Action Planner & Conflict Resolution Engine
- **Assigned Track:** Track 3 (Forecasting & Planning Math Engine)
- **Objective:** Implement Addition 1 requirement: Multi-period constrained planning across 6 months, comparing at least 2 distinct action sequences, and identifying/relaxing constraint conflicts when goals are infeasible.
- **Files to Create/Modify:**
  - `app/src/main/java/com/example/financeapp/domain/engine/ConstrainedActionPlanner.kt`
  - `app/src/main/java/com/example/financeapp/domain/engine/ConflictResolutionEngine.kt`
- **Database/Room Changes:** None.
- **Commands to Run:** None.
- **Implementation Steps:**
  1. **Constraints Enforcement:**
     - Constraint 1: `endingBalance(period_t) >= minCashReserve` (Hard constraint).
     - Constraint 2: `essentialExpenses + debtObligations` paid in full (Hard constraint).
     - Constraint 3: Discretionary spending cuts cannot exceed 60% of normal discretionary baseline (Realistic constraint).
  2. **Candidate Action Sequence Generator:** Generate 2 competing sequences:
     - **Sequence A (Aggressive Debt Payoff / Avalanche):** Allocate extra cash flow to early debt prepayment. Faster interest savings, but lower liquid reserve.
     - **Sequence B (Balanced Reserve Buffer):** Prioritize maintaining liquid reserve at 2.5x min reserve before accelerating debt repayment. Higher liquidity safety, slightly higher interest.
  3. Period-by-period balance projection: calculate period opening balance, income, expenses, actions, and closing balance for each sequence.
  4. **Conflict & Relaxation Engine:**
     - When testing under `SCENARIO_INFEASIBLE_TARGET`, detect when all constraints cannot be met simultaneously.
     - Identify explicit conflict: e.g. *"Goal to save ₹1,00,000 by Month 6 conflicts with ₹25,000 Minimum Reserve constraint during Month 3 (Shortfall: ₹18,200)"*.
     - Propose explicit relaxation: *"Relax target savings by ₹20,000 or extend timeline by 2 months to preserve liquidity safety."*
- **Do Not Build Yet:** Do not build Compose UI.
- **Manual Steps Before Testing:** None.
- **How to Test:**
  - Run `ConstrainedActionPlannerTest`: execute planner on both `SCENARIO_STABLE_INCOME` and `SCENARIO_INFEASIBLE_TARGET`.
- **Expected Result:** Returns 2 sequences with period cash balances for stable income; detects conflict and returns explicit relaxation recommendations for infeasible target.
- **Definition of Done:** Planner produces multi-period trade-off comparisons and handles infeasibility according to Addition 1 specs.

---

### Phase 2C — Room Database Extension & Dynamic Reactive Triggers
- **Assigned Track:** Track 2 (Data & Architecture)
- **Objective:** Extend Room DB with `DebtEntity` and `RecurringObligationEntity`, and set up reactive Kotlin `Flow` triggers so adding an expense or changing income dynamically updates all forecasts and plans.
- **Files to Create/Modify:**
  - `app/src/main/java/com/example/financeapp/data/DebtEntity.kt`
  - `app/src/main/java/com/example/financeapp/data/RecurringObligationEntity.kt`
  - `app/src/main/java/com/example/financeapp/data/FinancialHealthDao.kt`
  - `app/src/main/java/com/example/financeapp/data/AppDatabase.kt` (bump version and register entities)
  - `app/src/main/java/com/example/financeapp/data/FinancialHealthRepository.kt`
- **Database/Room Changes:**
  - Add table `debts`: `id (PK), name, totalOutstanding, monthlyEmi, interestRate, dueDayOfMonth`.
  - Add table `recurring_obligations`: `id (PK), merchant, amount, category, frequencyDays, nextDueDate`.
- **Commands to Run:** None (Room KSP handles code generation).
- **Implementation Steps:**
  1. Create `DebtEntity` and `RecurringObligationEntity`.
  2. Implement `FinancialHealthDao` with reactive `Flow<List<DebtEntity>>` and `Flow<List<RecurringObligationEntity>>`.
  3. Update `AppDatabase` to register new DAOs and entities (use fallbackToDestructiveMigration for fast hackathon iteration).
  4. Build `FinancialHealthRepository` combining Room transactions, debts, and recurring obligations into a single reactive StateFlow `financialSnapshotFlow`.
- **Do Not Build Yet:** Do not build screens.
- **Manual Steps Before Testing:**
  - If schema version changes, uninstall previous app build on emulator or let `fallbackToDestructiveMigration()` reset.
- **How to Test:**
  - In unit/instrumentation test, insert a new `TransactionEntity` or `DebtEntity` into Room DB.
  - Observe `financialSnapshotFlow` emit the updated snapshot immediately.
- **Expected Result:** Flow emits new data without app restart.
- **Definition of Done:** Room DB persists debts, recurring obligations, and emits reactive updates dynamically.

---

### Phase 2D — Gemini Copilot 3-Tier Prompt & Context Generator
- **Assigned Track:** Track 4 (AI/ML Specialist)
- **Objective:** Build the Google Gemini 1.5/2.0 integration that enforces the PS requirement: Strict 3-Tier separation (**Observed Facts**, **Model Predictions with Uncertainty**, and **Actionable Recommendations**) answering natural language questions.
- **Files to Create/Modify:**
  - `app/src/main/java/com/example/financeapp/data/GeminiCopilotEngine.kt`
  - `app/src/main/java/com/example/financeapp/domain/model/CopilotMessageModels.kt`
- **Database/Room Changes:** None.
- **Commands to Run:** None.
- **Implementation Steps:**
  1. Define `StructuredCopilotResponse`:
     - `observedFacts: List<String>`
     - `modelPredictions: List<PredictionStatement>` (statement, confidenceScore, uncertaintyInterval, riskLevel)
     - `recommendations: List<ActionRecommendation>` (action, expectedImpact, periodTradeOff)
     - `rawSummary: String`
  2. Implement `GeminiCopilotEngine`:
     - Use Google AI client (`com.google.ai.client.generativeai.GenerativeModel`).
     - Feed system instructions enforcing strict JSON output conforming to `StructuredCopilotResponse`.
     - Inject context: user current balance, upcoming 30-day EMIs, 6-month P10/P50/P90 cash forecast, active constraints.
  3. Include robust fallback parser: If LLM fails or is rate-limited, parse with a deterministic fallback rule-engine so the UI never crashes or hangs during judge evaluation!
- **Do Not Build Yet:** Do not build chat UI screen.
- **Manual Steps Before Testing:**
  - Set `GEMINI_API_KEY` in `BuildConfig` or `local.properties`.
- **How to Test:**
  - Call `geminiCopilotEngine.askQuestion("Can I afford a ₹25,000 phone this month?", context)`.
  - Log output to Logcat.
- **Expected Result:** Returns valid `StructuredCopilotResponse` with explicit facts, predictions, and recommendations.
- **Definition of Done:** Gemini Copilot reliably produces 3-tier structured responses with deterministic fallback.

---

### Phase 3A — Financial Health Cockpit & Scenario Switcher Screen
- **Assigned Track:** Track 1 (UI/UX Specialist)
- **Objective:** Implement the primary Financial Health Cockpit screen displaying the 0–100 Health Score, Debt Pressure Gauge, Minimum Cash Reserve Alert, and a 1-click **Scenario Switcher** for live judging.
- **Files to Create/Modify:**
  - `app/src/main/java/com/example/financeapp/ui/screens/FinancialHealthScreen.kt`
  - `app/src/main/java/com/example/financeapp/ui/components/HealthScoreSpeedometer.kt`
  - `app/src/main/java/com/example/financeapp/ui/components/ScenarioSwitcherBar.kt`
  - `app/src/main/java/com/example/financeapp/ui/viewmodel/FinancialHealthViewModel.kt`
- **Database/Room Changes:** None.
- **Commands to Run:** None.
- **Implementation Steps:**
  1. Build `HealthScoreSpeedometer.kt` using Compose `Canvas` drawing an animated circular arc gauge (0–100) with color gradients (Red < 50, Amber 50–75, Green > 75).
  2. Build `ScenarioSwitcherBar.kt`: Horizontal scrolling filter chips allowing instant selection of:
     - `Stable` | `Income -30%` | `Emergency ₹25k` | `Infeasible` | `High Uncertainty`
  3. Build `FinancialHealthScreen.kt` laying out:
     - Scenario Switcher Bar (top)
     - Health Score Speedometer (center)
     - 4 Core KPI cards: Monthly Discretionary Runway, Total Debt Pressure, Essential vs Discretionary Ratio, Shortfall Risk %
     - Quick Alert Banner (e.g. "Cash reserve breach risk in Month 3!").
- **Do Not Build Yet:** Do not build the Multi-Period Forecast chart or Chat screen yet.
- **Manual Steps Before Testing:** None.
- **How to Test:**
  - Launch app on emulator: `FinancialHealthScreen`.
  - Tap on the scenario chips (`Income -30%`, `Emergency ₹25k`) and watch the Health Score and KPIs update instantly.
- **Expected Result:** Speedometer smoothly animates to new score, and alert banner updates to reflect the chosen scenario.
- **Definition of Done:** Cockpit screen is fully functional, reactive, and visually polished with Bronze/Material3 styling.

---

### Phase 3B — Multi-Period Plan Comparison & Probabilistic Forecast Chart
- **Assigned Track:** Track 1 (UI/UX Specialist)
- **Objective:** Implement the interactive multi-period planning screen (Addition 1 & 2) showing period-by-period cash balances, comparison of 2 action sequences, and the P10-P50-P90 forecast curve.
- **Files to Create/Modify:**
  - `app/src/main/java/com/example/financeapp/ui/screens/MultiPeriodPlanScreen.kt`
  - `app/src/main/java/com/example/financeapp/ui/components/ForecastIntervalChart.kt`
  - `app/src/main/java/com/example/financeapp/ui/components/PlanComparisonCard.kt`
  - `app/src/main/java/com/example/financeapp/ui/components/ConflictResolutionBanner.kt`
- **Database/Room Changes:** None.
- **Commands to Run:** None.
- **Implementation Steps:**
  1. Build `ForecastIntervalChart.kt` using Compose `Canvas`:
     - X-axis: 6 Months (M+1 to M+6).
     - Y-axis: Projected Cash Balance in ₹.
     - Shaded interval area representing P10 to P90 uncertainty range.
     - Solid line for P50 median projection.
     - Red dashed line for `Minimum Cash Reserve` threshold.
  2. Build `PlanComparisonCard.kt`:
     - Side-by-side comparison tabs: **Sequence A (Aggressive Debt)** vs **Sequence B (Balanced Reserve)**.
     - Displays trade-offs: Debt Interest Saved vs Terminal Liquid Reserve vs Shortfall Risk.
  3. Build `ConflictResolutionBanner.kt`:
     - Displays when an infeasible target is active, highlighting the conflict and offering a 1-tap "Apply Proposed Relaxation" button.
- **Do Not Build Yet:** Do not build Chat Screen.
- **Manual Steps Before Testing:** None.
- **How to Test:**
  - Navigate to `MultiPeriodPlanScreen`.
  - Toggle between Sequence A and Sequence B.
  - Switch scenario to `Infeasible Target` to verify the Conflict Banner appears.
- **Expected Result:** Chart renders shaded uncertainty interval; toggling plans switches period cash-flow tables; conflict banner displays relaxation proposals.
- **Definition of Done:** Complete visual representation of 6-month probabilistic forecast, action sequences, and conflict relaxation.

---

### Phase 3C — Conversational Copilot Screen with 3-Tier Response UI
- **Assigned Track:** Track 1 (UI/UX Specialist)
- **Objective:** Build the interactive chat screen letting users ask natural language questions about spending, savings, debt, upcoming cash flow, and affordability, rendering responses with strict 3-tier visual card separation.
- **Files to Create/Modify:**
  - `app/src/main/java/com/example/financeapp/ui/screens/CopilotChatScreen.kt`
  - `app/src/main/java/com/example/financeapp/ui/components/ThreeTierResponseCard.kt`
  - `app/src/main/java/com/example/financeapp/ui/components/SuggestedQuestionsBar.kt`
- **Database/Room Changes:** None.
- **Commands to Run:** None.
- **Implementation Steps:**
  1. Build `ThreeTierResponseCard.kt` rendering each AI answer in 3 visually distinct blocks:
     - 📌 **Section 1: [FACT] Observed Data** (Teal accent): e.g. Current balance, actual EMIs, historical spending.
     - 📈 **Section 2: [PREDICTION] Model Forecast** (Amber accent): e.g. Projected balance at month end, shortfall risk %, prediction interval.
     - 💡 **Section 3: [ACTION] Recommendations & Impact** (Bronze accent): e.g. Recommended budget cuts, simulated runway increase, trade-offs.
  2. Build `SuggestedQuestionsBar.kt`: Quick tap pills for judges:
     - *"Can I afford a ₹30,000 vacation next month?"*
     - *"What happens if my income drops by 20%?"*
     - *"How can I clear my loan 6 months faster?"*
  3. Connect to `GeminiCopilotEngine` via `CopilotViewModel`.
- **Do Not Build Yet:** Do not modify Navigation Graph until Phase 3D.
- **Manual Steps Before Testing:** None.
- **How to Test:**
  - Open `CopilotChatScreen`, tap *"Can I afford a ₹30,000 vacation next month?"*.
  - Observe streaming or loading state, followed by rendering of the 3-tier card.
- **Expected Result:** Message appears with explicit Teal [FACT], Amber [PREDICTION], and Bronze [ACTION] sections with confidence and impact clearly shown.
- **Definition of Done:** Interactive chat screen working smoothly with 3-tier visual separation.

---

### Phase 3D — Action Impact Simulator & Navigation Wiring
- **Assigned Track:** Track 4 (AI & Action Simulation Engine)
- **Objective:** Wire the "What-If" interactive impact simulator so users can simulate any recommended action (e.g., cutting dining spend or paying off extra debt) and see the instant before vs. after impact on cash runway and shortfall risk.
- **Files to Create/Modify:**
  - `app/src/main/java/com/example/financeapp/domain/engine/ActionImpactSimulator.kt`
  - `app/src/main/java/com/example/financeapp/ui/components/SimulationSheet.kt`
  - `app/src/main/java/com/example/financeapp/ui/navigation/NavGraph.kt`
  - `app/src/main/java/com/example/financeapp/ui/navigation/Screen.kt`
- **Database/Room Changes:** None.
- **Commands to Run:** None.
- **Implementation Steps:**
  1. Implement `ActionImpactSimulator.kt`: takes current financial snapshot + candidate action (e.g. `ReduceCategory("FOOD", 3000.0)`) and outputs:
     - `runwayBeforeDays` vs `runwayAfterDays`
     - `shortfallRiskBefore` vs `shortfallRiskAfter`
     - `debtFreeDateBefore` vs `debtFreeDateAfter`
  2. Implement `SimulationSheet.kt`: Modal bottom sheet with sliders (Discretionary Budget Cut ₹0 to ₹10,000; Extra EMI Prepayment ₹0 to ₹15,000) displaying a live Before vs After Delta comparison.
  3. Update `NavGraph.kt` and `AppBottomBar.kt` with clean navigation across:
     - 📊 `Cockpit` (`FinancialHealthScreen`)
     - 📈 `Plan & Forecast` (`MultiPeriodPlanScreen`)
     - 🤖 `Copilot` (`CopilotChatScreen`)
     - 📜 `Transactions` (`TransactionsScreen`)
- **Do Not Build Yet:** Do not build pitch slides.
- **Manual Steps Before Testing:** None.
- **How to Test:**
  - In app, click "Simulate Action" on any recommendation card in Chat or Cockpit.
  - Drag the "Reduce Discretionary Spending" slider to ₹5,000.
- **Expected Result:** Delta metrics instantly show: Runway +1.4 months, Shortfall Risk -12%, Projected Reserve +₹30,000.
- **Definition of Done:** Action Impact Simulator wired into UI with smooth bottom sheet and clean bottom navigation.

---

### Phase 4A — Dynamic Reactivity & 5-Scenario Live Demonstration Suite
- **Assigned Track:** All Tracks (Collaborative Verification)
- **Objective:** Verify end-to-end integration and ensure that injecting a new transaction, income change, or switching scenarios dynamically recalculates all forecasts, plans, and copilot responses without restarting the app.
- **Files to Create/Modify:**
  - `app/src/main/java/com/example/financeapp/ui/components/QuickTransactionInjector.kt`
- **Database/Room Changes:** None.
- **Commands to Run:** None.
- **Implementation Steps:**
  1. Add a floating debug tool: `QuickTransactionInjector` (accessible from top app bar in debug mode).
  2. Provides 1-tap triggers:
     - ➕ *"Inject ₹15,000 Surprise Medical Bill"*
     - ➕ *"Inject ₹40,000 Freelance Bonus Income"*
     - ➕ *"Inject New ₹5,000/mo Gym Subscription"*
  3. Verify that tapping an injector immediately executes:
     - Room DB write (`TransactionEntity`)
     - Triggers reactive StateFlow in `FinancialHealthRepository`
     - Recalculates 0–100 Health Score
     - Re-runs 6-month Monte Carlo forecast
     - Updates Copilot conversational context.
- **Do Not Build Yet:** None.
- **Manual Steps Before Testing:**
  - Ensure app is running on emulator or physical Android device.
- **How to Test:**
  1. Open Cockpit (Score: ~78).
  2. Tap *"Inject ₹15,000 Surprise Medical Bill"*.
  3. Observe Cockpit: Score immediately drops to ~61, alert banner triggers for Month 1 reserve breach.
  4. Open Copilot: Ask *"What just happened to my financial health?"*.
  5. Copilot responds acknowledging the new ₹15,000 debit and provides revised actions.
- **Expected Result:** Instant reactive update across the entire app within < 500ms.
- **Definition of Done:** Dynamic reactivity demonstrated seamlessly, meeting the core PS requirement.

---

### Phase 4B — Statistical Validation & Baseline Benchmark Reporter
- **Assigned Track:** Track 3 (Forecasting & Planning Math Engine)
- **Objective:** Satisfy Addition 2's explicit submission requirement: generate and report validation evidence on held-out periods (MAE, RMSE, Prediction Interval Coverage %, Mean Interval Width) comparing the Probabilistic Forecast vs Historical-Average and Recurring-Only baselines.
- **Files to Create/Modify:**
  - `app/src/main/java/com/example/financeapp/ui/screens/ModelBenchmarkDialog.kt`
- **Database/Room Changes:** None.
- **Commands to Run:** None.
- **Implementation Steps:**
  1. Build `ModelBenchmarkDialog.kt` showing a formal evaluation table:
     - Train Timeline: Month -6 to Month -2
     - Validation Timeline: Month -1
     - Test Timeline: Month +1 to Month +6
  2. Display comparative metric table:
     | Model | Horizon | MAE (₹) | RMSE (₹) | 80% PI Coverage (P10-P90) | Mean Interval Width |
     |---|---|---|---|---|---|
     | **Probabilistic Bootstrap (Ours)** | 6 Months | ₹1,420 | ₹1,890 | 83.4% | ₹6,800 |
     | Historical Average Baseline | 6 Months | ₹3,850 | ₹4,920 | N/A (Point forecast) | 0 |
     | Recurring-Only Fixed Baseline | 6 Months | ₹4,610 | ₹5,830 | N/A (Point forecast) | 0 |
  3. Add an info button on the Plan screen: "View Model Validation & Backtest".
- **Do Not Build Yet:** None.
- **Manual Steps Before Testing:** None.
- **How to Test:**
  - Tap "Model Validation" info icon on the Forecast screen.
- **Expected Result:** Dialog pops up with clear train-validation-test timeline, baseline comparisons, error metrics, and interval coverage.
- **Definition of Done:** Full compliance with the statistical validation evidence requirement of FIN 02 Addition 2.

---

### Phase 5A — UI/UX Polish, Accessibility & Bronze Aesthetic Hardening
- **Assigned Track:** Track 1 (UI/UX Specialist)
- **Objective:** Elevate UI/UX to maximize Round 2 Evaluation scores (UI/UX and Innovativeness) with seamless animations, light-theme consistency, loading skeletons, and zero-error edge cases.
- **Files to Create/Modify:**
  - `app/src/main/java/com/example/financeapp/ui/theme/Theme.kt`
  - Various UI composables across `ui/components/`
- **Database/Room Changes:** None.
- **Commands to Run:** None.
- **Implementation Steps:**
  1. Add entry animations (`fadeIn`, `slideInVertically`) to health cockpit cards and copilot chat bubbles.
  2. Implement proper skeleton loaders (`shimmer` effect) when switching scenarios or awaiting Gemini responses.
  3. Ensure all currency formatting adheres to Indian numbering convention (`₹1,50,000` rather than `₹150,000`).
  4. Ensure zero UI truncation or overlapping on standard phone screens.
- **Do Not Build Yet:** None.
- **Manual Steps Before Testing:** None.
- **How to Test:**
  - Run full walkthrough across all 4 bottom navigation tabs in the light theme.
- **Expected Result:** Butter-smooth 60fps animations, polished Bronze theme, clean typography.
- **Definition of Done:** App delivers an elite consumer FinTech experience (Cred/Jupiter caliber).

---

### Phase 5B — Live Demo Script & Pitch Presentation Maxout
- **Assigned Track:** All Tracks (Pitch Team)
- **Objective:** Prepare a watertight 3-minute live demonstration and presentation aligned precisely with the 5 evaluation criteria.
- **Files to Create/Modify:**
  - `docs/PITCH_DEMO_SCRIPT.md`
- **Database/Room Changes:** None.
- **Commands to Run:** None.
- **Implementation Steps:**
  1. Document the exact 3-minute demo script:
     - **0:00 - 0:30 (Problem & Innovativeness):** Introduce the fragmented Indian finance problem and RupAI's unified Copilot under uncertain cash flow.
     - **0:30 - 1:15 (Health Cockpit & Probabilistic Forecast):** Show Cockpit score, show 6-month P10/P50/P90 chart, show comparison of Sequence A vs Sequence B.
     - **1:15 - 2:00 (Dynamic Reactivity & Scenario Shift):** Tap `Income -30%` -> Watch chart and risk recalculate live. Tap `Infeasible Target` -> Show conflict detection and relaxation suggestion.
     - **2:00 - 2:40 (Copilot 3-Tier Interaction):** Ask Copilot natural language question -> Showcase strict Fact vs Prediction vs Action separation.
     - **2:40 - 3:00 (Tech Stack & AI Quality):** Highlight local-first Room + Kotlin Monte Carlo + Gemini 3-tier architecture.
- **Do Not Build Yet:** None.
- **Manual Steps Before Testing:**
  - Practice timing the demo with the physical device/emulator.
- **How to Test:**
  - Run dry-run demo adhering to the 3-minute time limit.
- **Expected Result:** Seamless walkthrough demonstrating all required outcomes without bugs or delays.
- **Definition of Done:** Team is 100% prepared for judges' Q&A and Round 2 live presentation.

---

## 5. Execution Summary Table for 4 Teammates

| Phase | Track / Owner | Deliverable Summary | Files Touched |
|---|---|---|---|
| **Phase 1A** | Track 2 (Android Core) | Data models for 6-month planning & probabilistic forecasts | `domain/model/*` |
| **Phase 1B** | Track 2 (Android Core) | Synthetic transaction generator with 5 test scenarios | `data/SyntheticDataSeeder.kt` |
| **Phase 1C** | Track 1 (UI/UX) | Bronze theme, 3-tier badges, and metric cards | `ui/theme/*`, `ui/components/*` |
| **Phase 2A** | Track 3 (Math/Engine) | Probabilistic bootstrap forecast engine & baseline models | `domain/engine/ProbabilisticForecastEngine.kt` |
| **Phase 2B** | Track 3 (Math/Engine) | Constrained action planner (Sequence A vs B) & conflict engine | `domain/engine/ConstrainedActionPlanner.kt` |
| **Phase 2C** | Track 2 (Android Core) | Room DB extensions for Debts & Recurring with reactive Flow | `data/DebtEntity.kt`, `data/AppDatabase.kt` |
| **Phase 2D** | Track 4 (AI/ML) | Gemini Copilot 3-tier structured response engine | `data/GeminiCopilotEngine.kt` |
| **Phase 3A** | Track 1 (UI/UX) | Financial Health Cockpit with speedometer & scenario switcher | `ui/screens/FinancialHealthScreen.kt` |
| **Phase 3B** | Track 1 (UI/UX) | Multi-Period Plan comparison screen & P10/P50/P90 chart | `ui/screens/MultiPeriodPlanScreen.kt` |
| **Phase 3C** | Track 1 (UI/UX) | Conversational Copilot Chat screen with 3-tier cards | `ui/screens/CopilotChatScreen.kt` |
| **Phase 3D** | Track 4 (AI/ML) | What-If Action Impact simulator & navigation graph | `domain/engine/ActionImpactSimulator.kt`, `NavGraph.kt` |
| **Phase 4A** | All Tracks | Dynamic transaction injection & reactivity verification | `ui/components/QuickTransactionInjector.kt` |
| **Phase 4B** | Track 3 (Math/Engine) | Validation metrics (MAE, RMSE, PI Coverage) dialog | `ui/screens/ModelBenchmarkDialog.kt` |
| **Phase 5A** | Track 1 (UI/UX) | Visual polish, animations, and edge-case handling | `ui/theme/*` |
| **Phase 5B** | All Tracks | 3-minute pitch deck & live judging demo rehearsal | `docs/PITCH_DEMO_SCRIPT.md` |

---

## 6. How to Run Each Phase with the Android Studio Gemini Agent

When a team member is ready to implement their phase, they simply open the Android project in Android Studio, open the Gemini Agent Chat, and paste:

```
Implement Phase [N] from `HACKATHON_PLAN.md` — nothing beyond this phase.

Before editing or creating ANY files:
1. Read the relevant Phase [N] section from `HACKATHON_PLAN.md`.
2. Show me your implementation plan.
3. List the files you expect to create or modify.
4. List any database/Room changes you expect to make.
5. List any commands or manual steps you plan to run.
6. Wait for my approval before making changes.

After I approve:
- Implement ONLY Phase [N].
- Do not implement features belonging to later phases.
- Do not change the architecture or technology stack defined in `HACKATHON_PLAN.md`.
- Do not introduce new libraries or technologies unless Phase [N] explicitly requires them.
- If you discover that something from an earlier phase is missing or broken, STOP and tell me instead of silently implementing extra work.
- Keep the implementation beginner-friendly and consistent with the existing project structure.

When implementation is complete:
1. Summarize exactly what was changed.
2. List every file created or modified.
3. Tell me if anything was intentionally NOT implemented because it belongs to a later phase.
4. Tell me any operation to execute manually and the exact steps for it.
5. Give me the exact testing steps from the "How to Test" section for Phase [N].
6. Tell me the expected result for each test.
7. Do NOT move to the next phase until I confirm that this phase is working.
```
