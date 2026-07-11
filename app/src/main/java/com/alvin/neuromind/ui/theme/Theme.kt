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
    primary              = BrandTealDark,
    onPrimary            = Color(0xFF00332F),
    primaryContainer     = PrimaryContainerDark,
    onPrimaryContainer   = OnPrimaryContainerDark,
    secondary            = BrandSunsetDark,
    onSecondary          = Color(0xFF4A2000),
    secondaryContainer   = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary             = BrandSeagreenDark,
    onTertiary           = Color(0xFF00332B),
    tertiaryContainer    = TertiaryContainerDark,
    onTertiaryContainer  = OnTertiaryContainerDark,
    background           = DarkBackground,
    onBackground         = TextPrimaryDark,
    surface              = DarkSurface,
    onSurface            = TextPrimaryDark,
    surfaceVariant       = SurfaceVariantDark,
    onSurfaceVariant     = Color(0xFFBCC3D8),
    outline              = OutlineDark,
    error                = ErrorDark,
    onError              = Color(0xFF690005),
    errorContainer       = ErrorContainerDark,
    onErrorContainer     = OnErrorContainerDark,
)

private val LightColorScheme = lightColorScheme(
    primary              = BrandTealLight,
    onPrimary            = Color.White,
    primaryContainer     = PrimaryContainerLight,
    onPrimaryContainer   = OnPrimaryContainerLight,
    secondary            = BrandSunsetLight,
    onSecondary          = Color(0xFF3D1D00),
    secondaryContainer   = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary             = BrandSeagreenLight,
    onTertiary           = Color.White,
    tertiaryContainer    = TertiaryContainerLight,
    onTertiaryContainer  = OnTertiaryContainerLight,
    background           = LightBackground,
    onBackground         = TextPrimary,
    surface              = LightSurface,
    onSurface            = TextPrimary,
    surfaceVariant       = SurfaceVariantLight,
    onSurfaceVariant     = Color(0xFF44495C),
    outline              = OutlineLight,
    error                = ErrorLight,
    onError              = Color.White,
    errorContainer       = ErrorContainerLight,
    onErrorContainer     = OnErrorContainerLight,
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
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
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
