package com.example.financeapp.ui.components

import android.provider.Settings
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import com.example.financeapp.ui.icons.Icons
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.financeapp.ui.theme.ActionGreen
import com.example.financeapp.ui.theme.CanvasWhite
import com.example.financeapp.ui.theme.ErrorPink
import com.example.financeapp.ui.theme.Ink
import com.example.financeapp.ui.theme.Spacing
import com.example.financeapp.ui.theme.SurfaceNeutral
import com.example.financeapp.ui.voice.SupportedVoiceLanguages
import com.example.financeapp.ui.voice.VoiceInputState
import com.example.financeapp.ui.voice.VoiceLanguage

@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    language: VoiceLanguage,
    voiceState: VoiceInputState,
    onLanguageSelected: (VoiceLanguage) -> Unit,
    onMic: () -> Unit,
    onStop: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    voiceEnabled: Boolean = true,
    languageMenuExpanded: Boolean = false
) {
    var menuOpen by remember(languageMenuExpanded) { mutableStateOf(languageMenuExpanded) }
    val listening = voiceState is VoiceInputState.Listening
    val animationScale = Settings.Global.getFloat(LocalContext.current.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    val pulse by rememberInfiniteTransition(label = "voice pulse").animateFloat(
        initialValue = 1f,
        targetValue = if (listening && animationScale != 0f) .68f else 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "voice pulse alpha"
    )
    Surface(modifier.fillMaxWidth(), color = CanvasWhite, border = BorderStroke(1.dp, Ink)) {
        Column(Modifier.padding(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ask about your money") },
                minLines = 1,
                maxLines = 4
            )
            VoiceStatus(voiceState)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Column {
                    TextButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier.heightIn(min = Spacing.minimumTouchTarget).semantics {
                            contentDescription = "Voice language, ${language.nativeName}, ${language.code}"
                        }
                    ) {
                        Text("${language.nativeName} · ${language.code}")
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        SupportedVoiceLanguages.forEach { option ->
                            DropdownMenuItem(
                                text = { Text("${option.nativeName} · ${option.code}") },
                                onClick = { onLanguageSelected(option); menuOpen = false }
                            )
                        }
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = if (listening) ActionGreen else SurfaceNeutral,
                    contentColor = if (listening) CanvasWhite else Ink,
                    modifier = Modifier.size(Spacing.minimumTouchTarget).alpha(if (listening) pulse else 1f)
                ) {
                    IconButton(
                        onClick = if (listening) onStop else onMic,
                        enabled = voiceEnabled,
                        modifier = Modifier.semantics {
                            contentDescription = if (listening) "Stop voice input" else "Start voice input"
                        }
                    ) {
                        Icon(if (listening) Icons.Default.Stop else Icons.Default.Mic, contentDescription = null)
                    }
                }
                IconButton(
                    onClick = onSend,
                    enabled = text.isNotBlank(),
                    modifier = Modifier.size(Spacing.minimumTouchTarget).semantics { contentDescription = "Send question" }
                ) { Icon(Icons.Default.ArrowUpward, contentDescription = null) }
            }
        }
    }
}

@Composable
private fun VoiceStatus(state: VoiceInputState) {
    when (state) {
        VoiceInputState.Idle, is VoiceInputState.Complete -> Unit
        VoiceInputState.PermissionRequired -> Text("Microphone permission is needed for voice input.", style = MaterialTheme.typography.bodySmall)
        is VoiceInputState.Listening -> Text(
            if (state.partialText.isBlank()) "Listening…" else "Listening… ${state.partialText}",
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodySmall
        )
        is VoiceInputState.Error -> Surface(color = ErrorPink) {
            Text(
                "${state.reason.label} ${state.nextStep}",
                Modifier.padding(Spacing.xs).semantics { liveRegion = LiveRegionMode.Assertive },
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private val com.example.financeapp.ui.voice.VoiceError.label: String
    get() = when (this) {
        com.example.financeapp.ui.voice.VoiceError.NO_SPEECH -> "No speech heard."
        com.example.financeapp.ui.voice.VoiceError.NO_NETWORK -> "Voice input has no network."
        com.example.financeapp.ui.voice.VoiceError.LANGUAGE_NOT_INSTALLED -> "This language is not installed."
    }
