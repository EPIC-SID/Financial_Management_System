"""Stateless, aggregates-only Copilot proxy. No request content is logged or retained."""
import asyncio
import os
import time
from collections import deque
from enum import Enum
from typing import Annotated, Literal
from uuid import UUID

import jwt
from fastapi import APIRouter, Depends, HTTPException, Request
from fastapi.exception_handlers import request_validation_exception_handler
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from google import genai
from google.genai import types
from pydantic import BaseModel, ConfigDict, Field, model_validator
from llm import call_llm


class StrictModel(BaseModel):
    model_config = ConfigDict(extra="forbid", allow_inf_nan=False)


class Intent(str, Enum):
    ACCOUNT_OVERVIEW = "ACCOUNT_OVERVIEW"
    PLAN_COMPARISON = "PLAN_COMPARISON"
    RISK_EXPLANATION = "RISK_EXPLANATION"
    WHAT_IF_QUERY = "WHAT_IF_QUERY"
    GENERAL_INQUIRY = "GENERAL_INQUIRY"


Money = Annotated[float, Field(strict=True, ge=0, le=1e12)]
Balance = Annotated[float, Field(strict=True, ge=-1e12, le=1e12)]
Rate = Annotated[float, Field(strict=True, ge=0, le=1)]


class AggregateBundle(StrictModel):
    liquidRunwayMonths: Annotated[float, Field(strict=True, ge=0, le=1200)]
    projectedShortfallRisk: Rate
    recommendedPlanId: Literal["Plan A", "Plan B", "Status quo"]
    savingsRate: Rate
    bankCash: Money
    cardDues: Money
    monthlyLoanEmis: Money
    monthlySip: Money
    avgDiscretionary: Money
    forecastP10: Balance
    forecastP50: Balance
    forecastP90: Balance
    horizonMonths: Annotated[int, Field(strict=True, ge=1, le=24)]
    confidenceLabel: Literal["High", "Medium", "Low"]

    @model_validator(mode="after")
    def ordered_forecast(self):
        if not self.forecastP10 <= self.forecastP50 <= self.forecastP90:
            raise ValueError("Forecast quantiles must be ordered")
        return self


class InterpretRequest(StrictModel):
    # Only a locally selected category, never the user's raw question, crosses this boundary.
    intent: Intent


class InterpretResponse(StrictModel):
    intent: Intent
    params: dict[Literal["unused"], float] = Field(default_factory=dict, max_length=0)


class NarrateRequest(StrictModel):
    intent: Intent
    bundle: AggregateBundle
    languageCode: Literal["en", "hi-Latn", "hi-Deva", "mr-Deva"] = "en"


class TextFact(StrictModel):
    text: Annotated[str, Field(min_length=1, max_length=2000, pattern=r"\S")]


class TextNarration(StrictModel):
    observed: Annotated[list[TextFact], Field(min_length=1, max_length=1)]
    predicted: Annotated[list[TextFact], Field(min_length=1, max_length=1)]
    recommended: Annotated[list[TextFact], Field(min_length=1, max_length=1)]


class ObservedFact(TextFact):
    refs: list[str]


class NumberRange(StrictModel):
    p10: float
    p50: float
    p90: float


class PredictedFact(TextFact):
    range: NumberRange
    confidenceLabel: Literal["High", "Medium", "Low"]
    horizon: str


class Impact(StrictModel):
    runwayDeltaMonths: float = 0
    riskDelta: float = 0
    interestSaved: float = 0


class RecommendedAction(TextFact):
    impact: Impact
    tradeOff: str


class NarrateResponse(StrictModel):
    observed: list[ObservedFact]
    predicted: list[PredictedFact]
    recommended: list[RecommendedAction]
    intent: Intent
    source: Literal["REMOTE"] = "REMOTE"
    languageCode: Literal["en", "hi-Latn", "hi-Deva", "mr-Deva"] = "en"


# shortcut: limiter is process-local; use a shared gateway/store before deploying multiple workers.
_requests: dict[str, deque[float]] = {}


def require_user(request: Request) -> str:
    secret = os.getenv("SUPABASE_JWT_SECRET", "")
    issuer = os.getenv("SUPABASE_URL", "").rstrip("/")
    if not secret or not issuer:
        raise HTTPException(503, "Authentication unavailable")
    header = request.headers.get("authorization", "")
    if not header.startswith("Bearer "):
        raise HTTPException(401, "Authentication required", headers={"WWW-Authenticate": "Bearer"})
    try:
        payload = jwt.decode(header[7:], secret, algorithms=["HS256"], audience="authenticated",
                             issuer=issuer + "/auth/v1", options={"require": ["exp", "sub", "aud", "iss"]})
        user = str(UUID(payload["sub"]))
    except (jwt.InvalidTokenError, ValueError, TypeError, AttributeError):
        raise HTTPException(401, "Invalid access token", headers={"WWW-Authenticate": "Bearer"}) from None
    now = time.monotonic()
    for key in list(_requests):
        if not _requests[key] or _requests[key][-1] <= now - 60:
            del _requests[key]
    if user not in _requests and len(_requests) >= 10000:
        raise HTTPException(503, "Proxy busy")
    recent = _requests.setdefault(user, deque())
    while recent and recent[0] <= now - 60:
        recent.popleft()
    if len(recent) >= 30:
        raise HTTPException(429, "Request limit exceeded", headers={"Retry-After": "60"})
    recent.append(now)
    return user


