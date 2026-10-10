# RupAI Sprint 2: Execution Checklist

Plan and acceptance criteria: [plan.md](plan.md).
Status: Siddhant's Phases 1A-4A verified headlessly. Real localization integration, remaining phases and team-wide Phase 0 confirmations are pending.
Phase 5A core is verified; cross-track completion remains open (localization contract mismatch, missing anomaly/audio, four existing categorizer failures).
Only Siddhant updates this central checklist. Record commit, verification and blockers beside a phase when completed.

## Phase 0 - Team Setup

- [ ] 0: Freeze v1 boundary examples, confirm separate checkouts/deadline, record baseline status and snapshot-contract mismatch.

## Siddhant - Financial Intelligence & Integration

- [x] 1A: Financial history normalization and data-quality gate; FinancialHistoryNormalizerTest (16 cases; full suite 36/36 and production Kotlin compile passed).
- [x] 2A: Freelancer forecasting and monthly sampled-path adapter; 20 focused cases, full JVM suite 56/56 and production Kotlin compile passed.
- [x] 3A: Time-ordered evaluation and plan interoperability; 20 new cases, full JVM suite 76/76 and Kotlin compile passed; synthetic metrics and plan tables saved.
- [x] 4A: Regional proxy generation and injected verifier/fallback contract; 12 new JVM cases, full suite 88/88 and Kotlin compile passed; 12 proxy / 35 combined backend tests passed. Real translations/live language review pending Stage 5.
- [ ] 5A: Headless core implemented; 26 DecisionPipelineTest/ExpenseReplanTest cases and full JVM suite 114/114 plus Kotlin compile passed. Final real teammate integration remains pending; backend discovery 36/40, four unchanged categorizer failures. See review/phase5a-results.md.

## Roshan - Transaction Intelligence & Audio

- [x] 1B: Android merchant extraction, category policy, bounded client and incremental worker; SmsParserTest and CategorizationPolicyTest (commit `b704045`).
- [x] 2B: Backend uncertain-cache/provider failure handling and pending-row recovery; test_categorizer and CategorizationRetryTest (commit `39fa28a`).
- [x] 3B: Pure-Kotlin unusual-expense detector using prior-only observations; ExpenseAnomalyDetectorTest (commit `ca537e7`).
- [x] 4B: Regional read-aloud service and fake-engine lifecycle checks; SpeechTextFormatterTest and RegionalSpeechPlayerTest (commit `8920861`).
- [x] 5B: Category/anomaly acceptance results, actual or pending device checks and integration boundary review; RoshanAcceptanceBenchmarkTest passed (commit `49715c4`).

## Snehal - Localization & Quality Assurance

- [x] 1C: Language catalogue, script/locale mapping and examples; NarrationLanguageTest passed (commit `a92c673`).
- [x] 2C: Offline localized advice for five intents, preserving metadata; LocalizedAdvisorTest passed (commit `5eff527`).
- [x] 3C: Synthetic scenario fixtures, category reference labels and manifest with splits/seeds/provenance; categorization-labels.csv and sprint2-scenarios.json (commit `7545d1b`).
- [x] 4C: Language/meaning review, demo script and UI_HANDOFF without modifying UI files; UI_HANDOFF.md, demo-script.md, language-review.md (commit `85d77ef`).
- [x] 5C: Evidence index and final handoff examples checked against actual outputs; tasks/review/evidence-index.md (commit `b959d1c`).

## Checkpoints

- [ ] Checkpoint 1: Each owner's Stage 1 passes independently; no cross-track unfinished imports.
- [ ] Checkpoint 2: Stage 2 forecast/category/localization fixtures are published and focused regressions pass.
- [ ] Checkpoint 3: Detector/backtest/fixture reviews complete; mocked versus actual evidence is labelled.
- [ ] Checkpoint 4: No post-verification translation; speech stays downstream and UI remains untouched.
- [ ] Final: Consistent snapshot contract; one history/forecast/decision pipeline; no duplicate expense or receipt.
- [ ] Final: All available JVM/backend suites and production compilation pass, or blockers are explicitly recorded.
- [ ] Final: Baselines, forecast coverage/width, category/anomaly metrics and language review reflect real runs.
- [ ] Final: Uday receives stable callable contracts, sample outputs and pending device/manifest requirements.

