package com.example.financeapp.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = ActionGreen,
    onPrimary = OnAction,
    primaryContainer = SummaryGreen,
    onPrimaryContainer = Ink,
    // Generic secondary actions stay neutral. Yellow has an explicit guidance
    // token so controls cannot accidentally use it for status or text.
    secondary = Ink,
    onSecondary = CanvasWhite,
    secondaryContainer = SurfaceNeutral,
    onSecondaryContainer = Ink,
    tertiary = Ink,
    onTertiary = CanvasWhite,
    tertiaryContainer = SurfaceNeutral,
    onTertiaryContainer = Ink,
    background = CanvasWhite,
    onBackground = Ink,
    surface = CanvasWhite,
    onSurface = Ink,
    surfaceVariant = SurfaceNeutral,
    onSurfaceVariant = MutedInk,
    error = ErrorInk,
    onError = CanvasWhite,
    errorContainer = ErrorPink,
    onErrorContainer = ErrorInk,
    outline = OutlineNeutral,
    outlineVariant = OutlineNeutral,
    inverseSurface = Ink,
    inverseOnSurface = CanvasWhite,
    inversePrimary = SummaryGreen,
    surfaceTint = CanvasWhite
)

@Composable
fun FinanceAppTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let { window ->
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
