"""
Pydantic models for all RupAI API request and response payloads.
"""
import re
from typing import List, Literal, Optional
from pydantic import BaseModel, ConfigDict, Field, field_validator


# ─────────────────────────────────────────────
#  Categorize — Single
# ─────────────────────────────────────────────

class CategorizeRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")
    merchant: str = Field(..., strict=True, min_length=1, max_length=80)
    # Preserve current Android payload compatibility without accepting any raw SMS.
    sms_snippet: Literal[""] = ""

    @field_validator("merchant")
    @classmethod
    def safe_merchant(cls, value: str) -> str:
        if any(ord(c) < 32 or 127 <= ord(c) <= 159 for c in value) or "@" in value or re.search(r"\d{4,}", value):
            raise ValueError("Invalid merchant label")
        value = value.strip()
        if not value:
            raise ValueError("Invalid merchant label")
        return value


class CategorizeResponse(BaseModel):
    merchant: str
    category: str
    source: str  # "LOCAL_CACHE" | "GLOBAL_CACHE" | "PLACES" | "GEMINI" | "FALLBACK"


# ─────────────────────────────────────────────
#  Categorize — Batch
# ─────────────────────────────────────────────

class BatchCategorizeItem(CategorizeRequest):
    pass


class BatchCategorizeRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")
    transactions: List[BatchCategorizeItem] = Field(
        ..., min_length=1, max_length=100, description="Merchant labels only"
    )


class BatchCategorizeResponse(BaseModel):
    results: List[CategorizeResponse]
    total: int
    cache_hits: int
    api_calls: int


# ─────────────────────────────────────────────
#  Insights — Spending Analysis
# ─────────────────────────────────────────────

class TransactionContext(BaseModel):
    merchant: str
    amount: float  # negative = debit, positive = credit
    category: str
    date: str  # ISO 8601


class SpendingAnalysisRequest(BaseModel):
    transactions: List[TransactionContext]
    monthly_income: Optional[float] = None


class SpendingAnalysisResponse(BaseModel):
    summary: str
    insights: List[str]
    savings_tips: List[str]
    top_category: str
    health_score: int  # 0–100
    total_spent: float
    total_income: float
    category_breakdown: dict  # category -> total amount


# ─────────────────────────────────────────────
#  Insights — Financial Advisor Chat
# ─────────────────────────────────────────────

class AdvisorRequest(BaseModel):
    question: str = Field(..., description="User's natural language question")
    transaction_context: Optional[List[TransactionContext]] = Field(
        default=None,
        description="Recent transactions to give the AI context"
    )


class AdvisorResponse(BaseModel):
    answer: str
    follow_up_suggestions: List[str]


# ─────────────────────────────────────────────
#  Analytics — Recurring Detection
# ─────────────────────────────────────────────

class RecurringItem(BaseModel):
    merchant: str
    amount: float
    frequency: str  # "MONTHLY" | "WEEKLY" | "QUARTERLY"
    next_expected: Optional[str] = None  # ISO date string
    confidence: str  # "HIGH" | "MEDIUM" | "LOW"


class RecurringDetectionRequest(BaseModel):
    transactions: List[TransactionContext]


class RecurringDetectionResponse(BaseModel):
    recurring: List[RecurringItem]
    total_monthly_commitments: float
    summary: str


# ─────────────────────────────────────────────
#  Health
# ─────────────────────────────────────────────

class HealthResponse(BaseModel):
    status: str
    version: str
    services: dict
