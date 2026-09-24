package com.alvin.neuromind.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.alvin.neuromind.ui.theme.Dimens
import com.alvin.neuromind.ui.theme.Elevation

// The Organic system's ".card" — a surface-filled, over-rounded container.
// Elevation is a warm ink-tinted shadow (Elevation.ShadowTint) rather than
// Compose's default cool grey, matching --shadow-sm/md/lg. In light mode this
// surface color is deliberately DARKER than the page background (sand set
// into cream) — that inversion lives in the color tokens, not here.
@Composable
fun OrganicCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    shape: Shape = MaterialTheme.shapes.large,
    elevation: Dp = Elevation.Small,
    contentPadding: PaddingValues = PaddingValues(Dimens.CardPadding),
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Elevation.ShadowTint,
                spotColor = Elevation.ShadowTint
            )
            .clip(shape)
            .background(color)
            .padding(contentPadding)
    ) {
        content()
    }
}
