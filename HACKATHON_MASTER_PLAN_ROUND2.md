# RupAI — AI Financial Health Copilot: Master Plan (Round 2 Unified Edition)
## HackMatrix 5.0 Final Round — Track: FIN-02 Financial Health Copilot

> **Sprint Context:** Final Round Evaluation (Round 2 Criteria: AI/ML Depth, Innovativeness, UI/UX, Tech Stack, Presentation)  
> **Architecture:** Pure Kotlin JVM Core Engines + Material 3 Jetpack Compose UI + FastAPI Gemini Proxy with Anti-Hallucination Guard + Regional Multilingual Voice Assistant (TTS)  
> **Source of Truth:** Single unified document combining Foundational Architecture and Sprint 2 Extensions.

---

## 1. Executive Summary & Evaluation Alignment

We are extending **RupAI** from an SMS expense tracker into an autonomous **AI Financial Health Copilot** operating under **uncertain cash flow**.

### Round 2 Evaluation Criteria Alignment:
1. **Innovativeness & Originality:** 
   - Two-View Double-Entry Ledger (Credit card consumption vs cash liability settlement separation with zero double-counting).
   - Real-time Pareto-Optimal Goal Conflict Resolver (Bisection search across 4 relaxation policies).
   - Regional Script Advice (English, Hinglish, Hindi, Marathi) with verified voice read-aloud.
2. **Tech Stack & Modern Tools:**
   - 100% Pure Kotlin JVM domain engines (sub-2ms execution, zero Android/emulator lock-in).
   - Jetpack Compose Material 3 UI with reactive `StateFlow`.
   - FastAPI backend proxy with rate-limiting and PII sanitization.
   - Android Native `TextToSpeech` for offline multilingual voice.
3. **AI & ML Integration (Depth & Breadth):**
   - **Deterministic ML:** 2,000-path Monte Carlo bootstrap cash flow simulation with Conformal Prediction calibration (Winkler score optimization, 80% coverage).
   - **Anomaly ML:** Median/MAD-based unusual spending anomaly detector.
   - **Multi-Layer NLP Categorization:** 4-Layer Cascade (UPI VPA $\rightarrow$ Local Regex $\rightarrow$ Google Places $\rightarrow$ Batch Gemini Flash).
   - **Anti-Hallucination Guardrail:** Deterministic `NumberVerifier` regex bouncer cross-checking every digit against pre-calculated whitelists.
4. **UI/UX & Accessibility:**
   - Speedometer Health Gauge (0–100 score).
   - P10/P50/P90 probabilistic fan chart with tabular fallbacks.
   - 5-slider What-If simulation bottom sheet with live before/after delta chips.
5. **Presentation & Reproducibility (Page 2 Mandate):**
   - Standalone CLI JUnit runner (`ModelValidationBenchmarkTest`) printing ASCII comparison tables against baseline models.
   - Automated 9-artifact Evidence Pack Exporter into `build/evidence/`.

---

