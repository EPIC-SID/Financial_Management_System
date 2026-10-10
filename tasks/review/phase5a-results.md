# Phase 5A: Headless Financial Pipeline Core

Owner: Siddhant. Verified October 11, 2026 on the existing Siddhant branch.
Status: core implemented and tested; **final cross-track Phase 5A is NOT complete**.
No commit created; no earlier changes reverted; no UI, shared engine/build/manifest/Room edits.

## Implemented

`DecisionPipeline.evaluate(request)` runs the existing real normalizer, freelancer forecast,
path adapter and plan comparison. It returns Ready with the projection, period plans, warnings,
optional conflict/impact, selected-plan ResultBundle and numerically verified narration, or
Unavailable with a fixed reason code. This is explicitly opt-in freelancer orchestration,
not an automatic replacement of the app's old prediction route.

`observe(dao, snapshotMapper)` consumes existing `getAllTransactions()` read-only. No new SMS
reader, transaction table, writes or imports. The mapper must supply resolved financial event
kinds/funding, complete-history coverage, a current cash anchor and future obligations. Room's
current TransactionEntity lacks those facts, so a production guessing mapper is deliberately
not provided. Returning null/throwing in the mapper produces an unresolved-ledger outcome.
Source flow failures produce generic dependency failure; coroutine cancellation propagates.

`observe(snapshotFlow)` uses mapLatest, no output buffer and cancellation checks after compute
and provider boundaries. Later snapshots cancel earlier in-flight narration, including a test
provider that catches cancellation. CPU-bound stages finish their current call before the next
cancellation check; this is not a real-device latency guarantee. Revision is a caller-supplied
label; latest emission wins, not numeric sorting of revision values.

## Financial Semantics

- Cash anchor already includes all posted bank events through asOf. It is never debited again.
- A reviewed posted bank expense contributes an immediate counterfactual: current cash with
  that debit removed versus the current anchor. This is not historical cash-before-transaction
  reconstruction or a full probabilistic before/after experiment.
- Credit-card consumption does not reduce bank cash. Transfers/settlements are not expense
  anomalies; future card settlements belong in declared future obligations only once.
- Hypothetical changes invoke existing ImpactSimulator once without mutating cash/history.
  Impact is versus status quo paths, not versus the selected state-dependent plan. Non-finite
  simulated values and malformed periods are rejected rather than passed to a future UI.
- Selected-plan shortfallRisk is absolute any-reserve-breach frequency. Impact shortfallRiskDelta
  is a difference; neither is goal-success probability. The same sampled draws are reused.
- forecastP10/P50/P90 are the selected plan's terminal BANK CASH quantiles, never income
  component quantiles, net position or the deterministic point ending balance.
- Plan labels map to Plan A / Plan B / Status quo; horizon is exactly `6 months`.
- Liquid runway is max(0, opening bank cash - reserve) divided by mean declared essentials
  plus debt service. SIP is separately displayed; no infinite/undefined runway is serialized.
- Savings rate is six-period income less essentials, regular debt service and discretionary
  cash outflow, divided by income. SIP and extra debt prepayment remain savings allocations.
- monthlySip is mean declared six-period SIP; monthlyLoanEmis is the sum of supplied loan EMIs;
  supplied loans must be the complete modeled loan set. avgDiscretionary is mean historical
  discretionary consumption, including card purchases, distinct from forecast cash spending.
- Confidence is Low for the uncalibrated freelancer projection. No language/forecast accuracy
  or calibrated risk claim is implied. Unsupported API values return Unavailable; no clamping.
- Optional ConflictResolver retains its existing equal-goal-transfer, fixed-obligation and
  60%-discretionary-cut assumptions. Its relaxations are NOT selected-policy payoff results.

Synthetic tests demonstrate opening cash INR 100,000 -> already-posted INR 8,000 expense ->
anchor INR 92,000. Replanning reduces selected terminal cash by INR 8,000, not INR 16,000;
an identical event retry produces the same bundle. A separate hypothetical INR 15,000 expense
in period two changes only the simulator result. Payment-delay integration moves INR 60,000
from period one to period two (INR 0 then INR 120,000), preserving total receipts.

## Extension Boundaries

Detector callback receives a resolved candidate, unique strictly-earlier expense history and
expected-payment/local-merchant metadata. It does not receive the candidate in training history.
Missing/failed detector returns UNAVAILABLE, never a false NORMAL assessment. This is an adapter
port, not an implementation or accuracy evaluation of Roshan's anomaly detector.

