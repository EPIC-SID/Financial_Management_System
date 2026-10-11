"""
Categorization pipeline — Endpoints 2 & 3.

Layer order:
  1. Supabase shared global cache (merchant_categories table)  — 0ms, free
  2. Google Places Text Search API                             — ~200ms
  3. Groq LPU LLM (sub-300ms, llama-3.3-70b-versatile)        — ~200ms
  4. Fallback → MISCELLANEOUS (transient, never cached)

API keys are ONLY on this server — never in the APK.
"""
import asyncio
import json
import logging
import os
from typing import Optional

import httpx

from cache import (
    CONFIDENT_CATEGORIES,
    is_cachable_category,
    get_from_cache,
    save_to_cache,
    get_batch_from_cache,
)
from llm import call_llm
from models import BatchCategorizeItem, CategorizeResponse

logger = logging.getLogger(__name__)

PLACES_API_KEY = os.getenv("PLACES_API_KEY", "")
PLACES_ENDPOINT = "https://places.googleapis.com/v1/places:searchText"

VALID_CATEGORIES = {
    "FOOD", "TRAVEL", "SHOPPING", "BILLS",
    "ENTERTAINMENT", "INVESTMENTS", "HEALTH", "EDUCATION", "MISCELLANEOUS"
}

# Google Places type → app category mapping
PLACE_TYPE_TO_CATEGORY: dict[str, str] = {
    # Food
    "restaurant": "FOOD", "food": "FOOD", "bakery": "FOOD", "cafe": "FOOD",
    "meal_delivery": "FOOD", "meal_takeaway": "FOOD", "bar": "FOOD",
    "night_club": "ENTERTAINMENT",
    # Travel
    "gas_station": "TRAVEL", "transit_station": "TRAVEL", "bus_station": "TRAVEL",
    "train_station": "TRAVEL", "subway_station": "TRAVEL", "airport": "TRAVEL",
    "parking": "TRAVEL", "car_rental": "TRAVEL",
    # Health
    "pharmacy": "HEALTH", "hospital": "HEALTH", "doctor": "HEALTH",
    "dentist": "HEALTH", "health": "HEALTH", "physiotherapist": "HEALTH",
    "veterinary_care": "HEALTH",
    # Shopping
    "store": "SHOPPING", "grocery_or_supermarket": "SHOPPING", "supermarket": "SHOPPING",
    "convenience_store": "SHOPPING", "shopping_mall": "SHOPPING",
    "clothing_store": "SHOPPING", "department_store": "SHOPPING",
    "electronics_store": "SHOPPING", "book_store": "SHOPPING",
    "hardware_store": "SHOPPING", "furniture_store": "SHOPPING",
    "shoe_store": "SHOPPING", "jewelry_store": "SHOPPING",
    # Entertainment
    "movie_theater": "ENTERTAINMENT", "amusement_park": "ENTERTAINMENT",
    "bowling_alley": "ENTERTAINMENT", "casino": "ENTERTAINMENT", "stadium": "ENTERTAINMENT",
    # Education
    "school": "EDUCATION", "university": "EDUCATION", "library": "EDUCATION",
    # Bills / Finance
    "bank": "BILLS", "atm": "BILLS", "finance": "BILLS",
    "insurance_agency": "BILLS", "accounting": "BILLS",
}


# ─────────────────────────────────────────────
#  Layer 2: Google Places API
# ─────────────────────────────────────────────

async def _call_places_api(merchant: str) -> Optional[str]:
    """Calls Google Places Text Search API and returns a confident category or None."""
    if not PLACES_API_KEY:
        return None
    try:
        async with httpx.AsyncClient(timeout=4.0) as client:
            response = await client.post(
                PLACES_ENDPOINT,
                headers={
                    "Content-Type": "application/json",
                    "X-Goog-Api-Key": PLACES_API_KEY,
                    "X-Goog-FieldMask": "places.types",
                },
                json={
                    "textQuery": merchant,
                    "languageCode": "en",
                    "regionCode": "IN",    # Bias to India
                    "maxResultCount": 1,
                },
            )
            if response.status_code != 200:
                logger.warning("Places API returned HTTP %d", response.status_code)
                return None

            data = response.json()
            places = data.get("places", [])
            if not places:
                return None

            types = places[0].get("types", [])
            for place_type in types:
                category = PLACE_TYPE_TO_CATEGORY.get(place_type.lower())
                if category and is_cachable_category(category):
                    return category
            return None
    except Exception as e:
        logger.warning("Places API unavailable")
        return None


# ─────────────────────────────────────────────
#  Layer 3: Groq LLM (Bounded 5s timeout)
# ─────────────────────────────────────────────

