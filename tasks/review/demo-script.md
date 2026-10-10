# Demo script (live judges)

Persona B, `Scenario.STABLE_INCOME`, unless a step says otherwise. Figures below are **seeder inputs**, not measured model scores.

## Cold start (30 seconds)

1. Open the health cockpit on **Persona B**.
2. Point at bank cash. Say: card dues are shown beside it and are **not** subtracted. Seeder demo cash is ₹45,200 (`SyntheticPersonas.B.bank.balanceAtDemo`).
3. Salary line: ₹85,000 on the 1st (`SyntheticPersonas.B.income`).

## Language chips (45 seconds)

4. Switch **English → Hinglish → Hindi → Marathi**. Codes: `en`, `hi-Latn`, `hi-Deva`, `mr-Deva`.
5. Same numbers, new script. If proxy is off, card still fills from `LocalizedAdvisor`.

## Five copilot intents (2 minutes)

Ask each once. Offline fallback is enough if Gemini is down.

| Say | Intent | What the card must show |
|---|---|---|
| How much cash do I have? | `CASH_OVERVIEW` | Cash and card dues as two numbers |
| When is salary? | `INCOME` | ₹85,000, day 1 |
| Can I take an ₹8k hospital bill? | `SHOCK` | Pass shockAmount = `SHOCK_MEDIAN` (8000) |
| How long am I fine? | `RUNWAY` | Verdict `fineUntil` / `tightOn` from the engine, not a typed date |
| Can I hit the goal? | `GOAL` | Uses `DeclaredInputs.goal` if set |

Speaker: tap 🔊 on the **verified** text only (`en-IN` / `hi-IN` / `mr-IN`).

## Scenario overlays (1 minute)

6. Chip **Emergency ₹25k** (`Scenario.UNEXPECTED_EXPENSE`). Seeder injects Apollo Hospital ₹25,000 in test month 27.
7. Optional: **Income -30%** (test-window income becomes ₹55,000 from month 26 — `REDUCED_INCOME`).
8. Do **not** change seeder rent to ₹15,000. That case is fixture-only in `tasks/fixtures/sprint2-scenarios.json`. Persona B rent stays ₹22,000.

## Close (15 seconds)

9. If NumberVerifier rejects Gemini: "We fall back to on-device templates. Amounts still come from the snapshot."
10. Do not quote MAE, coverage %, or health-score tables. Those belong in Phase 5C evidence.

## Fixture pointers

- Stories: `tasks/fixtures/sprint2-scenarios.json`
- Merchant gold: `tasks/fixtures/categorization-labels.csv`
- Chip sample lines: `tasks/review/language-examples.md`
