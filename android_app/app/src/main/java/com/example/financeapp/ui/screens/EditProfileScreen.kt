package com.example.financeapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.example.financeapp.data.*
import com.example.financeapp.ui.components.*
import com.example.financeapp.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(onNavigateBack: () -> Unit, showHeader: Boolean = true) {
    val owner = remember { SupabaseClient.auth.currentUserOrNull()?.id }
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var income by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    val fieldColors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ink, focusedLabelColor = Ink, cursorColor = Ink)
    LaunchedEffect(retry) {
        loading = true
        when (val result = UserRepository.getProfile()) {
            is Resource.Success -> {
                val profile = result.data
                if (profile == null) message = "Your profile is not available yet. Please retry."
                else {
                    name = profile.fullName
                    phone = profile.phoneNumber.orEmpty()
                    income = profile.monthlyIncome?.toString().orEmpty()
                    email = profile.email
                    loaded = true
                    message = null
                }
            }
            is Resource.Error -> message = result.message
            Resource.Loading -> Unit
        }
        loading = false
    }
    Scaffold(containerColor = CanvasWhite,
        contentWindowInsets = if (showHeader) ScaffoldDefaults.contentWindowInsets else WindowInsets(0, 0, 0, 0), topBar = {
        if (showHeader) TopAppBar(title = { Text("Edit profile") }, navigationIcon = {
            IconButton(onClick = onNavigateBack, enabled = !busy) { Icon(Icons.Outlined.ArrowBack, "Back") }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState())
            .padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text("Your account", style = MaterialTheme.typography.headlineSmall, color = Ink)
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            message?.let { Text(it, color = ErrorInk) }
            if (!loaded && !loading) TextButton(onClick = { retry++ }) { Text("Retry", color = Ink) }
            if (loaded) {
                Text(email, style = MaterialTheme.typography.bodyLarge, color = MutedInk)
                OutlinedTextField(name, { if (it.length <= 100) name = it }, label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(), enabled = !busy, singleLine = true, colors = fieldColors)
                OutlinedTextField(phone, { if (it.length <= 16) phone = it }, label = { Text("Phone (optional)") },
                    modifier = Modifier.fillMaxWidth(), enabled = !busy, singleLine = true, colors = fieldColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                OutlinedTextField(income, { if (it.length <= 12) income = it }, label = { Text("Monthly income in INR (optional)") },
                    modifier = Modifier.fillMaxWidth(), enabled = !busy, singleLine = true, colors = fieldColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                Text("These details are saved to your Supabase profile. Leave optional fields empty to remove them. SMS transactions stay on this phone.",
                    style = MaterialTheme.typography.bodyMedium, color = MutedInk)
                RupAiButton("Save changes", onClick = {
                    if (owner == null || SupabaseClient.auth.currentUserOrNull()?.id != owner) {
                        message = "Your account changed. Reopen your profile before editing."
                    } else if (income.isNotBlank() && income.toDoubleOrNull() == null) {
                        message = "Enter a valid income or leave it empty."
                    } else {
                        val update = UserProfileUpdate(name.trim(), phone.trim().takeIf { it.isNotBlank() }, income.toDoubleOrNull())
                        val validation = update.validationError()
                        if (validation != null) message = validation
                        else {
                            busy = true
                            scope.launch {
                                try {
                                    when (val result = UserRepository.updateProfile(update)) {
                                        is Resource.Success -> onNavigateBack()
                                        is Resource.Error -> message = result.message
                                        Resource.Loading -> Unit
                                    }
                                } catch (e: CancellationException) { throw e }
                                catch (e: Exception) { message = "Could not save your profile. Please retry." }
                                finally { busy = false }
                            }
                        }
                    }
                }, state = if (busy) RupAiButtonState.LOADING else RupAiButtonState.DEFAULT)
                TextButton(onClick = onNavigateBack, enabled = !busy) { Text("Cancel", color = Ink) }
            }
        }
    }
}
