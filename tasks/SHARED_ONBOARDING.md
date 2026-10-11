# Shared Onboarding And Profile

## Deployment

Apply `backend/account_setup.sql` in the existing Supabase project's SQL Editor.
It creates only `account_setup` and hardens the existing `user_profiles` table.
It does not delete transactions or overwrite existing profile values. Existing
profile rows need not pass the new bounds until they are edited.
The existing Android Supabase HTTPS URL and public anon key are reused. Never
put a service-role key in Android. No new AI model or key is needed.

## Flow

First launch -> shared onboarding -> Demo or My Transactions.
Profile is available from both modes. Sign-in returns to shared onboarding so
the user can review and explicitly opt into cloud setup storage. Existing cloud
setup is loaded before enabling cloud save. Failures preserve the in-memory draft
and never report a successful cloud save. Unchecked consent does not delete a
previous cloud record; use Profile's explicit delete action for that.

SMS permission is separate from cloud consent. Choosing SMS never imports by
itself: Activity's Sync action chooses the import range. Cloud categorization
still has its own consent. Sign-out disables that device consent and cancels
pending categorization work; already dispatched network requests cannot be recalled.
CSV import is not connected and is not offered as an onboarding source.

Only declared income pattern, fixed-payment types, minimum reserve, optional
goal name/amount, owner ID and consent version/timestamp are saved in the new
table. No age, raw SMS, merchant history, balances, account numbers, or demo rows.
These declarations do not replace the sample planner's assumptions. Cloud setup
is retained until explicit deletion or auth-user deletion (foreign-key cascade).
Supabase backup retention is managed separately by the project operator.

Guests can complete onboarding without cloud writes. Their financial answers are
discarded when leaving the onboarding flow; only a device-level completion flag
is stored. That flag is not evidence of account-level cloud consent.

## Verification

Run from `android_app`:

```powershell
.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest :app:assembleDebug
```

Manual cloud acceptance after applying SQL, using disposable test accounts:

1. Sign in as A, select optional cloud save, enter reserve and paired goal fields.
2. Complete, open Profile in both modes, and confirm identical saved values.
3. Edit setup, clear reserve/goal, save, and verify the database fields are null.
4. Disconnect network; save must fail visibly and retain the current draft.
5. Sign in as B; A's setup must never appear. Requests with B's JWT selecting,
   updating or deleting A's `user_id` must be denied/return no rows by RLS.
6. Anonymous requests must not read or write either profile table.
7. Delete A's saved setup from Profile after confirmation; original SMS,
   local transactions and `user_profiles` remain unchanged.
8. Local reset requires separate confirmation; do not exercise it on real data.

Live authenticated Supabase persistence and cross-account RLS require the SQL
migration and test accounts; unit tests alone do not establish those guarantees.

Verified 2026-10-11: 259 JVM tests passed, debug APK built and installed without
clearing app data. Guest onboarding completed with cloud consent off. The same
Profile screen opened in Demo and My Transactions; no SMS sync or reset was
triggered. After the operator applied the migration, anonymous REST reads of
both `account_setup` and `user_profiles` returned HTTP 401 / PostgreSQL 42501.
Signed-in save/reload/delete and two-account isolation remain acceptance checks.

## Updated Account Entry

Welcome now offers Create account, Sign in, or Continue without an account.
Authenticated users see Continue setup instead. Both account forms use the current
RupAI components; signup asks only for name, email and password. No phone or income
is required to register. Only an actual authenticated Supabase session continues
to shared onboarding. With email confirmation enabled, signup instead shows a
confirmation reminder and leaves the user on the form.

Passwords stay in temporary Compose memory, never saved instance state or logs.
Requests have a 15-second deadline, bounded inputs and generic errors. Reset
responses do not disclose whether an account exists. Local cooldowns are UX only;
Supabase must enforce server-side rate limits. Password-reset completion depends
on the project's configured recovery URL; no new deep-link handler is added here.
Email delivery and recovery completion require live acceptance testing.

Legacy Home, Transactions, Insights and Review screen implementations are removed.
Saved legacy routes redirect to current screens without leaving a back-stack loop.
The profile editor uses current tokens and keeps owner checks and explicit optional
field clearing. No database migration, transaction reset or planner change is needed.

Account smoke checks: open Profile -> Sign in -> Create account, submit empty fields
and confirm local validation, test keyboard scrolling and password visibility,
switch forms and use Back, then open Review setup to check all three welcome choices.
Use disposable accounts for real signup, confirmation, login and recovery checks.

Verified account-entry refresh 2026-10-11: 273 JVM tests passed (14 auth-focused),
debug APK built and installed with existing data preserved. Phone screenshots
confirmed login/signup layout and welcome choices. Empty signup was blocked
locally; keyboard scrolling kept Create account reachable. System Back returned
to onboarding, and recovery opened/returned without sending email. No real
credentials, SMS import, cloud writes or reset were used. Authenticated login,
signup confirmation, recovery completion and profile editing remain live checks.

## Shared Navigation And Plain English

The main screens now share one header and a navigation drawer, with no bottom
navigation. The drawer contains destinations, the sample/SMS data choice and
sample situations. Live mode still excludes sample planning screens. Screen
labels explain cash risk without presenting it as goal-success probability.
Financial calculations, SMS permissions and cloud consent behavior are unchanged.

Verified 2026-10-11: 277 JVM tests passed with no failures or skipped tests,
including four navigation-choice tests. Debug APK built and installed without
clearing data. Overview, Plans, Ask AI and drawer layouts were checked on the
connected phone. No credentials, cloud writes, SMS import or database reset were
used; other screen sizes and complete live cloud flows were not checked.
