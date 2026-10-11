"""Shared fail-closed Supabase authentication and per-user request limits."""
import os
import time
from collections import deque
from uuid import UUID

import jwt
from fastapi import HTTPException, Request

# shortcut: process-local limiter; use a shared gateway/store before multiple workers.
_requests: dict[str, deque[float]] = {}


def require_user(request: Request) -> str:
    secret = os.getenv("SUPABASE_JWT_SECRET", "")
    issuer = os.getenv("SUPABASE_URL", "").rstrip("/")
    if not secret or not issuer.startswith("https://"):
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


def get_user_id(request: Request) -> str:
    return require_user(request)
