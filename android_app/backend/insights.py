"""
AI financial insights — Endpoints 4 & 5.

Endpoint 4: POST /api/v1/insights/analyze
  - Takes a user's transaction list + monthly income
  - Returns AI-generated spending summary, actionable tips, health score
  - Powers the AiReviewScreen (currently a hardcoded placeholder)

Endpoint 5: POST /api/v1/insights/advisor
  - Natural language Q&A about the user's finances
  - Returns a conversational AI answer + follow-up suggestions
  - Powers the "Finance Advisor" feature from todo.txt
"""
import asyncio
import json
import logging
import os
from collections import defaultdict
from typing import Optional

from google import genai

from models import (
    TransactionContext,
    SpendingAnalysisResponse,
    AdvisorResponse,
)

logger = logging.getLogger(__name__)

GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")

def _get_client() -> Optional[genai.Client]:
    if not GEMINI_API_KEY:
        return None
    return genai.Client(api_key=GEMINI_API_KEY)


def _build_transaction_summary(transactions: list[TransactionContext]) -> dict:
    """Compute basic stats from the transaction list before sending to Gemini."""
    category_totals: dict[str, float] = defaultdict(float)
    total_spent = 0.0
    total_income = 0.0

    for txn in transactions:
        if txn.amount < 0:
            total_spent += abs(txn.amount)
            category_totals[txn.category] += abs(txn.amount)
        else:
            total_income += txn.amount

    top_category = max(category_totals, key=category_totals.get) if category_totals else "MISCELLANEOUS"

    return {
        "total_spent": round(total_spent, 2),
        "total_income": round(total_income, 2),
        "category_totals": dict(category_totals),
        "top_category": top_category,
        "transaction_count": len(transactions),
    }


# ─────────────────────────────────────────────
#  Endpoint 4: Spending Analysis
# ─────────────────────────────────────────────

