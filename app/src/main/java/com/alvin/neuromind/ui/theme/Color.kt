package com.alvin.neuromind.ui.theme

import androidx.compose.ui.graphics.Color

// ── Brand Colors ──────────────────────────────────────────────────────────────
val BrandBlueLight       = Color(0xFF3A5FE0)   // vivid primary blue (light)
val BrandBlueDark        = Color(0xFF7B9FFF)   // softer blue for dark surfaces
val BrandOrangeLight     = Color(0xFFF5911F)   // punchy warm orange (light)
val BrandOrangeDark      = Color(0xFFFFB347)   // soft amber for dark surfaces
val BrandTealLight       = Color(0xFF00BFA5)   // bright teal tertiary (light)
val BrandTealDark        = Color(0xFF1DE9B6)   // vivid mint for dark surfaces

// ── Containers ────────────────────────────────────────────────────────────────
val PrimaryContainerLight    = Color(0xFFD6E4FF)
val PrimaryContainerDark     = Color(0xFF1A3878)
val SecondaryContainerLight  = Color(0xFFFFE0B2)
val SecondaryContainerDark   = Color(0xFF7F3F00)
val TertiaryContainerLight   = Color(0xFFB2EDE7)
val TertiaryContainerDark    = Color(0xFF00504A)

// ── On-Container ──────────────────────────────────────────────────────────────
val OnPrimaryContainerLight  = Color(0xFF001A60)
val OnPrimaryContainerDark   = Color(0xFFD6E4FF)
val OnSecondaryContainerLight = Color(0xFF3D1A00)
val OnSecondaryContainerDark = Color(0xFFFFE0B2)
val OnTertiaryContainerLight = Color(0xFF001E1B)
val OnTertiaryContainerDark  = Color(0xFFB2EDE7)

// ── Backgrounds & Surfaces ────────────────────────────────────────────────────
val LightBackground      = Color(0xFFF4F6FF)
val DarkBackground       = Color(0xFF0F0F18)
val LightSurface         = Color(0xFFFFFFFF)
val DarkSurface          = Color(0xFF1C1C2E)
val SurfaceVariantLight  = Color(0xFFE8ECF4)
val SurfaceVariantDark   = Color(0xFF2A2D3E)

// ── Text ──────────────────────────────────────────────────────────────────────
val TextPrimary          = Color(0xFF0D1133)
val TextPrimaryDark      = Color(0xFFEAEDFF)
val OutlineLight         = Color(0xFF8890A8)
val OutlineDark          = Color(0xFF5A5F78)

// ── Error ─────────────────────────────────────────────────────────────────────
val ErrorLight           = Color(0xFFBA1A1A)
val ErrorDark            = Color(0xFFFF897D)
val ErrorContainerLight  = Color(0xFFFFDAD6)
val ErrorContainerDark   = Color(0xFF93000A)
val OnErrorContainerLight = Color(0xFF410002)
val OnErrorContainerDark  = Color(0xFFFFDAD6)

// ── Gradient Endpoints (splash + header bands) ────────────────────────────────
val GradientStart        = Color(0xFF3A5FE0)   // brand blue
val GradientEnd          = Color(0xFF6A3FD8)   // indigo-violet

// ── Legacy aliases so existing code compiles without changes ──────────────────
val DeepCalmingBlue      = BrandBlueLight
val WarmOrange           = BrandOrangeLight
val CriticalRed          = Color(0xFFD32F2F)
