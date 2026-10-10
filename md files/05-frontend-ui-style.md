# 05 - Frontend UI Style (Design System)

**Stack:** Jetpack Compose, Material 3, light and dark themes.
**Related docs:** [02 Pages](02-pages.md) · [03 Target audience](03-target-audience.md) · [07 UI/UX flow](07-ui-ux-flow.md) · [08 Tech stack](08-tech-stack.md)

This is the single source of truth for how the app looks and sounds. Tokens here map one to one to the Kotlin files in section 11.

---

## 1. Design principles

1. **Verdict first, evidence second.** Every main screen opens with a one-line answer, then the supporting numbers.
2. **Label the kind of statement.** Every insight is tagged Observed, Predicted or Recommended.
3. **Ranges, not false precision.** Forecasts show an interval and how it was derived. Never an unexplained confidence number.
4. **Calm, never shaming.** "Tight on 25 Oct", not "you overspent".
5. **One primary action per screen.**
6. **Numbers are sacred.** Tabular figures, Indian grouping, always computed by the engine, never by the language model.
7. **Demo tools stay hidden** behind Demo mode.
8. **Spend boldness in one place.** The memorable element is the forecast fan chart with the verdict above it. Everything else is quiet.

## 2. Visual identity in one paragraph

A warm, calm, financial-grade interface. Warm off-white (or warm near-black in dark mode) surfaces, bronze as the single brand accent, and a small set of semantic colors that always mean the same thing: blue for predictions, teal for observed facts, green / amber / red for status. Numbers are set large in a geometric sans with tabular figures; reading text is in a neutral humanist sans. Cards are quiet, outlined and tonal, with no decorative gradients.

## 3. Color

### 3.1 Brand (bronze)

| Token | Light | Dark | Use |
|---|---|---|---|
| `BronzePrimary` | #CD7F32 | #E3A25E | Primary fills, active nav indicator, focus ring |
| `BronzeDark` | #8C531B | #F0C494 | Bronze text and icons on surfaces (plain `BronzePrimary` text does not reach contrast on white) |
| `BronzeContainer` | #F6E3CF | #4A2F14 | Selected cards, tonal buttons |
| `OnBronze` | #1F1A14 | #1F1A14 | Text on bronze fills |

### 3.2 Statement-type tags (never reused for status)

| Tag (label shown) | Enum | Color | Container (light / dark) | Icon |
|---|---|---|---|---|
| Observed | `FACT` | #00897B | #D5EFEB / #0F3B37 | check_circle |
| Predicted | `PREDICTION` | #2F6FDB | #DCE8FB / #162C52 | query_stats |
| Recommended | `RECOMMENDATION` | `BronzePrimary` | `BronzeContainer` | arrow_forward |

Prediction is blue, not amber, because amber already means "watch" and blue matches the forecast band. Labels use sentence case (Observed, Predicted, Recommended), not all caps, for readability. They match the wording of the problem statement.

### 3.3 Status (health, risk, balances)

| Status | Fill / icon | Text | Container |
|---|---|---|---|
| Safe | #2E7D32 | #1B5E20 | #DDF0DE |
| Watch | #F2A100 | #8A5A00 | #FFF1D1 |
| Risk | #D93025 | #B3261E | #FBE0DE |

Status is never conveyed by color alone: always an icon and a word.

### 3.4 Neutrals

| Token | Light | Dark |
|---|---|---|
| `Background` | #FBF8F4 | #14110D |
| `Surface` | #FFFFFF | #1D1914 |
| `SurfaceVariant` | #F3ECE3 | #2A241D |
| `Outline` | #D9CFC2 | #3D352B |
| `TextPrimary` | #1F1A14 | #F2EBE1 |
| `TextSecondary` | #6B6257 | #B5AA9A |

### 3.5 Chart colors

| Element | Color |
|---|---|
| P50 forecast line | Prediction blue |
| P10-P90 band | Prediction blue at 15% opacity |
| Minimum reserve line | Risk red, dashed, 1.5dp |
| Plan A | Bronze |
| Plan B | Slate #5C6B7A |
| Status-quo baseline | Grey, dashed |
| Categories (max 8) | Food #E8896B, Travel #5BA4CF, Shopping #B07CC6, Bills #8B8F94, Health #4DB6AC, Education #7986CB, Entertainment #F2A65A, Other #B0A79B |

