package com.example.financeapp.ui.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface VoiceInputState {
    data object Idle : VoiceInputState
    data object PermissionRequired : VoiceInputState
    data class Listening(val partialText: String = "") : VoiceInputState
    data class Complete(val transcript: String) : VoiceInputState
    data class Error(val reason: VoiceError, val nextStep: String) : VoiceInputState
}

enum class VoiceError { NO_SPEECH, NO_NETWORK, LANGUAGE_NOT_INSTALLED }

data class VoiceLanguage(val tag: String, val nativeName: String, val code: String)

val SupportedVoiceLanguages = listOf(
    VoiceLanguage("en-IN", "English", "EN"),
    VoiceLanguage("hi-IN", "हिन्दी", "HI"),
    VoiceLanguage("mr-IN", "मराठी", "MR"),
    VoiceLanguage("gu-IN", "ગુજરાતી", "GU"),
    VoiceLanguage("ta-IN", "தமிழ்", "TA"),
    VoiceLanguage("te-IN", "తెలుగు", "TE"),
    VoiceLanguage("kn-IN", "ಕನ್ನಡ", "KN"),
    VoiceLanguage("bn-IN", "বাংলা", "BN"),
    VoiceLanguage("ml-IN", "മലയാളം", "ML"),
    VoiceLanguage("pa-IN", "ਪੰਜਾਬੀ", "PA")
)

interface VoiceInputController {
    val state: StateFlow<VoiceInputState>
    fun start(languageTag: String)
    fun stop()
    fun release() = Unit
}

class AndroidVoiceInputController(private val context: Context) : VoiceInputController, RecognitionListener {
    private val mutableState = MutableStateFlow<VoiceInputState>(VoiceInputState.Idle)
    override val state: StateFlow<VoiceInputState> = mutableState.asStateFlow()
    private var recognizer: SpeechRecognizer? = null
    private var latestTranscript = ""

    override fun start(languageTag: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            mutableState.value = VoiceInputState.PermissionRequired
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            mutableState.value = VoiceInputState.Error(VoiceError.LANGUAGE_NOT_INSTALLED, "Install or enable a speech service, then try again.")
            return
        }
        latestTranscript = ""
        val speechRecognizer = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also {
            recognizer = it
            it.setRecognitionListener(this)
        }
        mutableState.value = VoiceInputState.Listening()
        speechRecognizer.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        })
    }

    override fun stop() {
        recognizer?.stopListening()
        mutableState.value = if (latestTranscript.isBlank()) VoiceInputState.Idle else VoiceInputState.Complete(latestTranscript)
    }

    override fun release() {
        recognizer?.destroy()
        recognizer = null
    }

    override fun onPartialResults(partialResults: Bundle?) {
        latestTranscript = partialResults.firstTranscript()
        mutableState.value = VoiceInputState.Listening(latestTranscript)
    }

    override fun onResults(results: Bundle?) {
        latestTranscript = results.firstTranscript()
        mutableState.value = if (latestTranscript.isBlank()) {
            VoiceInputState.Error(VoiceError.NO_SPEECH, "Move somewhere quieter and try again.")
        } else VoiceInputState.Complete(latestTranscript)
    }

    override fun onError(error: Int) {
        mutableState.value = when (error) {
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                VoiceInputState.Error(VoiceError.NO_NETWORK, "Check your connection and try again.")
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
                VoiceInputState.Error(VoiceError.LANGUAGE_NOT_INSTALLED, "Install the selected language, then try again.")
            else -> VoiceInputState.Error(VoiceError.NO_SPEECH, "Move somewhere quieter and try again.")
        }
    }

    override fun onReadyForSpeech(params: Bundle?) = Unit
    override fun onBeginningOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() = Unit
    override fun onEvent(eventType: Int, params: Bundle?) = Unit

    private fun Bundle?.firstTranscript(): String =
        this?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
}

class FakeVoiceInputController : VoiceInputController {
    private val mutableState = MutableStateFlow<VoiceInputState>(VoiceInputState.Idle)
    override val state: StateFlow<VoiceInputState> = mutableState.asStateFlow()

    override fun start(languageTag: String) {
        mutableState.value = VoiceInputState.Listening()
    }

    override fun stop() {
        val text = (mutableState.value as? VoiceInputState.Listening)?.partialText.orEmpty()
        mutableState.value = if (text.isBlank()) VoiceInputState.Idle else VoiceInputState.Complete(text)
    }

    fun requirePermission() { mutableState.value = VoiceInputState.PermissionRequired }
    fun emitPartial(text: String) { mutableState.value = VoiceInputState.Listening(text) }
    fun emitError(reason: VoiceError, nextStep: String) { mutableState.value = VoiceInputState.Error(reason, nextStep) }
}
