# Ollama and Groq: Synthetic Narration Comparison

This is an exploratory English fact-preservation test, not financial forecast accuracy,
general intelligence, translation quality, anomaly accuracy or a deployment-safety certificate.
Ollama/Groq are runtimes/providers: the actual default models compared are
`qwen2.5-coder:7b` (local) and `openai/gpt-oss-20b` (Groq). A difference cannot be attributed
to the hosting service alone because model sizes/families and runtime settings differ.

## Run Both from Your Existing Authenticated PowerShell Terminal

From the repository root:

```powershell
& .\backend\.venv\Scripts\python.exe .\backend\model_benchmark.py --providers both
```

The same terminal must have `$env:GROQ_API_KEY` configured. A key set in another terminal is
not inherited by Codex. The script does not read shell history, scrape other processes,
load `.env`, install models, download weights or enable a paid plan.

If a new terminal needs the key, use a hidden prompt, not a literal key in a command:

```powershell
$secret = Read-Host 'Paste Groq secret key' -AsSecureString
$env:GROQ_API_KEY = ([System.Net.NetworkCredential]::new('', $secret).Password).Trim()
Remove-Variable secret
```

Separate runs or repeated trials:

```powershell
& .\backend\.venv\Scripts\python.exe .\backend\model_benchmark.py --providers ollama
& .\backend\.venv\Scripts\python.exe .\backend\model_benchmark.py --providers groq
& .\backend\.venv\Scripts\python.exe .\backend\model_benchmark.py --providers both --repeats 3
```

One complete comparison makes 20 local and 20 Groq inference requests. Repeats multiply that
count; Groq requests consume account quota and use the account's configured billing plan.
Verify that the account remains free-tier; this script cannot enforce provider billing settings.
Missing keys/local models are marked NOT RUN, with null scores, not fake zero-accuracy results.
Exit code 2 means incomplete execution; a saved report can still contain useful local results.
There is no automatic retry or provider fallback; authentication, billing and 429 errors stop
that provider. Compare only reports with equal fixtures, prompt hash, repeat count and coverage.

## Cases

Twenty hand-authored aggregate fixtures cover the following cases. The first twelve remain
unchanged; eight numeric boundary cases have been appended.

| Case id | What is supplied/tested |
|---|---|
| stable | Cash 45,200, risk 12%, positive forecast interval |
| income_drop | Lower-cash/higher-risk aggregate pattern; negative lower forecast |
| zero_cash | No bank cash and high risk |
| zero_risk | Exactly 0% risk without inventing extra guarantees |
| certain_shortfall | Exactly 100% supplied risk and entirely negative forecast |
| decimal_values | Decimal currency, fractional percent and negative amount |
| one_month | One-month horizon preserved rather than defaulting to six |
| long_horizon | Maximum supported 24-month horizon |
| equal_quantiles | Identical lower/median/upper values |
| small_amounts | Small cash, negative/zero/positive forecasts |
| large_amounts | Multi-million amounts preserved, not abbreviated incorrectly |
| low_buffer | Lower-cash aggregate pattern with different risk/forecast |
| all_zero | Every cash/risk/forecast value zero |
| signed_zero | -0.0 and 0 treated as numerically equal |
| tiny_risk | 0.01% risk, not rounded to 0% or changed to 1% |
| near_certain_risk | 99.99% risk, not rounded to 100% |
| one_paisa | INR 0.01 and INR -0.01 preserved |
| api_balance_limit | Positive/negative 1,000,000,000,000 forecast bounds |
| nearly_equal_values | -100.01, -100.00 and -99.99 stay attached to correct quantiles |
| zero_crossing | Forecast interval crosses zero with zero median |

Values are synthetic expected narration facts, NOT claimed outputs of the forecasting engine.
The model is restating numbers, not identifying an income drop, detecting an anomaly or computing
reserve risk. Never use this runner for private data.

### Separate Offline Edge/Failure Tests

The scorer/transport tests also cover swapped quantiles, wrong plans, omitted facts, invented
numbers, repeated contradictory labels, malformed/duplicate-key JSON, extra keys, empty/null
text, wrong array counts, Indian comma grouping, signed zero, exponent/fraction/scale-word
numeric tricks, missing API keys, 429 responses, truncated generations, malformed response
envelopes, oversized output, timeouts and connection failures. Existing smoke tests separately
cover local-only transport, disabled redirects/proxies and rejection of cloud-backed aliases.
These are simulated failure tests, not deliberate real requests to exhaust quota or break services.

The new edge tests found and fixed scorer false positives for numeric prefixes, fractions and
scale words, plus a false negative for Indian comma grouping. Original 12-case reports remain
historical evidence; use a fresh full 20-case run for comparison after these scorer fixes.

## Metrics

- Fact accuracy: correctly preserved labelled facts / (7 x attempted cases). The seven are
  bank cash, shortfall-risk percent, P10, P50, P90, horizon and plan id. Swapped values fail.
- Schema validity: responses passing the existing TextNarration contract / attempted cases.
- Numeric grounding: schema-valid responses with no extra numeric values / attempted cases.
- Case pass: all seven facts correct, no extra numeric values, and the required simulation
  caveat present. Failed requests contribute zero to each metric, not disappear from denominators.
- Median attempt latency: includes failed request time and can include cold model loading.
  This sequential small-sample comparison is not a controlled infrastructure benchmark.

The prompt requires fixed English labels so field binding is independently checkable. Therefore
fact accuracy also depends on label-format compliance; correct free-form paraphrases can fail.
Both providers receive identical messages/schema instructions and JSON-object mode rather
than different schema-constrained decoding. Output budgets/runtime implementations still differ.
No LLM grades its own output. The scorer rejects swapped facts, missing/extra numbers and wrong
plan ids but does not prove semantic correctness, appropriate advice or regional-script quality.
Production Kotlin NumberVerifier is NOT invoked or replaced by this scorer.

These fixtures include previously used development values. Do not present them as a held-out
evaluation or tune prompts on them and then claim general model accuracy. Before a final
selection, freeze a separate unseen set, have reference labels independently reviewed, repeat
trials and perform human advice-safety/language review. A 100% score means only this small task.

## Evidence

Timestamped UTF-8 JSON reports are written under `backend/build/model-evaluation/` (git-ignored).
They retain prompt hash/text, synthetic fixtures, model ids, per-case raw synthetic output,
failed facts, request failures, attempted/planned counts and metrics. They never contain keys,
authorization headers or raw provider errors. No app route, UI or production provider is changed.

Review saved output even when every automated check passes. In particular, check unsupported
benefit claims and advice direction. No winner is declared automatically from partial results.

## Offline Regression Tests

From `backend/`:

```powershell
.\.venv\Scripts\python.exe -m unittest test_model_benchmark test_local_model_smoke test_copilot -v
```

Provider protocol references: [Groq compatibility](https://console.groq.com/docs/openai) and
[Ollama chat](https://docs.ollama.com/api/chat).