Unusualness and immediate reserve pressure are separate alert fields. Stable event-based alert
ids emit once per observe collector, even if the snapshot is retried. Current assessment remains
available in every result for UI updates. Dedup is in-memory and account-scoped by the caller's
collection lifecycle, not across restarts or multiple collectors. At 100,000 delivered ids it
returns ALERT_CAPACITY_REACHED rather than evicting ids and silently repeating alerts.

The injected localizedAdvisor callback is the existing Phase 4A ResultBundle contract. Missing
regional fallback, invalid fallback numbers/metadata or unsupported language is explicit
LANGUAGE_UNAVAILABLE. Default English uses LocalAdvisor. Real BackendClient is the default
narrator; focused tests also exercise the real client using mocked JSON transport. The final
wrapper re-verifies even an injected narrator before exposing text; offline/provider rejection
replaces the entire response with the verified requested-language template.

VerifiedNarration has a private constructor, language code, speech locale, provenance, captured
speechText and requiresReview. No translation occurs after verification. readAloud is a separate
user-triggered call; nothing speaks during evaluate/observe. Missing/failed/unsupported player
returns UNAVAILABLE; cancellation propagates. Infeasible plan/goal sets requiresReview, bypasses
remote recommendation generation, uses neutral English fallback and returns REVIEW_REQUIRED
without invoking speech. Future UI must display decision/conflict warnings alongside narration;
regional warning prose still requires human review. Numeric matching is not semantic checking.

## Verification

- DecisionPipelineTest: 15 passing cases.
- ExpenseReplanTest: 11 passing cases.
- Complete JVM suite: 114 tests, zero failures/errors/skips. Production Kotlin compile passed.
- Full backend unittest discovery: 40 tests ran, **36 passed / 4 failed**.
- Existing Copilot/local-model/benchmark suites: all 35 passed within that discovery run.
- Android SDK XML-version / Starlette-httpx deprecation warnings remain non-fatal.
- Domain wrapper has no Android imports; new files add no keys, payload logging or dependencies.

Tests were written first. Initial missing contracts failed compilation. A concurrency fixture
lost its first SharedFlow input before subscription; the corrected fixture waits for subscription,
not a timing sleep. Added runtime tests reproduced both overflowing what-if output and unsafe
speech before finite-result/review guards were implemented. No checks were skipped or weakened.

From android_app with Android Studio JBR:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests '*DecisionPipelineTest*' --tests '*ExpenseReplanTest*'
.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest :app:compileDebugKotlin
```

From backend:

```powershell
.\.venv\Scripts\python.exe -m unittest discover -v
```

All external providers/device speech were mocked. No live model calls, private SMS, API-key
configuration, phone benchmarks or actual Room database instrumentation were performed. The
Room test uses the real DAO interface with a fake read-only stream. Codebase graph excludes
Android sources; findings rely on direct source reads and actual test executions.

## Remaining Integration Gate

Remote refs were refreshed with git fetch origin, without switching branches or merging files.

Snehal ref `b959d1cbbbdf8a738bc1c0937f02e3f658fc275c` now contains localization, but its
LocalizedAdvisor takes `(language, intent, FinancialSnapshot, ConsolidatedView, Verdict, shock)`.
It uses same-qualified StructuredResponse/ConfidenceLabel with different item types, no intent
or source, and 30-day/date/derived-impact facts. Our frozen entry point takes ResultBundle and
preserves six-month terminal-cash metadata. Copying those files would break shared contracts;
inventing a 30-day snapshot from six-month results would misrepresent the forecast. Required:
agree a compatible ResultBundle overload/renderer with Snehal, preserve existing models and
test all real translations through NumberVerifier. Do not overwrite either model family.

Roshan ref `39fa28a1b1886ee533cd25e1382741ac840ee7be` contains categorization/provider/retry
changes but no anomaly/audio modules yet. His categorizer now calls a different llm/cache layer;
this is not a one-file drop-in migration. Those changes were inspected, not merged over current
backend files or the user's existing untracked categorizer tests.

The current untouched backend's failing test names are:

- test_old_misc_cache_is_retried_and_replaced: returns cached MISCELLANEOUS instead of HEALTH.
- test_legitimate_uncertainty_is_not_cached_by_merchant: saves MISCELLANEOUS to shared cache.
- test_malformed_model_output_is_failure_not_misc_classification: invalid prose becomes Misc.
- test_batch_ignores_old_misc_and_invalid_cache_entries: accepts Misc/invalid cached categories.

Required before final Phase 5A completion: reconcile actual teammate localization/anomaly/audio
contracts, migrate categorization/provider changes deliberately without losing these regressions,
provide resolved Room event mapping/current financial declarations, then rerun all suites and
record human-language/device acceptance separately. No UI or default app route is wired here.
