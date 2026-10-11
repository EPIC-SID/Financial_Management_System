# RupAI Backend API

FastAPI backend that powers the AI categorization and financial insights for the RupAI Android app.

## Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET`  | `/health` | None | Liveness probe |
| `POST` | `/api/v1/categorize` | JWT | Single merchant categorization |
| `POST` | `/api/v1/categorize/batch` | JWT | Batch merchant categorization |
| `POST` | `/api/v1/insights/analyze` | JWT | AI spending analysis |
| `POST` | `/api/v1/insights/advisor` | JWT | Financial advisor chat |
| `POST` | `/api/v1/analytics/recurring` | JWT | Recurring payment detection |

API docs available at `/docs` (Swagger UI) when running locally.

## Copilot proxy (Phase 2G)

`POST /api/v1/copilot/interpret` accepts `{"intent":"ACCOUNT_OVERVIEW"}` and returns
`{"intent":"ACCOUNT_OVERVIEW","params":{}}`. The Android client routes questions locally;
raw questions never cross the proxy boundary. Supported intents are `ACCOUNT_OVERVIEW`,
`PLAN_COMPARISON`, `RISK_EXPLANATION`, `WHAT_IF_QUERY`, and `GENERAL_INQUIRY`.

`POST /api/v1/copilot/narrate` accepts `{"intent":"ACCOUNT_OVERVIEW","bundle":{...}}`.
The typed bundle fields are documented in `/docs`: numeric aggregates, `horizonMonths`
(1-24), `confidenceLabel` (High/Medium/Low), and `recommendedPlanId`
(Plan A/Plan B/Status quo). Unknown fields and arbitrary text labels are rejected.
Responses use the observed/predicted/recommended schema. The model supplies section
text only; ranges and impact metadata come from trusted inputs. Android verifies every
narrated number and substitutes `LocalAdvisor` on verification or transport failure.

All `/api/v1/` routes require Supabase HS256 JWTs with a valid expiry, UUID subject,
`authenticated` audience, and issuer `${SUPABASE_URL}/auth/v1`. Missing auth configuration
fails closed. Asymmetric Supabase signing keys require a JWKS verifier before use.
Requests are capped at 32 KB, including chunked bodies, and 30 calls/user/minute across
all authenticated routes. The limiter is process-local: deploy one worker or enforce a shared gateway
limit before scaling. Copilot request bodies are not stored, cached, or logged.
Missing or invalid tokens are rejected; there is no development-user bypass.
Categorization accepts only a bounded merchant label and an optional empty
`sms_snippet` for Android compatibility. Nonempty snippets and unknown fields are
rejected, with generic validation errors that do not echo submitted data.

Set `GEMINI_API_KEY`, `SUPABASE_URL`, and `SUPABASE_JWT_SECRET` on the server.
`COPILOT_GEMINI_MODEL` defaults to the requested `gemini-1.5-flash`, which Google shut
down on September 29, 2025. Configure an available model for live narration; the retired
default safely causes local fallback. Provider calls time out after five seconds and
return generic 502/503/504 errors. Client requests require HTTPS except for debug loopback
and Android emulator connections. Android callers should use `BackendClient.answer`
or `narrate` only after AI-sharing consent is granted; no consent state is stored here.

Run offline proxy tests from `backend/`: `python -m unittest test_copilot -v`.

## Local Development Setup

### 1. Prerequisites
- Python 3.12+
- A Supabase project (use the existing RupAI project)
- A **new** Google AI Studio API key (create at https://aistudio.google.com/apikey)
- A Google Places API key (Google Cloud Console → Enable "Places API (New)")

### 2. Supabase Setup
Run `supabase_setup.sql` in your Supabase SQL Editor to create the `merchant_categories` table.

### 3. Environment Variables
```bash
cp .env.example .env
# Edit .env and fill in all values
```

Required values in `.env`:
- `SUPABASE_URL` — your Supabase project URL
- `SUPABASE_SERVICE_KEY` — service role key (NOT the anon key)
- `SUPABASE_JWT_SECRET` — JWT secret from Supabase project settings
- `GEMINI_API_KEY` — new Google AI Studio key
- `PLACES_API_KEY` — Google Places API key

### 4. Install Dependencies & Run
```bash
cd backend
python -m venv .venv
.venv\Scripts\activate       # Windows
# source .venv/bin/activate  # Mac/Linux

pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```

### 5. Test Locally
```bash
# Health check (no auth needed)
curl http://localhost:8000/health

# Categorize (requires valid Supabase JWT)
curl -X POST http://localhost:8000/api/v1/categorize \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_SUPABASE_JWT" \
  -d '{"merchant": "Zomato", "sms_snippet": ""}'
```

## Deployment on Railway (Free)

1. Go to [railway.app](https://railway.app) → New Project → Deploy from GitHub Repo
2. Set root directory to `backend/`
3. Add all environment variables from `.env` in Railway's Variables tab
4. Railway auto-detects the Dockerfile and deploys

After deployment, copy the Railway URL and set `BACKEND_URL=https://your-app.up.railway.app` in the Android app's `local.properties`.

## File Structure
```
backend/
├── main.py            ← FastAPI app + all route definitions
├── categorizer.py     ← Categorization pipeline (Places + Gemini)
├── insights.py        ← Spending analysis + advisor chat
├── analytics.py       ← Recurring payment detection
├── auth.py            ← Supabase JWT verification middleware
├── cache.py           ← Supabase merchant_categories table operations
├── models.py          ← Pydantic request/response models
├── supabase_setup.sql ← SQL to run in Supabase dashboard
├── requirements.txt
├── Dockerfile
└── .env.example
```
