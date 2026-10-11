"""Offline harness checks; no Ollama process or model download required."""
import io
import json
import unittest
from unittest.mock import patch

import local_model_smoke as smoke


class LocalModelSmokeTest(unittest.TestCase):
    def valid_response(self):
        return {"done": True, "done_reason": "stop", "message": {"content": json.dumps({
            "observed": [{"text": "Bank cash is INR 45200."}],
            "predicted": [{"text": "The supplied forecast covers 6 months."}],
            "recommended": [{"text": "Consider Plan B without guaranteed outcomes."}],
        })}}

    def test_synthetic_request_uses_existing_schema_and_bounded_local_generation(self):
        with patch.object(smoke, "request_json", side_effect=[{}, self.valid_response()]) as request:
            result = smoke.run_smoke("qwen2.5-coder:7b", 90)
        self.assertIn("observed", result["narration"])
        self.assertEqual("REQUIRED_NOT_SAFE_FOR_APP_USE", result["semantic_review"])
        payload = request.call_args.args[1]
        self.assertFalse(payload["stream"])
        self.assertFalse(payload["think"])
        self.assertEqual(2048, payload["options"]["num_ctx"])
        self.assertEqual(smoke.TextNarration.model_json_schema(), payload["format"])
        self.assertEqual("/api/chat", request.call_args.args[0])
        self.assertNotIn("merchant", json.dumps(payload))

    def test_invalid_output_and_incomplete_generation_fail(self):
        for response in ({}, {"done": False},
                         self.valid_response() | {"done_reason": "length"},
                         {"done": True, "message": {"content": "not JSON"}},
                         {"done": True, "message": {"content": '{"observed": []}'}}):
            with self.subTest(response=response), patch.object(smoke, "request_json", side_effect=[{}, response]):
                with self.assertRaises(smoke.SmokeError):
                    smoke.run_smoke("qwen3:8b", 90)

    def test_model_and_timeout_validation_happens_before_request(self):
        for model, timeout in (("https://external.example/model", 90), ("qwen3:8b", 0),
                               ("qwen3:8b", 301), ("qwen3:cloud", 90)):
            with patch.object(smoke, "request_json") as request:
                with self.assertRaises(smoke.SmokeError):
                    smoke.run_smoke(model, timeout)
                request.assert_not_called()

    def test_rejects_cloud_backed_alias_and_redirect(self):
        with patch.object(smoke, "request_json", return_value={"remote_host": "https://example.com"}) as request:
            with self.assertRaises(smoke.SmokeError):
                smoke.run_smoke("qwen3:8b")
            self.assertEqual(1, request.call_count)
        with self.assertRaises(smoke.SmokeError):
            smoke.NoRedirects().redirect_request(None, None, 302, "", {}, "https://example.com")

    def test_transport_is_loopback_only_bounded_and_does_not_use_environment_proxies(self):
        opener = unittest.mock.MagicMock()
        opener.open.return_value.__enter__.return_value = io.BytesIO(b'{"done":true}')
        with patch.object(smoke, "build_opener", return_value=opener) as build:
            self.assertEqual({"done": True}, smoke.request_json("/api/chat", {}, 90))
        self.assertEqual("http://127.0.0.1:11434/api/chat", opener.open.call_args.args[0].full_url)
        self.assertEqual({}, build.call_args.args[0].proxies)

    def test_transport_rejects_oversized_or_invalid_json(self):
        for body in (b"x" * (smoke.MAX_RESPONSE_BYTES + 1), b"not JSON", b"[]"):
            opener = unittest.mock.MagicMock()
            opener.open.return_value.__enter__.return_value = io.BytesIO(body)
            with patch.object(smoke, "build_opener", return_value=opener):
                with self.assertRaises(smoke.SmokeError):
                    smoke.request_json("/api/chat", {}, 90)

    def test_connection_failure_does_not_expose_external_error_payload(self):
        opener = unittest.mock.MagicMock()
        opener.open.side_effect = smoke.URLError("sensitive provider error")
        with patch.object(smoke, "build_opener", return_value=opener):
            with self.assertRaises(smoke.SmokeError) as failure:
                smoke.request_json("/api/chat", {}, 90)
        self.assertNotIn("sensitive", str(failure.exception))


if __name__ == "__main__":
    unittest.main()
