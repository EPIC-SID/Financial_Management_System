package com.example.financeapp.ui.screens.onboarding

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.financeapp.data.*
import com.example.financeapp.ui.viewmodel.*
import io.github.jan.supabase.gotrue.SessionStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun OnboardingRoute(onExistingAccount: () -> Unit, onCreateAccount: () -> Unit, onComplete: (OnboardingDataSource) -> Unit) {
    val status by SupabaseClient.auth.sessionStatus.collectAsState()
    if (status is SessionStatus.LoadingFromStorage) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val owner = SupabaseClient.auth.currentUserOrNull()?.id
    key(owner) { AccountOnboarding(owner, onExistingAccount, onCreateAccount, onComplete) }
}

@Composable
private fun AccountOnboarding(owner: String?, onExistingAccount: () -> Unit, onCreateAccount: () -> Unit, onComplete: (OnboardingDataSource) -> Unit) {
    val vm: OnboardingViewModel = viewModel(key = "onboarding-${owner ?: "guest"}")
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var consent by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(owner == null) }
    var retry by remember { mutableIntStateOf(0) }
    var loadError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(owner, retry) {
        if (owner != null) {
            vm.busy(true)
            when (val result = AccountSetupRepository.load()) {
                is Resource.Success -> { vm.load(result.data); loaded = true; loadError = null }
                is Resource.Error -> loadError = result.message
                Resource.Loading -> Unit
            }
            vm.busy(false)
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.next() else {
            val activity = context as? android.app.Activity
            vm.setPermissionState(if (activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_SMS))
                SmsPermissionState.PERMANENTLY_DENIED else SmsPermissionState.DENIED)
        }
    }
    BackHandler(enabled = state.busy || state.page != OnboardingPage.WELCOME) { if (!state.busy) vm.back() }
    fun finishSetup() = scope.launch {
        try { vm.finish(owner, consent && loaded, AccountSetupRepository::save) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { vm.error("We couldn’t save this yet. Try again.") }
    }
    OnboardingScreen(state, onPrimary = {
        when (state.page) {
            OnboardingPage.SMS_CONSENT -> {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) vm.next()
                else if (state.permissionState == SmsPermissionState.PERMANENTLY_DENIED) {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                } else permission.launch(Manifest.permission.READ_SMS)
            }
            OnboardingPage.SAFETY_NET -> finishSetup()
            OnboardingPage.BUILDING -> if (state.completeness != null) onComplete(state.selectedSource ?: OnboardingDataSource.SAMPLE)
            else -> vm.next()
        }
    }, onBack = vm::back, onExistingAccount = onExistingAccount,
        onCreateAccount = onCreateAccount, showAccountActions = owner == null,
        onSourceSelected = { vm.selectSource(it); vm.next() }, onAlternativeSelected = vm::chooseAlternative,
        onMoneyArrivalSelected = vm::setMoneyArrival, onObligationToggled = vm::toggleObligation,
        onCushionChanged = vm::setCushion,
        privacyContent = {
            Column(Modifier.padding(top = 16.dp)) {
                if (owner == null) {
                    Text("Create an account at the end if you want to save these answers.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(consent, { consent = it }, enabled = loaded && !state.busy)
                        Text("Save these setup answers", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Text("Messages and account numbers are never saved here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (loadError != null) {
                    Text(loadError!!, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { retry++ }, enabled = !state.busy) { Text("Retry account storage") }
                }
            }
        })
}
