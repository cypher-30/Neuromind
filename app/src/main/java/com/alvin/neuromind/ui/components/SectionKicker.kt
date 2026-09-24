package com.alvin.neuromind.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign

// Small uppercase eyebrow label ("Up next", "Neuromind noticed", "Deep
// work", "By subject") — 11sp with the +0.06em tracking already baked into
// Typography.labelSmall. Callers tint it to whichever accent role the
// surrounding card uses (primary for "Up next", secondary for coaching
// surfaces).
@Composable
fun SectionKicker(
    text: String,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        textAlign = TextAlign.Start,
        modifier = modifier
    )
}
