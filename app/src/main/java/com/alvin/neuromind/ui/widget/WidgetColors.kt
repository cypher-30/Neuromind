package com.alvin.neuromind.ui.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider

// Shared Glance widget palette (day, night). Widgets follow the phone's
// light/dark mode. Kept static and independent of Adaptive Organic (see
// domain/AdaptiveAppearance.kt): widgets live outside the app process.
//
// Light: neutral near-white card with ink text and a muted rust accent
// (2026-11-22 — the cream/orange Organic ground looked odd on the home
// screen). Dark: the warm charcoal card, unchanged.
internal val WidgetBg = ColorProvider(Color(0xFFFAFAF7), Color(0xFF2E2B25))
internal val WidgetText = ColorProvider(Color(0xFF1C1B1A), Color(0xFFF9F4ED))
internal val WidgetMuted = ColorProvider(Color(0x991C1B1A), Color(0x94F9F4ED))
internal val WidgetAccent = ColorProvider(Color(0xFFA4552B), Color(0xFFF6A06B))
internal val WidgetDivider = ColorProvider(Color(0x1F1C1B1A), Color(0x1FFFFFFF))
internal val WidgetDanger = ColorProvider(Color(0xFFB3412F), Color(0xFFE8826E))

// Activity Heat steps (none → 4+ tasks finished that day), rust ramp.
internal val Heat0 = ColorProvider(Color(0x141C1B1A), Color(0x1FFFFFFF))
internal val Heat1 = ColorProvider(Color(0x59A4552B), Color(0x66F6A06B))
internal val Heat2 = ColorProvider(Color(0xA6A4552B), Color(0xB3F6A06B))
internal val Heat3 = ColorProvider(Color(0xFFA4552B), Color(0xFFF6A06B))
