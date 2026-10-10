# RupAI Figma differences

Source: [RupAI · Design System](https://www.figma.com/design/7kzcHpw9HxHQclYoxVwFHU/RupAI-Design-System?node-id=5-2).

The user's decisions below take precedence over Figma. Copy must come verbatim from the selected frames. Use v4 wherever it supersedes v2; use light frames only. Historical extraction evidence is retained below; the Phase 1C section records the current implementation.

Confirmed implementation scope: both the existing Android app and a responsive desktop/mobile install website.

## Phase 1C — Android checkpoint

Original delivery branch: `uday`, following the user's explicit instruction to commit and push exactly there. Phase 1 closeout now uses the plan's `feat/track1-ui` branch, fast-forwarded to include that Phase 1C commit without rewriting `uday`. Android implementation paths: `android_app/app/src/main/java/com/example/financeapp/ui/{theme,components,util}`, matching unit tests, and `android_app/app/src/main/res/drawable-nodpi`. Website W1 is now implemented in `website/`. No product screens or landing page are implemented in either checkpoint.

This phase reads design tokens and copy only from the existing [figma-source.json](figma-source.json), plus explicit user overrides. No new Figma extraction or Figma MCP requests were made. The JSON retains text properties and written specifications, but does not retain shape geometry, variable bindings or effects. Historical live-variable values below are not newly verified Phase 1C source tokens.

### Five-value spot check

| Value | JSON evidence / override | Result |
| --- | --- | --- |
| Green fill | Explicit user override `#52A145`; the JSON is not a shape-fill binding export. | Exact override, not approximated. |
| H1 size | v4 Foundations: “H1 / Libre Baskerville 32–40 / Sitka when available”. | Exact range. Android exposes 32, 36 and 40sp roles; W1 uses 32px mobile / 40px desktop at default text size. Selecting roles within the range is an implementation mapping, not an extracted single H1 size. |
| Card radius | v4 Foundations: “Radius / small 8, input 12, button 16, card 20, sheet 24 dp.” | Exact 20dp Android / 20px web. |
| CTA height | v4 CTA v2: “16 sp labels. 56 dp default height. Controls grow with text.” | Exact 56dp Android / 56px web minimum; height grows with text. |
| Screen margin | No screen-margin specification or inner screen bounds in the JSON. | Unverified; no screen-margin token is defined. W1 specimen outer padding is authored 16px mobile / 32px desktop, with 1320px maximum width. Prior 24dp assumptions must not be presented as extracted. |

### Implemented overrides and remaining fidelity gaps

| Area | Phase 1C behavior / difference |
| --- | --- |
| Ink and actions | Ink is `#101010`; every new primary CTA uses `#52A145` and ink labels/icons, including loading. Existing compatibility colour aliases resolve to the new values. |
| Pressed and focused CTA | Fixed green retained, with a 2dp ink border instead of darker green/white labels or an offset shadow. JSON contains state names and copy, but no state border geometry. The 2dp emphasis and 1dp outline default are authored choices awaiting geometry verification. Runtime interaction states and explicit preview states use the same treatment. |
| Disabled CTA | Neutral `#F6F7F5` fill, `#60665D` label; visible supplied reason required. Preview uses verbatim “Add an account to enable this action.” These colours come from Foundations; applying them to disabled state is a mapping because state fill bindings are absent. |
| Loading CTA | Native ink progress indicator plus verbatim “Saving…”, polite accessibility state announcement, clicks blocked while loading. Indicator is 20dp with 2dp stroke, an authored implementation detail absent from JSON. |
| Yellow | Dedicated `#FFC200` guidance token with ink foreground. Not mapped to generic secondary actions, statuses or text. No yellow status is implemented. |
| Watch | Orange foreground `#9A4200` on pale orange `#FFE4CC`. User required orange but did not specify hexes; these are authored choices, not Figma-extracted values. |
| Status copy | Safe / Watch / Risk labels are preserved from the existing groundwork and requested status terminology. The extracted JSON does not independently establish all three status labels or icon shapes. Material outlined icons preserve icon-plus-label communication. |
| Statement types | `✓ Observed`, `≈ Predicted`, `→ Suggested`, matching v4 States and ranges. Ink replaces prior green foreground on Observed. |
| Radius and spacing | 8/12/16/20/24dp radii and 4/8/12/16/20/24/32/48dp spacing match written Foundations specs. Status badges use small 8dp radius; exact status silhouette is absent from JSON. Card internal padding 16dp, badge padding 8×4dp and CTA padding 20×16dp are implementation mappings, not extracted panel geometry. |
| Outline | Ink used for borders. Previous `#7A8275` outline binding is not present in JSON, so it is not treated as verified in this phase. |
| Contrast boundary | Fixed `#52A145` on neutral `#F6F7F5` measures 2.99:1, below 3:1 for essential thin UI/chart lines. That pairing is not approved; future charts should use ink. The prior chart-line assertion was removed from this component-phase test because no chart component is implemented. Green CTA on white measures 3.21:1; ink CTA labels on green measure 5.93:1. |
| Typography | Libre Baskerville headlines; Inter UI. H2 24sp Medium, H3 20sp Regular, body 16sp, compact 14sp, caption 12sp, glyph 16sp. All provided typography is at least 12sp. Existing line-height choices and Material role weights beyond explicit H1/H2/H3/body specs are retained; exact leading is not established by JSON. Inter regular/medium/semibold contain `₹` and `tnum`. Both the Android Libre Baskerville file and Google's full upstream font lack `₹`; W1 uses an explicit Inter currency fallback rather than inventing a serif glyph. Monetary values use Inter. W1 follows the Foundations H1 range instead of copying the CTA board's 42px title; its section headings use H2 24px rather than the board's 20px label. |
| Numbers | Existing IndianFormat helpers retained with their unit tests. `tnum, lnum` applied to Material typography, with tabular numeric styles. No locale-dependent western grouping or predicted `=` glyph introduced. |
| Targets and scaling | CTA minimum 56dp exceeds 48dp touch requirement; no fixed maximum height, truncation or ellipsis. Passive tags/status pills are not touch controls. 100%, 130% and 200% component previews provided. |
| Theme and effects | Light-only theme; all elevation tokens zero. New cards and buttons have no rendered elevation, offset shadow, gradients or blur. Existing screens are outside this phase's migration. |
| Illustrations | All 14 supplied PNGs copied as lossless WebP at original dimensions with alpha and every RGBA pixel preserved. Descriptive screen-role resource names are recorded in [illustration-assets.json](illustration-assets.json). Names are preparation for later placement; exact screen-to-art assignments and tinted rounded panels await the screen phase. No artwork redrawn. |
| Preview copy | Invented sample prose removed. Component previews now use v4 Home, States and ranges, Illustration slots and CTA v2 copy. Metric combinations are review fixtures, not implemented financial decisions or screens. |
| Metric text | Metric titles use Inter 14sp, matching Home “Month-end cushion”; values use Inter 32sp, matching Home “₹18K–₹26K”. Prior groundwork used a 16sp medium title. |
| Existing screens | Caller inspection found explicit white-on-green `ButtonDefaults` overrides in AiReviewScreen, EditProfileScreen and SignUpScreen; legacy screens also use green text and their own shapes/spacing. Shared theme changes do not override explicit callsite colours. Those screens remain unmigrated at the user's phase boundary, so the whole existing app is not yet compliant with the new foundation contract. |

### Verification

Verified on 11 October 2026:

- Requested command on Windows, `gradlew.bat test --tests "*IndianFormatTest" --offline`, is rejected because Android's aggregate `test` task does not support `--tests`.
- Equivalent filtered variant task, `gradlew.bat :app:testDebugUnitTest --tests "*IndianFormatTest" --tests "*ThemeContrastTest" --offline`, **passed**: 11 IndianFormat tests and one contrast test, zero failures/errors. Reports: `android_app/app/build/reports/tests/testDebugUnitTest/index.html`.
- An initial generated Room Java class-lookup failure was resolved by `:app:clean` and rebuilding. No Room source or Gradle configuration was changed for this phase. Non-fatal existing SDK XML version and unused-variable warnings remain.
- `:app:compileDebugKotlin` and unit-test compilation passed, including all preview functions and providers. Tags/statuses enumerate all three variants; CTA previews enumerate all five states for both primary and outline at 100%, 130% and 200%. Metric gallery covers observed, predicted, suggested and missing-value fixtures, delta and status options, plus all tags/statuses at those scales.
- 14 WebP resources verified against their PNG originals: original dimensions, alpha and decoded RGBA bytes identical. Asset manifest includes hashes. Total WebP bytes: 5,427,168.
- Inter regular/medium/semibold cmap and GSUB tables verified for `₹` and `tnum`. Libre Baskerville's missing rupee glyph is recorded above.
- Scoped `git diff --check` passed. Existing unrelated working-tree changes were preserved. The subsequent Android delivery commit is `d5c7666`.

Dedicated Compose preview rendering requires Android Studio and was **not visually verified**. Successful Kotlin compilation verifies preview code but does not prove rendered layout fidelity. Screens remain deferred; the W1 closeout below records website verification separately.

The codebase-memory coverage and graph requests returned `Transport closed`; implementation and caller checks therefore used direct source reads and `rg`. No completeness claim is made from the unavailable graph.

## Inspection status — 11 October 2026

Figma MCP metadata, variable-definition, and read-only Plugin API requests returned the Starter-plan tool-call-limit error. Entering Dev Mode displayed “Upgrade to work in Dev Mode”. On 11 October, the user authorized inspection through the regular Figma window instead. This route works: the Design panel exposes exact layer properties, and the native SVG exporter preserves text, font sizes, colours, dimensions, and layer names. MCP and Dev Mode limitations no longer block source inspection. No plan or access changes were made.

The live browser Variables panel exposed the RupAI Primitives, RupAI Colors Light, and RupAI Geometry collections. Their values below were read directly, not estimated from a screenshot. Text-preserving SVG references were downloaded directly from the existing file for v4 Home, Landing and privacy, CTA v2, Navigation, States and ranges, Foundations, Illustration slots, archived v2 Install landing desktop and mobile, and Illustration brief v2. Their exact copy and text properties are recorded in [figma-source.json](figma-source.json). Temporary export settings were removed after downloading. Local `output/design-system` files remain historical context, not a substitute for the live source.

The source Home board measures 700 × 1200; the v2 desktop and mobile install boards measure 1440 × 1930 and 390 × 1660 respectively. Home text-layer inspection independently confirmed Inter Regular 14px, `#60665D`, and position `(179, 964)` for “12 Oct · Confirmed · ₹4,200”. Foundations specifies Libre Baskerville for Latin headlines and Inter for body text, with Eczar and Noto Sans Devanagari for Devanagari.

The Foundations page currently lists only “RupAI / v4 / Foundations”; “The pieces of RupAI” is not present there. The Components page exposes archived Currency field, Selectable card, and Choice chip families for further inspection. The requested older financial guidance is superseded by the exported v4 States and ranges board.

## Verified token differences

| Source variable | Live Figma value | User decision | Implementation status |
| --- | --- | --- | --- |
| `color/ink`, `text/primary`, `border/strong`, `statement/predicted`, `statement/recommended` | `#111111` (direct or alias) | Ink is `#101010`; use it for ink text, including labels on green and yellow fills. | Phase 1C foundations/components applied |
| `action/primary` | `#36732E` | Green fills use `#52A145`. The primitive `color/green/500` already has this value. | Phase 1C applied |
| `action/on-primary` | `#FFFFFF` | Use `#101010`, never white text on green. | Phase 1C applied |
| `action/pressed`, `action/on-pressed` | `#285A22`, alias to `#FFFFFF` | Green-fill labels must remain ink. | Phase 1C fixed green/ink; authored border geometry noted above |
| `action/secondary`, `action/on-secondary` | Aliases to `#111111`, `#FFFFFF` | Secondary yellow is `#FFC200` and is limited to guidance, highlights, and the closing band; labels on yellow are ink. Do not globally turn secondary actions yellow. | Dedicated guidance token; generic secondary neutral |
| `status/watch`, `status/watch-soft` | Aliases to `#855215`, `#FFF0CB` | Watch is orange, never yellow. | Phase 1C orange pair; authored hexes noted above |
| `elevation/base`, `elevation/raised`, `elevation/sheet` | `0`, `1`, `3` | No shadows or blur. Preserve layout/layer meaning without rendered elevation effects. | Phase 1C all zero |

## Decisions requiring frame comparison

These are authoritative requirements. The live exported references now establish these individual frame differences:

| Frame / layer | Source | User override |
| --- | --- | --- |
| v4 Home / See plan | White label on green action | Ink label on `#52A145` |
| v4 CTA v2 / Default, Pressed, Focused, Loading primary labels | White labels | Ink labels on green fills |
| v4 Landing and privacy / Get RupAI for Android; Allow bank-alert access | White labels | Ink labels on green fills |
| v4 States and ranges / Add data | White label | Ink label on green fill |
| v2 Install landing desktop and mobile / closing band headings and supporting text | White text | `#101010` text on closing band `#FFC200` |
| v2 Install landing mobile / FOR ANDROID | 11px | Minimum 12px |
| v2 Install landing desktop / YOUR MONEY, IN VIEW | 11px | Minimum 12px; replace the older app preview with v4 Home |
| v2 Install landing desktop and mobile / PROJECTED MONTH-END CUSHION | 10px | Minimum 12px; v4 Home range and confidence take precedence over the older estimate |
| v4 Foundations / Elevation raised and sheet | 1dp and 3dp | No rendered shadows or blur |
| v4 Illustration slots / all slots | Bayer coin-stack placeholders | Supplied transparent PNG figures on tinted rounded panels |
| v4 Illustration slots / essential financial values | Native text outside artwork | Retain; never bake amounts or status into the PNGs |

Foundations implement these rules. Screen-level migration and comparison remain deferred:

| Requirement | Comparison status |
| --- | --- |
| Light theme only; ignore every dark frame. | Applied in Android foundation and W1; legacy screens still require migration. |
| `#101010` text on `#52A145` green fills, never white. | Applied in new components; explicit legacy screen overrides remain listed above. |
| `#FFC200` only for guidance, highlights, and closing band; never status, text, or white labels. | Dedicated guidance token applied; landing/callsite comparison deferred. |
| Orange Watch status. | Authored orange token pair applied; exact frame hexes intentionally differ. |
| Predicted glyph is `≈`, never `=`. | Applied in TypeTag and range fixtures; future amount callsites deferred. |
| Text minimum is 12px (12sp in Android); touch targets minimum 48px (48dp in Android). | Applied to foundations; W1 reflow verified. Native screen/control comparison deferred. |
| No gradients, blur, or shadows. | Applied to new foundations; legacy screen effects comparison deferred. |
| Indian lakh grouping and tabular numerals. | Formatting tests and Inter feature checks passed; future screen amount callsites deferred. |
| Use supplied transparent PNG illustrations on tinted rounded panels. | Lossless assets prepared; W1 demonstrates one tinted panel. Product screen assignments deferred. |
| “The pieces of RupAI”: use Inputs and choices; v1 Button system is retired. | Component/frame inspection pending. |

Illustration source directory: `C:\Users\udays\Documents\rupai\Financial_Management_System\rupai_illustrations`. Preserve supplied artwork and transparency; do not redraw or substitute historical SVG artwork.

## Geometry read directly from live variables

| Variable family | Values |
| --- | --- |
| `space/*` | `4`, `8`, `12`, `16`, `20`, `24`, `32`, `48` |
| `radius/button` | `16` |
| `radius/card` | `20` |
| `radius/sheet` | `24` |
| `radius/tag` | `8` |
| `radius/field` | `12` |
| `size/touch` | `48` |
| `size/nav` | `80` |

## Required source frames

- RupAI / v4 / Home
- RupAI / v4 / Landing and privacy
- RupAI / v2 / Install landing desktop and mobile
- RupAI / v2 / CTA system (prefer the current v4 CTA v2 where it supersedes v2)
- RupAI / v2 / Mobile navigation (prefer the current v4 Navigation where it supersedes v2)
- The pieces of RupAI — Inputs and choices
- 04 / Financial meaning and feedback (prefer v4 States and ranges where it supersedes older guidance)
- Illustration brief v2

## Work remaining

1. Screen margin, precise state-border geometry, icon silhouettes and leading remain unverified in the current JSON. Do not invent exact source values.
2. Screen implementation, final illustration assignments, inputs/choices, navigation, charts and landing page work are deferred by the user's phase boundary.
3. Legacy Android screen colour/shape overrides and current native preview/device validation remain outstanding; W1 does not migrate them.

## Delivery verification on uday

- Delivery branch confirmed as `uday`; unrelated local changes are excluded.
- The user approved the pre-existing manifest lint fix: optional `android.hardware.telephony` declaration. SMS permissions and database configuration are unchanged.
- `gradlew.bat :app:testDebugUnitTest --tests "*IndianFormatTest" --tests "*ThemeContrastTest" :app:assembleDebug :app:lintDebug --console=plain --max-workers=2` passed with Java 17.
- Computed report totals: 12 tests, 0 failures, 0 test errors; 20 permitted contrast pairings; lint 0 errors and 28 warnings.
- That Android delivery did not include W1. Screen migration, current native visual previews, TalkBack and physical-device validation remain outside this verification.

## W1 closeout — website foundations only

Implemented Next.js App Router, TypeScript, static export, Tailwind/shadcn CSS variables and native Button/CVA pattern, lazy Motion features, local subset fonts, the CTA v2 override and lossless WebP density variants in `website/`. The root route is a foundation specimen, with one decorative art panel; it is not a product screen or install landing page. No database, Android source or architecture changes were made for W1.

Font subsets retain source outlines, include licenses and use distinct internal family names. Inter supplies `₹` and tabular numerals; Libre Baskerville's missing rupee glyph uses Inter fallback. This is a documented font-source limitation, not a claim that the serif subset contains `₹`.

Verification command, from `website/`: `npm run verify`. It generates Next route types, checks TypeScript, builds the static export and runs browser checks against that export using Python and installed Chrome. Asset preparation performs assertions on font coverage and decoded RGBA equality. Mobile and desktop screenshots were reviewed at default and enlarged text sizes; this does not verify native Compose rendering.

### Exact Compose previews to open

Open these files in Android Studio's Design/Split view under `android_app/app/src/main/java/com/example/financeapp/ui/components/`:

- `RupAiButton.kt` → `RupAiButtonPreview`: “CTA · every state”, “CTA · every state · 130%”, “CTA · every state · 200%”. The parameter provider enumerates Default, Pressed, Focused, Disabled and Loading, each showing primary and outline.
- `TypeTag.kt` → `TypeTagPreview`: “Statement tags · every type” and “Statement tags · 200%”. Each enumerates Observed, Predicted and Suggested.
- `StatusPill.kt` → `StatusPillPreview`: “Status pills · every status” and “Status pills · 200%”. Each enumerates Safe, Watch and Risk.
- `MetricCard.kt` → `Phase1CPreview`: “Metric cards · 100%”, “Metric cards · 130%”, “Metric cards · 200%”. The gallery includes observed, range, suggested and unavailable fixtures, plus all tags/statuses (including at 130%).

### Computed W1 verification results

- Browser report: 14 passed, 0 failed, 0 skipped, 0 flaky; 0 runner errors. TypeScript and static export also passed.
- Asset manifest: 4 local font subsets, 14 supplied illustrations, 28 WebP variants; 7327072 total WebP bytes. All asset roundtrip and Inter rupee/tabular assertions passed.
- Browser checks include WCAG AA, keyboard focus, visible disabled reasons, loading semantics, light-only output under a dark system preference, reduced motion, local resource loading and reflow at the widths/scales encoded in the test matrix.
