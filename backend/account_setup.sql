-- Apply in Supabase SQL Editor before enabling cloud setup storage.
-- New table only: no SMS, merchant labels, account numbers or demo data.
BEGIN;
CREATE TABLE IF NOT EXISTS public.account_setup (
    user_id uuid PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    income_pattern text CHECK (income_pattern IN ('Fixed salary', 'Varies month to month', 'A mix')),
    obligations text[] NOT NULL DEFAULT '{}'
        CHECK (cardinality(obligations) <= 8 AND obligations <@ ARRAY['Rent','Home loan','Vehicle EMI','Credit card','Gadget EMI','Insurance','Subscriptions','Mutual fund SIP']::text[]),
    minimum_reserve bigint CHECK (minimum_reserve BETWEEN 0 AND 999999999),
    goal_name text CHECK (length(btrim(goal_name)) BETWEEN 1 AND 80 AND goal_name !~ '[[:cntrl:]]'),
    goal_amount bigint CHECK (goal_amount BETWEEN 1 AND 999999999),
    consent_version integer NOT NULL CHECK (consent_version = 1),
    consented_at timestamptz NOT NULL DEFAULT now(),
    CHECK ((goal_name IS NULL) = (goal_amount IS NULL))
);
ALTER TABLE public.account_setup ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.account_setup FORCE ROW LEVEL SECURITY;
REVOKE ALL ON public.account_setup FROM anon;
REVOKE ALL ON public.account_setup FROM PUBLIC;
REVOKE ALL ON public.account_setup FROM authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.account_setup TO authenticated;
DROP POLICY IF EXISTS "Owner manages setup" ON public.account_setup;
CREATE POLICY "Owner manages setup" ON public.account_setup FOR ALL TO authenticated
    USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
DROP POLICY IF EXISTS "Setup owner boundary" ON public.account_setup;
CREATE POLICY "Setup owner boundary" ON public.account_setup AS RESTRICTIVE FOR ALL TO authenticated
    USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

-- Harden profile ownership as well. Existing broad permissive policies must not
-- bypass these checks; restrictive policies apply alongside existing policies.
ALTER TABLE public.user_profiles ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Profile owner boundary" ON public.user_profiles;
CREATE POLICY "Profile owner boundary" ON public.user_profiles AS RESTRICTIVE FOR ALL TO authenticated
    USING (auth.uid() = id) WITH CHECK (auth.uid() = id);
DROP POLICY IF EXISTS "Profile owner access" ON public.user_profiles;
CREATE POLICY "Profile owner access" ON public.user_profiles FOR ALL TO authenticated
    USING (auth.uid() = id) WITH CHECK (auth.uid() = id);
REVOKE ALL ON public.user_profiles FROM anon;
REVOKE ALL ON public.user_profiles FROM PUBLIC;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.user_profiles TO authenticated;
-- NOT VALID preserves legacy rows while validating all new writes.
ALTER TABLE public.user_profiles DROP CONSTRAINT IF EXISTS profile_input_bounds;
ALTER TABLE public.user_profiles ADD CONSTRAINT profile_input_bounds CHECK (
    length(btrim(full_name)) BETWEEN 1 AND 100 AND full_name !~ '[[:cntrl:]]'
    AND (phone_number IS NULL OR phone_number ~ '^\+?[0-9]{7,15}$')
    AND (monthly_income IS NULL OR (monthly_income::text NOT IN ('NaN','Infinity','-Infinity')
        AND monthly_income BETWEEN 0 AND 999999999))
) NOT VALID;
COMMIT;
