# 08 - Tech Stack

**Related docs:** [05 Frontend UI style](05-frontend-ui-style.md) · [06 Backend](06-backend.md) · [09 Overall plan](09-overall-plan.md)

**Decision in one line:** native Kotlin with Jetpack Compose. No Flutter. Analytical engines in pure Kotlin on the device. A thin Python proxy for Gemini and Supabase for auth.

> **Version policy.** Versions marked "existing" come from the current RupAI build. Do not upgrade any dependency during the hackathon. Where a library is new, pin the version that resolves cleanly in your Gradle setup, and check its current status before adding it.

---

## 1. Summary table

| Layer | Choice | Why |
|---|---|---|
| Language | Kotlin 1.9, JVM target 17 (existing) | Same language as RupAI; shared by app and engines |
| UI | Jetpack Compose, Material 3 (existing) | Native, fast to build, matches the design system |
| Architecture | MVVM with unidirectional data flow; repository pattern; single `AppContainer` for dependency wiring | Simple for a four-person beginner team; no new framework |
| Async and state | Kotlin Coroutines 1.7.3 (existing), Flow, StateFlow | Reactive recomputation when data changes |
| Local database | Room 2.6.1 with KSP (existing) | Persistence; in-memory first for engines |
| DB encryption | SQLCipher for Android | Encrypted at rest on the device |
| Secrets on device | Android Keystore (tokens, hash secret, DB key) | Keys never in plain storage |
| Background work | WorkManager 2.9.0 (existing) | Batch categorisation, sync |
| SMS | `BroadcastReceiver` plus `ContentResolver` inbox query (existing) | Real-time and historical bank alerts |
| Networking | Ktor client 2.3.11 (existing), kotlinx-serialization | Calls to the proxy and Supabase |
| Analytical engines | Pure Kotlin JVM modules, no `android.*` | Tested in seconds with Gradle; no emulator needed |
| Account consolidation | `ConsolidatedViewBuilder` in pure Kotlin over `Account`, `Liability`, `Investment`; no brokerage, portfolio or aggregator API | Five streams from bank messages and declared inputs; nothing new to install |
| Charts | Compose `Canvas` (custom) | Full control of the fan chart and gauge; no heavy chart library |
| Auth and profile | Supabase (GoTrue, PKCE, Postgrest) (existing) | Already integrated |
| Cloud proxy | FastAPI (Python 3.11), Pydantic, Uvicorn (existing) | Keeps the Gemini key off the device; rate limiting and cache |
| AI | Google Gemini via the proxy; local deterministic fallback | Intent and narration only; engines compute |
| Places | Google Places API (existing, optional) | Merchant resolution, layer 3 |
| Testing | JUnit 4, Truth, MockK, Turbine (Flow tests); Compose UI tests for key screens | Fast engine tests; reproducible evaluation |
| CI | GitHub Actions: `./gradlew test`, lint, `pip-audit` for the proxy | Catch breakage early |
| Design | Figma (design system and prototypes) | Spec in [05](05-frontend-ui-style.md) |
| IDE and assistant | Android Studio with the Gemini agent | Team workflow from the existing plan |

## 2. Client (Android)

### 2.1 Project settings (existing)
- `minSdk` 26 (Android 8.0), `targetSdk` and `compileSdk` 34.
- Compose compiler and Material 3 as in the current build.
- KSP for Room.

### 2.2 Architecture
```
UI (Compose screens)
   ↑ StateFlow
ViewModels
   ↑
FinancialHealthRepository  ← combines transactions, declared inputs, scenario state
   ↑
Room (encrypted)      Engines (pure Kotlin)      BackendClient (Ktor)
```
- Every screen has one ViewModel exposing an immutable `UiState`.
- The repository exposes one `StateFlow<FinancialSnapshot>`; any change retriggers the engines.
- Engines are plain functions or small classes over plain data classes. They never touch Room or Android classes.
- Dependency wiring through one `AppContainer` object created in the `Application` class. No Hilt or Koin during the hackathon.

### 2.3 Theming and fonts
- Material 3 theme with custom color, type and shape files from [05](05-frontend-ui-style.md).
- Fonts: Manrope and Inter in `res/font` (both free, bundled).
- Icons: Material Symbols Rounded.

### 2.4 SMS and permissions
| Permission | Purpose |
|---|---|
| `READ_SMS` | Historical sync |
| `RECEIVE_SMS` | Real-time bank alerts |
| `INTERNET` | Proxy and Supabase |

Google Play restricts the SMS permission; check the current policy and the permitted-use declaration before any public release. The demo and evaluation do not depend on it. Card, loan and SIP parsing uses regex templates per allow-listed sender and needs no new permission.

### 2.5 Security-related settings
- `android:allowBackup="false"`.
- `FLAG_SECURE` on screens that show money.
- Biometric lock (`BiometricPrompt`) as a should-have.
- Token and key storage via the Android Keystore. Note: the AndroidX `security-crypto` library has been deprecated, so check its status before relying on it and prefer a Keystore-backed approach (for example Tink) if needed.

