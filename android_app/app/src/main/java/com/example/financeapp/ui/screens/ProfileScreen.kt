package com.example.financeapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.financeapp.data.*
import com.example.financeapp.ui.util.formatInr
import io.github.jan.supabase.gotrue.SessionStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProfileScreen(onNavigateToHome: () -> Unit, onNavigateToTransactions: () -> Unit,
    onNavigateToLogin: () -> Unit, onNavigateToEditProfile: () -> Unit, onEditSetup: () -> Unit = {}, showHeader: Boolean = true) {
    val status by SupabaseClient.auth.sessionStatus.collectAsState()
    if (status is SessionStatus.LoadingFromStorage) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val owner = SupabaseClient.auth.currentUserOrNull()?.id
    key(owner) { SharedProfile(owner, onNavigateToHome, onNavigateToTransactions,
        onNavigateToLogin, onNavigateToEditProfile, onEditSetup, showHeader) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SharedProfile(owner: String?, onBack: () -> Unit, onActivity: () -> Unit,
    onLogin: () -> Unit, onEdit: () -> Unit, onSetup: () -> Unit, showHeader: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var setup by remember { mutableStateOf<AccountSetup?>(null) }
    var loading by remember { mutableStateOf(owner != null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var confirmation by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(owner, retry) {
        if (owner != null) {
            loading = true
            message = null
            when (val result = UserRepository.getProfile()) {
                is Resource.Success -> profile = result.data
                is Resource.Error -> message = result.message
                Resource.Loading -> Unit
            }
            when (val result = AccountSetupRepository.load()) {
                is Resource.Success -> setup = result.data
                is Resource.Error -> message = result.message
                Resource.Loading -> Unit
            }
            loading = false
        }
    }
    Scaffold(contentWindowInsets = if (showHeader) ScaffoldDefaults.contentWindowInsets else WindowInsets(0, 0, 0, 0),
        topBar = { if (showHeader) TopAppBar(title = { Text("Profile") }, navigationIcon = {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
    }, actions = {
        if (owner != null) IconButton(onClick = onEdit, enabled = !busy) { Icon(Icons.Default.Edit, "Edit profile") }
    }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Your account", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                if (!showHeader && owner != null) IconButton(onClick = onEdit, enabled = !busy) { Icon(Icons.Default.Edit, "Edit profile") }
            }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            message?.let {
                Text(it)
                TextButton(onClick = { retry++ }, enabled = !loading && !busy) { Text("Retry") }
            }
            if (owner == null) {
                Text("You are not signed in. Your SMS transactions stay on this phone. Sign in to save your profile and, if you choose, your financial setup.")
                Button(onClick = onLogin) { Text("Sign in") }
                OutlinedButton(onClick = onSetup) { Text("Review setup") }
            } else {
                ProfileField("Name", profile?.fullName ?: "Not available")
                ProfileField("Email", profile?.email ?: SupabaseClient.auth.currentUserOrNull()?.email.orEmpty())
                ProfileField("Phone", profile?.phoneNumber ?: "Not provided")
                ProfileField("Monthly income you entered", profile?.monthlyIncome?.let(::formatInr) ?: "Not set")
                Divider()
                Text("Financial setup", style = MaterialTheme.typography.titleMedium)
                ProfileField("Income pattern", setup?.incomePattern ?: "Not set")
                ProfileField("Regular payments", setup?.obligations?.joinToString()?.ifEmpty { "None entered" } ?: "Not set")
                ProfileField("Safety balance", setup?.minimumReserve?.toDouble()?.let(::formatInr) ?: "Not set")
                ProfileField("Goal", setup?.goalName ?: "Not set")
                setup?.goalAmount?.let { ProfileField("Goal amount", formatInr(it.toDouble())) }
                Text("Plans currently use sample data. The details saved here do not verify your bank balance or create a plan from your SMS.", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = onSetup, enabled = !loading && !busy) { Text("Edit financial setup") }
                if (setup != null) TextButton(onClick = { confirmation = "setup" }, enabled = !busy) { Text("Delete saved financial setup") }
                TextButton(onClick = {
                    busy = true
                    scope.launch {
                        try {
                            context.getSharedPreferences(RemoteCategorizationPolicy.PREFS, android.content.Context.MODE_PRIVATE)
                                .edit().putBoolean(RemoteCategorizationPolicy.ENABLED, false).apply()
                            androidx.work.WorkManager.getInstance(context).cancelUniqueWork("ai_categorization")
                            UserRepository.signOut()
                            onLogin()
                        }
                        catch (e: CancellationException) { throw e }
                        catch (e: Exception) { message = "Could not sign out. Please retry." }
                        finally { busy = false }
                    }
                }, enabled = !busy) { Text("Sign out") }
            }
            Divider()
            Text("Device data", style = MaterialTheme.typography.titleMedium)
            Text("Imported SMS transactions and category corrections are stored on this phone, not in your cloud profile.")
            TextButton(onClick = onActivity) { Text("Open Activity") }
            TextButton(onClick = { confirmation = "local" }, enabled = !busy) { Text("Reset local transactions and category cache") }
        }
    }
    confirmation?.let { action ->
        AlertDialog(onDismissRequest = { if (!busy) confirmation = null },
            title = { Text(if (action == "setup") "Delete saved setup?" else "Reset imported device data?") },
            text = { Text(if (action == "setup")
                "Removes your income pattern, payment types, reserve and goal from Supabase. Sign-in profile and phone transactions are kept. Provider backups follow their own retention period."
                else "Deletes imported transactions, manual category corrections and category caches. Original SMS and cloud profile are kept. Nothing is re-imported automatically.") },
            confirmButton = { TextButton(onClick = {
                busy = true
                scope.launch {
                    try {
                        if (action == "setup") when (val result = AccountSetupRepository.delete()) {
                            is Resource.Success -> { setup = null; message = "Saved financial setup deleted." }
                            is Resource.Error -> message = result.message
                            Resource.Loading -> Unit
                        } else {
                            withContext(Dispatchers.IO) {
                                AppDatabase.getInstance(context).transactionDao().deleteAll()
                                listOf("BackendCategorizerCache", "GeminiCategorizerCache", "PlacesCategorizerCache").forEach {
                                    context.getSharedPreferences(it, android.content.Context.MODE_PRIVATE).edit().clear().commit()
                                }
                            }
                            message = "Local data cleared. Use Sync in Activity to import again."
                        }
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) { message = "The operation could not be completed. Please retry." }
                    finally { busy = false; confirmation = null }
                }
            }, enabled = !busy) { Text(if (action == "setup") "Delete setup" else "Reset local data") } },
            dismissButton = { TextButton(onClick = { confirmation = null }, enabled = !busy) { Text("Cancel") } })
    }
}

@Composable
private fun ProfileField(label: String, value: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
