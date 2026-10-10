"""Deterministic scorer/transport tests; never call a real provider."""
import json
import unittest
from unittest.mock import patch

import model_benchmark as bench


class ModelBenchmarkTest(unittest.TestCase):
    def setUp(self):
        self.case = bench.CASES[0]
        self.output = {
            "observed": [{"text": "Bank cash: INR 45,200; shortfall risk: 12%."}],
            "predicted": [{"text": "P10: INR 38,000; P50: INR 52,000; P90: INR 69,000; horizon: 6 months."}],
            "recommended": [{"text": "Plan B requires simulation before commitment."}],
        }

    def test_correct_bound_values_pass_all_seven_facts(self):
        score = bench.score(self.case, json.dumps(self.output))
        self.assertEqual(7, score["correct_facts"])
        self.assertTrue(score["case_pass"])

    def test_swapped_values_fail_even_though_both_numbers_exist(self):
        self.output["predicted"][0]["text"] = "P10: INR 52000; P50: INR 38000; P90: INR 69000; horizon: 6 months."
        score = bench.score(self.case, json.dumps(self.output))
        self.assertEqual(5, score["correct_facts"])
        self.assertFalse(score["case_pass"])

    def test_extra_number_wrong_plan_and_omitted_fields_fail(self):
        self.output["recommended"][0]["text"] = "Plan A saves INR 9999."
        score = bench.score(self.case, json.dumps(self.output))
        self.assertFalse(score["numbers_grounded"])
        self.assertFalse(score["case_pass"])
        self.assertEqual(6, score["correct_facts"])
        self.output["predicted"][0]["text"] = "Nothing supplied."
        self.assertEqual(2, bench.score(self.case, json.dumps(self.output))["correct_facts"])

    def test_invalid_schema_and_duplicate_json_keys_fail(self):
        for text in ("not JSON", "{}", '{"observed":[],"observed":[]}'):
            self.assertFalse(bench.score(self.case, text)["schema_valid"])

    def test_negative_and_decimal_values_are_preserved(self):
        case = dict(self.case, bankCash=10.25, riskPercent=0.5, p10=-100.75)
        self.output["observed"][0]["text"] = "Bank cash: INR 10.25; shortfall risk: 0.5%."
        self.output["predicted"][0]["text"] = "P10: INR -100.75; P50: INR 52000; P90: INR 69000; horizon: 6 months."
        self.assertTrue(bench.score(case, json.dumps(self.output))["case_pass"])

    def test_duplicate_label_with_contradictory_value_fails(self):
        self.output["observed"][0]["text"] += " Bank cash: INR 1."
        self.assertFalse(bench.score(self.case, json.dumps(self.output))["case_pass"])

    def test_summary_counts_failure_in_denominator_and_no_results_as_unavailable(self):
        good = bench.score(self.case, json.dumps(self.output)) | {"elapsed_seconds": 1, "status": "OK"}
        failed = bench.failed_result("TIMEOUT", 2)
        summary = bench.summarize([good, failed])
        self.assertEqual(50, summary["fact_accuracy_percent"])
        self.assertEqual(50, summary["case_pass_percent"])
        self.assertEqual(14, summary["expected_facts"])
        self.assertIsNone(bench.summarize([])["fact_accuracy_percent"])

    def test_missing_key_never_sends_request(self):
        with patch.dict("os.environ", {"GROQ_API_KEY": ""}), patch.object(bench.httpx, "Client") as client:
            with self.assertRaises(bench.BenchmarkError):
                bench.generate("groq", "openai/gpt-oss-20b", self.case, 10)
            client.assert_not_called()

    def test_mocked_groq_uses_https_no_redirects_or_proxy_and_same_messages(self):
        response = bench.httpx.Response(200, json={"choices": [{"finish_reason": "stop", "message": {"content": json.dumps(self.output)}}]})
        with patch.dict("os.environ", {"GROQ_API_KEY": "test-only-secret"}), patch.object(bench.httpx, "Client") as client:
            client.return_value.__enter__.return_value.stream.return_value.__enter__.return_value = response
            content = bench.generate("groq", "openai/gpt-oss-20b", self.case, 10)
        self.assertEqual(self.output, json.loads(content))
        self.assertFalse(client.call_args.kwargs["trust_env"])
        self.assertFalse(client.call_args.kwargs["follow_redirects"])

    def test_groq_quota_error_is_generic_without_leaking_response_or_key(self):
        response = bench.httpx.Response(429, json={"error": "sensitive provider payload"})
        with patch.dict("os.environ", {"GROQ_API_KEY": "test-only-secret"}), patch.object(bench.httpx, "Client") as client:
            client.return_value.__enter__.return_value.stream.return_value.__enter__.return_value = response
            with self.assertRaises(bench.BenchmarkError) as failure:
                bench.generate("groq", "openai/gpt-oss-20b", self.case, 10)
        self.assertEqual("HTTP_429", str(failure.exception))

    def test_rejects_numeric_prefixes_scientific_notation_and_scale_words(self):
        for money in ("45200e3", "45200 lakh", "45200/12", "45,200,12"):
            self.output["observed"][0]["text"] = f"Bank cash: INR {money}; shortfall risk: 12%."
            with self.subTest(money=money):
                self.assertFalse(bench.score(self.case, json.dumps(self.output))["case_pass"])

    def test_indian_grouping_matches_amount_without_changing_value(self):
        case = dict(self.case, bankCash=12345678)
        self.output["observed"][0]["text"] = "Bank cash: INR 1,23,45,678; shortfall risk: 12%."
        self.assertTrue(bench.score(case, json.dumps(self.output))["case_pass"])

    def test_signed_zero_and_tiny_percent_are_numeric_not_string_comparisons(self):
        case = dict(self.case, bankCash=0, riskPercent=0.01)
        self.output["observed"][0]["text"] = "Bank cash: INR -0.00; shortfall risk: 0.01%."
        self.assertTrue(bench.score(case, json.dumps(self.output))["case_pass"])

    def test_schema_rejects_extra_keys_empty_text_wrong_types_and_multiple_items(self):
        for change in ({"unexpected": "injected"}, {"observed": [{"text": " "}]},
                       {"observed": [{"text": None}]}, {"observed": []},
                       {"observed": [{"text": "a"}, {"text": "b"}]}):
            with self.subTest(change=change):
                self.assertFalse(bench.score(self.case, json.dumps(self.output | change))["schema_valid"])

    def test_transport_failure_shapes_truncation_and_oversize_are_not_success(self):
        bodies = [([], "INVALID_RESPONSE"), ({"choices": []}, "INVALID_RESPONSE"),
                  ({"choices": [{"finish_reason": "length", "message": {"content": "{}"}}]}, "INCOMPLETE_GENERATION"),
                  ({"choices": [{"finish_reason": "stop", "message": {"content": None}}]}, "INVALID_RESPONSE")]
        for body, expected in bodies:
            response = bench.httpx.Response(200, json=body)
            with patch.dict("os.environ", {"GROQ_API_KEY": "test-only-secret"}), patch.object(bench.httpx, "Client") as client:
                client.return_value.__enter__.return_value.stream.return_value.__enter__.return_value = response
                with self.assertRaises(bench.BenchmarkError) as failure:
                    bench.generate("groq", "openai/gpt-oss-20b", self.case, 10)
                self.assertEqual(expected, str(failure.exception))
        response = bench.httpx.Response(200, content=b"x" * (bench.MAX_RESPONSE_BYTES + 1))
        with patch.dict("os.environ", {"GROQ_API_KEY": "test-only-secret"}), patch.object(bench.httpx, "Client") as client:
            client.return_value.__enter__.return_value.stream.return_value.__enter__.return_value = response
            with self.assertRaisesRegex(bench.BenchmarkError, "OVERSIZED_RESPONSE"):
                bench.generate("groq", "openai/gpt-oss-20b", self.case, 10)

    def test_timeout_and_connection_errors_have_fixed_codes(self):
        for error, code in ((bench.httpx.ReadTimeout("private details"), "TIMEOUT"),
                            (bench.httpx.ConnectError("private details"), "CONNECTION_ERROR")):
            with patch.dict("os.environ", {"GROQ_API_KEY": "test-only-secret"}), patch.object(bench.httpx, "Client") as client:
                client.return_value.__enter__.return_value.stream.side_effect = error
                with self.assertRaises(bench.BenchmarkError) as failure:
                    bench.generate("groq", "openai/gpt-oss-20b", self.case, 10)
                self.assertEqual(code, str(failure.exception))


if __name__ == "__main__":
    unittest.main()
