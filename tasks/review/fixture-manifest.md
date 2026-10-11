# Fixture manifest (Phase 3C)

Gold labels and evaluation stories for Sprint 2. Amounts are seeder inputs, not benchmark results.

| File | What it is | Who uses it |
|---|---|---|
| `tasks/fixtures/sprint2-scenarios.json` | Freelancer income, delayed receipts, ₹8k shock, ₹15k rent | T3/T4 engines, LocalizedAdvisor SHOCK, demo what-if |
| `tasks/fixtures/categorization-labels.csv` | Merchant → category gold list | T3 categorizer QA (Roshan) |
| `tasks/review/language-examples.md` | 1C narration samples | T1 chips, T4 proxy codes |

## Scenario sources

| id | Comes from | Do not treat as a measured score |
|---|---|---|
| `freelancer_income` | Persona A `IncomeKind.AR1`, base 30000, `logSigma` 0.2, `phi` 0.3 | input |
| `delayed_receipts` | Persona A `slipChance` 0.2 | input |
| `shock_8k` | `SyntheticDataSeeder.SHOCK_MEDIAN` | input |
| `rent_15k` | Named in `HACKATHON_MASTER_PLAN_ROUND2.md` Phase 3C | fixture-only; seeder A rent is 8000, B is 22000 |

## Category review

CSV rows are the unique merchants from `SyntheticPersonas` plus seeder shock / emergency / sweep names.

One conflict was reviewed: **Local Kirana** is `GROCERIES` (kirana = grocery). Persona B/C currently tag it `FOOD` in the seeder. Do not change the seeder in this phase.

## Not in this pack

Forecast MAE/RMSE, health-score tables, demo script, UI handoff, evidence index (Phases 4C and 5C).
