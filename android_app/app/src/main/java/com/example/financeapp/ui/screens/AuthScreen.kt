package com.example.financeapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.example.financeapp.ui.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.financeapp.R
import com.example.financeapp.data.*
import com.example.financeapp.ui.components.*
import com.example.financeapp.ui.theme.*
import io.github.jan.supabase.gotrue.SessionStatus
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onNavigateToHome: () -> Unit, onNavigateToSignUp: () -> Unit, onBack: () -> Unit) =
    AuthRoute(AuthAction.SIGN_IN, onNavigateToHome, onNavigateToSignUp, onBack)

@Composable
fun SignUpScreen(onNavigateToHome: () -> Unit, onNavigateBackToLogin: () -> Unit, onBack: () -> Unit) =
    AuthRoute(AuthAction.SIGN_UP, onNavigateToHome, onNavigateBackToLogin, onBack)

@Composable
private fun AuthRoute(initialAction: AuthAction, onAuthenticated: () -> Unit, onSwitch: () -> Unit, onBack: () -> Unit) {
    val session by SupabaseClient.auth.sessionStatus.collectAsState()
    val flow = remember { AuthFlow() }
    val state by flow.state.collectAsState()
    val scope = rememberCoroutineScope()
    var action by remember { mutableStateOf(initialAction) }
    LaunchedEffect(state.authenticated) { if (state.authenticated) onAuthenticated() }
    val restoring = session is SessionStatus.LoadingFromStorage
    BackHandler {
        if (!state.busy) {
            if (action == AuthAction.RESET_PASSWORD) { flow.clearFeedback(); action = initialAction } else onBack()
        }
    }
    AuthForm(action, state, restoring, onSubmit = { input ->
        scope.launch {
            flow.submit(action, input) { requested, credentials ->
                when (requested) {
                    AuthAction.SIGN_IN -> UserRepository.signIn(credentials.email, credentials.password)
                    AuthAction.SIGN_UP -> UserRepository.signUp(credentials.email, credentials.password, credentials.name)
                    AuthAction.RESET_PASSWORD -> UserRepository.resetPassword(credentials.email)
                }
                UserRepository.isLoggedIn()
            }
        }
    }, onBack = { if (action == AuthAction.RESET_PASSWORD) { flow.clearFeedback(); action = initialAction } else onBack() },
        onSwitch = onSwitch, onReset = { flow.clearFeedback(); action = AuthAction.RESET_PASSWORD })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthForm(action: AuthAction, state: AuthState, restoring: Boolean = false,
    onSubmit: (AuthInput) -> Unit, onBack: () -> Unit, onSwitch: () -> Unit, onReset: () -> Unit) {
    // Do not use rememberSaveable for credentials: no passwords in saved instance state.
    var email by remember { mutableStateOf("") }
    var password by remember(action) { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var visible by remember(action) { mutableStateOf(false) }
    val enabled = !state.busy && !restoring && !state.authenticated
    val fieldColors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ink, focusedLabelColor = Ink, cursorColor = Ink)
    val title = when (action) {
        AuthAction.SIGN_IN -> "Welcome back"
        AuthAction.SIGN_UP -> "Create your account"
        AuthAction.RESET_PASSWORD -> "Reset your password"
    }
    fun submit() { if (enabled) onSubmit(AuthInput(email, password, name)) }
    Scaffold(containerColor = CanvasWhite, topBar = {
        TopAppBar(title = { Text("RupAI", style = MaterialTheme.typography.titleLarge) },
            navigationIcon = { IconButton(onClick = onBack, enabled = enabled) { Icon(Icons.Outlined.ArrowBack, "Back") } })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Image(painterResource(R.drawable.privacy_control), null, contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().height(112.dp))
            Text(title, style = MaterialTheme.typography.headlineMedium, color = Ink)
            Text(when (action) {
                AuthAction.SIGN_IN -> "Sign in to your profile and saved setup."
                AuthAction.SIGN_UP -> "One account for Demo and My Transactions."
                AuthAction.RESET_PASSWORD -> "Request a reset link for your account email."
            }, style = MaterialTheme.typography.bodyLarge, color = MutedInk)
            if (action == AuthAction.SIGN_UP) OutlinedTextField(name, { if (it.length <= 100) name = it },
                label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), enabled = enabled,
                singleLine = true, colors = fieldColors, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
            OutlinedTextField(email, { if (it.length <= 254) email = it }, label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(), enabled = enabled, singleLine = true, colors = fieldColors,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email,
                    imeAction = if (action == AuthAction.RESET_PASSWORD) ImeAction.Done else ImeAction.Next),
                keyboardActions = KeyboardActions(onDone = { submit() }))
            if (action != AuthAction.RESET_PASSWORD) {
                OutlinedTextField(password, { if (it.length <= 128) password = it }, label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(), enabled = enabled, singleLine = true, colors = fieldColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrect = false, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { IconButton(onClick = { visible = !visible }, enabled = enabled) {
                        Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            if (visible) "Hide password" else "Show password")
                    } }, supportingText = if (action == AuthAction.SIGN_UP) ({ Text("At least 8 characters. Use a unique password.") }) else null)
            }
            if (action == AuthAction.SIGN_IN) TextButton(onClick = onReset, enabled = enabled,
                modifier = Modifier.align(Alignment.End)) { Text("Forgot password?", color = Ink) }
            state.message?.let { Text(it, color = if (state.error) ErrorInk else Ink,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            RupAiButton(when (action) {
                AuthAction.SIGN_IN -> "Sign in"
                AuthAction.SIGN_UP -> "Create account"
                AuthAction.RESET_PASSWORD -> "Send reset link"
            }, ::submit, state = if (state.busy || restoring || state.authenticated) RupAiButtonState.LOADING else RupAiButtonState.DEFAULT,
                loadingLabel = if (restoring) "Restoring session" else "Please wait")
            TextButton(onClick = onSwitch, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                Text(if (action == AuthAction.SIGN_UP) "Already registered? Sign in" else "Create an account", color = Ink)
            }
            Divider()
            Text("Account details are handled by Supabase. Signing in does not upload your SMS or enable remote categorization. Cloud financial setup requires a separate choice.",
                style = MaterialTheme.typography.bodySmall, color = MutedInk)
            TextButton(onClick = onBack, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text("Back to RupAI", color = Ink) }
        }
    }
}

@Preview(name = "Sign in", widthDp = 360, heightDp = 800, showBackground = true)
@Preview(name = "Sign in large text", widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable
private fun LoginPreview() = FinanceAppTheme(false) { AuthForm(AuthAction.SIGN_IN, AuthState(), onSubmit = {}, onBack = {}, onSwitch = {}, onReset = {}) }

@Preview(name = "Create account", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun SignUpPreview() = FinanceAppTheme(false) { AuthForm(AuthAction.SIGN_UP, AuthState(), onSubmit = {}, onBack = {}, onSwitch = {}, onReset = {}) }