## 2. Current Implementation Status (Completed vs In-Progress)

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                CURRENT LIVE STATUS BOARD                               │
├──────────────┬──────────────┬──────────────────┬───────────────────────────────────────┤
│ Teammate     │ Track        │ Git Branch       │ Live Status                           │
├──────────────┼──────────────┼──────────────────┼───────────────────────────────────────┤
│ **Siddhant** │ **Track 4**  │ `Siddhant`       │ 🟢 100% Stage 2 DONE (Pushed)         │
│              │ (Copilot/AI) │                  │    Copilot, Proxy, Verifier, Conflict │
├──────────────┼──────────────┼──────────────────┼───────────────────────────────────────┤
│ **Roshan**   │ **Track 3**  │ `Roshan`         │ 🟢 100% Stage 2 & 4 DONE (Tested)     │
│              │ (Math/Eng)   │                  │    Forecast, Calibrator, Plan, Bench  │
├──────────────┼──────────────┼──────────────────┼───────────────────────────────────────┤
│ **Snehal**   │ **Track 2**  │ `Snehal`         │ 🟢 100% Stage 1 & 2 DONE (Tested)     │
│              │ (Data/Repo)  │                  │    Contracts, Seeder, Detectors, Repo │
├──────────────┼──────────────┼──────────────────┼───────────────────────────────────────┤
│ **Uday**     │ **Track 1**  │ `uday`           │ ⏳ Stage 3 Screen UI Assembly NEXT    │
│              │ (UI/Screens) │                  │    Cockpit, Plan, Copilot Screens     │
└──────────────┴──────────────┴──────────────────┴───────────────────────────────────────┘
```

---

## 3. Sprint 2 Parallel Task Breakdown (The Round 2 Boosters)

To blow away the judges in Round 2, each team member executes their dedicated booster modules:

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           4-TRACK PARALLEL WORKSTREAMS                          │
├────────────────────┬────────────────────┬───────────────────┬───────────────────┤
│ Track 1: UDAY      │ Track 2: SNEHAL    │ Track 3: ROSHAN   │ Track 4: SIDDHANT │
│ UI Assembly & Mic  │ Localization & QA  │ Txn AI & Audio    │ Master Pipeline   │
├────────────────────┼────────────────────┼───────────────────┼───────────────────┤
│ • CockpitScreen    │ • Language Enum    │ • Fix 80% Misc    │ • Multilingual    │
│ • PlanScreen       │   (EN, HI, MR, HG) │   Category Bug    │   Gemini Proxy    │
│ • CopilotScreen    │ • LocalizedAdvisor │ • Expense Anomaly │ • Multilingual    │
│ • Language Chips   │   Offline Fallback │   Detector (MAD)  │   NumberVerifier  │
│ • Speaker 🔊 Button│ • Demo Script & QA │ • Regional Voice  │ • Headless Master │
│                    │ • UI Handoff Docs  │   Player (TTS)    │   DecisionPipeline│
└────────────────────┴────────────────────┴───────────────────┴───────────────────┘
```

---

### Track 4: Siddhant (Financial Intelligence & Integration)
**Git Branch:** `Siddhant`  
**Exclusive Directory Ownership:** `domain/freelancer/`, `data/planning/`, `domain/ai/`, `backend/copilot.py`, `data/remote/BackendClient.kt`

* [ ] **Phase 1A: Financial History Normalization & Data-Quality Gate**
  - **Files:** `domain/freelancer/FreelancerModels.kt`, `FinancialHistoryNormalizer.kt`; Test: `FinancialHistoryNormalizerTest.kt`
  - **Details:** Normalizes monthly history, preserves zero-income months, canonicalizes categories, excludes internal transfers/refunds from earned income, and enforces the two-view ledger rule (spending vs bank cash settlements).
* [ ] **Phase 2A: Freelancer Forecasting & Monthly Sampled-Path Adapter**
  - **Files:** `domain/freelancer/FreelancerForecaster.kt`, `ForecastPathAdapter.kt`; Tests: `FreelancerForecasterTest.kt`, `ForecastPathAdapterTest.kt`
  - **Details:** 2,000-path block bootstrap (length 2) for irregular/freelancer income; models delayed client payments by shifting receipts; outputs percentiles P10/P50/P90 matching simulation inputs.
* [ ] **Phase 3A: Time-Ordered Evaluation & Plan Interoperability**
  - **Files:** `domain/freelancer/FreelancerEvaluator.kt`, `FreelancerDecisionAdapter.kt`; Tests: `FreelancerEvaluationTest.kt`, `FreelancerDecisionTest.kt`
  - **Details:** Connects freelancer forecast paths to PlannerEngine (Plan A vs Plan B vs Status Quo); evaluates absolute reserve breach risk; reports MAE/RMSE and 80% coverage on held-out periods.
* [ ] **Phase 4A: Regional Proxy Generation & Multilingual Verifier Contract**
  - **Files:** `backend/copilot.py`, `backend/test_copilot.py`, `data/remote/BackendClient.kt`, `domain/ai/NumberVerifier.kt`; Test: `LocalizedNarrationTest.kt`
  - **Details:** Adds optional narration language code (`english`, `hinglish`, `hindi`, `marathi`) to Gemini proxy; generates localized text BEFORE `NumberVerifier`; verifier regex-checks Western and Devanagari numerals against whitelist.
