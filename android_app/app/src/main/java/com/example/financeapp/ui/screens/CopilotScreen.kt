package com.example.financeapp.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.financeapp.ui.components.ChatInputBar
import com.example.financeapp.ui.components.RupAiButton
import com.example.financeapp.ui.components.SuggestionChips
import com.example.financeapp.ui.components.ThreeSectionAnswerCard
import com.example.financeapp.ui.theme.CanvasWhite
import com.example.financeapp.ui.theme.ErrorPink
import com.example.financeapp.ui.theme.FinanceAppTheme
import com.example.financeapp.ui.theme.Ink
import com.example.financeapp.ui.theme.Spacing
import com.example.financeapp.ui.theme.SurfaceNeutral
import com.example.financeapp.ui.viewmodel.CopilotContentState
import com.example.financeapp.ui.viewmodel.CopilotFixtures
import com.example.financeapp.ui.viewmodel.CopilotMessageUi
import com.example.financeapp.ui.viewmodel.CopilotUiState
import com.example.financeapp.ui.viewmodel.CopilotViewModel
import com.example.financeapp.ui.voice.AndroidVoiceInputController
import com.example.financeapp.ui.voice.SupportedVoiceLanguages
import com.example.financeapp.ui.voice.VoiceError
import com.example.financeapp.ui.voice.VoiceInputState
import com.example.financeapp.ui.voice.VoiceLanguage

const val COPILOT_PREFS = "copilot_preferences"
private const val VOICE_LANGUAGE = "voice_language"
const val VOICE_INPUT_ENABLED = "voice_input_enabled"

@Composable
fun CopilotRoute(
    viewModel: CopilotViewModel = viewModel(),
    onLaunchWhatIf: () -> Unit = {},
    onKeyboardVisibilityChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences(COPILOT_PREFS, Context.MODE_PRIVATE) }
    val controller = remember { AndroidVoiceInputController(context.applicationContext) }
    val state by viewModel.uiState.collectAsState()
    val voiceState by controller.state.collectAsState()
    var language by remember {
        mutableStateOf(SupportedVoiceLanguages.firstOrNull { it.tag == preferences.getString(VOICE_LANGUAGE, "en-IN") }
            ?: SupportedVoiceLanguages.first())
    }
    var disclosureSeen by rememberSaveable { mutableStateOf(preferences.getBoolean("voice_disclosure_seen", false)) }
    var showDisclosure by rememberSaveable { mutableStateOf(false) }
    val voiceEnabled = preferences.getBoolean(VOICE_INPUT_ENABLED, true)
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) controller.start(language.tag)
    }

    LaunchedEffect(voiceState) {
        when (val current = voiceState) {
            is VoiceInputState.Listening -> if (current.partialText.isNotBlank()) viewModel.updateDraft(current.partialText)
            is VoiceInputState.Complete -> viewModel.updateDraft(current.transcript)
            else -> Unit
        }
    }
    DisposableEffect(controller) { onDispose { controller.release() } }
    CopilotScreen(
        state = state,
        voiceState = voiceState,
        language = language,
        voiceEnabled = voiceEnabled,
        showVoiceDisclosure = showDisclosure,
        onDismissDisclosure = {
            showDisclosure = false
            disclosureSeen = true
            preferences.edit().putBoolean("voice_disclosure_seen", true).apply()
        },
        onDraftChanged = viewModel::updateDraft,
        onSuggestion = viewModel::submit,
        onSend = viewModel::submit,
        onRetry = viewModel::retry,
        onLanguageSelected = {
            language = it
            preferences.edit().putString(VOICE_LANGUAGE, it.tag).apply()
        },
        onMic = {
            if (!disclosureSeen) showDisclosure = true
            controller.start(language.tag)
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        onStop = controller::stop,
        onLaunchWhatIf = onLaunchWhatIf,
        onKeyboardVisibilityChanged = onKeyboardVisibilityChanged
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CopilotScreen(
    state: CopilotUiState,
    voiceState: VoiceInputState = VoiceInputState.Idle,
    language: VoiceLanguage = SupportedVoiceLanguages.first(),
    voiceEnabled: Boolean = true,
    showVoiceDisclosure: Boolean = false,
    onDismissDisclosure: () -> Unit = {},
    onDraftChanged: (String) -> Unit = {},
    onSuggestion: (String) -> Unit = {},
    onSend: (String) -> Unit = {},
    onRetry: () -> Unit = {},
    onLanguageSelected: (VoiceLanguage) -> Unit = {},
    onMic: () -> Unit = {},
    onStop: () -> Unit = {},
    onLaunchWhatIf: () -> Unit = {},
    onAddToPlan: () -> Unit = {},
    onKeyboardVisibilityChanged: (Boolean) -> Unit = {}
) {
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    LaunchedEffect(keyboardVisible) { onKeyboardVisibilityChanged(keyboardVisible) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Copilot") }) },
        bottomBar = {
            Column(Modifier.imePadding()) {
                if (showVoiceDisclosure) VoiceDisclosure(onDismissDisclosure)
                ChatInputBar(
                    state.draft,
                    onDraftChanged,
                    language,
                    voiceState,
                    onLanguageSelected,
                    onMic,
                    onStop,
                    { onSend(state.draft) },
                    voiceEnabled = voiceEnabled
                )
            }
        }
    ) { inner ->
        LazyColumn(
            Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            item { SuggestionChips(CopilotFixtures.suggestions, onSuggestion) }
            when (state.contentState) {
                CopilotContentState.EMPTY -> item { EmptyCopilot(onSuggestion) }
                CopilotContentState.THINKING -> {
                    items(state.messages) { Message(it, onLaunchWhatIf, onAddToPlan) }
                    item { ThinkingCard() }
                }
                CopilotContentState.ERROR -> {
                    items(state.messages) { Message(it, onLaunchWhatIf, onAddToPlan) }
                    item { ErrorCard(onRetry) }
                }
                CopilotContentState.ANSWER, CopilotContentState.OFFLINE -> {
                    if (state.contentState == CopilotContentState.OFFLINE) item { OfflineNotice() }
                    items(state.messages) { Message(it, onLaunchWhatIf, onAddToPlan) }
                }
            }
        }
    }
}

@Composable
private fun Message(message: CopilotMessageUi, onSimulate: () -> Unit, onAddToPlan: () -> Unit) {
    if (message.fromUser) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Surface(color = SurfaceNeutral, shape = MaterialTheme.shapes.large, border = BorderStroke(1.dp, Ink)) {
                Text(message.text, Modifier.padding(Spacing.sm), style = MaterialTheme.typography.bodyLarge)
            }
        }
    } else message.answer?.let { ThreeSectionAnswerCard(it, onSimulate, onAddToPlan) }
}