**Contrast rule:** body text 4.5:1, large text and icons 3:1. Verify every pair with a contrast checker before polish.

## 4. Typography

Fonts: **Manrope** for headings and all large numbers; **Inter** for body and labels. Both are free Google Fonts, bundled in `res/font`. Enable tabular numerals on every money value.

| Style | Font | Size / line | Weight | Use |
|---|---|---|---|---|
| NumberXL | Manrope | 36/40 | ExtraBold, tabular | Score, hero balance |
| HeadlineL | Manrope | 28/36 | Bold | Verdict line, screen heroes |
| HeadlineM | Manrope | 22/28 | Bold | Section titles |
| Title | Manrope | 18/24 | SemiBold | Card titles, top bar |
| BodyL | Inter | 16/24 | Regular | Main reading text |
| BodyM | Inter | 14/20 | Regular | Secondary text, list subtitles |
| LabelL | Inter | 14/20 | Medium | Buttons, chips |
| LabelS | Inter | 12/16 | Medium | Tags, axis labels, captions |

Rules:
- Sentence case everywhere. No all-caps labels.
- Keep reading lines under about 80 characters.
- Support font scaling to 200% with no clipping: use minimum heights, never fixed heights, on text containers.

## 5. Spacing, grid, shape, elevation

| Token | Values |
|---|---|
| Spacing (dp) | 4, 8, 12, 16, 20, 24, 32, 48 |
| Screen margin | 16 |
| Card padding | 16 |
| Gap between cards | 12 |
| Section gap | 24 |
| Radius | tags 6 · buttons and inputs 12 · cards 16 · sheets 24 (top corners) · chips and pills fully rounded |
| Elevation | Level 0 flat · Level 1 = 1dp outline border (cards) · Level 2 = soft shadow (bottom sheets, FAB) |

- Single column on phones (360-412dp). Metric cards use a two-column grid with a 12dp gutter. Cap content width at 600dp on larger screens.
- Use tonal surfaces and borders, not stacked drop shadows.
- Do not use one identical card everywhere. Vary emphasis: the verdict card is larger and filled, supporting cards are quiet.

## 6. Iconography

Material Symbols Rounded, weight 400, 24dp (20dp in chips, 14dp in tags). Icons always sit beside a label or carry a content description.

| Meaning | Icon |
|---|---|
| Cockpit | dashboard |
| Plan | stacked_line_chart |
| Copilot | auto_awesome |
| Activity | receipt_long |
| Observed | check_circle |
| Predicted | query_stats |
| Recommended | arrow_forward |
| Safe / Watch / Risk | check_circle / warning / error |
| EMI, card, rent, subscription | account_balance, credit_card, home, autorenew |
| Cash (bank accounts), SIP | account_balance_wallet, trending_up |

## 7. Motion

| Use | Duration | Notes |
|---|---|---|
| State change | 120ms | Pressed, selected, toggled |
| Component transition | 200ms | Expand, collapse, chip change |
| Screen and sheet transition | 300ms | Standard easing (0.2, 0, 0, 1) |
| Number count-up | 600ms | First load only |
| Forecast band draw | 400ms | Left to right, once |
| Gauge arc | 500ms | On value change |

- Motion answers a user action (open, expand, confirm) or draws attention once. No decorative entrance animations on every card.
- Only looping animation: skeleton shimmer.
- Respect the system "remove animations" setting by swapping all motion for instant changes.

## 8. Indian formatting

- Currency uses lakh grouping: **₹1,50,000**, never ₹150,000.
- Compact form for charts and chips: ₹25K, ₹1.5L, ₹2.3Cr.
- Negative amounts: −₹4,200 (true minus sign). Color only when the context is a risk, not every debit.
- Dates: "18 Oct"; add the year only if it differs. Periods: "Month 3 (Dec)".
- Percentages: whole numbers, "38% chance".
- Account digits: "•• 4091" (two bullets and a space). Never show more than four digits, and only on the device.

## 9. Components

### 9.1 Navigation

**Top app bar** (small, 64dp): title in Title style, left-aligned; back arrow on sub-screens; right side holds the data-source chip ("Your data" neutral or "Demo data" Watch container) and an overflow menu. Scrolls away and returns on scroll up.

**Bottom navigation** (Material 3 NavigationBar, 80dp, labels always visible):