* [ ] **Phase 5A: Single Headless DecisionPipeline & Exactly-Once Expense Replan**
  - **Files:** `data/planning/DecisionPipeline.kt`, `ForecastResultAdapter.kt`, `domain/freelancer/VerifiedNarration.kt`; Tests: `DecisionPipelineTest.kt`, `ExpenseReplanTest.kt`
  - **Details:** Master headless pipeline uniting normalized history $\rightarrow$ freelancer forecast $\rightarrow$ plan evaluation $\rightarrow$ anomaly detector $\rightarrow$ multilingual copilot $\rightarrow$ read-aloud TTS; handles real vs hypothetical expense replanning.

---

### Track 3: Roshan (Transaction Intelligence & Audio)
**Git Branch:** `Roshan`  
**Exclusive Directory Ownership:** `domain/anomaly/`, `data/audio/`, `data/SmsParser.kt`, `data/BackendCategorizerClient.kt`, `data/AiCategorizationWorker.kt`, `backend/categorizer.py`

* [ ] **Phase 1B: Android Merchant Extraction, Category Policy & Bounded Client**
  - **Files:** `data/SmsParser.kt`, `BackendCategorizerClient.kt`, `AiCategorizationWorker.kt`; Tests: `SmsParserTest.kt`, `CategorizationPolicyTest.kt`
  - **Details:** Fixes the 80% Miscellaneous bug: preserves business context (Medical Store = HEALTH, Steam = ENTERTAINMENT), adds micro-debit heuristics (₹10–₹150 = FOOD), bounds batch client requests with timeouts, and avoids caching transient FALLBACKs.
* [ ] **Phase 2B: Backend Uncertain-Cache / Provider Failure Handling & Retry Recovery**
  - **Files:** `backend/categorizer.py`, `backend/test_categorizer.py`, `data/TransactionViewModel.kt`; Test: `CategorizationRetryTest.kt`
  - **Details:** Ignores uncertain/invalid cache hits; handles Gemini timeouts/rate limits without cache poisoning; retries pending uncategorized rows even when sync finds 0 new SMS.
* [ ] **Phase 3B: Pure-Kotlin Unusual-Expense Detector Using Prior-Only Observations**
  - **Files:** `domain/anomaly/AnomalyModels.kt`, `ExpenseAnomalyDetector.kt`; Test: `ExpenseAnomalyDetectorTest.kt`
  - **Details:** Robust anomaly detector using historical median & MAD (Median Absolute Deviation) per merchant and category; excludes transfers and regular recurring bills; outputs status (`NORMAL`, `UNUSUAL`).
* [ ] **Phase 4B: Regional Read-Aloud Service & Lifecycle Checks**
  - **Files:** `data/audio/RegionalSpeechPlayer.kt`, `SpeechTextFormatter.kt`; Tests: `SpeechTextFormatterTest.kt`, `RegionalSpeechPlayerTest.kt`
  - **Details:** Wraps Android native `TextToSpeech`; formats currency and numbers for speech; supports `hi-IN`, `mr-IN`, `en-IN`; speaks strictly verified text downstream.
* [ ] **Phase 5B: Category / Anomaly Acceptance Results & Boundary Review**
  - **Files:** `tasks/review/roshan-results.md`
  - **Details:** Measures category precision/recall, anomaly false-alert rate, and records real-device audio checks.

---

### Track 2: Snehal (Localization & Quality Assurance)
**Git Branch:** `Snehal`  
**Exclusive Directory Ownership:** `domain/ai/localization/`, `tasks/fixtures/`, `tasks/review/`, `tasks/UI_HANDOFF.md`

* [ ] **Phase 1C: Language Catalogue, Script/Locale Mapping & Examples**
  - **Files:** `domain/ai/localization/NarrationLanguage.kt`; Test: `NarrationLanguageTest.kt`; Documentation: `tasks/review/language-examples.md`
  - **Details:** Defines 4 language modes: `ENGLISH ("en", "en-IN")`, `HINGLISH ("hi-Latn", "hi-IN")`, `HINDI ("hi-Deva", "hi-IN")`, `MARATHI ("mr-Deva", "mr-IN")`; human-readable examples for cash overview, income, and shocks.
