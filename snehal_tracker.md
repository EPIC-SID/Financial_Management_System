# Snehal Tracker: Track 2 (Data and Persistence)

**Owner:** Snehal · **Branch:** `Snehal` · **Source of truth:** `md files/09-overall-plan.md` (phases) and `md files/06-backend.md` (specs)

**Update rule:** this file is updated only after Snehal has checked and verified a change. Implementation happens first, verification second, tracker update last.

**Status legend:** `Not started` · `Old version exists` (built against `HACKATHON_PLAN_Main.md`, must be reworked to the new contract) · `In progress` · `Awaiting verification` · `Verified`

---

## 1. Track 2 ownership (from 09, section 5)

- `data/**` except `data/remote/`
- `domain/model/` (T2 is contract steward; other tracks request changes through T2)
- `domain/detect/`
- `domain/engine/VerdictEngine.kt`, `domain/engine/ConsolidatedView.kt`
- Phases: **1A, 1B, 2C, 2E, 4A (Demo mode data)**, plus the ingest side of 3E

Not mine (ask the owner instead): `app/build.gradle.kts`, `NavGraph.kt`, `data/local/AppDatabase.kt`, `AppContainer` (Integration Lead) · `HealthScore.kt` and all `ui/**` (T1) · forecast, planner and evaluator engines (T3) · `domain/ai/`, `data/remote/`, `ConflictResolver.kt`, `ImpactSimulator.kt` (T4)

---

## 2. Phase status

### 1A. Domain contracts (Must, hours 0-1). Status: `Verified` (freeze announcement pending)
Spec: 06 section 3.1. Files: `domain/model/PlanningModels.kt`, `ForecastModels.kt`, `AccountModels.kt`, `FinancialSnapshot.kt`, `CopilotModels.kt`

- [x] Enums `Direction`, `Source`, `PeriodType`, `AccountType`
- [x] `Txn`, `Liability`, `Account`, `Investment`, `RecurringItem`
- [x] `DeclaredInputs`, `Goal`, `MonthlyAggregate`
- [x] `FinancialSnapshot` (new shape: `asOf`, `cash`, accounts, liabilities, investments, recurring, history, declared, completeness, missing; no stored score)
- [x] `ConsolidatedView`
- [x] `ProbabilisticForecast` (new fields: `method`, `horizon`, `intervalLevel`)
- [x] `PlanningPeriod`, `ActionSequence`, `GoalConflict`, `Relaxation` (new shapes)
- [x] `CopilotModels.kt`: response schema from 06 section 13.3 only; `ResultBundle` waits for T4's proposal
- [x] No `android.*` imports; `assembleDebug` passes
- [ ] Contract announced as frozen (team stand-up)

### 1B. Synthetic data and scenarios (Must, hours 1-3). Status: `Old version exists`
Spec: 06 sections 12.1-12.3. File: `data/ingest/SyntheticDataSeeder.kt`. Test: `SyntheticDataSeederTest`

- [ ] Personas A, B, C, plus B-sparse
- [ ] 30 months: train 1-18 (seed A), validation 19-24 (seed B), test 25-30 (seed C)
- [ ] Income processes: fixed with bonus months; lognormal AR(1) with payment slips
- [ ] Recurring payments with ±2 day jitter; lognormal variable spend with festival uplift; optional Poisson shocks
- [ ] Account sets per persona (bank, card, loans, SIP) from 12.2a
- [ ] Raw transactions only (card-use alerts, statement alerts, bill payments, EMI debits, SIP debits), so detection is real
- [ ] Opening balance back-solved so persona B shows ₹45,200 on the demo date (month 25)
- [ ] Persona B baseline surplus positive (target ₹5,000-₹8,000)
- [ ] Five scenario overlays on the test window (A stable, B income ₹55,000 from test month 2, C ₹25,000 bill in test month 3, D infeasible, E ±40% volatility)
- [ ] Tests: same seed identical; windows do not overlap; 30 months; balances reconcile with the ledger; persona B surplus positive
- [ ] Hand persona B transactions to T3 (hour 3)

### 2C. Snapshot, consolidated view, verdict (Must, hours 3-9). Status: `Old version exists` (repository only)
Spec: 06 sections 7.1-7.4. Files: `data/FinancialHealthRepository.kt`, `domain/engine/VerdictEngine.kt`, `domain/engine/ConsolidatedView.kt`. Tests: `FinancialHealthRepositoryTest`, `ConsolidatedViewTest`

