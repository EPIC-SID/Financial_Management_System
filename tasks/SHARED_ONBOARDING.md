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
