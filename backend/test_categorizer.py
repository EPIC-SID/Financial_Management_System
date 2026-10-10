"""Offline regressions for uncertainty and transient provider failures."""
import unittest
from types import SimpleNamespace
from unittest.mock import AsyncMock, Mock, patch

import categorizer
from models import BatchCategorizeItem


class CategorizerTest(unittest.IsolatedAsyncioTestCase):
    async def test_old_misc_cache_is_retried_and_replaced(self):
        with patch.object(categorizer, "get_from_cache", return_value="MISCELLANEOUS"), \
                patch.object(categorizer, "_call_places_api", AsyncMock(return_value=None)), \
                patch.object(categorizer, "_call_gemini", AsyncMock(return_value="HEALTH")), \
                patch.object(categorizer, "save_to_cache") as save:
            result = await categorizer.categorize_merchant("Shree Ganesh Medical", "")
            self.assertEqual("HEALTH", result.category)
            save.assert_called_once_with("Shree Ganesh Medical", "HEALTH", "GEMINI")

    async def test_legitimate_uncertainty_is_not_cached_by_merchant(self):
        with patch.object(categorizer, "get_from_cache", return_value=None), \
                patch.object(categorizer, "_call_places_api", AsyncMock(return_value=None)), \
                patch.object(categorizer, "_call_gemini", AsyncMock(return_value="MISCELLANEOUS")), \
                patch.object(categorizer, "save_to_cache") as save:
            result = await categorizer.categorize_merchant("Ravi", "")
            self.assertEqual("GEMINI", result.source)
            save.assert_not_called()

    async def test_provider_failure_is_explicit_fallback(self):
        with patch.object(categorizer, "get_from_cache", return_value=None), \
                patch.object(categorizer, "_call_places_api", AsyncMock(return_value=None)), \
                patch.object(categorizer, "_call_gemini", AsyncMock(return_value=None)), \
                patch.object(categorizer, "save_to_cache") as save:
            result = await categorizer.categorize_merchant("Merchant", "")
            self.assertEqual("FALLBACK", result.source)
            save.assert_not_called()

    async def test_malformed_model_output_is_failure_not_misc_classification(self):
        client = SimpleNamespace(models=SimpleNamespace(generate_content=Mock(
            return_value=SimpleNamespace(text="Category: FOOD"))))
        with patch.object(categorizer, "_gemini_client", client):
            self.assertIsNone(await categorizer._call_gemini("Merchant", ""))
        self.assertEqual(categorizer.CATEGORIZATION_MODEL,
                         client.models.generate_content.call_args.kwargs["model"])

    async def test_batch_ignores_old_misc_and_invalid_cache_entries(self):
        with patch.object(categorizer, "get_batch_from_cache", return_value={
                    "merchant": "MISCELLANEOUS", "bad": "INVALID"}), \
                patch.object(categorizer, "get_from_cache", return_value=None), \
                patch.object(categorizer, "_call_places_api", AsyncMock(return_value="FOOD")), \
                patch.object(categorizer, "save_to_cache"):
            results, hits, calls = await categorizer.categorize_batch([
                BatchCategorizeItem(merchant="Merchant"), BatchCategorizeItem(merchant="Bad")])
            self.assertEqual(["FOOD", "FOOD"], [r.category for r in results])
            self.assertEqual((0, 2), (hits, calls))


if __name__ == "__main__":
    unittest.main()
