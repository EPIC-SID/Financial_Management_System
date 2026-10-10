# Language / meaning review (Phase 4C)

Reviewed `NarrationLanguage.examples()` and `LocalizedAdvisor` copy in all four modes. No templates were edited in this phase.

## Financial clarity (keep)

| Rule | English | Hinglish / Hindi / Marathi |
|---|---|---|
| Two-view ledger | Card dues are separate and are not subtracted from cash | Card dues alag / बकाया अलग / थकबाकी वेगळी — minus nahi |
| Shock uses cash | Bill leaves `afterShock` in cash | Same meaning; card dues unchanged |
| Reserve is a floor | Keep at least `reserve` | कम से कम / किमान रिज़र्व |
| P10 is not a promise | "P10 is a low path, not a promise" | निम्न पथ / निम्न मार्ग — वादा नहीं / वचन नाही |

Persona B sample lines (copy, not scores) are in `tasks/review/language-examples.md`.

## Obligation wording

Recommendations are **advice**, not a legal must.

| Tone used | Example | Do not read as |
|---|---|---|
| Imperative advice | Keep / rakho / रखो / ठेवा the reserve | Court order |
| Soften when blocked | Delay or split the payment | You must pay today |
| Goal miss | Relax the date or the amount | Goal is cancelled |

Income line says **expected** / उम्मीद / अपेक्षित — not "guaranteed salary".

## Pronunciation (Roshan TTS)

| Token | Speak |
|---|---|
| `₹` | rupaye (en-IN / hi-IN / mr-IN) |
| `P10` | "pee ten" — keep Latin; do not expand to a fake Hindi word |
| Dates | ISO `yyyy-mm-dd` as written (engine output) |
| Hinglish | `hi-IN` voice, Latin script on screen |
| Hindi | `hi-IN` voice, Devanagari |
| Marathi | `mr-IN` voice, Devanagari |

## Left as-is (flag, do not fix in 4C)

Hindi and Marathi still contain English tokens **P10**, **inflow**, **runway**. Meaning is clear for a demo; a later copy pass can replace them (e.g. inflow → incoming credit / आगम). Do not change `LocalizedAdvisor.kt` in this phase.

## Local Kirana (from 3C)

Gold category is `GROCERIES`. Seeder B/C still tag `FOOD`. Not a translation issue.