- [ ] Build `FinancialSnapshot` from the ledger and declared inputs
- [ ] Completeness score and `missing` list (weights in 7.1)
- [ ] `ConsolidatedViewBuilder` (pure function; card dues never subtracted from cash; SIP excluded from DTI)
- [ ] `VerdictEngine`: 30-day daily simulation, "fine until" and "tight on" dates, shortfall chance
- [ ] Consume T1's `HealthScore.kt` (do not write it)
- [ ] `StateFlow<FinancialSnapshot>` with `switchScenario`, `injectTransaction`, `updateDeclared`
- [ ] Hand snapshot and `ConsolidatedView` sample JSON to T1 (hour 3)
- [ ] Hand working repository to T1 and T4 (hour 9)

### 2E. Recurring, income, liability, SIP detection (Must, hours 3-9). Status: `Not started`
Spec: 06 sections 5 and 6. Files: `domain/detect/RecurringDetector.kt`, `IncomeDetector.kt`, `LiabilityExtractor.kt`, `InvestmentDetector.kt`. Tests: `RecurringDetectorTest`, `InvestmentDetectorTest`

- [ ] Recurring detector (median gap snapped to 7/14/30/90/365, amount CV, confidence formula, threshold 0.6)
- [ ] Income detector (salary vs irregular; declared type overrides detected)
- [ ] Liability extractor (cards, EMIs, declared loans; gaps go to `missing`)
- [ ] SIP detector (platform allow-list or NACH/ECS/SIP keywords; CV ≤ 0.05)
- [ ] Ledger rules: transfers, card-bill two-view rule, refunds, cashback, SIP not counted as spending
- [ ] Detects rent, EMIs, SIPs and jittered utilities on all personas, with no false positives on variable spend

### 4A. Demo mode data and reactivity (Must, hours 14-18). Status: `Old version exists` (partial)
- [ ] Five scenario chips backed by the seeder overlays
- [ ] Injectors: surprise bill, bonus income, new recurring payment, pause SIP
- [ ] "What changed" diff between runs (06 section 9.6)

### Should / Could (only if time; see cut order in 09 section 2)
- [ ] 3E onboarding ingest side (sample profile first, then SMS and CSV)
- [ ] Room tables for the new entities in `data/local/` (registration in `AppDatabase.kt` is the Integration Lead's)
- [ ] `OtpFilter`, card / EMI / SIP SMS parsers, counterparty hashing
- [ ] `CsvImporter`

---

## 3. Handoffs I owe (09 section 16.4)

| To | What | By hour | Status |
|---|---|---|---|
| Everyone | Frozen `domain/model/` | 1 | Ready on `Snehal`; freeze not yet announced |
| T3 | Persona B seeded transactions | 3 | Not started |
| T1 | `FinancialSnapshot` and `ConsolidatedView` sample JSON | 3 | Not started |
| T1, T4 | Repository `StateFlow` with `switchScenario` and `injectTransaction` | 9 | Not started |

## 4. What I need from others

| From | What | Why |
|---|---|---|
| Integration Lead | `testImplementation("junit:junit:4.13.2")` in `app/build.gradle.kts` | No track can run tests without it |
| Integration Lead | Register new Room entities in `AppDatabase.kt`; SQLCipher dependency if encryption is kept | Shared files |
| T1 | `HealthScore.kt` | 2C consumes it |
| T4 | Proposed content for `CopilotModels.kt` | Contract file is mine, content is theirs |

---

## 5. Old-plan work (commit `7210b03`) and what happened to it

| File | Old phase | Now |
|---|---|---|
| `domain/model/*` | 1A | Replaced by the new contract in Phase 1A |
| `data/SyntheticDataSeeder.kt` + test | 1B | Deleted in Phase 1A (no longer compiled against the new contract); rebuild in 1B under `data/ingest/` |
| `data/FinancialHealthRepository.kt` + test | 2C | Deleted in Phase 1A; rebuild in 2C (reuse the `StateFlow` + mutation pattern) |
| `app/build.gradle.kts` (+ JUnit line) | infra | Keep until the Integration Lead adds it on `main` |
| Root `build.gradle.kts` (AGP 8.13.2) + Gradle wrapper 8.13 | infra | Separate commit on `Snehal`; needed because AGP 8.2.0 fails on Android Studio's JDK 21. Clashes with `master` (AGP 8.2.2) at merge time; Integration Lead decides |

---

## 6. Verified change log

_Entries are added only after Snehal verifies a change._

| Date | Phase | What changed | Verified by | Test command |
|---|---|---|---|---|
| 2026-10-10 | 1A | New contract in `domain/model/` (5 files, 26 types per 06 section 3.1); old seeder, repository and their tests deleted | Snehal | `.\gradlew.bat assembleDebug` |
