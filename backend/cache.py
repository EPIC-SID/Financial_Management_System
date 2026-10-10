"""
Supabase shared merchant cache — merchant_categories table.

This is the core scalability mechanism:
- First user to pay at "Jay Ganesh" anywhere → 1 Gemini call
- All subsequent users → instant cache hit from this table
- Effectively makes the free tier quota grow with unique merchants, not total users

Table schema (run in Supabase SQL editor):
    CREATE TABLE merchant_categories (
        merchant_key  TEXT PRIMARY KEY,
        category      TEXT NOT NULL,
        source        TEXT,
        call_count    INTEGER DEFAULT 1,
        created_at    TIMESTAMPTZ DEFAULT now(),
        updated_at    TIMESTAMPTZ DEFAULT now()
    );

    ALTER TABLE merchant_categories ENABLE ROW LEVEL SECURITY;

    -- Server (service role) can write; authenticated users can read
    CREATE POLICY "Authenticated users can read merchant cache"
        ON merchant_categories FOR SELECT
        USING (auth.role() = 'authenticated');

    CREATE POLICY "Service role can insert and update merchant cache"
        ON merchant_categories FOR ALL
        USING (auth.role() = 'service_role');
"""
import logging
import os
from typing import Optional

from supabase import create_client, Client

logger = logging.getLogger(__name__)

_supabase_client: Optional[Client] = None

CONFIDENT_CATEGORIES: set[str] = {
    "FOOD", "TRAVEL", "SHOPPING", "BILLS",
    "ENTERTAINMENT", "INVESTMENTS", "HEALTH", "EDUCATION"
}


def is_cachable_category(category: Optional[str]) -> bool:
    """Returns True only for high-confidence non-fallback categories."""
    if not category:
        return False
    return category.strip().upper() in CONFIDENT_CATEGORIES


def get_supabase() -> Client:
    """Returns a singleton Supabase client using the service role key."""
    global _supabase_client
    if _supabase_client is None:
        url = os.getenv("SUPABASE_URL", "")
        key = os.getenv("SUPABASE_SERVICE_KEY", "")
        if not url or not key:
            raise RuntimeError("SUPABASE_URL and SUPABASE_SERVICE_KEY must be set")
        _supabase_client = create_client(url, key)
    return _supabase_client


def _normalize_key(merchant: str) -> str:
    """Normalize merchant name to a consistent cache key."""
    return merchant.strip().lower()


def get_from_cache(merchant: str) -> Optional[str]:
    """
    Look up a merchant in the Supabase shared cache.
    Returns the category string or None if not found or uncertain.
    """
    key = _normalize_key(merchant)
    try:
        client = get_supabase()
        result = (
            client.table("merchant_categories")
            .select("category")
            .eq("merchant_key", key)
            .limit(1)
            .execute()
        )
        if result.data:
            cat = result.data[0]["category"]
            if not is_cachable_category(cat):
                logger.info(f"Ignoring uncertain/invalid cache hit '{cat}' for merchant '{merchant}'")
                return None

            # Increment call_count in background (fire-and-forget, don't block)
            try:
                client.table("merchant_categories").update(
                    {"call_count": result.data[0].get("call_count", 1) + 1}
                ).eq("merchant_key", key).execute()
            except Exception:
                pass  # Don't fail the request if this update fails
            return cat
        return None
    except Exception as e:
        logger.warning(f"Cache lookup failed for '{merchant}': {e}")
        return None


def save_to_cache(merchant: str, category: str, source: str) -> None:
    """
    Save a categorization result to the Supabase shared cache.
    Rejects uncertain / fallback categories (MISCELLANEOUS, FALLBACK, UNKNOWN).
    Uses upsert so duplicate merchants are handled gracefully.
    """
    if not is_cachable_category(category):
        logger.info(f"Refusing to cache uncertain/transient category '{category}' for merchant '{merchant}'")
        return

    key = _normalize_key(merchant)
    try:
        client = get_supabase()
        client.table("merchant_categories").upsert(
            {
                "merchant_key": key,
                "category": category,
                "source": source,
                "call_count": 1,
            },
            on_conflict="merchant_key",
        ).execute()
        logger.debug(f"Cached '{merchant}' → {category} (source: {source})")
    except Exception as e:
        logger.warning(f"Failed to save cache for '{merchant}': {e}")


def get_batch_from_cache(merchants: list[str]) -> dict[str, str]:
    """
    Batch lookup for multiple merchants in a single Supabase query.
    Returns a dict of { normalized_key: category } for all valid cache hits.
    Ignores uncertain or invalid categories.
    """
    if not merchants:
        return {}
    keys = [_normalize_key(m) for m in merchants]
    try:
        client = get_supabase()
        result = (
            client.table("merchant_categories")
            .select("merchant_key, category")
            .in_("merchant_key", keys)
            .execute()
        )
        return {
            row["merchant_key"]: row["category"]
            for row in (result.data or [])
            if is_cachable_category(row.get("category"))
        }
    except Exception as e:
        logger.warning(f"Batch cache lookup failed: {e}")
        return {}
