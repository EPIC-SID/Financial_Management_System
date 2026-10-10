# Evidence index (Phase 5C)

Map for submission. **Do not paste MAE / RMSE / coverage / health-score totals here.** Those come from the test run or T3 exporter. This page only points at sources and how to regenerate them.

## Seeds (reproduce the ledger)

From `SyntheticDataSeeder.Seeds` defaults and `VerdictEngine.run`:

| Stream | Constant | Value |
|---|---|---|
| Train months 1–18 | `Seeds.train` | `101` |
| Validation months 19–24 | `Seeds.validation` | `202` |
| Test months 25–30 | `Seeds.test` | `303` |
| Demo date (month 25) | `SyntheticDataSeeder.DEMO_DATE` | `2026-10-01` |
| Verdict P10 paths | `VerdictEngine` default seed | `42` |

Same `Seeds(train, validation, test)` → same transactions (`SyntheticDataSeederTest`).

## Forecast baselines

T3 owns the judge ASCII table. **This branch does not contain** `ModelValidationBenchmarkTest`. Run it where it exists; do not copy the printed numbers into git.

```powershell
cd android_app
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat testDebugUnitTest --tests "*ModelValidationBenchmarkTest*"
```

Seeder overlays used as inputs (not scores): `Scenario.STABLE_INCOME`, `INCOME_REDUCTION` (`REDUCED_INCOME` from month 26), `UNEXPECTED_EXPENSE` (Apollo ₹25k in month 27), `INFEASIBLE_TARGET`, `HIGH_UNCERTAINTY`. Persona A is the freelancer / delayed-receipt fixture (`tasks/fixtures/sprint2-scenarios.json`).

## Test reports on `Snehal`

```powershell
cd android_app
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat testDebugUnitTest --tests "com.example.financeapp.domain.ai.localization.*"
.\gradlew.bat testDebugUnitTest --tests "com.example.financeapp.data.ingest.SyntheticDataSeederTest"
.\gradlew.bat testDebugUnitTest --tests "com.example.financeapp.data.FinancialHealthRepositoryTest"
.\gradlew.bat testDebugUnitTest --tests "com.example.financeapp.domain.detect.*"
.\gradlew.bat testDebugUnitTest --tests "com.example.financeapp.domain.engine.ConsolidatedViewTest"
```

HTML report after a run: `android_app/app/build/reports/tests/testDebugUnitTest/index.html`.

| Class | What it proves |
|---|---|
| `NarrationLanguageTest` | 4 codes + locales; cash/income/shock examples non-blank |
| `LocalizedAdvisorTest` | 4 languages × 5 intents; refs/range/impact stable across language |
| `SyntheticDataSeederTest` | seed replay, window split, persona B cash reconcile |
| `FinancialHealthRepositoryTest` | scenario / inject / declared recompute |
| `RecurringDetectorTest` / `InvestmentDetectorTest` | rent / SIP detection |
| `ConsolidatedViewTest` | card dues not subtracted from cash |

## Verified localized outputs

| Artifact | Role |
|---|---|
| `tasks/review/language-examples.md` | Chip samples (Persona B copy, not scores) |
| `NarrationLanguage.examples()` | Same four lines in code |
| `LocalizedAdvisor.advise(...)` | Offline three-section card; metadata from snapshot/view/verdict |
| `tasks/review/language-review.md` | Meaning + TTS notes; P10/inflow/runway left as English tokens |
| `tasks/UI_HANDOFF.md` | Chip codes `en` / `hi-Latn` / `hi-Deva` / `mr-Deva` |

Final handoff examples for judges: follow `tasks/review/demo-script.md` (Persona B → 4 chips → 5 intents → Emergency overlay).

## Fixtures

| File | Contents |
|---|---|
| `tasks/fixtures/sprint2-scenarios.json` | freelancer, delayed receipts, ₹8k shock, ₹15k rent (fixture-only) |
| `tasks/fixtures/categorization-labels.csv` | merchant gold labels |
| `tasks/review/fixture-manifest.md` | who uses which file |

## Nine-file evidence pack (T3)

Master plan: `EvidenceExporterTest` writes into `android_app/build/evidence/` (or `app/build/evidence/`). **Not on this branch.** Run, then attach that folder — do not type the tables into this file.

```powershell
cd android_app
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat testDebugUnitTest --tests "*EvidenceExporterTest*"
```

## Out of this index

Cockpit/Plan/Copilot screens (Uday). Proxy/NumberVerifier (Siddhant). TTS player (Roshan). Tracker update after you verify this page.
