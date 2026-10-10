package com.example.financeapp.data.audio

import java.util.Locale

/**
 * Normalizes text, currency, and numerical expressions for natural-sounding Text-to-Speech (TTS).
 *
 * Supported regional targets:
 * - English ("en", "en-IN")
 * - Hinglish ("hi-Latn", "en-IN")
 * - Hindi ("hi", "hi-IN", "hi-Deva")
 * - Marathi ("mr", "mr-IN", "mr-Deva")
 */
object SpeechTextFormatter {

    /**
     * Prepares raw text (such as Copilot responses, insights, or alert messages)
     * for spoken playback by removing markdown artifacts and phonetically expanding
     * currencies and abbreviations.
     */
    fun formatForSpeech(rawText: String, languageCode: String = "en-IN"): String {
        if (rawText.isBlank()) return ""

        var text = rawText

        // 1. Strip markdown syntax
        text = stripMarkdown(text)

        // 2. Strip emojis that TTS would otherwise read as literal descriptions (e.g. "money bag")
        text = stripEmojis(text)

        // 3. Normalize amounts and currencies
        val isDevanagari = isDevanagariTarget(languageCode)
        text = normalizeCurrencies(text, isDevanagari)

        // 4. Normalize abbreviations (e.g. "k" for thousand, "p.m." for per month)
        text = normalizeAbbreviations(text, isDevanagari)

        // 5. Clean up excessive punctuation and whitespace
        text = text.replace(Regex("\\s+"), " ")
            .replace(Regex("[,;]\\s*[,;]+"), ",")
            .replace(Regex("\\.{2,}"), ".")
            .trim()

        return text
    }

    /**
     * Resolves the target BCP-47 language tag to a standard java.util.Locale.
     */
    fun resolveLocale(languageCode: String): Locale {
        val lower = languageCode.lowercase().trim()
        return when {
            lower.startsWith("mr") -> Locale("mr", "IN")
            lower.startsWith("hi") -> Locale("hi", "IN")
            lower.contains("hinglish") -> Locale("hi", "IN")
            else -> Locale("en", "IN")
        }
    }

    private fun isDevanagariTarget(languageCode: String): Boolean {
        val lower = languageCode.lowercase().trim()
        return lower.startsWith("hi") || lower.startsWith("mr")
    }

    private fun stripMarkdown(input: String): String {
        var res = input
        // Bold/Italic asterisks and underscores: **bold**, *italic*, _italic_
        res = res.replace(Regex("[*_~`]+"), "")
        // Headers: ### Header -> Header
        res = res.replace(Regex("(?m)^#{1,6}\\s*"), "")
        // Bullet points: * Item, - Item, • Item -> Item
        res = res.replace(Regex("(?m)^\\s*[-*•]\\s+"), "")
        // Links: [Text](url) -> Text
        res = res.replace(Regex("\\[([^\\]]+)\\]\\([^)]+\\)"), "$1")
        return res
    }

    private fun stripEmojis(input: String): String {
        return input
            .replace(Regex("[\\p{So}\\p{Sk}]"), "")
            .replace(Regex("[\\uD800-\\uDBFF][\\uDC00-\\uDFFF]"), "")
            .replace(Regex("[\\u2600-\\u27BF]"), "")
    }

    private fun normalizeCurrencies(input: String, isDevanagari: Boolean): String {
        var res = input
        val currencyWord = if (isDevanagari) "रुपये" else "rupees"
        val paiseWord = if (isDevanagari) "पैसे" else "paise"

        // Pattern 1: ₹ 1,500.50 or Rs. 1,500.50
        val withDecimals = Regex("(?:₹|Rs\\.?|INR)\\s*([0-9,]+)\\.([0-9]{1,2})", RegexOption.IGNORE_CASE)
        res = res.replace(withDecimals) { match ->
            val whole = match.groupValues[1].replace(",", "")
            val decimal = match.groupValues[2]
            if (isDevanagari) {
                "$whole $currencyWord $decimal $paiseWord"
            } else {
                "$whole $currencyWord and $decimal $paiseWord"
            }
        }

        // Pattern 2: ₹ 1,500 or Rs. 1,500
        val wholeCurrency = Regex("(?:₹|Rs\\.?|INR)\\s*([0-9,]+)", RegexOption.IGNORE_CASE)
        res = res.replace(wholeCurrency) { match ->
            val num = match.groupValues[1].replace(",", "")
            if (isDevanagari) "$num $currencyWord" else "$num $currencyWord"
        }

        return res
    }

    private fun normalizeAbbreviations(input: String, isDevanagari: Boolean): String {
        var res = input
        val thousandWord = if (isDevanagari) "हजार" else "thousand"

        // Pattern: 15k or 15K
        val kNumber = Regex("\\b([0-9]+)\\s*[kK]\\b")
        res = res.replace(kNumber) { match ->
            val num = match.groupValues[1]
            "$num $thousandWord"
        }

        // Percentage: 15% -> 15 percent / 15 प्रतिशत
        val percentWord = if (isDevanagari) "प्रतिशत" else "percent"
        res = res.replace(Regex("([0-9]+)\\s*%")) { match ->
            val num = match.groupValues[1]
            "$num $percentWord"
        }

        return res
    }
}

