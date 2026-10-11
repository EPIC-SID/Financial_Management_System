package com.example.financeapp.data.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

/**
 * Interface abstraction over the native speech synthesis engine.
 * Allows decoupling Android OS TextToSpeech service in tests.
 */
interface SpeechEngine {
    fun setLanguage(locale: Locale): Int
    fun speak(text: String, queueMode: Int, params: Bundle?, utteranceId: String): Int
    fun stop(): Int
    fun shutdown()
    fun setOnUtteranceProgressListener(listener: UtteranceProgressListener)
}

/**
 * Production Android TextToSpeech wrapper.
 */
class AndroidTextToSpeechEngine(
    context: Context,
    onInit: (status: Int) -> Unit
) : SpeechEngine {

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        onInit(status)
    }

    override fun setLanguage(locale: Locale): Int = tts.setLanguage(locale)

    override fun speak(text: String, queueMode: Int, params: Bundle?, utteranceId: String): Int {
        return tts.speak(text, queueMode, params, utteranceId)
    }

    override fun stop(): Int = tts.stop()

    override fun shutdown() = tts.shutdown()

    override fun setOnUtteranceProgressListener(listener: UtteranceProgressListener) {
        tts.setOnUtteranceProgressListener(listener)
    }
}

/**
 * Regional Read-Aloud Service for RupAI.
 *
 * Capabilities:
 * - Supports `en-IN` (Indian English), `hi-IN` (Hindi & Hinglish), and `mr-IN` (Marathi).
 * - Automatic phonetics & currency formatting via [SpeechTextFormatter].
 * - Graceful fallback to default English if regional voice data is not installed on device.
 * - Reactive [isPlaying] StateFlow for speaker icons and UI controls.
 * - Lifecycle-safe stop and release methods.
 */
class RegionalSpeechPlayer(
    private val context: Context? = null,
    engineOverride: SpeechEngine? = null
) {
    companion object {
        private const val TAG = "RegionalSpeechPlayer"
    }

    private object SafeLog {
        fun d(tag: String, msg: String) {
            try { Log.d(tag, msg) } catch (_: Throwable) { println("[$tag] $msg") }
        }
        fun w(tag: String, msg: String, tr: Throwable? = null) {
            try { Log.w(tag, msg, tr) } catch (_: Throwable) { println("[$tag] WARN: $msg") }
        }
        fun e(tag: String, msg: String, tr: Throwable? = null) {
            try { Log.e(tag, msg, tr) } catch (_: Throwable) { println("[$tag] ERROR: $msg") }
        }
    }

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private var activeUtteranceId: String? = null
    private var onCompleteCallback: (() -> Unit)? = null

    private val engine: SpeechEngine? = engineOverride ?: run {
        if (context != null) {
            AndroidTextToSpeechEngine(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    _isInitialized.value = true
                    SafeLog.d(TAG, "TextToSpeech engine initialized successfully")
                } else {
                    _isInitialized.value = false
                    SafeLog.e(TAG, "Failed to initialize TextToSpeech engine: status $status")
                }
            }
        } else {
            null
        }
    }

    init {
        // If an engineOverride was provided directly (e.g. in tests), mark initialized
        if (engineOverride != null) {
            _isInitialized.value = true
        }

        setupUtteranceListener()
    }

    private fun setupUtteranceListener() {
        engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isPlaying.value = true
            }

            override fun onDone(utteranceId: String?) {
                if (utteranceId == activeUtteranceId) {
                    _isPlaying.value = false
                    val cb = onCompleteCallback
                    onCompleteCallback = null
                    cb?.invoke()
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                if (utteranceId == activeUtteranceId) {
                    _isPlaying.value = false
                    onCompleteCallback = null
                    SafeLog.w(TAG, "TTS playback error for utterance $utteranceId")
                }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                if (utteranceId == activeUtteranceId) {
                    _isPlaying.value = false
                    onCompleteCallback = null
                    SafeLog.w(TAG, "TTS playback error $errorCode for utterance $utteranceId")
                }
            }
        })
    }

    /**
     * Speaks the given text using the specified regional language.
     *
     * @param text The text (e.g. Copilot explanation, alert, or forecast).
     * @param languageCode Target language code ("en-IN", "hi-IN", "mr-IN").
     * @param onComplete Optional callback fired when playback finishes.
     * @return true if speech was successfully dispatched, false otherwise.
     */
    fun speak(
        text: String,
        languageCode: String = "en-IN",
        onComplete: (() -> Unit)? = null
    ): Boolean {
        if (text.isBlank() || engine == null) {
            return false
        }

        // 1. Format text for regional speech
        val formattedSpeech = SpeechTextFormatter.formatForSpeech(text, languageCode)
        if (formattedSpeech.isBlank()) return false

        // 2. Resolve and set regional language
        val targetLocale = SpeechTextFormatter.resolveLocale(languageCode)
        val langResult = engine.setLanguage(targetLocale)

        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            SafeLog.w(TAG, "Language $targetLocale not supported, falling back to Indian English")
            engine.setLanguage(Locale("en", "IN"))
        }

        // 3. Queue playback
        val utteranceId = UUID.randomUUID().toString()
        activeUtteranceId = utteranceId
        onCompleteCallback = onComplete

        val result = engine.speak(
            formattedSpeech,
            TextToSpeech.QUEUE_FLUSH,
            null,
            utteranceId
        )

        return result == TextToSpeech.SUCCESS
    }

    /**
     * Immediately stops ongoing playback.
     */
    fun stop() {
        try {
            engine?.stop()
        } catch (e: Exception) {
            SafeLog.w(TAG, "Error stopping TTS playback", e)
        } finally {
            _isPlaying.value = false
            onCompleteCallback = null
        }
    }

    /**
     * Releases speech synthesis resources.
     */
    fun shutdown() {
        stop()
        try {
            engine?.shutdown()
        } catch (e: Exception) {
            SafeLog.w(TAG, "Error shutting down TTS", e)
        }
    }
}

