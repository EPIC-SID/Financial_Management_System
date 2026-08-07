-- RupAI — Supabase SQL Setup
-- Run this in your Supabase project: Dashboard → SQL Editor → New Query
-- 
-- This creates the shared merchant cache table that is the core
-- scalability mechanism for the backend.

-- ─────────────────────────────────────────────────────────────
--  merchant_categories — Shared AI categorization cache
--  One row per unique merchant, shared across ALL users.
--  First user to pay at a new merchant → 1 Gemini call.
--  All subsequent users → instant cache hit.
-- ─────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS merchant_categories (
    merchant_key  TEXT        PRIMARY KEY,        -- normalized lowercase merchant name
    category      TEXT        NOT NULL,           -- FOOD, TRAVEL, SHOPPING, etc.
    source        TEXT,                           -- PLACES, GEMINI, REGEX
    call_count    INTEGER     DEFAULT 1,          -- how many times this merchant was looked up
    created_at    TIMESTAMPTZ DEFAULT now(),
    updated_at    TIMESTAMPTZ DEFAULT now()
);

-- Auto-update updated_at on any row change
CREATE OR REPLACE FUNCTION update_merchant_categories_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER merchant_categories_updated_at
    BEFORE UPDATE ON merchant_categories
    FOR EACH ROW EXECUTE FUNCTION update_merchant_categories_updated_at();

-- ─────────────────────────────────────────────────────────────
--  Row Level Security
-- ─────────────────────────────────────────────────────────────

ALTER TABLE merchant_categories ENABLE ROW LEVEL SECURITY;

-- Authenticated users can read the cache (to pre-warm their local cache on login)
CREATE POLICY "Authenticated users can read merchant cache"
    ON merchant_categories
    FOR SELECT
    USING (auth.role() = 'authenticated');

-- Only the service role (backend server) can write to the cache
CREATE POLICY "Service role can manage merchant cache"
    ON merchant_categories
    FOR ALL
    USING (auth.role() = 'service_role');

-- ─────────────────────────────────────────────────────────────
--  Index for faster lookups
-- ─────────────────────────────────────────────────────────────

CREATE INDEX IF NOT EXISTS idx_merchant_categories_category
    ON merchant_categories (category);

-- ─────────────────────────────────────────────────────────────
--  Verification query — run after setup to confirm
-- ─────────────────────────────────────────────────────────────
-- SELECT COUNT(*) FROM merchant_categories;
-- Expected: 0 (empty, ready to fill as transactions are processed)
