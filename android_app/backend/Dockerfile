FROM python:3.12-slim

WORKDIR /app

# Install dependencies first (cached layer)
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY . .

# Railway sets PORT env var automatically
ENV PORT=8000

EXPOSE 8000

# Start the server
CMD uvicorn main:app --host 0.0.0.0 --port ${PORT}
