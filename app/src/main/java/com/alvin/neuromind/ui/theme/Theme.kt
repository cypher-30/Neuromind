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

private val DarkColorScheme = darkColorScheme(
    primary = DeepCalmingBlue,
    secondary = WarmOrange,
    background = DarkBackground,
    surface = DarkSurface,
    error = ErrorRed,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    errorContainer = CriticalRed,
    onErrorContainer = Color.White,
    primaryContainer = DeepCalmingBlue.copy(alpha = 0.2f),
    onPrimaryContainer = TextPrimaryDark,
    tertiaryContainer = Color(0xFF333333),
    onTertiaryContainer = TextPrimaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = DeepCalmingBlue,
    secondary = WarmOrange,
    background = LightBackground,
    surface = LightSurface,
    error = ErrorRed,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    errorContainer = CriticalRed,
    onErrorContainer = Color.White,
    primaryContainer = DeepCalmingBlue.copy(alpha = 0.1f),
    onPrimaryContainer = TextPrimary,
    tertiaryContainer = Color(0xFFE0E0E0),
    onTertiaryContainer = TextPrimary
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
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = {
            // This Surface applies the correct background color to the entire app
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                content()
            }
        }
    )
}