## Completion Record

For each phase use: `ID | commit | commands/results | sample output | remaining limitations`.
Do not mark a phase done because code exists, an API is mocked, a tracker previously said done, or a different branch passed once.

### 1A - October 11, 2026

- Commit: not created; changes remain in the existing Siddhant working tree.
- Baseline: `:app:testDebugUnitTest :app:compileDebugKotlin` passed before code changes. Initial sandbox run could not load Gradle's native Windows library; approved outside-sandbox execution succeeded.
- Red: focused normalizer test command failed on missing new types, as expected before implementation.
- Green: `:app:testDebugUnitTest --tests "*FinancialHistoryNormalizerTest*" :app:compileDebugKotlin` passed with the initial 12 cases.
- Final: `:app:testDebugUnitTest :app:compileDebugKotlin` passed with all 36 tests: 16 normalizer, 11 conflict/simulator, 7 number verifier and 2 orchestrator; zero failures/errors/skips.
- Example: INR 10,000 August card purchase -> August discretionary consumption INR 10,000, bank discretionary outflow zero. INR 45,000 September bill -> September cash debt service INR 45,000, new consumption zero. The declared cash snapshot is not debited again.
- Limits: no Room/UI integration, semantic SMS duplicate matching, forecast model, live provider/key validation or app-wide security audit yet. Phase 0 team agreements remain explicitly unchecked; this isolated boundary does not import unfinished teammate modules.
- Existing SDK XML-version warning is non-fatal. Existing untracked master plan and backend categorizer test preserved.

### 2A - October 11, 2026

- Commit: not created; changes remain on the existing Siddhant branch. Existing untracked changes preserved.
- Added FreelancerForecaster.kt and ForecastPathAdapter.kt, plus matching JVM test files. Existing normalizer, engine, UI and build files unchanged.
- Red: new focused tests initially failed compilation on the missing new types. First green: 15 focused tests passed. Expanded edge tests reproduced an upper historical-month overflow; lower cash-anchor-date regression reproduced a second date overflow. Both now return INVALID_HISTORY through arithmetic-safe month comparisons.
- Focused verification: `:app:testDebugUnitTest --tests "*FreelancerForecasterTest*" --tests "*ForecastPathAdapterTest*"` passed with 20 cases after the upper-bound fix. Final complete verification after both date fixes: `:app:testDebugUnitTest :app:compileDebugKotlin` passed, 56 tests, zero failures/errors/skips (16 forecaster, 4 adapter, 36 existing).
- Cases cover fixed seeds/different seeds, zero income, two-month adjacency, exact nearest-rank percentiles, constant/all-zero histories, sparse/stale/future/gapped/duplicate/reversed histories, negative/extreme amounts and dates, paired EWMA cash-spend residuals, card-consumption exclusion, six rolling boundaries, fifth-period delay and amount conservation, unit conversion, unchanged obligations, missing commitments, EMI validation and real ImpactSimulator interoperability.
- Example from the adapter fixtures: opening cash INR 55,000.01; monthly income INR 55,000.01, essentials INR 20,000.01, SIP INR 1,000.01, discretionary cash spending INR 1,234.56, declared debt INR 5,000 plus period-specific paise. A first-period delay gives receipts INR 0 then INR 110,000.02; total income and obligations are unchanged.
- Statistical method: empirical income moving-block bootstrap, length 2; cash discretionary EWMA alpha 0.4 with causal one-step residuals, sampled at paired historical indices; 2,000 seeded paths, six rolling months, integer-paise nearest-rank P10/P50/P90. No API, secret, remote model or private SMS involved. Zero Android imports/network/log calls found in domain/freelancer.
- History gate requires 6-120 consecutive completed months ending before the anchor month. Rolling forecast begins at the cash anchor with no skipped current-month gap; declared commitments must cover those intervals. Last-period delays are rejected rather than dropping money outside the horizon.
- Limits: intervals are uncalibrated, predictive accuracy and runtime benchmark not measured; no intramonth due-date/seasonality model, individual invoice tracking, Room/UI integration, absolute plan-risk selection or unusual-expense detector in this phase. Phase 3A evaluation/decision work remains next. Existing SDK XML warning is non-fatal.

