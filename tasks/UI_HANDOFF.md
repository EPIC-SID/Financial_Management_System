# UI handoff (Track 2 → Track 1)

Uday owns screens. Do not edit `NavGraph.kt` from this track. Wire Copilot to these contracts.

## Language chips

Source: `NarrationLanguage.entries`

| Chip label | `code` (proxy) | `locale` (TTS) |
|---|---|---|
| English | `en` | `en-IN` |
| Hinglish | `hi-Latn` | `hi-IN` |
| Hindi | `hi-Deva` | `hi-IN` |
| Marathi | `mr-Deva` | `mr-IN` |

`NarrationLanguage.fromCode(code)` rejects unknown codes. Hinglish and Hindi share `hi-IN`; script differs (`hi-Latn` vs `hi-Deva`).

Sample chip copy: `tasks/review/language-examples.md`.

## Three-section card

`StructuredResponse`: **observed** / **predicted** / **recommended**.

Each predicted row already has `range` (p10/p50/p90), `confidenceLabel`, `horizon`. Each recommended row has `impact` and `tradeOff`. Do not recompute those in the UI.

## Offline fallback

When the proxy is down or NumberVerifier rejects:

```
LocalizedAdvisor.advise(
    language,          // selected chip
    intent,            // CopilotIntent
    snapshot,          // HealthState.snapshot
    view,              // HealthState.view
    verdict,           // HealthState.verdict
    shockAmount,       // finite, >= 0
)
```

Intents: `CASH_OVERVIEW`, `INCOME`, `SHOCK`, `RUNWAY`, `GOAL`.

Speaker button speaks **verified text only**, using `language.locale`. Roshan owns TTS.

## Numbers on screen

- Bank cash = `ConsolidatedView.liquidCash`. **Do not subtract** `cardDues`.
- Default demo persona: `Persona.B` + `Scenario.STABLE_INCOME` (`FinancialHealthRepository`).
- Scenario chips use `Scenario.label` (`Stable`, `Income -30%`, `Emergency ₹25k`, `Infeasible`, `High Uncertainty`).
- Shock demo: pass `SyntheticDataSeeder.SHOCK_MEDIAN` (8_000) or the emergency overlay (25_000). Do not type a new amount in the UI.

## Do not build here

Cockpit / Plan / Copilot composables, NavGraph routes, evidence zip (Phase 5C).