## 3. Engines (pure Kotlin)

| Need | Approach | Library |
|---|---|---|
| Random numbers, seeded | `java.util.Random` or `kotlin.random.Random(seed)` | standard library |
| Statistics (quantiles, mean, variance) | Small in-house utilities, tested | none, or Apache Commons Math (optional) |
| Monte Carlo and bootstrap | Plain loops over `DoubleArray` | none |
| Optimisation (relaxation) | Bisection and rule-based policies, no LP solver | none |
| JSON and CSV export | kotlinx-serialization, simple CSV writer | existing |
| Date handling | `java.time` (core library desugaring for API 26) | enable desugaring if needed |

Rationale: the planner is a state-dependent policy simulated over sampled paths, and relaxations are found by bisection. This avoids a solver dependency and keeps the code readable for beginners.

## 4. Cloud

### 4.1 FastAPI proxy (existing, small additions)
- Python 3.11, FastAPI, Pydantic v2, Uvicorn, a Gemini client library.
- Endpoints: `/api/v1/categorize`, `/api/v1/categorize/batch` (existing); `/api/v1/copilot/interpret`, `/api/v1/copilot/narrate`, `/api/v1/health` (new). See [06](06-backend.md), section 14.
- JWT verification against Supabase on every call; rate limiting; request size limit; redacted logging.
- Hosting: Render, Railway or Cloud Run (Docker). Choose whichever the team can deploy in under an hour. Keep one warm instance during judging to avoid cold starts.

### 4.2 Supabase (existing)
- Auth with PKCE; `user_profiles`; new `consents` table; optional `synced_aggregates` (opt-in).
- Row-level security on all tables; the service-role key stays on the server.

### 4.3 Gemini
- Used for two jobs only: question to intent, and narration of engine results. Categorisation fallback (layer 4) stays as in RupAI.
- Model choice: the current RupAI notes reference Gemini 1.5 Flash and 2.0. **Verify which models are currently available and their quotas before the event**, and keep the local fallback working in case of quota or network failure.

## 5. Data and files

| Item | Format |
|---|---|
| Synthetic data | Generated in code; seeded; exportable as CSV |
| Evidence pack | CSV, JSON, short PDF or Markdown README |
| Benchmark output | ASCII table in the test log plus `benchmark_results.json` |
| Design tokens | Kotlin files, mirrored in Figma styles |

## 6. Developer workflow

### 6.1 Repository layout
```
/android_app        Kotlin app (app module)
/backend            FastAPI proxy
/docs               These documents (01 to 09)
/evidence           Generated evidence pack (git-ignored except README)
```

### 6.2 Commands
```bash
./gradlew assembleDebug                                          # build the app
./gradlew test                                                   # all unit tests
./gradlew test --tests "*ModelValidationBenchmarkTest*"          # evaluation and evidence pack
cd backend && uvicorn main:app --reload                          # run the proxy locally
```

### 6.3 Git
Work happens on feature branches per track; the Integration Lead merges in a fixed order. See [09 Overall plan](09-overall-plan.md), section 5.

### 6.4 CI (GitHub Actions)
1. Set up JDK 17; run `./gradlew test`.
2. Run lint.
3. For the proxy: install dependencies and run `pip-audit` and the proxy tests.
4. Fail the build on any test failure.

## 7. Options considered and rejected

| Option | Why not |
|---|---|
| Flutter | Team and existing RupAI are Kotlin; adds a second stack and a platform channel for SMS; no benefit in 24 hours |
| Python engines on the server | Raw data would have to leave the device; adds latency and a deployment dependency to the demo |
| LP or MILP solver for planning | Not needed for rule-based policies plus bisection; extra dependency and learning curve |
| On-device LLM | Too heavy for this scope; Gemini with local fallback is enough |
| Account Aggregator | Requires registration and approvals; out of scope in the time available |
| Hilt or Koin | New framework to learn; a single `AppContainer` is enough |
| Third-party chart library | The fan chart and gauge need custom drawing anyway; fewer dependencies |
| Brokerage or portfolio APIs (Zerodha, Groww) | Need user credentials or OAuth and registration; portfolio value is out of scope; SIPs are detected from bank debits only |

## 8. Risks specific to the stack

| Risk | Mitigation |
|---|---|
| Gradle or dependency problems | Pin existing versions; engines have no Android dependency so tests run regardless |
| Emulator unavailable or slow | Engines tested on the JVM; Compose previews for UI; a physical phone for the demo |
| Gemini quota or network failure | Local advisor fallback; number verifier; cache |
| Room schema problems | In-memory first; engines depend on plain data classes, not DAOs |
| Proxy cold start during judging | Keep a warm instance; local fallback covers the gap |
| Play Store SMS policy | Out of scope for the hackathon; flagged for release |
| Card, loan and SIP SMS templates differ per bank | Allow-list known senders; unknown formats become "possible" items to confirm; CSV and synthetic fallbacks |