### 3A - October 11, 2026

- Commit: not created; existing Siddhant branch and all earlier untracked work preserved. Added FreelancerEvaluator.kt, FreelancerDecisionAdapter.kt and matching test files; no shared engine, UI, build, manifest, backend or teammate file edits.
- Source reuse: inspected PlannerEngine, RiskSimulator, Calibrator and BaselineForecasts at origin/Roshan `019de806f9835b6efac521cad82d45e48b41e3e0`. Their snapshot contracts cannot be blindly merged. Ported the documented policy/math into a SimulationInputs-compatible entry point; preserved SIP and loan-maturity accounting and removed independent risk re-sampling. Graph excludes Android, so these findings rely on direct source reads, not graph absence.
- Red: focused decision/evaluator test compilation failed on their missing new types. Green: initial 8 decision and 9 evaluator cases passed. Additional integration/evidence/overflow cases reproduced an infinite aggregate debt objective; fixed finite-sum checks and output guards. Final focused run: 20/20. Strengthened the initial-breach case to verify all later periods recover while path-level risk still counts the initial breach.
- Final verification: `:app:testDebugUnitTest :app:compileDebugKotlin` passed with 76 tests, zero failures/errors/skips: 11 decision, 9 evaluation and 56 existing. Non-fatal SDK XML-version warning remains.
- Evaluation: train months 1-18; calibration origins 19-24; held-out test origins 25-30. Refit only on prior observations, with no threshold/prompt tuning on the final results. 21 overlapping origin-step records per model/target/run; six distinct held-out months. Report MAE/RMSE, coverage, width and P10/P50/P90 pinball loss, overall and by step.
- Synthetic pack: stable income, irregular/zero income, sudden income drop, surprise essential expense; seeds 42/73, independent split generation seeds. Generated aggregates, declared recurring-income baseline, opening cash and all saved predictions are included in the JSON for replay. No private financial data/API/model key used.
- Decisions: A cuts discretionary 30%, prepays 80% of surplus above 1x reserve; B uses 2.5x reserve; status quo has no cut/prepayment. Full essentials/debt/SIP retained. Absolute risk counts any initial or monthly reserve breach; deterministic infeasibility also excludes recommendation eligibility. If neither action plan meets limits, return the minimum-risk policy with a warning, not a safety guarantee.
- Demonstration: identical point path and candidate policies; with 2,000 certain paths A is preferred. Replacing 1,000 paths with the month-2 missed-receipt path makes A risk 50%, B risk 0%, and selects B at the unchanged 15% limit. Separate end-to-end test uses actual FreelancerForecaster -> ForecastPathAdapter -> decision comparison and moves receipts exactly once.
- Artifacts: `android_app/app/build/reports/freelancer/phase3a-evaluation.json` and `phase3a-decisions.json`; generated by JVM tests, gitignored build outputs. Summary/reproduction/limitations: `tasks/review/phase3a-results.md`.
- Honest failure: income-drop flow interval coverage is 2/21 (9.52%) and 4/21 (19.05%) on seeds 42/73. Historical-average MAE beats bootstrap in several runs. Do not label this a universal accuracy score or hide these outcomes.
- Limits: pooled calibration pairs are dependent; no guaranteed 80% coverage or calibrated plan-risk probability. Replay persists last observed fixed obligations; real future schedules and Room integration remain later work. No real-device performance, real-user validation, provider migration or backend regression run in this Android-only phase. Next planned phase is 4A; 5A is still required for the assembled app-facing pipeline.

### 4A - October 11, 2026

