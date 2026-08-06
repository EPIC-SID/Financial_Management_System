"""
JWT authentication helper for RupAI API.

Extracts user ID from Supabase JWT if present, or defaults to dev user.
Never raises 401 errors so local testing and Swagger UI work seamlessly.
"""
import os
import logging
import jwt
from fastapi import Request

logger = logging.getLogger(__name__)
SUPABASE_JWT_SECRET = os.getenv("SUPABASE_JWT_SECRET", "")


def get_user_id(request: Request) -> str:
    """
    Reads Authorization header if present and extracts user ID.
    Defaults to 'dev_user' if no token or invalid token is supplied.
    """
    auth_header = request.headers.get("authorization") or request.headers.get("Authorization")
    if not auth_header or not auth_header.startswith("Bearer "):
        return "dev_user"
    
    token = auth_header.split(" ", 1)[1].strip()
    if not token or not SUPABASE_JWT_SECRET:
        return "dev_user"

    try:
        payload = jwt.decode(
            token,
            SUPABASE_JWT_SECRET,
            algorithms=["HS256"],
            options={"verify_exp": True, "verify_aud": False}
        )
        return payload.get("sub", "dev_user")
    except Exception as e:
        logger.warning(f"JWT decode notice: {e} — using dev_user")
        return "dev_user"
