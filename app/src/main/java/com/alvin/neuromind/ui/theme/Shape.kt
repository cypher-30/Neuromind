package com.alvin.neuromind.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small      = RoundedCornerShape(14.dp),
    medium     = RoundedCornerShape(18.dp),
    large      = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

// Buttons, chips, inputs, the FAB and nav pills are fully round in the
// Organic system (border-radius: 999px) — a dedicated shape rather than an
// oversized RoundedCornerShape so it stays correct at any element height.
val Pill = CircleShape