* [ ] **Phase 2C: Offline Localized Advice for Every Intent Preserving Metadata**
  - **Files:** `domain/ai/localization/LocalizedAdvisor.kt`; Test: `LocalizedAdvisorTest.kt`
  - **Details:** Pure deterministic fallback templates in all 4 languages for all 5 Copilot intents; guarantees offline response availability if cloud proxy is unreachable.
* [ ] **Phase 3C: Synthetic Scenario Fixtures, Category Reference Labels & Manifest**
  - **Files:** `tasks/fixtures/sprint2-scenarios.json`, `tasks/fixtures/categorization-labels.csv`, `tasks/review/fixture-manifest.md`
  - **Details:** Curates evaluation fixtures for freelancer income, delayed receipts, ₹8k shock, ₹15k rent, and independently reviewed merchant category labels.
* [ ] **Phase 4C: Language / Meaning Review, Demo Script & UI Handoff**
  - **Files:** `tasks/UI_HANDOFF.md`, `tasks/review/language-review.md`, `tasks/review/demo-script.md`
  - **Details:** Reviews translations for financial clarity, obligation wording, and pronunciation; writes complete demo sequence script for live judges.
* [ ] **Phase 5C: Evidence Index & Final Handoff Examples**
  - **Files:** `tasks/review/evidence-index.md`
  - **Details:** Indexes forecast baselines, test reports, seeds, and verified localized outputs for submission.

---

### Track 1: Uday (UI Screens, Controls & Audio Triggers)
**Git Branch:** `uday`  
**Exclusive Directory Ownership:** `android_app/app/src/main/java/com/example/financeapp/ui/**`

* [ ] **Phase 3A: Cockpit Screen (M1 & M10)** (`CockpitScreen.kt`, `VerdictCard.kt`, `HealthGauge.kt`, `AccountsRow.kt`)
* [ ] **Phase 3B: Plan & Forecast Screen (M2 & M4)** (`PlanScreen.kt`, `ForecastFanChart.kt`, `PlanCard.kt`, `PeriodTable.kt`)
* [ ] **Phase 3C: Copilot Chat Screen with Audio Trigger (M5)** (`CopilotScreen.kt`, `ThreeSectionAnswerCard.kt`, Language Chips, 🔊 Speaker TTS Trigger Button)

---

