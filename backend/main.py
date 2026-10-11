"""
RupAI FastAPI Backend — main application entry point.

Endpoints:
  GET  /health                      — Liveness probe
  POST /api/v1/categorize           — Single merchant categorization
  POST /api/v1/categorize/batch     — Batch merchant categorization
  POST /api/v1/insights/analyze     — AI spending analysis
  POST /api/v1/insights/advisor     — Financial advisor chat
  POST /api/v1/analytics/recurring  — Recurring payment detection
"""
import logging
import os
from contextlib import asynccontextmanager

from pathlib import Path
from dotenv import load_dotenv

# Load .env file from the current directory regardless of execution CWD
_ENV_FILE = Path(__file__).resolve().parent / ".env"
load_dotenv(_ENV_FILE)

from fastapi import Depends, FastAPI, HTTPException, Request, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from analytics import detect_recurring
from auth import get_user_id
from categorizer import categorize_batch, categorize_merchant
from insights import analyze_spending, get_advisor_response
from copilot import install_copilot
from models import (
    AdvisorRequest,
    AdvisorResponse,
    BatchCategorizeRequest,
    BatchCategorizeResponse,
    CategorizeRequest,
    CategorizeResponse,
    HealthResponse,
    RecurringDetectionRequest,
    RecurringDetectionResponse,
    SpendingAnalysisRequest,
    SpendingAnalysisResponse,
)

# ─────────────────────────────────────────────
#  Logging
# ─────────────────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s | %(levelname)s | %(name)s | %(message)s",
)
logger = logging.getLogger(__name__)

VERSION = "1.0.0"


# ─────────────────────────────────────────────
#  App Factory
# ─────────────────────────────────────────────

@asynccontextmanager
async def lifespan(app: FastAPI):
    """Startup and shutdown events."""
    logger.info(f"RupAI Backend v{VERSION} starting up")
    logger.info(f"Groq API key configured:   {bool(os.getenv('GROQ_API_KEY'))}")
    logger.info(f"Gemini API key configured: {bool(os.getenv('GEMINI_API_KEY'))}")
    logger.info(f"Places API key configured: {bool(os.getenv('PLACES_API_KEY'))}")
    logger.info(f"Supabase URL configured:   {bool(os.getenv('SUPABASE_URL'))}")
    yield
    logger.info("RupAI Backend shutting down")


app = FastAPI(
    title="RupAI Backend API",
    description="AI-powered financial transaction categorization and insights backend for RupAI Android app.",
    version=VERSION,
    lifespan=lifespan,
    docs_url="/docs",     # Swagger UI at /docs
    redoc_url="/redoc",   # ReDoc UI at /redoc
)

install_copilot(app)

# CORS — allow the Android app and any web frontend to call this API
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Restrict to specific origins in production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ─────────────────────────────────────────────
#  Global Exception Handler
# ─────────────────────────────────────────────

@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    logger.error(f"Unhandled exception on {request.url}: {exc}", exc_info=True)
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={"detail": "Internal server error. Please try again."},
    )


# ─────────────────────────────────────────────
#  Endpoint 1: Health Check
# ─────────────────────────────────────────────

@app.get(
    "/health",
    response_model=HealthResponse,
    summary="Health check",
    tags=["System"],
)
async def health_check() -> HealthResponse:
    """
    Returns the health status of the API and all downstream services.
    Used by Railway for liveness probes.
    """
    return HealthResponse(
        status="ok",
        version=VERSION,
        services={
            "groq": "configured" if os.getenv("GROQ_API_KEY") else "missing_key",
            "gemini": "configured" if os.getenv("GEMINI_API_KEY") else "missing_key",
            "places": "configured" if os.getenv("PLACES_API_KEY") else "missing_key",
            "supabase": "configured" if os.getenv("SUPABASE_URL") else "missing_key",
        },
    )


# ─────────────────────────────────────────────
#  Endpoint 2: Single Categorize
# ─────────────────────────────────────────────

@app.post(
    "/api/v1/categorize",
    response_model=CategorizeResponse,
    summary="Categorize a single merchant",
    tags=["Categorization"],
)
async def categorize_single(
    body: CategorizeRequest,
    request: Request,
) -> CategorizeResponse:
    user_id = get_user_id(request)
    """
    Categorizes a single merchant name using the 3-layer pipeline:
    Global Cache → Google Places → Gemini 2.0 Flash

    Used by AiCategorizationWorker for one transaction at a time.
    Requires a valid Supabase JWT in the Authorization header.
    """
    logger.info(f"[{user_id[:8]}] Categorize: '{body.merchant}'")
    return await categorize_merchant(body.merchant, body.sms_snippet)


