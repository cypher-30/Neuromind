package com.alvin.neuromind.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Organic design system: dark mode is a token re-point (bg -> neutral-900,
// surface -> neutral-800, accent -> accent-400, accent-2 -> accent-2-400),
// not a hand-tuned second palette. See ui/theme/Color.kt for the ramps.
private val DarkColorScheme = darkColorScheme(
    primary              = Accent400,
    onPrimary            = DarkBackground,
    primaryContainer     = AccentContainerDark,
    onPrimaryContainer   = Accent100,
    secondary            = Accent2_400,
    onSecondary          = DarkBackground,
    secondaryContainer   = Accent2ContainerDark,
    onSecondaryContainer = Accent2_100,
    tertiary             = Accent2_400,
    onTertiary           = DarkBackground,
    tertiaryContainer    = Accent2ContainerDark,
    onTertiaryContainer  = Accent2_100,
    background           = DarkBackground,
    onBackground         = TextPrimaryDark,
    surface              = DarkSurface,
    onSurface            = TextPrimaryDark,
    surfaceVariant       = Neutral700,
    onSurfaceVariant     = TextMutedDark,
    outline              = DividerDark,
    outlineVariant       = DividerDark,
    error                = DangerDark,
    onError              = DarkBackground,
    errorContainer       = DangerContainerDark,
    onErrorContainer     = OnDangerContainerDark,
    // M3 dialogs, date/time pickers, menus and sheets read these roles; left
    // unset they fall back to Material's stock lavender-grey baseline.
    surfaceTint             = Color.Transparent,
    surfaceBright           = Neutral700,
    surfaceDim              = DarkBackground,
    surfaceContainerLowest  = Neutral900,
    surfaceContainerLow     = DarkBackground,
    surfaceContainer        = DarkSurface,
    surfaceContainerHigh    = DarkSurface,
    surfaceContainerHighest = Neutral700,
    inverseSurface          = Neutral100,
    inverseOnSurface        = TextPrimary,
    inversePrimary          = AccentBase,
)

private val LightColorScheme = lightColorScheme(
    primary              = AccentBase,
    onPrimary            = LightBackground,
    primaryContainer     = AccentContainerLight,
    onPrimaryContainer   = Accent800,
    secondary            = Accent2Base,
    onSecondary          = LightBackground,
    secondaryContainer   = Accent2ContainerLight,
    onSecondaryContainer = Accent2_800,
    tertiary             = Accent2Base,
    onTertiary           = LightBackground,
    tertiaryContainer    = Accent2ContainerLight,
    onTertiaryContainer  = Accent2_800,
    background           = LightBackground,
    onBackground         = TextPrimary,
    surface              = LightSurface,
    onSurface            = TextPrimary,
    surfaceVariant       = Neutral300,
    onSurfaceVariant     = TextMutedLight,
    outline              = DividerLight,
    outlineVariant       = DividerLight,
    error                = DangerLight,
    onError              = LightBackground,
    errorContainer       = DangerContainerLight,
    onErrorContainer     = OnDangerContainerLight,
    surfaceTint             = Color.Transparent,
    surfaceBright           = Neutral100,
    surfaceDim              = Neutral300,
    surfaceContainerLowest  = Neutral100,
    surfaceContainerLow     = LightBackground,
    surfaceContainer        = LightBackground,
    surfaceContainerHigh    = LightBackground,
    surfaceContainerHighest = LightSurface,
    inverseSurface          = Neutral900,
    inverseOnSurface        = Neutral100,
    inversePrimary          = Accent400,
)

@Composable
fun NeuromindTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        shapes      = AppShapes,
        content = {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color    = MaterialTheme.colorScheme.background
            ) {
                content()
            }
        }
    )
}
