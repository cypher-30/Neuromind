package com.alvin.neuromind.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.alvin.neuromind.R

// Organic design system: Caprasimo (display) over Figtree (body). Caprasimo
// ships as a single static weight; Figtree ships as a variable font, so its
// three weights are the same file selected via a variation-axis setting
// (requires API 26+, which matches this app's minSdk).
val CaprasimoFamily = FontFamily(
    Font(R.font.caprasimo, FontWeight.Normal)
)

@OptIn(ExperimentalTextApi::class)
val FigtreeFamily = FontFamily(
    Font(R.font.figtree, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.figtree, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.figtree, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700)))
)

// Caprasimo is a chunky, high-x-height display face — the design tunes it to
// 1.12 line height and -0.015em tracking; Compose's default M3 leading reads
// too airy against it, so line height and letter spacing are set explicitly
// on every Caprasimo style rather than left to fontSize defaults.
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily   = CaprasimoFamily,
        fontWeight   = FontWeight.Normal,
        fontSize     = 42.sp,
        lineHeight   = 47.sp,
        letterSpacing = (-0.6).sp
    ),
    displayMedium = TextStyle(
        fontFamily   = CaprasimoFamily,
        fontWeight   = FontWeight.Normal,
        fontSize     = 36.sp,
        lineHeight   = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    displaySmall = TextStyle(
        fontFamily   = CaprasimoFamily,
        fontWeight   = FontWeight.Normal,
        fontSize     = 30.sp,
        lineHeight   = 34.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineLarge = TextStyle(
        fontFamily   = CaprasimoFamily,
        fontWeight   = FontWeight.Normal,
        fontSize     = 26.sp, // dashboard greeting name
        lineHeight   = 29.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineMedium = TextStyle(
        fontFamily   = CaprasimoFamily,
        fontWeight   = FontWeight.Normal,
        fontSize     = 24.sp, // top-level screen titles: Tasks, Insights, Settings
        lineHeight   = 27.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineSmall = TextStyle(
        fontFamily   = CaprasimoFamily,
        fontWeight   = FontWeight.Normal,
        fontSize     = 20.sp, // sheet/detail headings: New task, Focus mode, How was today?
        lineHeight   = 22.sp,
        letterSpacing = (-0.2).sp
    ),
    titleLarge = TextStyle(
        fontFamily   = CaprasimoFamily,
        fontWeight   = FontWeight.Normal,
        fontSize     = 18.sp, // card titles
        lineHeight   = 20.sp,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily   = FigtreeFamily,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 14.sp,
        lineHeight   = 20.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily   = FigtreeFamily,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 13.sp,
        lineHeight   = 18.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily   = FigtreeFamily,
        fontWeight   = FontWeight.Normal,
        fontSize     = 15.sp,
        lineHeight   = 22.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily   = FigtreeFamily,
        fontWeight   = FontWeight.Normal,
        fontSize     = 14.sp,
        lineHeight   = 20.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily   = FigtreeFamily,
        fontWeight   = FontWeight.Normal,
        fontSize     = 12.sp,
        lineHeight   = 17.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily   = FigtreeFamily,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 13.sp,
        lineHeight   = 18.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily   = FigtreeFamily,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 12.sp,
        lineHeight   = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily   = FigtreeFamily,
        fontWeight   = FontWeight.Medium,
        fontSize     = 11.sp,
        lineHeight   = 15.sp,
        letterSpacing = 0.06.em // kickers: uppercase, +0.06em tracking
    )
)
