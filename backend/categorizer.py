"""
Categorization pipeline — Endpoints 2 & 3.

Layer order (same as the original Android pipeline, but now server-side):
  1. Supabase shared global cache (merchant_categories table)  — 0ms, free
  2. Google Places Text Search API                             — ~200ms
  3. Gemini 2.0 Flash LLM                                     — ~500ms
  4. Fallback → MISCELLANEOUS

API keys are ONLY on this server — never in the APK.
"""
import asyncio
import json
import logging
import os
from typing import Optional

import httpx
from google import genai
from google.genai import types as genai_types

from cache import get_from_cache, save_to_cache, get_batch_from_cache
from models import BatchCategorizeItem, CategorizeResponse

logger = logging.getLogger(__name__)

PLACES_API_KEY = os.getenv("PLACES_API_KEY", "")
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")
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


def _init_gemini() -> Optional[genai.Client]:
    """Initializes and returns the Gemini client, or None if key is missing."""
    if not GEMINI_API_KEY:
        logger.error("GEMINI_API_KEY is not set")
        return None
    try:
        return genai.Client(api_key=GEMINI_API_KEY)
    except Exception as e:
        logger.error(f"Failed to initialize Gemini client: {e}")
        return None


# Initialize client once at module load
_gemini_client: Optional[genai.Client] = _init_gemini()


# ─────────────────────────────────────────────
#  Layer 2: Google Places API
# ─────────────────────────────────────────────

async def _call_places_api(merchant: str) -> Optional[str]:
    """Calls Google Places Text Search API and returns a category or None."""
    if not PLACES_API_KEY:
        return None
    try:
        async with httpx.AsyncClient(timeout=6.0) as client:
            response = await client.post(
                PLACES_ENDPOINT,
                headers={
                    "Content-Type": "application/json",
                    "X-Goog-Api-Key": PLACES_API_KEY,
                    "X-Goog-FieldMask": "places.types",  # Only request types — minimizes cost
                },
                json={
                    "textQuery": merchant,
                    "languageCode": "en",
                    "regionCode": "IN",    # Bias to India
                    "maxResultCount": 1,
                },
            )
            if response.status_code != 200:
                logger.warning(f"Places API HTTP {response.status_code} for '{merchant}'")
                return None

            data = response.json()
            places = data.get("places", [])
            if not places:
                return None

            types = places[0].get("types", [])
            for place_type in types:
                category = PLACE_TYPE_TO_CATEGORY.get(place_type.lower())
                if category:
                    return category
            return None
    except Exception as e:
        logger.warning(f"Places API failed for '{merchant}': {e}")
        return None


# ─────────────────────────────────────────────
#  Layer 3: Gemini LLM
# ─────────────────────────────────────────────

async def _call_gemini(merchant: str, sms_snippet: str) -> Optional[str]:
    """Calls Gemini 2.0 Flash and returns a validated category string."""
    client = _gemini_client
    if client is None:
        return None
    try:
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

        # Run blocking Gemini call in a thread pool to not block the event loop
        loop = asyncio.get_event_loop()
        response = await loop.run_in_executor(
            None,
            lambda: client.models.generate_content(
                model="gemini-2.0-flash",
                contents=prompt,
            )
        )
        raw = (response.text or "").strip().upper()
        return raw if raw in VALID_CATEGORIES else "MISCELLANEOUS"
    except Exception as e:
        logger.error(f"Gemini failed for '{merchant}': {e}")
        return None


# ─────────────────────────────────────────────
#  Public API: Categorize Single
# ─────────────────────────────────────────────

async def categorize_merchant(merchant: str, sms_snippet: str) -> CategorizeResponse:
    """
    Full categorization pipeline for a single merchant.
    Returns CategorizeResponse with category and source.
    """
    merchant = merchant.strip()
    if not merchant:
        return CategorizeResponse(merchant=merchant, category="MISCELLANEOUS", source="FALLBACK")

    # Layer 1: Supabase shared global cache
    cached = get_from_cache(merchant)
    if cached:
        logger.info(f"Global cache HIT: '{merchant}' → {cached}")
        return CategorizeResponse(merchant=merchant, category=cached, source="GLOBAL_CACHE")

    # Layer 2: Google Places API
    places_cat = await _call_places_api(merchant)
    if places_cat:
        save_to_cache(merchant, places_cat, "PLACES")
        logger.info(f"Places resolved: '{merchant}' → {places_cat}")
        return CategorizeResponse(merchant=merchant, category=places_cat, source="PLACES")

    # Layer 3: Gemini LLM
    gemini_cat = await _call_gemini(merchant, sms_snippet)
    if gemini_cat:
        save_to_cache(merchant, gemini_cat, "GEMINI")
        logger.info(f"Gemini resolved: '{merchant}' → {gemini_cat}")
        return CategorizeResponse(merchant=merchant, category=gemini_cat, source="GEMINI")

    # Fallback
    logger.warning(f"All layers failed for '{merchant}' — returning MISCELLANEOUS")
    return CategorizeResponse(merchant=merchant, category="MISCELLANEOUS", source="FALLBACK")


# ─────────────────────────────────────────────
#  Public API: Categorize Batch
# ─────────────────────────────────────────────

async def categorize_batch(
    transactions: list[BatchCategorizeItem],
) -> tuple[list[CategorizeResponse], int, int]:
    """
    Categorize a batch of transactions efficiently.

    Strategy:
    1. Batch-check all merchants against Supabase cache in ONE query
    2. For cache misses, deduplicate merchants (same merchant in batch = 1 API call)
    3. Process cache misses concurrently (up to 5 at a time to respect rate limits)

    Returns: (results, cache_hits_count, api_calls_count)
    """
    if not transactions:
        return [], 0, 0

    results: list[CategorizeResponse] = []
    cache_hits = 0
    api_calls = 0

    # Step 1: Deduplicate merchants for cache lookup
    merchant_names = [t.merchant for t in transactions]
    cache_map = get_batch_from_cache(merchant_names)  # Single Supabase query

    # Step 2: Separate cache hits from misses
    cached_results: dict[str, CategorizeResponse] = {}
    for txn in transactions:
        key = txn.merchant.strip().lower()
        if key in cache_map:
            cached_results[txn.merchant] = CategorizeResponse(
                merchant=txn.merchant,
                category=cache_map[key],
                source="GLOBAL_CACHE",
            )
            cache_hits += 1

    # Step 3: Collect unique cache misses
    misses = list({
        t.merchant: t for t in transactions
        if t.merchant.strip().lower() not in cache_map
    }.values())

    # Step 4: Process misses with a concurrency limit (semaphore)
    semaphore = asyncio.Semaphore(3)  # Max 3 concurrent API calls

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
