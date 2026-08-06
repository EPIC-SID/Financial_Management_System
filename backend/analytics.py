"""
Recurring transaction detection — Endpoint 6.

POST /api/v1/analytics/recurring

Identifies subscription and recurring payments from a user's transaction history.
Uses Gemini to understand patterns that simple regex can't catch
(e.g., "Jio Platforms" every ~28 days = monthly phone recharge).
"""
import asyncio
import json
import logging
import os
from collections import defaultdict
from typing import Optional

from google import genai

from models import TransactionContext, RecurringItem, RecurringDetectionResponse

logger = logging.getLogger(__name__)

GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")


def _get_client() -> Optional[genai.Client]:
    if not GEMINI_API_KEY:
        return None
    return genai.Client(api_key=GEMINI_API_KEY)


def _pre_filter_candidates(transactions: list[TransactionContext]) -> list[dict]:
    """
    Pre-filter transactions to find candidates for recurring detection.
    Groups by merchant and returns merchants that appear 2+ times.
    This reduces the data sent to Gemini.
    """
    merchant_groups: dict[str, list] = defaultdict(list)
    for txn in transactions:
        if txn.amount < 0:  # Only look at debits
            merchant_groups[txn.merchant.strip().lower()].append(txn)

    candidates = []
    for merchant_key, txns in merchant_groups.items():
        if len(txns) >= 2:
            candidates.append({
                "merchant": txns[0].merchant,
                "occurrences": len(txns),
                "amounts": [abs(t.amount) for t in txns],
                "dates": [t.date for t in txns],
            })
    return candidates


async def detect_recurring(
    transactions: list[TransactionContext],
) -> RecurringDetectionResponse:
    """
    Detects recurring and subscription payments from transaction history.
    """
    if len(transactions) < 3:
        return RecurringDetectionResponse(
            recurring=[],
            total_monthly_commitments=0.0,
            summary="Not enough transaction history to detect recurring payments. Please sync more transactions."
        )

    candidates = _pre_filter_candidates(transactions)
    if not candidates:
        return RecurringDetectionResponse(
            recurring=[],
            total_monthly_commitments=0.0,
            summary="No recurring payment patterns detected in your transaction history."
        )

    # Build candidate summary for Gemini
    candidate_lines = "\n".join(
        f"- {c['merchant']}: appears {c['occurrences']} times, amounts: {c['amounts']}, dates: {c['dates']}"
        for c in candidates[:20]  # Cap at 20 to stay within token limits
    )

    prompt = f"""You are analyzing Indian UPI/bank transaction history to detect recurring and subscription payments.

Merchant payment patterns (appeared 2+ times):
{candidate_lines}

Identify which of these are recurring payments (subscriptions, EMIs, utility bills, recharges, rent, etc.).
For each recurring payment, determine the frequency.

Respond in this EXACT JSON format (no markdown, just the JSON array):
{{
  "recurring": [
    {{
      "merchant": "exact merchant name from the list",
      "amount": <most common amount as number>,
      "frequency": "MONTHLY" or "WEEKLY" or "QUARTERLY" or "ANNUAL",
      "confidence": "HIGH" or "MEDIUM" or "LOW",
      "next_expected": "YYYY-MM-DD estimated next payment date or null"
    }}
  ],
  "summary": "Brief summary of recurring payment patterns found"
}}

Rules:
- Only include payments that are genuinely recurring (not just coincidental repeats)
- HIGH confidence: same amount, regular intervals, known subscription service
- MEDIUM confidence: similar amounts, roughly regular intervals
- LOW confidence: merchant appears multiple times but pattern is unclear
- Common Indian recurring payments: Jio/Airtel recharge, Netflix, Amazon Prime, EMIs, insurance premiums, rent"""

    client = _get_client()
    if not client:
        return RecurringDetectionResponse(
            recurring=[],
            total_monthly_commitments=0.0,
            summary="AI analysis unavailable. Please check back later."
        )

    try:
        loop = asyncio.get_event_loop()
        response = await loop.run_in_executor(
            None,
            lambda: client.models.generate_content(
                model="gemini-2.0-flash",
                contents=prompt,
            )
        )
        raw = response.text or "{}"
        raw = raw.strip().removeprefix("```json").removeprefix("```").removesuffix("```").strip()
        parsed = json.loads(raw)

        recurring_items = []
        total_monthly = 0.0

        for item in parsed.get("recurring", []):
            freq = item.get("frequency", "MONTHLY")
            amount = float(item.get("amount", 0))

            # Calculate monthly equivalent for total commitments
            if freq == "MONTHLY":
                total_monthly += amount
            elif freq == "WEEKLY":
                total_monthly += amount * 4
            elif freq == "QUARTERLY":
                total_monthly += amount / 3
            elif freq == "ANNUAL":
                total_monthly += amount / 12

            recurring_items.append(RecurringItem(
                merchant=item.get("merchant", ""),
                amount=amount,
                frequency=freq,
                next_expected=item.get("next_expected"),
                confidence=item.get("confidence", "MEDIUM"),
            ))

        return RecurringDetectionResponse(
            recurring=recurring_items,
            total_monthly_commitments=round(total_monthly, 2),
            summary=parsed.get("summary", f"Found {len(recurring_items)} recurring payment(s).")
        )

    except Exception as e:
        logger.error(f"Recurring detection failed: {e}")
        return RecurringDetectionResponse(
            recurring=[],
            total_monthly_commitments=0.0,
            summary="Could not analyze recurring payments. Please try again."
        )
