package com.example.financeapp.ui.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceInputControllerTest {
    @Test
    fun fakeMovesFromIdleThroughListeningToEditableTranscript() {
        val controller = FakeVoiceInputController()

        controller.start("hi-IN")
        assertTrue(controller.state.value is VoiceInputState.Listening)

        controller.emitPartial("क्या मैं यात्रा कर सकता हूं")
        assertEquals("क्या मैं यात्रा कर सकता हूं", (controller.state.value as VoiceInputState.Listening).partialText)

        controller.stop()
        assertEquals("क्या मैं यात्रा कर सकता हूं", (controller.state.value as VoiceInputState.Complete).transcript)
    }

    @Test
    fun fakeExposesPermissionAndActionableErrors() {
        val controller = FakeVoiceInputController()
        controller.requirePermission()
        assertEquals(VoiceInputState.PermissionRequired, controller.state.value)

        controller.emitError(VoiceError.NO_NETWORK, "Check your connection and try again.")
        val error = controller.state.value as VoiceInputState.Error
        assertEquals(VoiceError.NO_NETWORK, error.reason)
        assertTrue(error.nextStep.isNotBlank())
    }
}