| Tab | Icon | Destination |
|---|---|---|
| Cockpit | dashboard | Home |
| Plan | stacked_line_chart | Plan and forecast |
| Copilot | auto_awesome | Chat |
| Activity | receipt_long | Transactions and repeating payments |

Active tab: `BronzeContainer` pill indicator with `BronzeDark` icon and label. Inactive: `TextSecondary`. An 8dp Risk dot appears on Cockpit while a risk alert is active. Hidden during onboarding, inside sheets, and when the keyboard is open in Copilot.

**Segmented button** (2-4 options, 40dp): plan switch (A / B / Status quo) and period switch (6 months / 8 weeks). Selected state: `BronzeContainer` with a check.

### 9.2 Buttons

| Type | Look | Use |
|---|---|---|
| Primary | Filled `BronzePrimary`, `OnBronze` label, 48dp, radius 12 | One per screen: Continue, Apply plan |
| Secondary | Tonal `BronzeContainer`, `BronzeDark` label | Try what-if, Compare |
| Tertiary | Text only, `BronzeDark` | Skip, Not now, Learn more |
| Destructive | 1dp Risk outline, Risk text | Delete my data |
| Icon button | 48dp target, 24dp icon | Info, close, overflow |
| Extended FAB | Bronze, 56dp, icon plus label, level 2 | Cockpit: "Add" |

**States:** default; pressed (12% state layer); focused (2dp `BronzePrimary` ring); disabled (38% opacity, no ripple); loading (label replaced by a 20dp spinner, width locked so the layout does not jump). Light haptic on Apply and Confirm. Buttons never wrap to two lines: shorten the label.

### 9.3 Chips and tags

- **Type tag:** pill 24dp, 14dp icon plus LabelS text, tag color on container color. Appears on every card and every Copilot section.
- **Status pill:** same shape; Safe, Watch or Risk with icon.
- **Filter chip** (32dp): categories; selected uses `BronzeContainer` plus a check.
- **Suggestion chip:** Copilot starter questions, outlined, one line, horizontally scrollable.
- **Scenario chip:** Demo mode only.
- **Confidence chip:** High, Medium or Low plus range text, for example "Medium · likely ₹18K to ₹26K". Tap opens the confidence explainer.
  - Starting thresholds (tune on the backtest): High = relative interval width under 25% and 6 or more months of history; Low = width over 60% or under 3 months of history; otherwise Medium.

### 9.4 Inputs

- **Text field:** outlined, 56dp, floating label, supporting text below, error text in Risk with an icon.
- **Currency field:** ₹ prefix, numeric keypad, live lakh grouping, helper "or enter 0 if unsure".
- **Slider:** 4dp track, 20dp thumb, value label above the thumb in Indian format, min and max labels below, snap ticks where useful.
- **Selectable card:** full width, radius 16, selected state with a 2dp `BronzePrimary` border and a check; used for single-choice onboarding questions.
- **Switch, checkbox, radio:** Material 3 defaults tinted bronze. **Date picker:** Material 3 modal.

### 9.5 Cards

All cards: `Surface`, 1dp `Outline`, radius 16, 16dp padding.

| Card | Anatomy |
|---|---|
| Verdict | Status icon, HeadlineL verdict, range line, confidence chip, Primary button |
| Metric | Title, NumberXL value, subtitle, delta pill, type tag |
| Health score | Gauge, status word, four component bars, "How this is calculated" link |
| Obligation row | Icon, name, amount, due date, detection-confidence tag |
| Accounts row | Four neutral pills (`SurfaceVariant`, fully rounded): Cash, Card dues, Loan EMIs, SIP. Each has a Material icon, LabelS label and a LabelL tabular amount. One Observed tag for the whole row. A Watch status icon appears on a pill only when a due date is within 3 days or a balance is stale. No emoji |
| Account row (sheet) | Institution, "•• 4091" digits, balance and "as of" date; group headers carry the Observed tag |
| Plan | Plan name, ending reserve, interest saved, shortfall risk, trade-off line |
| Insight | Type tag, one sentence, optional action |
| Conflict banner | Risk container, conflict statement with numbers, Primary "Apply proposed relaxation" |
| Data completeness | Progress bar, "What we don't know" list with one-tap add buttons |
| Evidence | Metric name, ours vs baselines, plain-language reading |

### 9.6 Charts

All drawn with Compose `Canvas`. Each has a table toggle and a TalkBack summary.