async def analyze_spending(
    transactions: list[TransactionContext],
    monthly_income: Optional[float],
) -> SpendingAnalysisResponse:
    """
    Generates a comprehensive AI spending analysis for the AiReviewScreen.
    """
    stats = _build_transaction_summary(transactions)
    income = monthly_income or stats["total_income"] or 1  # avoid div/0

    # Build a compact transaction summary string for the prompt
    top_transactions = sorted(
        [t for t in transactions if t.amount < 0],
        key=lambda x: abs(x.amount),
        reverse=True
    )[:15]  # Top 15 expenses for context

    txn_lines = "\n".join(
        f"- {t.merchant}: ₹{abs(t.amount):.0f} ({t.category})"
        for t in top_transactions
    )

    category_lines = "\n".join(
        f"- {cat}: ₹{amt:.0f} ({(amt/stats['total_spent']*100):.1f}%)"
        for cat, amt in sorted(stats["category_totals"].items(), key=lambda x: -x[1])
    )

    prompt = f"""You are a personal financial advisor for Indian users. Analyze these spending patterns and provide actionable advice.

Monthly Income: ₹{income:.0f}
Total Spent This Period: ₹{stats['total_spent']:.0f}
Total Income Credited: ₹{stats['total_income']:.0f}

Spending by Category:
{category_lines}

Top Transactions:
{txn_lines}

Respond in this EXACT JSON format (no markdown, no explanation, just the JSON object):
{{
  "summary": "A 2-3 sentence overview of the spending pattern in a friendly, supportive tone",
  "insights": ["insight 1", "insight 2", "insight 3"],
  "savings_tips": ["specific actionable tip 1", "specific actionable tip 2", "specific actionable tip 3"],
  "health_score": <integer 0-100 based on savings rate and spending balance>
}}

Rules:
- Be specific about Indian merchants, Indian currencies (₹), Indian context
- Health score: 80+ = excellent (saving >30%), 60-79 = good (saving 15-30%), 40-59 = fair (saving <15%), below 40 = needs attention (spending > income)
- Insights should be data-driven observations
- Savings tips should be practical for Indian users"""

    client = _get_client()
    if not client:
        # Return a basic computed response if Gemini is unavailable
        return SpendingAnalysisResponse(
            summary=f"You spent ₹{stats['total_spent']:.0f} this period across {len(stats['category_totals'])} categories.",
            insights=[f"Your top spending category is {stats['top_category']}."],
            savings_tips=["Try to track your expenses weekly to identify patterns."],
            top_category=stats["top_category"],
            health_score=50,
            total_spent=stats["total_spent"],
            total_income=stats["total_income"],
            category_breakdown=stats["category_totals"],
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
        # Strip markdown code blocks if Gemini wraps the JSON
        raw = raw.strip().removeprefix("```json").removeprefix("```").removesuffix("```").strip()
        parsed = json.loads(raw)

        return SpendingAnalysisResponse(
            summary=parsed.get("summary", ""),
            insights=parsed.get("insights", []),
            savings_tips=parsed.get("savings_tips", []),
            top_category=stats["top_category"],
            health_score=max(0, min(100, int(parsed.get("health_score", 50)))),
            total_spent=stats["total_spent"],
            total_income=stats["total_income"],
            category_breakdown=stats["category_totals"],
        )
    except Exception as e:
        logger.error(f"Spending analysis failed: {e}")
        return SpendingAnalysisResponse(
            summary=f"You spent ₹{stats['total_spent']:.0f} this period.",
            insights=[f"Top category: {stats['top_category']}"],
            savings_tips=["Track your spending weekly to identify patterns."],
            top_category=stats["top_category"],
            health_score=50,
            total_spent=stats["total_spent"],
            total_income=stats["total_income"],
            category_breakdown=stats["category_totals"],
        )


# ─────────────────────────────────────────────
#  Endpoint 5: Financial Advisor Chat
# ─────────────────────────────────────────────

async def get_advisor_response(
    question: str,
    transaction_context: Optional[list[TransactionContext]],
) -> AdvisorResponse:
    """
    Answers a user's natural language financial question using their transaction data.
    """
    context_section = ""
    if transaction_context:
        stats = _build_transaction_summary(transaction_context)
        top = sorted(
            [t for t in transaction_context if t.amount < 0],
            key=lambda x: abs(x.amount), reverse=True
        )[:10]
        txn_lines = "\n".join(
            f"- {t.merchant}: ₹{abs(t.amount):.0f} ({t.category})"
            for t in top
        )
        cat_lines = "\n".join(
            f"- {cat}: ₹{amt:.0f}" for cat, amt in
            sorted(stats["category_totals"].items(), key=lambda x: -x[1])
        )
        context_section = f"""
User's recent transactions:
{txn_lines}

Spending by category:
{cat_lines}
Total spent: ₹{stats['total_spent']:.0f}
"""

    prompt = f"""You are a friendly, knowledgeable personal financial advisor for Indian users.
Answer the user's question based on their transaction data. Be specific, practical, and empathetic.
Use Indian context (₹, UPI, Jio, Swiggy, etc.) where relevant.
{context_section}

User question: "{question}"

Respond in this EXACT JSON format (no markdown, just JSON):
{{
  "answer": "Your detailed, friendly answer here (2-4 sentences, specific and actionable)",
  "follow_up_suggestions": ["Question 1 the user might want to ask next", "Question 2", "Question 3"]
}}"""

    client = _get_client()
    if not client:
        return AdvisorResponse(
            answer="I'm unable to provide advice right now. Please try again later.",
            follow_up_suggestions=[]
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

        return AdvisorResponse(
            answer=parsed.get("answer", ""),
            follow_up_suggestions=parsed.get("follow_up_suggestions", [])
        )
    except Exception as e:
        logger.error(f"Advisor response failed: {e}")
        return AdvisorResponse(
            answer="I encountered an issue analyzing your data. Please try again.",
            follow_up_suggestions=["How much did I spend this month?", "What is my biggest expense category?"]
        )
