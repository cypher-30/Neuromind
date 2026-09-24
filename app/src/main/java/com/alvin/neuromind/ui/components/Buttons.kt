package com.alvin.neuromind.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alvin.neuromind.ui.theme.Dimens
import com.alvin.neuromind.ui.theme.Pill

// The Organic system's circular icon affordance (back, search, theme toggle):
// a 36-44dp surface-filled circle, never a bare IconButton ripple square.
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = Dimens.CircleButtonSize,
    background: Color = MaterialTheme.colorScheme.surface,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

// The Organic system's primary/ghost pill button (.btn-primary / .btn-ghost):
// a solid-accent-fill CTA, or a text-only accent-colored variant for
// secondary actions like "Clear date" / "See all".
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    ghost: Boolean = false
) {
    val bg = if (ghost) Color.Transparent else MaterialTheme.colorScheme.primary
    val fg = if (ghost) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary
    Box(
        modifier = modifier
            .let { if (!ghost) it.fillMaxWidth() else it }
            .clip(Pill)
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(if (ghost) PaddingValues(horizontal = 4.dp, vertical = 8.dp) else PaddingValues(vertical = 16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = fg.copy(alpha = if (enabled) 1f else 0.45f),
            textAlign = TextAlign.Center
        )
    }
}
