# Hybrid Demo Runbook

## Data Boundaries

### Unified Activity Modes

The top mode switch now selects **Demo** or **My Transactions**. Both use the same Activity layout. Demo includes Overview, Plan and Copilot; My Transactions shows local SMS Activity and account/profile navigation only. Switching modes clears Copilot replies, saved What-if selections and the navigation back stack. Demo planner inputs never come from the SMS database.

Activity date/category filters do not scan SMS or request permission. The refresh icon starts an explicit SMS permission/date-range flow. Activity settings contains separate cloud-sharing consent, cloud retry and a local-only category recheck. Nothing is uploaded merely by opening Activity or switching modes.

Credits and debits are raw transaction totals, not verified income/consumption or bank balance: the legacy SMS entity does not distinguish every transfer, refund or card settlement. Demo Activity is also a raw ledger and excludes held-out future transactions. Unknown dates appear only under All dates.

Tap a live transaction to correct its merchant category locally. The save action updates matching saved merchant labels; late cloud responses cannot overwrite an already corrected category. The local recheck repairs only still-unclassified debits using known merchant rules, including BLINK COMMERCE. Ambiguous labels such as Google remain unclassified unless resolved or manually corrected. No automatic rewrite of existing transactions runs on mode switch.

- Activity reads real SMS/Room transactions. Synthetic planner data is never inserted into Room.
- Cockpit, Plan, Conflict and Copilot use explicitly labelled synthetic scenarios, including demo loans, SIP and EMI.
- DemoFinanceCalculator -> history normalizer -> freelancer forecast / declared stable income -> 2,000 cash-flow paths -> plan comparison and conflict resolver -> shared HybridViewModel -> screens.
- Copilot sends anonymous numeric aggregates through BackendClient. Backend narration uses Groq when configured, then Gemini; unavailable or invalid narration falls back to LocalAdvisor. NumberVerifier checks returned numbers before display.
- English, Hinglish, Hindi and Marathi narration use the same calculated ResultBundle. Language changes clear the previous answer.
- What-if runs ImpactSimulator. Saving keeps a separate demo-session draft; it does not execute financial actions or change the baseline.
- Applying a feasible plan records the selected demo-session plan only. No payments are initiated.

## Run on the Connected Phone

The backend is currently bound to localhost port 8000. USB forwarding lets the debug app reach it without exposing the service to the LAN.

```powershell
# Repository root; start this only when port 8000 is not already running.
cd backend
.\.venv\Scripts\python.exe -m uvicorn main:app --host 127.0.0.1 --port 8000
```

In another PowerShell terminal:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb reverse tcp:8000 tcp:8000
Invoke-RestMethod http://127.0.0.1:8000/health
```

The current debug BACKEND_URL is http://127.0.0.1:8000. USB forwarding is needed after reconnecting the phone. Remote routes still require the existing authenticated session. Provider keys belong only in the backend environment, never the APK or Git. The current health check reports Groq missing its key; Gemini configuration exists but successful live narration has not been verified.

## Demo Checklist

1. Open Demo: confirm the synthetic label and calculated cash/card/EMI/SIP metrics.
2. Open Plan: compare three policies and six monthly balances with sampled ranges.
3. Select income reduction, unexpected expense, high uncertainty and infeasible-target scenarios from the Demo menu. Wait for recalculation.
4. For an infeasible target, review Conflict. Impossible relaxations must remain disabled.
5. Open What-if: change income, surprise expense or SIP and confirm metric changes; reset or save the separate draft.
6. Ask Copilot about risk, cash or plans. Switch narration language, then ask again. Local fallback must be labelled.
7. Open Live SMS only with the device owner's permission. Grant SMS access and sync explicitly. Cloud merchant categorization is off by default and requires separate consent.

## Checks

```powershell
cd android_app
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest :app:assembleDebug
```

```powershell
cd backend
.\.venv\Scripts\python.exe -m unittest discover -v
```

Focused new coverage: DemoFinanceTest, HybridIntegrationTest and RemoteCategorizationPolicyTest. Tests cover historical-only inputs, settlement separation, scenario changes, sampled bands, conflict gating, verified narration, fallback, stale request cancellation, language switching and remote categorization input restrictions.

## Limits

### Simulation Evidence and UI Coverage

The scenario selector is visible above navigation on every screen. Selecting Plan A/B is a policy change within the current scenario, not a scenario switch. Period details now show whole-rupee point balances, empirical P10/P50/P90 and period reserve-breach risks. Plan summaries show the actual breached-path count and denominator; nonzero risks below 0.1% are not displayed as zero.

Seeded regression results (current explicit synthetic assumptions):

| Scenario | Plan A reserve breaches | Plan B reserve breaches | Status quo reserve breaches |
| --- | --- | --- | --- |
| Stable | 0/2000 | 0/2000 | 0/2000 |
| Income reduction | 2000/2000 | 2000/2000 | 2000/2000 |
| Emergency expense | 2000/2000 | 0/2000 | 0/2000 |
| Infeasible target | 2000/2000 | 2000/2000 | 2000/2000 |
| High uncertainty | 1756/2000 | 692/2000 | 613/2000 |

The emergency Cockpit reports zero because the recommended Plan B avoids a reserve breach in all sampled paths; Compare mode exposes Plan A's risk. Zero empirical breaches is not a guarantee. Risk means cash below the declared reserve at least once, not failure to reach a savings goal.

The stable demo deliberately uses fixed salary, declared recurring essentials/SIP and a fixed historical-average card settlement. Only bank-paid discretionary spending varies in its samples, so its intervals are narrow. These are limited assumptions, not a full learned model of all expenses. No random variation was added merely to make the screen look different.

| Capability | Current runtime UI integration |
| --- | --- |
| 2,000 forecast paths, three policies, monthly bands and risks | Connected through DemoFinanceCalculator / HybridViewModel |
| ImpactSimulator | Connected to What-if; saved separately, baseline unchanged |
| ConflictResolver | Connected to infeasible goal and feasible relaxation actions |
| Backend narration, verification and localized fallback | Connected; live provider/session setup still required |
| DecisionPipeline orchestration | Implemented but not called by this hybrid navigation path |
| Unusual-expense detector and posted-expense alerts | Not wired to hybrid screens |
| Explicit freelancer payment-delay scenario | Forecaster supports it; no runtime UI control yet |
| Regional speech playback | Player exists; no runtime playback control yet |
| Held-out forecast evaluation and baseline comparison | Evaluator exists; no runtime evidence screen yet |
| Real SMS -> financial planning | Deliberately not connected without resolved account/obligation inputs |

- Synthetic forecasts are demonstration outputs, not calibrated real-world financial predictions.
- Live SMS balances and history do not feed planning in this hybrid slice; missing account details are not guessed.
- Eight-week planning is disabled because this integration provides six monthly periods.
- Regional speech playback is not newly integrated here; existing microphone input is separate from narration language.
- No live SMS ingestion or cloud merchant upload was triggered during automated device checks.
- Old snapshot/verdict implementations remain outside this demo path; this work does not certify those legacy paths.
