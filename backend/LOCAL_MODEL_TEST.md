# Local Model Smoke Test

Test model feasibility now, then continue Phase 2A. This optional probe does not replace or
complete any forecast, provider integration, NumberVerifier or localization phase.

## Run from the repository root in PowerShell

```powershell
& '.\backend\.venv\Scripts\python.exe' '.\backend\local_model_smoke.py' --model qwen2.5-coder:7b --timeout 90
```

Use `ollama list` to see installed tags. Qwen3-8B remains an evaluation candidate, not an installed
or approved default on this laptop. The script defaults to `qwen3:8b`; select an installed model
explicitly until a download is agreed. It never installs Ollama, pulls models or upgrades anything.

The installed 7B coder model is a connectivity/format baseline, not a recommended financial advisor.
The installed 27B model is much larger than the laptop's GPU memory; do not assume it will meet
latency requirements. RAM and GPU offloading affect speed. No new provider API key is needed.

## Scope and Safety

- Fixed synthetic English aggregates only; no user SMS, files, merchant names or account numbers.
- Uses the existing `NarrateRequest` and `TextNarration` schemas, not a competing production API.
- Calls only `127.0.0.1:11434`; disables redirects/proxies and rejects cloud model tags/remote metadata.
- Requests bounded context/output, validates response sizes/schema and fails with fixed error messages.
- Prints JSON-escaped generated text to the terminal; does not persist prompts or responses.
- No route registration, Android change, Gemini fallback or production provider switch.
- Does not implement Snehal's regional templates or Roshan's speech integration.

Ollama needs to be running locally. Keep its service loopback-only; do not expose port 11434
publicly or set a broad network bind for this test. HTTP here is limited to the same machine,
not permitted for external production connections.

## Interpret the Result Honestly

`schema_valid: true` proves only completion and response shape. The reported elapsed seconds
cover the chat request and can include model loading; one run is not a latency benchmark.

`production_number_verifier: NOT_RUN` and `semantic_review: REQUIRED_NOT_SAFE_FOR_APP_USE`
are intentional. This Python probe does not reproduce the Kotlin NumberVerifier. Manually check
numbers, risk direction and whether advice is actually supported. Neither JSON validation nor
number matching proves the meaning is correct. Never pass this raw output to UI or speech.

Before selecting a provider, use a separate frozen evaluation set, compare results, measure warm
and cold latency, and route production output through the existing verifier/fallback contract.
Hindi/Marathi/Hinglish review belongs to Snehal and is not completed by this English probe.

The first local coder-model trial produced valid JSON but unsupported plan-benefit and SIP advice.
The prompt was tightened afterwards; prompt-tuning examples must not also count as held-out
evaluation. Production narration remains unchanged until a reviewed provider adapter is added.

## Offline Harness and Existing Proxy Tests

```powershell
Push-Location '.\backend'
try {
    & '.\.venv\Scripts\python.exe' -m unittest test_local_model_smoke test_copilot -v
} finally {
    Pop-Location
}
```

The transport/schema implementation follows [Ollama chat](https://docs.ollama.com/api/chat) and
[structured-output documentation](https://docs.ollama.com/capabilities/structured-outputs).