## 4. End-to-End System Workflow

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                              RUPAI END-TO-END DATAFLOW                                 │
└────────────────────────────────────────────────────────────────────────────────────────┘

 [Bank SMS / CSV / Seeder]
             │
             ▼
   [OTP & PII Filter]  ───────> Drops OTPs instantly in-memory (0ms)
             │
             ▼
  [4-Layer Categorizer] ──────> 80% resolved on-device (VPA + Regex), Places & Gemini fallback
             │
             ▼
  [Two-View Double Ledger] ───> Card Swipe = Spending View; Bill Pay = Cash View (isTransfer=true)
             │
             ▼
  [Deterministic Engines] ────> 2,000-Path Monte Carlo + Goal Conflict Resolver (sub-2ms)
             │
             ▼
  [Pre-Calculated Whitelist] ─> Strict JSON payload sent to Gemini Proxy
             │
             ▼
  [Gemini Multilingual Proxy] > Generates text in English / Hinglish / Hindi / Marathi
             │
             ▼
   [NumberVerifier Bouncer] ──> Cross-checks every currency amount against whitelist
        │             │
   (Approved)     (Rejected) ──> Fallback to Snehal's LocalizedAdvisor
        │             │
        └──────┬──────┘
               ▼
     [Verified Narration]
        │              │
        ▼              ▼
 [Material 3 UI]  [Audio TTS Speaker] (Roshan's RegionalSpeechPlayer speaks out loud!)
```

---

## 5. Verification Commands (CLI Ready)

### Android Unit Tests (PowerShell):
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# 1. Run all unit tests
.\gradlew.bat testDebugUnitTest

# 2. Run Reproducible Benchmark for Judges (ASCII Table)
.\gradlew.bat testDebugUnitTest --tests "*ModelValidationBenchmarkTest*"

# 3. Export all 9 evidence submission files
.\gradlew.bat testDebugUnitTest --tests "*EvidenceExporterTest*"
```

### Backend Proxy Tests:
```powershell
.\.venv\Scripts\python.exe -m unittest test_copilot -v
.\.venv\Scripts\python.exe -m unittest test_categorizer -v
```

---

## 6. Ready-to-Send Quick Handoff Messages

### To Siddhant (Track 4: Financial Intelligence & Integration):
> - **1A:** Financial history normalization & data-quality gate (`FinancialHistoryNormalizerTest.kt`)
> - **2A:** Freelancer forecasting & monthly sampled-path adapter (`FreelancerForecasterTest.kt`)
> - **3A:** Time-ordered evaluation & plan interoperability (`FreelancerEvaluationTest.kt`)
> - **4A:** Regional proxy generation & multilingual verifier contract (`LocalizedNarrationTest.kt`, `NumberVerifierTest.kt`)
> - **5A:** Single headless DecisionPipeline & exactly-once expense replan (`DecisionPipelineTest.kt`, `ExpenseReplanTest.kt`)

### To Roshan (Track 3: Transaction Intelligence & Audio):
> "Bhai Roshan, tera 2A, 2B, 2F, 4B, 4C ka math engine & benchmark suite 100% test pass ho chuka hai! Round 2 ke liye Sprint 2 mein tere 5 focused phases hain:  
> - **1B:** Android merchant extraction & category policy (`SmsParser.kt`, `SmsParserTest.kt`, `CategorizationPolicyTest.kt`) — Fixes 80% Misc bug!  
> - **2B:** Backend uncertain-cache & pending-row recovery (`backend/categorizer.py`, `test_categorizer.py`, `CategorizationRetryTest.kt`)  
> - **3B:** Pure-Kotlin unusual-expense detector (`domain/anomaly/ExpenseAnomalyDetector.kt`, `ExpenseAnomalyDetectorTest.kt`)  
> - **4B:** Regional read-aloud service (`data/audio/RegionalSpeechPlayer.kt`, `SpeechTextFormatterTest.kt`, `RegionalSpeechPlayerTest.kt`)  
> - **5B:** Category/anomaly acceptance report (`tasks/review/roshan-results.md`)  
> Check Section 3 of [HACKATHON_MASTER_PLAN_ROUND2.md] for exact specs."

### To Snehal (Track 2: Localization & Quality Assurance):
> "Snehal, tera Stage 1, 2C repo aur 2E detectors ka code verified aur passing hai! Round 2 ke liye Sprint 2 mein tere 5 focused phases hain:  
> - **1C:** Language catalogue, script/locale mapping & examples (`NarrationLanguage.kt`, `NarrationLanguageTest.kt`)  
> - **2C:** Offline localized advice for every intent (`LocalizedAdvisor.kt`, `LocalizedAdvisorTest.kt`)  
> - **3C:** Synthetic scenario fixtures & category reference labels (`tasks/fixtures/sprint2-scenarios.json`, `categorization-labels.csv`)  
> - **4C:** Language review, demo script & UI handoff (`tasks/UI_HANDOFF.md`, `tasks/review/language-review.md`)  
> - **5C:** Evidence index & final handoff examples (`tasks/review/evidence-index.md`)  
> Check Section 3 of [HACKATHON_MASTER_PLAN_ROUND2.md] for exact specs."

### To Uday (Track 1: UI Screen Assembly):
> "Uday, backend ke saare math engines aur benchmarks 100% test passing hain! Tere liye Stage 3 UI assembly open hai:  
> - **3A:** `CockpitScreen.kt` (Health gauge + accounts row + verdict card)  
> - **3B:** `PlanScreen.kt` (P10/P50/P90 fan chart + Plan A/B cards + 6-period table)  
> - **3C:** `CopilotScreen.kt` (Three-section card + Language selector chips + 🔊 Speaker audio button)  
> Check Section 3 of [HACKATHON_MASTER_PLAN_ROUND2.md] aur `md files/02-pages.md` for exact UI tokens & design specs."
