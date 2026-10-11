package com.example.financeapp.data.audio

import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class SpeechTextFormatterTest {

    @Test
    fun stripMarkdown_removesAsterisksHashesAndBullets() {
        val markdown = "### Financial Review\n* **Your spend** on food was ₹500.\n- Save *more* this month!"
        val result = SpeechTextFormatter.formatForSpeech(markdown, "en-IN")

        assertFalse(result.contains("*"))
        assertFalse(result.contains("#"))
        assertFalse(result.contains("-"))
        assertTrue(result.contains("Your spend on food was 500 rupees."))
    }

    @Test
    fun stripEmojis_removesPictograms() {
        val text = "🚨 Alert! 💰 Your balance increased by ₹1,000."
        val result = SpeechTextFormatter.formatForSpeech(text, "en-IN")

        assertFalse(result.contains("🚨"))
        assertFalse(result.contains("💰"))
        assertTrue(result.contains("Alert! Your balance increased by 1000 rupees."))
    }

    @Test
    fun normalizeCurrencies_englishFormat_replacesRupeeSymbol() {
        val text = "Total bill: ₹1,500.50 at Swiggy"
        val result = SpeechTextFormatter.formatForSpeech(text, "en-IN")

        assertEquals("Total bill: 1500 rupees and 50 paise at Swiggy", result)
    }

    @Test
    fun normalizeCurrencies_devanagariFormat_replacesWithHindiWords() {
        val text = "आपका खर्च ₹2,000 है"
        val result = SpeechTextFormatter.formatForSpeech(text, "hi-IN")

        assertEquals("आपका खर्च 2000 रुपये है", result)
    }

    @Test
    fun normalizeCurrencies_marathiFormat_replacesWithMarathiWords() {
        val text = "तुमचा खर्च ₹3,500 आहे"
        val result = SpeechTextFormatter.formatForSpeech(text, "mr-IN")

        assertEquals("तुमचा खर्च 3500 रुपये आहे", result)
    }

    @Test
    fun normalizeAbbreviations_convertsThousandAndPercent() {
        val text = "You saved 15k, which is 25% of your target"
        val result = SpeechTextFormatter.formatForSpeech(text, "en-IN")

        assertEquals("You saved 15 thousand, which is 25 percent of your target", result)
    }

    @Test
    fun resolveLocale_mapsLanguagesCorrectly() {
        assertEquals(Locale("en", "IN"), SpeechTextFormatter.resolveLocale("en-IN"))
        assertEquals(Locale("hi", "IN"), SpeechTextFormatter.resolveLocale("hi-IN"))
        assertEquals(Locale("hi", "IN"), SpeechTextFormatter.resolveLocale("hinglish"))
        assertEquals(Locale("mr", "IN"), SpeechTextFormatter.resolveLocale("mr-IN"))
        assertEquals(Locale("en", "IN"), SpeechTextFormatter.resolveLocale("unknown"))
    }

    @Test
    fun formatForSpeech_blankInput_returnsEmptyString() {
        assertEquals("", SpeechTextFormatter.formatForSpeech("", "en-IN"))
        assertEquals("", SpeechTextFormatter.formatForSpeech("   ", "hi-IN"))
    }
}