@Composable
private fun EmptyCopilot(onSuggestion: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text("Ask what a decision could do to your cash, debt or upcoming commitments.", style = MaterialTheme.typography.bodyLarge)
        TextButton(onClick = { onSuggestion(CopilotFixtures.starter) }) { Text(CopilotFixtures.starter) }
    }
}

@Composable
private fun ThinkingCard() {
    Surface(Modifier.fillMaxWidth(), color = SurfaceNeutral, shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(Spacing.md).semantics { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text("Calculating...", style = MaterialTheme.typography.titleMedium)
            repeat(3) { Surface(Modifier.fillMaxWidth().height(18.dp), color = CanvasWhite) {} }
        }
    }
}

@Composable
private fun OfflineNotice() {
    Surface(Modifier.fillMaxWidth(), color = SurfaceNeutral, border = BorderStroke(1.dp, Ink)) {
        Text("Gemini is unavailable. Showing on-device advice.", Modifier.padding(Spacing.sm), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ErrorCard(onRetry: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = ErrorPink) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text("The answer could not be calculated.", style = MaterialTheme.typography.bodyLarge)
            RupAiButton("Retry", onRetry)
        }
    }
}

@Composable
private fun VoiceDisclosure(onDismiss: () -> Unit) {
    Surface(color = SurfaceNeutral, border = BorderStroke(1.dp, Ink)) {
        Column(Modifier.fillMaxWidth().padding(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text("Your phone’s speech service turns voice into text. RupAI doesn’t store audio. It may send audio to Google.", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onDismiss) { Text("Got it") }
        }
    }
}

private class CopilotPreviewProvider : CollectionPreviewParameterProvider<CopilotUiState>(CopilotFixtures.all)
private class VoicePreviewProvider : CollectionPreviewParameterProvider<VoiceInputState>(
    listOf(
        VoiceInputState.Idle,
        VoiceInputState.PermissionRequired,
        VoiceInputState.Listening("Can I afford a trip"),
        VoiceInputState.Error(VoiceError.NO_SPEECH, "Move somewhere quieter and try again."),
        VoiceInputState.Error(VoiceError.NO_NETWORK, "Check your connection and try again."),
        VoiceInputState.Error(VoiceError.LANGUAGE_NOT_INSTALLED, "Install the selected language, then try again.")
    )
)

@Preview(name = "Copilot states · 360 · 200%", widthDp = 360, heightDp = 800, fontScale = 2f, showBackground = true)
@Preview(name = "Copilot states · 412 · 200%", widthDp = 412, heightDp = 900, fontScale = 2f, showBackground = true)
@Composable
private fun CopilotStatePreview(@PreviewParameter(CopilotPreviewProvider::class) state: CopilotUiState) {
    FinanceAppTheme { CopilotScreen(state) }
}

@Preview(name = "Voice states · 360 · 200%", widthDp = 360, heightDp = 600, fontScale = 2f, showBackground = true)
@Preview(name = "Voice states · 412 · 200%", widthDp = 412, heightDp = 600, fontScale = 2f, showBackground = true)
@Composable
private fun VoiceStatePreview(@PreviewParameter(VoicePreviewProvider::class) voiceState: VoiceInputState) {
    FinanceAppTheme { ChatInputBar("Editable transcript", {}, SupportedVoiceLanguages[1], voiceState, {}, {}, {}, {}) }
}

@Preview(name = "Language dropdown · 360 · 200%", widthDp = 360, heightDp = 600, fontScale = 2f, showBackground = true)
@Preview(name = "Language dropdown · 412 · 200%", widthDp = 412, heightDp = 600, fontScale = 2f, showBackground = true)
@Composable
private fun LanguageDropdownPreview() {
    FinanceAppTheme {
        ChatInputBar("", {}, SupportedVoiceLanguages[1], VoiceInputState.Idle, {}, {}, {}, {}, languageMenuExpanded = true)
    }
}
