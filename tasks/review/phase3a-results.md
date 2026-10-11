# Phase 3A: Forecast Evaluation and Plan Evidence

Date: October 11, 2026. Owner: Siddhant. Changes remain uncommitted on the existing Siddhant branch.

## What Was Implemented

- FreelancerDecisionAdapter accepts the existing SimulationInputs contract. No competing FinancialSnapshot class or blind branch merge. Plan A/B policies use the exact same supplied component paths as status quo.
- State-dependent prepayment follows section 9: discretionary cut 30%, surplus prepayment 80%, reserve multiples 1.0 and 2.5. Essentials, actual scheduled loan payments, non-loan debt settlements and SIP are retained. Loans accrue monthly interest, amortize, then prepay in descending interest-rate order.
- Recommendations first enforce deterministic reserve feasibility and the supplied risk ceiling, then maximize terminal cash minus remaining debt. If no action policy meets both limits, choose minimum empirical risk and explicitly flag failure. Status quo is the comparison reference, not an action candidate.
- FreelancerEvaluator replays thirty completed calendar months. Initial fit: 1-18; validation/calibration: 19-24; final test: 25-30. Every origin refits using only earlier months. Earlier observed test outcomes may enter later origins; hyperparameters/calibration never use future or final-test outcomes.

## Protocol and Denominators

Four synthetic histories (stable income, irregular/zero income, income drop, expense shock), two seeds (42, 73), eight runs. Each split has a separate generation seed stream; complete generated monthly inputs are saved. They are monthly aggregates, not a replay of the app's SMS/account ingestion.

Each run has six validation origins and six test origins, forecasting up to six steps without crossing the split endpoint: 6+5+4+3+2+1 = 21 observations per target/model. Step denominators are 6, 5, 4, 3, 2, 1. Overlapping observations are dependent, not 21 independent months. There are six distinct test months per run.

Targets: net bank cash flow (including actual refunds) and cumulative ending bank cash from the explicit synthetic opening balance. Cumulative intervals are quantiles of complete sampled cash trajectories, never sums of marginal quantiles.

Calibration: separate validation-only factor for each target; rank ceil((n+1)*0.8), factor at least 1. Half-width floor INR 0.01 is used identically when fitting and applying, including zero raw width. Median and asymmetric raw bounds are preserved. This rank adjustment does not establish a coverage guarantee for dependent time-series data. Calibration widens reported forecast intervals only; it does not rescale the component paths or calibrate plan-risk probabilities.

Baselines: historical net-flow mean with 1.28155 standard deviations (cumulative uncertainty scales with sqrt(h), an independence approximation); recurring-only uses externally declared recurring income minus last observed essentials, debt service and SIP, excludes variable spending, and has a zero-width deterministic band. Neither baseline invents salary/refunds. Replay forecasts persist the last observed fixed obligations; they do not inspect future expense shocks or future commitments.

## Actual Held-Out Results

Net cash flow, all 21 test origin-step pairs per run. INR amounts rounded from the generated report; coverage below is an observed percentage, not a general model accuracy score. Full RMSE, interval widths, pinball losses, ending-cash results and per-step scores are in the JSON.

| Scenario | Seed | Bootstrap MAE | Mean-baseline MAE | Recurring MAE | Bootstrap Coverage | Bootstrap Mean Width |
|---|---:|---:|---:|---:|---:|---:|
| Stable | 42 | 2,954.38 | 3,270.07 | 8,333.99 | 100.00% | 12,463.61 |
| Stable | 73 | 3,387.14 | 2,841.01 | 9,842.37 | 85.71% | 10,645.83 |
| Irregular | 42 | 25,347.77 | 25,566.09 | 41,314.30 | 90.48% | 90,691.19 |
| Irregular | 73 | 41,905.87 | 36,146.20 | 34,332.30 | 95.24% | 116,220.90 |
| Income drop | 42 | 28,620.01 | 26,093.36 | 21,666.01 | 9.52% | 20,133.08 |
| Income drop | 73 | 28,966.15 | 28,274.40 | 20,157.63 | 19.05% | 17,644.16 |
| Expense shock | 42 | 5,550.53 | 4,118.89 | 51,666.01 | 71.43% | 12,463.61 |
| Expense shock | 73 | 7,438.31 | 5,085.97 | 50,157.63 | 61.90% | 10,645.83 |

The model does not consistently beat the historical baseline. Abrupt income drops are a documented failure: only 2/21 and 4/21 flow outcomes are covered. High coverage on irregular income also comes with wide intervals. No retuning was done to hide these test outcomes. Real-user forecasting accuracy has not been established.

## Decision Contribution

Computed synthetic example: cash INR 100,000; reserve INR 40,000; monthly point income INR 60,000; essentials INR 30,000; loan EMI INR 10,000; debt INR 200,000 at annual rate 0.12. SIP and discretionary spending are zero in this isolated demonstration.

The SAME point path/policies are used for both comparisons. Certain run: all 2,000 paths match the point path. Uncertain run: 1,000 point paths and 1,000 otherwise identical paths with zero income in period 2. This synthetic uncertainty test is distinct from the receipt-delay test; it does not claim a moved receipt unless the typed delay scenario is used.

| Comparison | A Risk | B Risk | Selected |
|---|---:|---:|---|
| Certain | 0% | 0% | A |
| Uncertain | 50% | 0% | B |

Risk ceiling remains 15%. A's deterministic terminal net position remains INR 74,919.29, versus B's INR 72,014.34. Selection changes because empirical reserve-breach risk changes, not because the point forecast or candidate policies were changed. Full computed monthly tables and loan/interest results are saved. A separate integration test runs FreelancerForecaster -> ForecastPathAdapter -> comparison with a typed period-2 delay, conserving total receipts.

## Reproduce

From android_app in PowerShell:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests '*FreelancerEvaluationTest*' --tests '*FreelancerDecisionTest*'
.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest :app:compileDebugKotlin
```

Actual verification: 20 focused cases; final full JVM suite 76 tests, zero failures/errors/skips; production Kotlin compile passed. Existing SDK XML-version warning is non-fatal. If tests are up-to-date after cleaning only the reports, add --rerun-tasks to regenerate evidence.

Artifacts generated by tests under android_app/app/build/reports/freelancer/:

- phase3a-evaluation.json: inputs, declarations, seeds, calibration factors/sample counts, validation predictions, test predictions and metrics.
- phase3a-decisions.json: inputs/path templates and frequencies, selected plan, absolute/period risk, terminal cash quantiles and per-period tables.

Build artifacts are gitignored and should be regenerated or included explicitly in the submission evidence package; this note does not claim they were committed.

## Remaining Limits

No Room/UI integration, automatic financial action, speech/localization/provider changes, intramonth due-date simulation, real-user validation or phone performance benchmark. Existing ImpactSimulator and ConflictResolver remain unchanged. Loan-policy parity has regression tests, but branch-wide snapshot reconciliation is not claimed. Data remain local synthetic aggregates; no API key or private SMS is used. Backend tests were not rerun in this Android-only change.
