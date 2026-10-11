package com.example.financeapp.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeout

enum class AuthAction { SIGN_IN, SIGN_UP, RESET_PASSWORD }

data class AuthInput(val email: String, val password: String = "", val name: String = "") {
    override fun toString(): String = "AuthInput(redacted)"

    fun validationError(action: AuthAction): String? = when {
        email.length > 254 || !Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+").matches(email.trim()) -> "Enter a valid email address."
        action != AuthAction.RESET_PASSWORD && (password.isBlank() || password.length > 128) -> "Enter a password of at most 128 characters."
        action == AuthAction.SIGN_UP && password.length < 8 -> "Use at least 8 characters for your password."
        action == AuthAction.SIGN_UP -> UserProfileUpdate(fullName = name.trim()).validationError()
        else -> null
    }
}

data class AuthState(val busy: Boolean = false, val authenticated: Boolean = false,
    val message: String? = null, val error: Boolean = false)

/** Credentials are passed to the operation, never retained in observable state. */
class AuthFlow(private val now: () -> Long = System::currentTimeMillis, private val timeoutMillis: Long = 15_000) {
    private val mutableState = MutableStateFlow(AuthState())
    val state = mutableState.asStateFlow()
    private val retryAt = mutableMapOf<AuthAction, Long>()
    private var failures = 0

    fun clearFeedback() {
        if (!state.value.busy && !state.value.authenticated) mutableState.value = AuthState()
    }

    suspend fun submit(action: AuthAction, input: AuthInput, operation: suspend (AuthAction, AuthInput) -> Boolean) {
        if (state.value.busy || state.value.authenticated) return
        input.validationError(action)?.let { mutableState.value = AuthState(message = it, error = true); return }
        if (now() < (retryAt[action] ?: 0)) {
            mutableState.value = AuthState(message = "Please wait before trying again.", error = true)
            return
        }
        mutableState.value = AuthState(busy = true)
        // These are UX cooldowns. Supabase must enforce actual abuse protection.
        retryAt[action] = now() + when (action) {
            AuthAction.RESET_PASSWORD -> 60_000
            AuthAction.SIGN_UP -> 5_000
            AuthAction.SIGN_IN -> 0
        }
        try {
            val authenticated = withTimeout(timeoutMillis) { operation(action, input.copy(email = input.email.trim(), name = input.name.trim())) }
            if (action == AuthAction.SIGN_IN && !authenticated) error("Missing authenticated session")
            failures = 0
            mutableState.value = AuthState(authenticated = authenticated && action != AuthAction.RESET_PASSWORD,
                message = when (action) {
                    AuthAction.SIGN_UP -> if (authenticated) null else "Check your email to confirm your account, then sign in. If you already have an account, sign in instead."
                    AuthAction.RESET_PASSWORD -> "If an account exists for this email, a reset link will be sent."
                    AuthAction.SIGN_IN -> null
                })
        } catch (e: TimeoutCancellationException) {
            mutableState.value = AuthState(message = "Request timed out. Check your connection and try again.", error = true)
        } catch (e: CancellationException) {
            mutableState.value = AuthState()
            throw e
        } catch (e: Exception) {
            if (action == AuthAction.SIGN_IN && ++failures >= 3) {
                retryAt[action] = now() + 30_000
                failures = 0
            }
            mutableState.value = AuthState(message = when (action) {
                AuthAction.SIGN_IN -> "Could not sign in. Check your credentials, email confirmation and connection."
                AuthAction.SIGN_UP -> "Could not create an account. Check your details and connection, or try signing in."
                AuthAction.RESET_PASSWORD -> "Could not request a reset. Please try again later."
            }, error = true)
        }
    }
}
