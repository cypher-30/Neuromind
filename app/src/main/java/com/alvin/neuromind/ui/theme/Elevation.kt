package com.alvin.neuromind.ui.theme

import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// The app had no elevation tokens before this redesign — every Card used M3
// defaults. Organic's --shadow-sm/md/lg are ink-tinted rather than the
// default cool grey, so shadows stay warm against the cream ground; the
// tint below approximates that (#2E2B25, the neutral-900 ink) since Compose
// Card shadows don't expose a shadow-color API directly, only ambient/spot
// tint via Modifier.shadow — callers that need the exact tint should use
// Modifier.shadow(elevation, shape, ambientColor = Elevation.ShadowTint,
// spotColor = Elevation.ShadowTint) instead of CardDefaults.cardElevation.
object Elevation {
    val Small = 1.dp
    val Medium = 3.dp
    val Large = 8.dp
    val ShadowTint = Color(0xFF2E2B25)
}

@Composable
fun organicCardElevation(elevated: Boolean = true) = CardDefaults.cardElevation(
    defaultElevation = if (elevated) Elevation.Small else 0.dp
)
