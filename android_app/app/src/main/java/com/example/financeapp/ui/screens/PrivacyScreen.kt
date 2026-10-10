package com.example.financeapp.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.financeapp.ui.theme.FinanceAppTheme
import com.example.financeapp.ui.theme.Spacing

@Composable
fun PrivacyRoute() {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences(COPILOT_PREFS, Context.MODE_PRIVATE) }
    var enabled by remember { mutableStateOf(preferences.getBoolean(VOICE_INPUT_ENABLED, true)) }
    PrivacyScreen(enabled) {
        enabled = it
        preferences.edit().putBoolean(VOICE_INPUT_ENABLED, it).apply()
    }
}

@Composable
fun PrivacyScreen(voiceInputEnabled: Boolean, onVoiceInputChanged: (Boolean) -> Unit) {
    Column(Modifier.fillMaxSize().padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        Text("Privacy", style = MaterialTheme.typography.headlineMedium)
        Text("Your raw messages never leave this phone.", style = MaterialTheme.typography.bodyLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Voice input", style = MaterialTheme.typography.titleMedium)
                Text("Allow the phone’s speech service to turn audio into editable text.", style = MaterialTheme.typography.bodyMedium)
            }
            Switch(checked = voiceInputEnabled, onCheckedChange = onVoiceInputChanged)
        }
    }
}

@Preview(widthDp = 360, fontScale = 2f, showBackground = true)
@Composable
private fun PrivacyPreview() { FinanceAppTheme { PrivacyScreen(true, {}) } }
