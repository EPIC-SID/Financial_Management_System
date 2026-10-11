package com.example.financeapp.ui.theme

import androidx.compose.ui.graphics.Color
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeContrastTest {
    @Test fun permittedPairingsMeetWcagAA() {
        val textPairs = listOf(
            Triple("ink on canvas", Ink, CanvasWhite),
            Triple("muted on canvas", MutedInk, CanvasWhite),
            Triple("ink on surface", Ink, SurfaceNeutral),
            Triple("muted on surface", MutedInk, SurfaceNeutral),
            Triple("ink on green", Ink, SummaryGreen),
            Triple("muted on green", MutedInk, SummaryGreen),
            Triple("ink on pink", Ink, ErrorPink),
            Triple("muted on pink", MutedInk, ErrorPink),
            Triple("error on pink", ErrorInk, ErrorPink),
            Triple("error on canvas", ErrorInk, CanvasWhite),
            Triple("ink on action", OnAction, ActionGreen),
            Triple("ink on pressed", OnAction, ActionGreenPressed),
            Triple("ink on guidance", OnGuidance, GuidanceYellow),
            Triple("disabled text on surface", MutedInk, SurfaceNeutral),
            Triple("observed and safe on green", Ink, SummaryGreen),
            Triple("watch on wash", WatchInk, WatchWash)
        )
        val uiPairs = listOf(
            Triple("action edge on canvas", ActionGreen, CanvasWhite),
            Triple("outline on canvas", OutlineNeutral, CanvasWhite),
            Triple("outline on surface", OutlineNeutral, SurfaceNeutral),
            Triple("selected rail on green", Ink, SummaryGreen)
        )
        // Charts are deferred; green on the neutral surface is not an approved thin-line pairing.
        textPairs.forEach { (label, foreground, background) -> check(label, foreground, background, 4.5) }
        uiPairs.forEach { (label, foreground, background) -> check(label, foreground, background, 3.0) }
    }

    private fun check(label: String, foreground: Color, background: Color, minimum: Double) {
        val first = luminance(foreground)
        val second = luminance(background)
        val ratio = (max(first, second) + 0.05) / (min(first, second) + 0.05)
        println(String.format(Locale.ROOT, "%s: %.2f:1 (minimum %.1f:1)", label, ratio, minimum))
        assertTrue("$label has contrast $ratio; requires $minimum", ratio >= minimum)
    }

    private fun luminance(color: Color): Double {
        fun linear(channel: Float): Double = channel.toDouble().let {
            if (it <= 0.04045) it / 12.92 else ((it + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * linear(color.red) + 0.7152 * linear(color.green) + 0.0722 * linear(color.blue)
    }
}