- Commit: not created; existing Siddhant branch, earlier untracked work and shared files preserved. Edited BackendClient, NumberVerifier, proxy and proxy tests; added LocalizedNarrationTest. Narrow LocalAdvisor money-format fix preserves paise and avoids Int overflow in the now-validated fallback.
- Optional languageCode defaults to en; supports en, hi-Latn, hi-Deva and mr-Deva. Backend uses an allow-listed language instruction before generation and echoes the language. Interpret still receives only the canonical local intent, never the raw question.
- Injected requested-language fallback is checked against original bundle metadata/numbers and must have LOCAL source before narration transport. Missing regional provider or unsupported language fails explicitly; no quiet English replacement. Legacy English text-only responses remain accepted.
- Rejected numbers, blank/oversized text, wrong language echo, malformed supplied range/confidence/horizon/impact/intent/source, offline, timeout and HTTP failures return the whole localized template. Cancellation propagates. Authoritative engine metadata and local trade-off remain unchanged.
- Red: new JVM contract did not compile without language/fallback parameters; backend regional requests returned 422. Green: focused tests passed, then expanded boundary cases and full regressions passed: 88 JVM tests, zero failures/errors/skips, production Kotlin compile; 35 backend tests (12 proxy, 7 smoke, 16 benchmark).
- Limits: language fixtures are test-only. Snehal's actual templates, live Gemini language quality, semantic translation/risk-direction review and speech/device/UI integration remain pending. NumberVerifier matches numeric values, not field meaning; prompt bans number words and translated units, and verifier rejects known localized scale/percentage words, not every possible paraphrase. Provider stays existing Gemini; no Groq switch, model/API key validation or forecast routing changes.
- Evidence, contract usage and reproduction: tasks/review/phase4a-results.md. Next phase is 5A, requiring deliberate teammate-module reconciliation.

### 5A Core - October 11, 2026 (Final Integration Pending)

- Added data/planning/DecisionPipeline.kt, ForecastResultAdapter.kt, domain/freelancer/VerifiedNarration.kt, DecisionPipelineTest.kt and ExpenseReplanTest.kt. Earlier changes preserved; no prior engine, UI, shared build/manifest/schema, backend or teammate implementation edits. No commit created.
- Real core chain: typed local events -> FinancialHistoryNormalizer -> FreelancerForecaster -> ForecastPathAdapter -> FreelancerDecisionAdapter -> selected-plan terminal-bank-cash ResultBundle -> BackendClient/injected narrator -> NumberVerifier -> VerifiedNarration. Default confidence Low; no fake field clamping or fabricated income/cash. Negative savings, zero-income denominators, undefined runway and unsupported magnitudes return explicit unavailable states.
- Existing Room DAO flow is observed read-only through an explicit snapshot mapper. Current Room rows cannot identify event kind/funding/account from their fields, so no guessing mapper is supplied. mapLatest, zero output buffer and cancellation checks suppress stale concurrent narration. Alert ids are deduplicated per collector, capped at 100,000; no persistent notification ledger.
- Posted expense uses the already-updated cash snapshot and is never applied as a future one-off. Hypothetical change calls ImpactSimulator once against status quo. Prior-only anomaly port preserves expected-payment metadata; missing/failed detector is UNAVAILABLE, not NORMAL. Unusualness and immediate reserve-pressure severity are independent.
- Optional existing goal conflict keeps its separate 60%-cut/fixed-obligation semantics. Infeasible plan/goal results carry requiresReview, bypass remote recommendation generation, use neutral English fallback text and refuse read-aloud with REVIEW_REQUIRED. Regional templates need equivalent human-reviewed warning copy at final reconciliation. Speech is never automatic.
- Red: initial focused compile failed on missing pipeline contracts. A SharedFlow test initially lost its first input before subscription; fixed explicit subscription gating rather than sleeps. Added runtime regressions reproduced non-finite what-if output and unsafe speech; finite-output/review guards fixed both.
- Verification: 26 focused cases (15 DecisionPipeline, 11 ExpenseReplan); complete 114 JVM tests, zero failures/errors/skips; Kotlin compile passed. Full backend discover ran 40 tests: 36 passed, four pre-existing categorizer tests failed. Existing Copilot/model suites all passed within that run; nothing deleted/skipped to hide failures.
- Remote refs refreshed without checkout/merge: Roshan 39fa28a1b1886ee533cd25e1382741ac840ee7be has newer categorization/provider changes; Snehal b959d1cbbbdf8a738bc1c0937f02e3f658fc275c has localization using different same-qualified response types and a 30-day snapshot/verdict contract. No blind merge or duplicate model class. Anomaly/audio still absent on refreshed Roshan ref.
- Remaining gate: agree a ResultBundle-compatible localization entry point preserving six-month engine metadata, reconcile Roshan's categorization tests/provider migration, connect real anomaly/audio adapters and provide explicit Room event classification/cash declarations. UI, device/live provider verification and production routing remain pending.
- Reproduction, field semantics, failure names and integration handoff: tasks/review/phase5a-results.md. Phase 5A deliberately remains unchecked.

