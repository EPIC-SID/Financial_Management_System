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
  -d '{"merchant": "Zomato", "sms_snippet": "Rs 250 debited at Zomato"}'
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
