package com.example.financeapp.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AuthFlowTest {
    private val input = AuthInput(" person@example.com ", "strong password", " Person ")
    @Test fun credentialsAreRedactedFromDiagnosticStrings() {
        assertEquals("AuthInput(redacted)", input.toString())
    }
    @Test fun invalidInputsNeverReachSupabase() = runBlocking {
        for (bad in listOf(input.copy(email = "wrong"), input.copy(password = "short"), input.copy(name = ""), input.copy(name = "bad\nname"))) {
            val flow = AuthFlow()
            flow.submit(AuthAction.SIGN_UP, bad) { _, _ -> error("Must not call auth") }
            assertTrue(flow.state.value.error)
        }
    }
    @Test fun loginDoesNotRejectExistingShortPasswords() {
        assertNull(input.copy(password = "old123").validationError(AuthAction.SIGN_IN))
        assertNotNull(input.copy(password = "x".repeat(129)).validationError(AuthAction.SIGN_IN))
    }
    @Test fun confirmedLoginNormalizesOnlyNameAndEmail() = runBlocking {
        val flow = AuthFlow()
        flow.submit(AuthAction.SIGN_IN, input.copy(password = " secret ")) { _, received ->
            assertEquals("person@example.com", received.email)
            assertEquals(" secret ", received.password)
            true
        }
        assertTrue(flow.state.value.authenticated)
        assertFalse(flow.state.value.busy)
    }
    @Test fun unconfirmedSignupDoesNotNavigateAsAuthenticated() = runBlocking {
        val flow = AuthFlow()
        flow.submit(AuthAction.SIGN_UP, input) { _, _ -> false }
        assertFalse(flow.state.value.authenticated)
        assertTrue(flow.state.value.message!!.contains("confirm"))
    }
    @Test fun resetResponseDoesNotRevealAccountExistence() = runBlocking {
        val flow = AuthFlow()
        flow.submit(AuthAction.RESET_PASSWORD, input.copy(password = "")) { _, _ -> true }
        assertFalse(flow.state.value.authenticated)
        assertTrue(flow.state.value.message!!.startsWith("If an account exists"))
    }
    @Test fun errorsNeverExposeProviderMessagesOrCredentials() = runBlocking {
        val flow = AuthFlow()
        flow.submit(AuthAction.SIGN_IN, input) { _, _ -> error("secret token and database URL") }
        assertTrue(flow.state.value.error)
        assertFalse(flow.state.value.toString().contains("secret token"))
        assertFalse(flow.state.value.toString().contains(input.password))
    }
    @Test fun cancellationPropagatesAndReleasesBusyState() = runBlocking {
        val flow = AuthFlow()
        try {
            flow.submit(AuthAction.SIGN_IN, input) { _, _ -> throw CancellationException() }
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { assertFalse(flow.state.value.busy) }
    }
    @Test fun timeoutIsRecoverable() = runBlocking {
        val flow = AuthFlow(timeoutMillis = 5)
        flow.submit(AuthAction.SIGN_IN, input) { _, _ -> delay(100); true }
        assertTrue(flow.state.value.message!!.contains("timed out"))
        assertFalse(flow.state.value.busy)
    }
    @Test fun concurrentSubmissionCannotCreateDuplicateRequests() = runBlocking {
        val flow = AuthFlow()
        flow.submit(AuthAction.SIGN_IN, input) { _, _ ->
            flow.submit(AuthAction.SIGN_IN, input) { _, _ -> error("Duplicate") }
            true
        }
        assertTrue(flow.state.value.authenticated)
    }
    @Test fun failedLoginCooldownExpires() = runBlocking {
        var clock = 1L
        val flow = AuthFlow(now = { clock })
        repeat(3) { flow.submit(AuthAction.SIGN_IN, input) { _, _ -> error("Rejected") } }
        flow.submit(AuthAction.SIGN_IN, input) { _, _ -> error("Cooldown must block") }
        assertEquals("Please wait before trying again.", flow.state.value.message)
        clock += 30_001
        flow.submit(AuthAction.SIGN_IN, input) { _, _ -> true }
        assertTrue(flow.state.value.authenticated)
    }
    @Test fun signupAndResetHaveSeparateCooldowns() = runBlocking {
        val flow = AuthFlow()
        flow.submit(AuthAction.SIGN_UP, input) { _, _ -> false }
        flow.submit(AuthAction.RESET_PASSWORD, input) { _, _ -> false }
        assertFalse(flow.state.value.error)
        flow.submit(AuthAction.RESET_PASSWORD, input) { _, _ -> error("Cooldown must block") }
        assertTrue(flow.state.value.error)
    }
    @Test fun loginRequiresActualSession() = runBlocking {
        val flow = AuthFlow()
        flow.submit(AuthAction.SIGN_IN, input) { _, _ -> false }
        assertTrue(flow.state.value.error)
        assertFalse(flow.state.value.authenticated)
    }
    @Test fun clearingFeedbackDoesNotBypassResetCooldown() = runBlocking {
        val flow = AuthFlow()
        flow.submit(AuthAction.RESET_PASSWORD, input) { _, _ -> false }
        flow.clearFeedback()
        assertNull(flow.state.value.message)
        flow.submit(AuthAction.RESET_PASSWORD, input) { _, _ -> error("Cooldown must remain") }
        assertTrue(flow.state.value.error)
    }
}
