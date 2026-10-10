# Phase 4A: Localized Proxy and Verification

Owner: Siddhant. Verified October 11, 2026 on the existing Siddhant branch.
No commit created. Earlier user work preserved; no shared engine, UI, manifest or build edits.

## Implemented Boundary

ResultBundle + languageCode -> proxy language instruction -> localized model text ->
original engine metadata + NumberVerifier -> accepted response OR entire requested-language fallback.

Codes: en (default), hi-Latn (Hinglish), hi-Deva (Hindi script), mr-Deva (Marathi script).
Unsupported codes are rejected before requests. Raw questions, merchant names and account identities
are not sent to Copilot: interpret still receives only the locally selected intent.

BackendClient accepts an optional constructor callback:

```kotlin
localizedAdvisor: ((CopilotIntent, ResultBundle, String) -> StructuredResponse)?
```

Use `client.narrate(intent, bundle, "hi-Deva")` or
`client.answer(question, "hi-Deva") { intent -> resultBundle }` once a real fallback provider is installed.
Existing English call signatures remain compatible, including trailing result-provider lambdas.
Without a regional provider, regional mode throws IllegalArgumentException, not an English response.
An injected fallback with invented numbers, invalid metadata or non-LOCAL source also fails explicitly.
Provider callbacks are trusted application code; their actual language/meaning requires human review.

The proxy's optional languageCode defaults to en for old clients. It echoes the code and derives
range/confidence/horizon/impact from numeric input, not from generated text. Client checks supplied
metadata but never installs it over local engine metadata. Old English responses may omit metadata
and language echo; regional responses must echo the selected code. Sections still contain exactly
one item on the wire. Client retains the localized fallback's refs and tradeOff.

Authentication, aggregate-only allowlists, bounded transport, redirects policy, rate limiting,
five-second timeouts and generic provider errors are unchanged. No new dependencies or secrets.
Existing Gemini provider/configuration is unchanged; this is not a Groq/Ollama production adapter.

LocalAdvisor money formatting now uses decimal INR to two places, retaining paise without Int
clamping above INR 2,147,483,647. This extra small fix is necessary for fallback numeric verification.

## Verification

Tests were written first. JVM compilation failed on missing language/fallback parameters; new
proxy tests failed because languageCode was forbidden/missing in responses. Implementation then
passed focused tests, followed by expanded boundary tests and full regression.

- LocalizedNarrationTest: 12 cases, synthetic fixtures and mocked transport only.
- Existing NumberVerifierTest: 7 cases unchanged and passing.
- Complete JVM suite: 88 tests, no failures/errors/skips; production Kotlin compilation passed.
- Proxy suite: 12 tests passed (9 existing, 3 new).
- Combined backend narration/harness regression: 35 tests passed (12 + 7 + 16).
- git diff --check: passed; Git reports normal LF/CRLF conversion notices.
- Non-fatal existing Android SDK XML-version and Starlette/httpx deprecation warnings remain.

Coverage: four language codes, old English requests/responses, language echo mismatch/missing,
wrong or missing JSON sections, correct numbers, hallucinated numbers, empty/oversized text,
wrong intent/source/forecast range/horizon/confidence/impact, invalid fallback, native digits,
localized scale/percentage words, offline, timeout, HTTP failure, coroutine cancellation,
large INR values, paise, and aggregate-only localized answer requests.

From android_app, using Android Studio JBR:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests '*LocalizedNarrationTest*' --tests '*NumberVerifierTest*'
.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest :app:compileDebugKotlin
```

From backend:

```powershell
.\.venv\Scripts\python.exe -m unittest test_copilot -v
.\.venv\Scripts\python.exe -m unittest test_copilot test_local_model_smoke test_model_benchmark -v
```

Graph coverage excludes these Android sources and is unavailable for the proxy; verification
relied on direct source reads and actual tests, not graph absence.

## Explicit Limits

NumberVerifier is a lexical numeric matcher with existing displayed-precision rounding. It does
not bind each number to its financial field, verify risk direction, recognize every number word,
or certify language/meaning from a language-code echo. A valid bundle number attached to the wrong
concept can still pass. Native digits and known Hindi/Marathi multipliers/percentage words are
rejected; the prompt requires ASCII digits, INR, % and preserved signs, and bans number/scale words.
These restrictions are not a complete multilingual semantic validator.

Real offline translations remain Snehal's responsibility and were not duplicated here. Integration
uses tiny test-only fallback fixtures as agreed in the plan. Human review of accepted/fallback
Hindi/Marathi meaning and financial units is required before enabling regional output in the app.
No live provider call, supported model-ID check, speech, phone behavior or UI verification was run.
Current Gemini default/model availability remains unverified; failures still use offline templates.
Production prediction engine and freelancer routing remain unchanged. Phase 5A assembly is pending.
