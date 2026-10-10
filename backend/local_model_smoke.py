"""Opt-in local narration probe. Synthetic English only; not an application provider adapter."""
import argparse
import json
import re
import time
from urllib.error import URLError
from urllib.request import HTTPRedirectHandler, ProxyHandler, Request, build_opener

from pydantic import ValidationError

from copilot import NarrateRequest, TextNarration


MAX_RESPONSE_BYTES = 65_536


class SmokeError(Exception):
    pass


class NoRedirects(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        raise SmokeError("Redirects are disabled for this local-only probe.")


def request_json(path: str, payload: dict, timeout: int) -> dict:
    if path not in {"/api/show", "/api/chat"}:
        raise SmokeError("Unsupported local endpoint.")
    request = Request("http://127.0.0.1:11434" + path,
                      data=json.dumps(payload).encode("utf-8"),
                      headers={"Content-Type": "application/json"}, method="POST")
    # Never route even synthetic local prompts through an environment-configured HTTP proxy.
    opener = build_opener(ProxyHandler({}), NoRedirects())
    try:
        with opener.open(request, timeout=timeout) as response:
            body = response.read(MAX_RESPONSE_BYTES + 1)
        if len(body) > MAX_RESPONSE_BYTES:
            raise SmokeError("Local response exceeded the size limit.")
        result = json.loads(body)
        if not isinstance(result, dict):
            raise SmokeError("Invalid local response shape.")
        return result
    except (URLError, OSError, ValueError) as error:
        raise SmokeError("Local model request failed. Check Ollama is running and the model is installed.") from error


def run_smoke(model: str, timeout: int = 90) -> dict:
    if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9_.:-]{0,100}", model) or "cloud" in model.lower():
        raise SmokeError("Use an installed local model tag, not a URL or cloud model.")
    if not 1 <= timeout <= 300:
        raise SmokeError("Timeout must be between 1 and 300 seconds.")
    metadata = request_json("/api/show", {"model": model}, min(timeout, 10))
    if metadata.get("remote_host") or metadata.get("remote_model"):
        raise SmokeError("Cloud-backed models are disabled for this local-only probe.")
    fixture = NarrateRequest.model_validate({"intent": "ACCOUNT_OVERVIEW", "bundle": {
        "liquidRunwayMonths": 3.4, "projectedShortfallRisk": .12,
        "recommendedPlanId": "Plan B", "savingsRate": .18,
        "bankCash": 45200., "cardDues": 14300., "monthlyLoanEmis": 26700.,
        "monthlySip": 10000., "avgDiscretionary": 6400.,
        "forecastP10": 38000., "forecastP50": 52000., "forecastP90": 69000.,
        "horizonMonths": 6, "confidenceLabel": "Medium",
    }})
    schema = TextNarration.model_json_schema()
    payload = {
        "model": model, "stream": False, "think": False, "format": schema,
        "options": {"temperature": 0, "num_ctx": 2048, "num_predict": 384},
        "keep_alive": "2m",
        "messages": [
            {"role": "system", "content":
             "Write short English financial narration using only the supplied aggregate facts. "
             "Return one observed, one predicted and one recommended text item as JSON. "
             "Do not calculate or invent numbers, guarantee outcomes, use tools or number lists. "
             "Observed text must state bankCash and projectedShortfallRisk without judging adequacy. "
             "Predicted text must state forecastP10, forecastP50, forecastP90 and horizonMonths; "
             "the interval is not a guarantee. Recommended text may only name recommendedPlanId "
             "and say it requires engine simulation before commitment. No plan effects, income trends, "
             "SIP changes, debt changes or extra advice are supplied: do not infer them. "
             "Keep each text to at most two short sentences. "
             "Use ASCII digits and INR. Follow this JSON schema: " + json.dumps(schema)},
            {"role": "user", "content": fixture.model_dump_json()},
        ],
    }
    started = time.perf_counter()
    response = request_json("/api/chat", payload, timeout)
    elapsed = time.perf_counter() - started
    if response.get("done") is not True or response.get("done_reason") not in (None, "stop"):
        raise SmokeError("Model generation did not complete normally.")
    message = response.get("message")
    if not isinstance(message, dict) or not isinstance(message.get("content"), str):
        raise SmokeError("Model returned no text message.")
    try:
        narration = TextNarration.model_validate_json(message["content"])
    except ValidationError as error:
        raise SmokeError("Model output failed the existing narration schema.") from error
    return {"model": model, "elapsed_seconds": round(elapsed, 3), "schema_valid": True,
            "production_number_verifier": "NOT_RUN", "language_quality": "NOT_EVALUATED",
            "semantic_review": "REQUIRED_NOT_SAFE_FOR_APP_USE",
            "narration": narration.model_dump()}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--model", default="qwen3:8b", help="Already installed local Ollama model tag")
    parser.add_argument("--timeout", type=int, default=90)
    args = parser.parse_args()
    try:
        result = run_smoke(args.model, args.timeout)
    except SmokeError as error:
        print(json.dumps({"status": "FAILED", "reason": str(error)}))
        return 1
    print(json.dumps(result, ensure_ascii=True, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
