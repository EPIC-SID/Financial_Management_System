"""
Unified LLM Client supporting Groq (primary, ultra-fast LPU inference) and Gemini.
"""
import asyncio
import logging
import os
from typing import Optional

import httpx

logger = logging.getLogger(__name__)

GROQ_ENDPOINT = "https://api.groq.com/openai/v1/chat/completions"
DEFAULT_GROQ_MODEL = "llama-3.3-70b-versatile"


def get_configured_provider() -> str:
    """Returns 'groq' if GROQ_API_KEY is present, 'gemini' if GEMINI_API_KEY is present, else 'none'."""
    if os.getenv("GROQ_API_KEY"):
        return "groq"
    if os.getenv("GEMINI_API_KEY"):
        return "gemini"
    return "none"


async def call_llm(
    prompt: str,
    system_prompt: Optional[str] = None,
    temperature: float = 0.1,
    max_tokens: int = 512,
    timeout: float = 5.0,
) -> tuple[Optional[str], str]:
    """
    Calls the configured LLM provider. Prioritizes Groq for sub-300ms latency.
    Returns tuple: (reply_text or None, provider_name: 'GROQ' | 'GEMINI' | 'NONE')
    """
    groq_key = os.getenv("GROQ_API_KEY", "").strip()
    if groq_key:
        reply = await _call_groq(
            api_key=groq_key,
            prompt=prompt,
            system_prompt=system_prompt,
            temperature=temperature,
            max_tokens=max_tokens,
            timeout=timeout,
        )
        return reply, "GROQ"

    gemini_key = os.getenv("GEMINI_API_KEY", "").strip()
    if gemini_key:
        reply = await _call_gemini_fallback(
            api_key=gemini_key,
            prompt=prompt,
            timeout=timeout,
        )
        return reply, "GEMINI"

    logger.warning("No LLM API key configured (neither GROQ_API_KEY nor GEMINI_API_KEY)")
    return None, "NONE"


async def _call_groq(
    api_key: str,
    prompt: str,
    system_prompt: Optional[str] = None,
    temperature: float = 0.1,
    max_tokens: int = 512,
    timeout: float = 5.0,
) -> Optional[str]:
    model = os.getenv("GROQ_MODEL", DEFAULT_GROQ_MODEL)
    messages = []
    if system_prompt:
        messages.append({"role": "system", "content": system_prompt})
    messages.append({"role": "user", "content": prompt})

    payload = {
        "model": model,
        "messages": messages,
        "temperature": temperature,
        "max_tokens": max_tokens,
    }

    headers = {
        "Authorization": f"Bearer {api_key}",
        "Content-Type": "application/json",
    }

    try:
        async with httpx.AsyncClient(timeout=timeout) as client:
            resp = await client.post(GROQ_ENDPOINT, headers=headers, json=payload)
            if resp.status_code == 200:
                data = resp.json()
                choices = data.get("choices", [])
                if choices:
                    return choices[0].get("message", {}).get("content", "").strip()
                return None
            else:
                logger.error(f"Groq API returned HTTP {resp.status_code}: {resp.text}")
                return None
    except httpx.TimeoutException:
        logger.warning(f"Groq API call timed out after {timeout}s")
        return None
    except Exception as e:
        logger.error(f"Groq API request failed: {e}")
        return None


async def _call_gemini_fallback(
    api_key: str,
    prompt: str,
    timeout: float = 5.0,
) -> Optional[str]:
    try:
        from google import genai
        client = genai.Client(api_key=api_key)
        loop = asyncio.get_event_loop()
        response = await asyncio.wait_for(
            loop.run_in_executor(
                None,
                lambda: client.models.generate_content(
                    model="gemini-2.0-flash",
                    contents=prompt,
                )
            ),
            timeout=timeout
        )
        return (response.text or "").strip()
    except Exception as e:
        logger.error(f"Gemini fallback failed: {e}")
        return None