- **Fan chart:** x = periods, y = balance (₹K and ₹L labels in LabelS). P50 line 2dp, P10-P90 band, dashed reserve line, "Today" marker. A touch scrubber shows period, P10, P50, P90 and shortfall chance in a tooltip.
- **Health gauge:** 240° arc, 12dp stroke, zones Risk below 50, Watch 50-75, Safe above 75. NumberXL score in the centre with a one-word status below.
- **Obligation timeline:** 30-day horizontal strip, one marker per payment (EMI, card, rent, subscription and SIP icons), risk band beneath, today at the left.
- **Balance bars, category donut, sparkline:** same palette, 8dp bar radius, gridlines no heavier than 1dp `Outline` at 40%.

### 9.7 Feedback and containers

- **Bottom sheet:** drag handle, 24dp top radius, 32% scrim, max height 90%.
- **Dialog:** only for destructive or irreversible actions; Delete uses type-to-confirm.
- **Snackbar:** bottom, above the nav bar, 4 seconds, optional Undo.
- **Banner:** inline at the top of a screen, status container with an icon and one action.
- **Skeleton:** shimmer blocks matching the final layout; used when switching scenario or waiting on AI.
- **Empty state:** icon, one-line explanation, one action.
- **Error state:** what happened and how to fix it. AI outage: "Gemini is unavailable. Showing on-device advice."

### 9.8 Lists

**Transaction row** (64dp): 40dp category icon circle, merchant (BodyL), category and date (BodyM), amount right-aligned in tabular figures. Debits in `TextPrimary`, credits in Safe with a plus sign. Sticky date headers. Swipe left to recategorise.

## 10. Writing and voice

Words are design content. Each element does exactly one job.

- **Plain words, sentence case, active voice.** Name things by what users understand, not by how the system works.
- **Buttons say what happens:** "Save changes", "Apply plan", "Delete my data". Not "Submit" or "OK".
- **Keep action names consistent through a flow.** The button that says "Apply plan" leads to a confirmation that says "Plan applied".
- **Errors state what happened and how to fix it** and do not apologise.
- **Empty screens are invitations:** one line and one action.
- **Warnings end with a next step.**
- **Predictions state their range.** Never a bare number.
- **No blame words** (overspent, failed, bad, wasted).
- **Cohort tone** (see [03 Target audience](03-target-audience.md)): casual for early earners, clear for mid-career, calm and precise for the 40s.

Examples:

| Situation | Copy |
|---|---|
| Verdict | "You're fine until 24 Oct. Tight on 25 Oct." |
| Forecast | "Likely ₹18K to ₹26K left on 25 Oct (80% range)." |
| Warning | "Your cushion could dip in March. Moving ₹4,000 to the buffer avoids it." |
| Empty | "No repeating payments yet. Add a rent or EMI payment." |
| Error | "Couldn't reach the assistant. Showing on-device advice." |
| Accounts | "₹45,200 in your bank account. ₹14,300 on your card, due 25 Oct." |

## 11. Accessibility

- Touch targets at least 48dp.
- Contrast per section 3; status never by color alone.
- Charts expose a text summary to TalkBack ("Balance stays above your cushion in 8 of 10 cases through March") and a table toggle.
- Focus order follows visual order; dialogs trap focus.
- Support dark theme, font scaling to 200%, screen readers and reduced motion from day one.

## 12. Compose implementation map

| File | Contents |
|---|---|
| `ui/theme/Color.kt` | All tokens in section 3, light and dark schemes |
| `ui/theme/Type.kt` | Manrope and Inter styles, tabular-figure helper |
| `ui/theme/Shape.kt` | Radius scale |
| `ui/theme/Spacing.kt` | Spacing scale |
| `ui/theme/Theme.kt` | Material 3 theme, dark-mode switch |
| `ui/components/TypeTag.kt` | Observed, Predicted, Recommended pills |
| `ui/components/StatusPill.kt`, `ConfidenceChip.kt` | Status and confidence |
| `ui/components/MetricCard.kt`, `VerdictCard.kt`, `PlanCard.kt`, `InsightCard.kt` | Card family |
| `ui/components/HealthGauge.kt`, `ForecastFanChart.kt`, `ObligationTimeline.kt`, `PeriodTable.kt` | Charts and tables, each with a TalkBack summary |
| `ui/components/ConflictBanner.kt`, `CompletenessCard.kt`, `WhatIfSheet.kt` | Banners and sheets |
| `ui/components/AccountsRow.kt`, `AccountsSheet.kt` | Accounts row on the Cockpit and the accounts overview sheet (M10) |
| `ui/util/IndianFormat.kt` | ₹ lakh grouping, compact form, dates |