# ─────────────────────────────────────────────
#  Endpoint 3: Batch Categorize
# ─────────────────────────────────────────────

@app.post(
    "/api/v1/categorize/batch",
    response_model=BatchCategorizeResponse,
    summary="Categorize multiple merchants in one request",
    tags=["Categorization"],
)
async def categorize_batch_endpoint(
    body: BatchCategorizeRequest,
    request: Request,
) -> BatchCategorizeResponse:
    user_id = get_user_id(request)
    """
    Efficiently categorizes a batch of transactions in a single API call.

    - Batch-checks the Supabase global cache with one query
    - Deduplicates repeated merchants (same merchant = 1 API call)
    - Processes cache misses concurrently with a rate-limit semaphore

    Used during initial SMS sync to categorize 50+ transactions at once.
    """
    if len(body.transactions) > 100:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Batch size cannot exceed 100 transactions per request."
        )

    logger.info(f"[{user_id[:8]}] Batch categorize: {len(body.transactions)} transactions")
    results, cache_hits, api_calls = await categorize_batch(body.transactions)

    return BatchCategorizeResponse(
        results=results,
        total=len(results),
        cache_hits=cache_hits,
        api_calls=api_calls,
    )


# ─────────────────────────────────────────────
#  Endpoint 4: Spending Analysis
# ─────────────────────────────────────────────

@app.post(
    "/api/v1/insights/analyze",
    response_model=SpendingAnalysisResponse,
    summary="Get AI-powered spending analysis",
    tags=["Insights"],
)
async def spending_analysis(
    body: SpendingAnalysisRequest,
    request: Request,
) -> SpendingAnalysisResponse:
    user_id = get_user_id(request)
    """
    Analyzes a user's transaction history and returns:
    - Spending summary in natural language
    - Data-driven insights (e.g., "You overspent on food by 40%")
    - Actionable savings tips tailored to Indian context
    - Financial health score (0–100)
    - Category breakdown

    Powers the AiReviewScreen (currently a placeholder in the Android app).
    """
    if not body.transactions:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="At least one transaction is required for analysis."
        )
    if len(body.transactions) > 500:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Cannot analyze more than 500 transactions at once."
        )

    logger.info(
        f"[{user_id[:8]}] Spending analysis: {len(body.transactions)} transactions, "
        f"income: ₹{body.monthly_income}"
    )
    return await analyze_spending(body.transactions, body.monthly_income)


# ─────────────────────────────────────────────
#  Endpoint 5: Financial Advisor Chat
# ─────────────────────────────────────────────

@app.post(
    "/api/v1/insights/advisor",
    response_model=AdvisorResponse,
    summary="Ask the AI financial advisor a question",
    tags=["Insights"],
)
async def financial_advisor(
    body: AdvisorRequest,
    request: Request,
) -> AdvisorResponse:
    user_id = get_user_id(request)
    """
    Natural language Q&A about the user's financial situation.
    Optionally provide transaction context for personalized answers.

    Examples:
    - "Where am I overspending?"
    - "How can I save ₹5000 this month?"
    - "Am I spending too much on food?"

    Powers the "Finance Advisor" feature from the app's todo list.
    """
    if not body.question.strip():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Question cannot be empty."
        )

    logger.info(f"[{user_id[:8]}] Advisor question: '{body.question[:60]}'")
    return await get_advisor_response(body.question, body.transaction_context)


# ─────────────────────────────────────────────
#  Endpoint 6: Recurring Detection
# ─────────────────────────────────────────────

@app.post(
    "/api/v1/analytics/recurring",
    response_model=RecurringDetectionResponse,
    summary="Detect recurring and subscription payments",
    tags=["Analytics"],
)
async def recurring_detection(
    body: RecurringDetectionRequest,
    request: Request,
) -> RecurringDetectionResponse:
    user_id = get_user_id(request)
    """
    Analyzes transaction history to identify:
    - Monthly subscriptions (Netflix, Spotify, Amazon Prime)
    - Utility bills (Jio, Airtel recharge, electricity)
    - EMIs and insurance premiums
    - Rent payments

    Returns the list of detected recurring payments and total monthly commitments.
    Powers the "Recurring Transaction Detection" feature from todo.txt.
    """
    if not body.transactions:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="At least one transaction is required."
        )

    logger.info(f"[{user_id[:8]}] Recurring detection: {len(body.transactions)} transactions")
    return await detect_recurring(body.transactions)