async def _call_llm_categorizer(merchant: str, sms_snippet: str) -> tuple[Optional[str], str]:
    """Calls Groq (or fallback) with bounded timeout and returns (category, provider_name)."""
    prompt = f"""You are a financial transaction categorizer for Indian users.
Given a merchant name and a brief transaction description, reply with ONLY one word from this exact list:
FOOD, TRAVEL, SHOPPING, BILLS, ENTERTAINMENT, INVESTMENTS, HEALTH, EDUCATION, MISCELLANEOUS

Classification Rules for Indian Merchants:
- Grocery, supermarket, provision store, kirana, retail, clothing, electronics → SHOPPING
- Restaurant, dhaba, hotel, sweets, bakery, canteen, tea stall, dairy, snacks, tiffin → FOOD
- Travel, auto, cab, petrol, fuel, parking, metro, train, flight, IRCTC, Fastag → TRAVEL
- Pharmacy, medical, hospital, clinic, doctor, chemist, medicine → HEALTH
- School, college, tuition, books, stationery, online course → EDUCATION
- Electricity, mobile recharge, wifi, water, DTH, utility, EMI, insurance → BILLS
- Mutual fund, SIP, stocks, Zerodha, Groww, Upstox → INVESTMENTS
- Netflix, Spotify, cinema, gaming, BookMyShow → ENTERTAINMENT
- Personal name without business context → MISCELLANEOUS
- If uncertain → MISCELLANEOUS
- Reply with ONLY the single category word in uppercase. No punctuation. No explanation.

Merchant: "{merchant}"
Description: "{sms_snippet[:100]}"
Category:"""

    raw_reply, provider = await call_llm(prompt, timeout=5.0)
    if not raw_reply:
        return None, provider

    clean_cat = raw_reply.strip().upper()
    if clean_cat in CONFIDENT_CATEGORIES:
        return clean_cat, provider
    return None, provider


# ─────────────────────────────────────────────
#  Public API: Categorize Single
# ─────────────────────────────────────────────

async def categorize_merchant(merchant: str, sms_snippet: str = "") -> CategorizeResponse:
    """
    Full categorization pipeline for a single merchant.
    Guarantees:
    - Uncertain cache hits (MISCELLANEOUS/FALLBACK) are ignored and re-resolved.
    - Transient fallbacks are NEVER cached.
    """
    merchant = merchant.strip()
    if not merchant:
        return CategorizeResponse(merchant=merchant, category="MISCELLANEOUS", source="FALLBACK")

    # Layer 1: Supabase shared global cache
    cached = get_from_cache(merchant)
    if cached and is_cachable_category(cached):
        logger.info("Category cache hit")
        return CategorizeResponse(merchant=merchant, category=cached, source="GLOBAL_CACHE")

    # Layer 2: Google Places API
    places_cat = await _call_places_api(merchant)
    if places_cat and is_cachable_category(places_cat):
        save_to_cache(merchant, places_cat, "PLACES")
        logger.info("Category resolved by Places")
        return CategorizeResponse(merchant=merchant, category=places_cat, source="PLACES")

    # Layer 3: Groq LLM
    llm_cat, provider = await _call_llm_categorizer(merchant, sms_snippet)
    if llm_cat and is_cachable_category(llm_cat):
        source_name = provider if provider != "NONE" else "GROQ"
        save_to_cache(merchant, llm_cat, source_name)
        logger.info("Category resolved by LLM")
        return CategorizeResponse(merchant=merchant, category=llm_cat, source=source_name)

    # Fallback (never saved to cache)
    logger.warning("Category uncertain; using transient fallback")
    return CategorizeResponse(merchant=merchant, category="MISCELLANEOUS", source="FALLBACK")


# ─────────────────────────────────────────────
#  Public API: Categorize Batch
# ─────────────────────────────────────────────

async def categorize_batch(
    transactions: list[BatchCategorizeItem],
) -> tuple[list[CategorizeResponse], int, int]:
    """
    Categorize a batch of transactions efficiently without cache poisoning.

    Strategy:
    1. Batch-check all merchants against Supabase cache in ONE query
    2. Filter out uncertain/invalid cache hits
    3. For cache misses, deduplicate merchants (same merchant in batch = 1 API call)
    4. Process cache misses concurrently (up to 3 at a time to respect rate limits)

    Returns: (results, cache_hits_count, api_calls_count)
    """
    if not transactions:
        return [], 0, 0

    results: list[CategorizeResponse] = []
    cache_hits = 0
    api_calls = 0

    # Step 1: Query shared cache
    merchant_names = [t.merchant for t in transactions]
    cache_map = get_batch_from_cache(merchant_names)  # Already filters out non-confident entries

    # Step 2: Separate valid cache hits from misses
    cached_results: dict[str, CategorizeResponse] = {}
    for txn in transactions:
        key = txn.merchant.strip().lower()
        cat = cache_map.get(key)
        if cat and is_cachable_category(cat):
            cached_results[txn.merchant] = CategorizeResponse(
                merchant=txn.merchant,
                category=cat,
                source="GLOBAL_CACHE",
            )
            cache_hits += 1

    # Step 3: Collect unique cache misses (excluding confirmed cache hits)
    misses = list({
        t.merchant: t for t in transactions
        if t.merchant.strip().lower() not in cache_map or not is_cachable_category(cache_map.get(t.merchant.strip().lower()))
    }.values())

    # Step 4: Process misses with a concurrency limit
    semaphore = asyncio.Semaphore(3)

    async def _process_one(txn: BatchCategorizeItem) -> CategorizeResponse:
        async with semaphore:
            return await categorize_merchant(txn.merchant, txn.sms_snippet)

    if misses:
        miss_results_list = await asyncio.gather(*[_process_one(t) for t in misses])
        miss_results = {r.merchant: r for r in miss_results_list}
        api_calls = len(misses)
    else:
        miss_results = {}

    # Step 5: Assemble results in original order
    for txn in transactions:
        result = cached_results.get(txn.merchant) or miss_results.get(txn.merchant)
        if result:
            results.append(result)
        else:
            results.append(CategorizeResponse(
                merchant=txn.merchant,
                category="MISCELLANEOUS",
                source="FALLBACK"
            ))

    return results, cache_hits, api_calls