Build order: tokens and `IndianFormat`, then `TypeTag` and cards, then charts, then screens.

### 12.1 Color tokens (starter code)

```kotlin
// ui/theme/Color.kt
import androidx.compose.ui.graphics.Color

val BronzePrimaryLight = Color(0xFFCD7F32)
val BronzePrimaryDark  = Color(0xFFE3A25E)
val BronzeTextLight    = Color(0xFF8C531B)
val BronzeTextDark     = Color(0xFFF0C494)
val BronzeContainerLight = Color(0xFFF6E3CF)
val BronzeContainerDark  = Color(0xFF4A2F14)
val OnBronze = Color(0xFF1F1A14)

// Statement types
val ObservedTeal   = Color(0xFF00897B)
val PredictedBlue  = Color(0xFF2F6FDB)
// Recommended uses Bronze

// Status
val SafeGreen  = Color(0xFF2E7D32)
val WatchAmber = Color(0xFFF2A100)
val RiskRed    = Color(0xFFD93025)

// Neutrals (light)
val BackgroundLight = Color(0xFFFBF8F4)
val SurfaceLight    = Color(0xFFFFFFFF)
val OutlineLight    = Color(0xFFD9CFC2)
val TextPrimaryLight   = Color(0xFF1F1A14)
val TextSecondaryLight = Color(0xFF6B6257)
// Neutrals (dark)
val BackgroundDark = Color(0xFF14110D)
val SurfaceDark    = Color(0xFF1D1914)
val OutlineDark    = Color(0xFF3D352B)
val TextPrimaryDark   = Color(0xFFF2EBE1)
val TextSecondaryDark = Color(0xFFB5AA9A)
```

### 12.2 Spacing (starter code)

```kotlin
// ui/theme/Spacing.kt
import androidx.compose.ui.unit.dp

object Spacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp   // screen margin, card padding
    val lg = 20.dp
    val xl = 24.dp   // section gap
    val xxl = 32.dp
    val xxxl = 48.dp
}
```

### 12.3 Indian number formatting (starter code)

```kotlin
// ui/util/IndianFormat.kt
import kotlin.math.abs
import kotlin.math.roundToLong

private const val MINUS = "\u2212"
private const val RUPEE = "\u20B9"

/** 150000.0 -> "₹1,50,000" ; -4200.0 -> "−₹4,200" */
fun formatInr(amount: Double): String {
    val sign = if (amount < 0) MINUS else ""
    val whole = abs(amount).roundToLong().toString()
    val grouped = if (whole.length <= 3) {
        whole
    } else {
        val last3 = whole.takeLast(3)
        val rest = whole.dropLast(3).reversed().chunked(2).joinToString(",").reversed()
        "$rest,$last3"
    }
    return "$sign$RUPEE$grouped"
}

/** 25000.0 -> "₹25K" ; 150000.0 -> "₹1.5L" ; 23000000.0 -> "₹2.3Cr" */
fun formatInrCompact(amount: Double): String {
    val sign = if (amount < 0) MINUS else ""
    val a = abs(amount)
    fun trim(v: Double) = String.format(java.util.Locale.US, "%.1f", v).removeSuffix(".0")
    return when {
        a >= 1e7 -> "$sign$RUPEE${trim(a / 1e7)}Cr"
        a >= 1e5 -> "$sign$RUPEE${trim(a / 1e5)}L"
        a >= 1e3 -> "$sign$RUPEE${trim(a / 1e3)}K"
        else -> "$sign$RUPEE${a.roundToLong()}"
    }
}
```

Unit-test `formatInr` with 999, 1000, 12345, 150000 and 1234567 (expected `₹999`, `₹1,000`, `₹12,345`, `₹1,50,000`, `₹12,34,567`).

## 13. Open design decisions

1. Final health-score formula and thresholds (proposal in [06 Backend](06-backend.md)).
2. Confidence thresholds in 9.3 are starting values, to be tuned on the backtest.
3. Hindi and regional-language support: post-hackathon.
4. Tablet layouts: out of scope for v1.
5. Contrast verification of every color pair before polish.