### Optional Local Model Feasibility Probe - October 11, 2026

- [x] Add isolated synthetic English harness and test the already installed local model without switching production providers.
- Files: `backend/local_model_smoke.py`, `backend/test_local_model_smoke.py`, `backend/LOCAL_MODEL_TEST.md`.
- Hardware readout: 23.60 GB system RAM; NVIDIA RTX 5050 Laptop GPU, 8151 MiB VRAM.
- Existing tags: `qwen2.5-coder:7b` (4.7 GB) and `qwen3.6:27b` (17 GB). No model downloaded or upgraded.
- Live command: backend virtual-environment Python, `local_model_smoke.py --model qwen2.5-coder:7b --timeout 90`.
- First chat: 9.369 seconds, schema-valid but unsupported plan-benefit/SIP claims. Prompt tightened; this is development evidence, not held-out evaluation.
- Second chat: 2.483 seconds, schema-valid; manually inspected cash/risk/forecast values matched synthetic inputs and recommendation was limited to simulation before commitment. Different prompts/load conditions mean these two timings are not a comparative benchmark.
- Offline verification: `python -m unittest test_local_model_smoke test_copilot -v`; all 16 tests passed (7 harness + 9 existing proxy tests).
- No application integration, language templates, API keys or private transactions involved. Production NumberVerifier was NOT run on the model output. It remains marked unverified/not safe for app use.
- Next planned work remains 2A. Local provider adapter selection/verification is separate from this completed smoke probe and does not mark 4A complete.

### Synthetic Ollama/Groq Comparison - October 11, 2026

- [x] Add 12 common synthetic English cases, deterministic field-bound scorer and offline regression tests.
- [x] Run local Ollama comparison: `qwen2.5-coder:7b`, 12/12 cases passed, 84/84 facts correct; schema/numeric-grounding/case-pass all 100%; median attempt latency 2.179 seconds. These are small controlled fact-restatement metrics, NOT general model/financial prediction accuracy.
- [ ] Run live Groq `openai/gpt-oss-20b` on the same cases. Agent process has no GROQ_API_KEY; the user's separate terminal environment is not accessible. Mocked transport tests are not counted as live model results.
- Verification: `python -m unittest test_model_benchmark test_local_model_smoke test_copilot -v`; all 26 tests passed (10 scorer/transport, 7 smoke harness, 9 existing proxy).
- Evidence: `backend/build/model-evaluation/20261010T201520221567Z.json`; Groq correctly marked NOT RUN with null scores. No comparative winner declared.
- Reproduction and limitations: `backend/MODEL_BENCHMARK.md`. Run `model_benchmark.py --providers both` from the user's authenticated terminal. No secrets persisted, new SDK, UI edits, production provider change or forecast-phase completion.

### Expanded Edge Cases - October 11, 2026

- [x] Append eight numeric boundaries to the original twelve cases: all zero, signed zero, tiny/near-certain percentages, one paisa, API balance limits, nearly equal quantiles and zero-crossing intervals.
- [x] Add offline scorer/schema/transport edge tests; reproduce and fix numeric-prefix/fraction/scale-word false positives and Indian-grouping false negative. Preserve original reports rather than overwriting evidence.
- Verification: all 32 tests passed (16 benchmark, 7 smoke, 9 existing proxy).
- Live Ollama: 20 attempted, 19 passed; 137/140 facts correct (97.86%), 100% schema validity, 95% numeric grounding, median attempt 2.141 seconds.
- Failure: `one_paisa` changed bank cash 0.01 to 1, P10 -0.01 to -1, and P90 0.01 to 1. Recorded unmodified; prompt not retuned to hide this failure.
- Evidence: `backend/build/model-evaluation/20261010T202207222333Z.json`. Groq remains NOT RUN because the agent process lacks its key. No winner or general-accuracy claim.
- Complete case inventory and distinction between real model runs and simulated failure tests are in `backend/MODEL_BENCHMARK.md`. No UI/production integration changes.
