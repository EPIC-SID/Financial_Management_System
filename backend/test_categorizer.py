"""
Unit tests for the backend categorization pipeline, Groq LLM integration, and cache protection.
"""
import asyncio
import logging
import unittest
from unittest.mock import patch, MagicMock, AsyncMock

from cache import is_cachable_category, CONFIDENT_CATEGORIES
from categorizer import categorize_merchant, categorize_batch
from llm import call_llm
from models import BatchCategorizeItem, CategorizeResponse


class TestCategorizer(unittest.IsolatedAsyncioTestCase):

    @classmethod
    def setUpClass(cls):
        # Silence runtime informational/warning logs during unit testing
        logging.disable(logging.CRITICAL)

    @classmethod
    def tearDownClass(cls):
        logging.disable(logging.NOTSET)

    def test_is_cachable_category(self):
        for cat in CONFIDENT_CATEGORIES:
            self.assertTrue(is_cachable_category(cat))

        self.assertFalse(is_cachable_category("MISCELLANEOUS"))
        self.assertFalse(is_cachable_category("FALLBACK"))
        self.assertFalse(is_cachable_category("UNKNOWN"))
        self.assertFalse(is_cachable_category(None))
        self.assertFalse(is_cachable_category(""))

    @patch("categorizer.get_from_cache")
    async def test_confident_cache_hit_returns_global_cache(self, mock_get_cache):
        mock_get_cache.return_value = "FOOD"

        resp = await categorize_merchant("Swiggy")
        self.assertEqual(resp.category, "FOOD")
        self.assertEqual(resp.source, "GLOBAL_CACHE")

    @patch("categorizer.save_to_cache")
    @patch("categorizer._call_llm_categorizer", new_callable=AsyncMock)
    @patch("categorizer._call_places_api", new_callable=AsyncMock)
    @patch("categorizer.get_from_cache")
    async def test_uncertain_cache_hit_is_ignored_and_re_resolved(
        self, mock_get_cache, mock_places, mock_llm, mock_save_cache
    ):
        # Cache returns uncertain MISCELLANEOUS hit
        mock_get_cache.return_value = "MISCELLANEOUS"
        mock_places.return_value = None
        mock_llm.return_value = ("ENTERTAINMENT", "GROQ")

        resp = await categorize_merchant("Steam")
        self.assertEqual(resp.category, "ENTERTAINMENT")
        self.assertEqual(resp.source, "GROQ")
        mock_save_cache.assert_called_once_with("Steam", "ENTERTAINMENT", "GROQ")

    @patch("categorizer.save_to_cache")
    @patch("categorizer._call_llm_categorizer", new_callable=AsyncMock)
    @patch("categorizer._call_places_api", new_callable=AsyncMock)
    @patch("categorizer.get_from_cache")
    async def test_llm_timeout_returns_fallback_without_cache_poisoning(
        self, mock_get_cache, mock_places, mock_llm, mock_save_cache
    ):
        mock_get_cache.return_value = None
        mock_places.return_value = None
        # Simulate timeout returning None
        mock_llm.return_value = (None, "GROQ")

        resp = await categorize_merchant("Unknown Dhaba")
        self.assertEqual(resp.category, "MISCELLANEOUS")
        self.assertEqual(resp.source, "FALLBACK")
        # Ensure save_to_cache was NEVER called with MISCELLANEOUS / FALLBACK
        mock_save_cache.assert_not_called()

    @patch("categorizer.get_from_cache")
    @patch("categorizer.save_to_cache")
    @patch("categorizer._call_llm_categorizer", new_callable=AsyncMock)
    @patch("categorizer._call_places_api", new_callable=AsyncMock)
    @patch("categorizer.get_batch_from_cache")
    async def test_batch_categorization_handles_mixed_and_invalid_cache(
        self, mock_batch_cache, mock_places, mock_llm, mock_save_cache, mock_get_cache
    ):
        # Cache returns hit for Swiggy, but nothing for Steam or Random Vendor
        mock_get_cache.return_value = None
        mock_batch_cache.return_value = {
            "swiggy": "FOOD"
        }
        mock_places.side_effect = lambda m: "HEALTH" if "apollo" in m.lower() else None
        mock_llm.side_effect = lambda m, s: ("ENTERTAINMENT", "GROQ") if "steam" in m.lower() else (None, "GROQ")

        items = [
            BatchCategorizeItem(merchant="Swiggy"),
            BatchCategorizeItem(merchant="Apollo Pharmacy"),
            BatchCategorizeItem(merchant="Steam Games"),
            BatchCategorizeItem(merchant="Random Vendor"),
        ]

        results, cache_hits, api_calls = await categorize_batch(items)

        self.assertEqual(len(results), 4)
        self.assertEqual(results[0].category, "FOOD")
        self.assertEqual(results[0].source, "GLOBAL_CACHE")
        self.assertEqual(results[1].category, "HEALTH")
        self.assertEqual(results[1].source, "PLACES")
        self.assertEqual(results[2].category, "ENTERTAINMENT")
        self.assertEqual(results[2].source, "GROQ")
        self.assertEqual(results[3].category, "MISCELLANEOUS")
        self.assertEqual(results[3].source, "FALLBACK")

        self.assertEqual(cache_hits, 1)
        self.assertEqual(api_calls, 3)

    @patch.dict("os.environ", {"GROQ_API_KEY": "gsk_test_key", "GROQ_MODEL": "llama-3.3-70b-versatile"})
    @patch("httpx.AsyncClient.post")
    async def test_groq_llm_call_success(self, mock_post):
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = {
            "choices": [{"message": {"content": "FOOD"}}]
        }
        mock_post.return_value = mock_response

        reply, provider = await call_llm("Classify Swiggy")
        self.assertEqual(reply, "FOOD")
        self.assertEqual(provider, "GROQ")


if __name__ == "__main__":
    unittest.main()
