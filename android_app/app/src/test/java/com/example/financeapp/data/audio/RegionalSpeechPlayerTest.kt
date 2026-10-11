package com.example.financeapp.data.audio

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Locale

class RegionalSpeechPlayerTest {

    private lateinit var mockEngine: TestSpeechEngine
    private lateinit var player: RegionalSpeechPlayer

    class TestSpeechEngine : SpeechEngine {
        var lastSpokenText: String? = null
        var lastLanguage: Locale? = null
        var lastQueueMode: Int? = null
        var lastUtteranceId: String? = null
        var isStopped: Boolean = false
        var isShutdown: Boolean = false
        var listener: UtteranceProgressListener? = null
        var languageSupportStatus: Int = TextToSpeech.LANG_AVAILABLE

        override fun setLanguage(locale: Locale): Int {
            lastLanguage = locale
            return languageSupportStatus
        }

        override fun speak(text: String, queueMode: Int, params: Bundle?, utteranceId: String): Int {
            lastSpokenText = text
            lastQueueMode = queueMode
            lastUtteranceId = utteranceId
            return TextToSpeech.SUCCESS
        }

        override fun stop(): Int {
            isStopped = true
            return TextToSpeech.SUCCESS
        }

        override fun shutdown() {
            isShutdown = true
        }

        override fun setOnUtteranceProgressListener(listener: UtteranceProgressListener) {
            this.listener = listener
        }

        fun simulateStart(utteranceId: String) {
            listener?.onStart(utteranceId)
        }

        fun simulateDone(utteranceId: String) {
            listener?.onDone(utteranceId)
        }
    }

    @Before
    fun setUp() {
        mockEngine = TestSpeechEngine()
        player = RegionalSpeechPlayer(context = null, engineOverride = mockEngine)
    }

    @Test
    fun speak_formatsCurrencyAndDispatchesToEngine() {
        val success = player.speak("Your balance is ₹1,200", "en-IN")

        assertTrue(success)
        assertEquals("Your balance is 1200 rupees", mockEngine.lastSpokenText)
        assertEquals(Locale("en", "IN"), mockEngine.lastLanguage)
        assertEquals(TextToSpeech.QUEUE_FLUSH, mockEngine.lastQueueMode)
        assertNotNull(mockEngine.lastUtteranceId)
    }

    @Test
    fun speak_regionalHindi_setsHindiLocaleAndFormattedSpeech() {
        val success = player.speak("बैलेंस ₹5,000 है", "hi-IN")

        assertTrue(success)
        assertEquals("बैलेंस 5000 रुपये है", mockEngine.lastSpokenText)
        assertEquals(Locale("hi", "IN"), mockEngine.lastLanguage)
    }

    @Test
    fun speak_languageNotSupported_fallsBackToIndianEnglish() {
        // Simulate device missing Hindi voice data
        mockEngine.languageSupportStatus = TextToSpeech.LANG_NOT_SUPPORTED

        val success = player.speak("बैलेंस ₹5,000 है", "hi-IN")

        assertTrue(success)
        // Should fall back to English locale
        assertEquals(Locale("en", "IN"), mockEngine.lastLanguage)
    }

    @Test
    fun speak_lifecycle_tracksIsPlayingStateFlow() {
        var completed = false
        player.speak("Hello world", "en-IN") {
            completed = true
        }

        val utteranceId = mockEngine.lastUtteranceId!!

        // Simulate playback start
        mockEngine.simulateStart(utteranceId)
        assertTrue(player.isPlaying.value)

        // Simulate playback completion
        mockEngine.simulateDone(utteranceId)
        assertFalse(player.isPlaying.value)
        assertTrue(completed)
    }

    @Test
    fun stop_resetsPlayingStateAndStopsEngine() {
        player.speak("Testing stop", "en-IN")
        mockEngine.simulateStart(mockEngine.lastUtteranceId!!)
        assertTrue(player.isPlaying.value)

        player.stop()

        assertTrue(mockEngine.isStopped)
        assertFalse(player.isPlaying.value)
    }

    @Test
    fun shutdown_releasesEngineResources() {
        player.shutdown()
        assertTrue(mockEngine.isShutdown)
    }

    @Test
    fun speak_blankInput_returnsFalseWithoutDispatching() {
        val result = player.speak("   ", "en-IN")
        assertFalse(result)
        assertNull(mockEngine.lastSpokenText)
    }
}