async def generate(payload: StrictModel, schema: type[StrictModel], instruction: str):
    try:
        return await asyncio.wait_for(_generate(payload, schema, instruction), timeout=4.5)
    except (asyncio.TimeoutError, TimeoutError):
        raise HTTPException(504, "Narration timed out") from None


async def _generate(payload: StrictModel, schema: type[StrictModel], instruction: str):
    if os.getenv("GROQ_API_KEY", "").strip():
        try:
            text, _ = await asyncio.wait_for(call_llm(
                prompt=payload.model_dump_json(),
                system_prompt=instruction + " Return only JSON matching this schema: " + str(schema.model_json_schema()),
                temperature=0, max_tokens=1024, timeout=2.0), timeout=2.0)
            if text and len(text) <= 32768:
                return schema.model_validate_json(text)
        except (asyncio.TimeoutError, TimeoutError, ValueError):
            pass
    key = os.getenv("GEMINI_API_KEY", "")
    if not key:
        raise HTTPException(503, "Narration unavailable")
    try:
        client = genai.Client(api_key=key, http_options=types.HttpOptions(timeout=5000))
        async with client.aio as session:
            response = await asyncio.wait_for(session.models.generate_content(
                model=os.getenv("COPILOT_GEMINI_MODEL", "gemini-1.5-flash"),
                contents=payload.model_dump_json(),
                config=types.GenerateContentConfig(
                    system_instruction=instruction, temperature=0, max_output_tokens=2048,
                    response_mime_type="application/json", response_schema=schema,
                ),
            ), timeout=5)
        return schema.model_validate_json(response.text)
    except (asyncio.TimeoutError, TimeoutError):
        raise HTTPException(504, "Narration timed out") from None
    except Exception:
        raise HTTPException(502, "Narration unavailable") from None


router = APIRouter(prefix="/api/v1/copilot", tags=["Copilot"], dependencies=[Depends(require_user)])


@router.post("/interpret", response_model=InterpretResponse)
async def interpret(body: InterpretRequest):
    result = await generate(body, InterpretRequest, "Return the supplied allow-listed intent unchanged. No tools.")
    if result.intent != body.intent:
        raise HTTPException(502, "Invalid interpretation")
    return InterpretResponse(intent=result.intent)


@router.post("/narrate", response_model=NarrateResponse)
async def narrate(body: NarrateRequest):
    languages = {"en": "English", "hi-Latn": "Hinglish in Latin script",
                 "hi-Deva": "Hindi in Devanagari script", "mr-Deva": "Marathi in Devanagari script"}
    trade_offs = {
        "en": "Actions require engine simulation before commitment.",
        "hi-Latn": "Koi action lene se pehle engine simulation zaroori hai.",
        "hi-Deva": "कोई कदम लेने से पहले इंजन सिमुलेशन जरूरी है।",
        "mr-Deva": "कोणतीही कृती करण्यापूर्वी इंजिन सिम्युलेशन आवश्यक आहे.",
    }
    result = await generate(body, TextNarration,
        f"Write in {languages[body.languageCode]}. "
        "Write observed facts, predictions, and recommendations using only supplied aggregate values. "
        "Do not calculate, invent numbers, use tools, or claim guaranteed outcomes. "
        "Do not number lists. Percentages may format supplied fractional rates. Use ASCII digits. "
        "Use INR for money and % for percentages; preserve signs and plan identifiers. "
        "Do not use number words or translated scale words/units such as lakh, hazaar or crore.")
    b = body.bundle
    return NarrateResponse(
        observed=[ObservedFact(text=result.observed[0].text,
            refs=[key for key in AggregateBundle.model_fields if key not in {"horizonMonths", "confidenceLabel"}])],
        predicted=[PredictedFact(text=result.predicted[0].text,
            range=NumberRange(p10=b.forecastP10, p50=b.forecastP50, p90=b.forecastP90),
            confidenceLabel=b.confidenceLabel, horizon=f"{b.horizonMonths} months")],
        recommended=[RecommendedAction(text=result.recommended[0].text, impact=Impact(),
            tradeOff=trade_offs[body.languageCode])],
        intent=body.intent, languageCode=body.languageCode,
    )


class CopilotBodyLimit:
    """Bound streamed requests, including chunked bodies with no Content-Length."""
    def __init__(self, app):
        self.app = app

    async def __call__(self, scope, receive, send):
        if scope["type"] != "http" or not scope["path"].startswith("/api/v1/copilot/"):
            return await self.app(scope, receive, send)
        body = bytearray()
        while True:
            message = await receive()
            if message["type"] == "http.disconnect":
                return
            body.extend(message.get("body", b""))
            if len(body) > 32768:
                return await JSONResponse({"detail": "Request too large"}, status_code=413)(scope, receive, send)
            if not message.get("more_body", False):
                break
        delivered = False

        async def bounded_receive():
            nonlocal delivered
            if delivered:
                return await receive()
            delivered = True
            return {"type": "http.request", "body": bytes(body), "more_body": False}

        await self.app(scope, bounded_receive, send)


def install_copilot(app):
    app.include_router(router)
    app.add_middleware(CopilotBodyLimit)

    @app.exception_handler(RequestValidationError)
    async def safe_validation_error(request, exc):
        if request.url.path.startswith("/api/v1/copilot/"):
            return JSONResponse({"detail": "Invalid copilot request"}, status_code=422)
        return await request_validation_exception_handler(request, exc)
