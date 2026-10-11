package com.example.financeapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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

private val DarkColorScheme = darkColorScheme(
    primary = BronzePrimaryDark,
    onPrimary = OnBronze,
    primaryContainer = BronzeContainerDark,
    onPrimaryContainer = BronzeTextDark,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = OutlineDark,
    error = RiskRed,
    onError = OnBronze
)

@Composable
fun FinanceAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let { window ->
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
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
