package com.alvin.neuromind.ui.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider

// Shared Glance widget palette — kept static and independent of Adaptive
// Organic (see domain/AdaptiveAppearance.kt): widgets/tiles live outside the
// app process and are refreshed on their own schedule, so they intentionally
// never reflect the in-app adaptive ground shift. Was previously duplicated
// identically in WidgetSuite.kt and TodayWidget.kt; both files reference
// these same top-level, package-visible vals now.
internal val WidgetBg = ColorProvider(Color(0xFFF5EAD8), Color(0xFF2E2B25))
internal val WidgetSurface = ColorProvider(Color(0xFFEBDDC5), Color(0xFF474238))
internal val WidgetText = ColorProvider(Color(0xFF201E1D), Color(0xFFF9F4ED))
internal val WidgetMuted = ColorProvider(Color(0x94201E1D), Color(0x94F9F4ED))
internal val WidgetAccent = ColorProvider(Color(0xFFC67139), Color(0xFFF6A06B))
internal val WidgetDivider = ColorProvider(Color(0x29201E1D), Color(0x1FFFFFFF))

// Activity Heat steps (none → 4+ tasks finished that day), terracotta ramp.
internal val Heat0 = ColorProvider(Color(0x1F201E1D), Color(0x1FFFFFFF))
internal val Heat1 = ColorProvider(Color(0x66C67139), Color(0x66F6A06B))
internal val Heat2 = ColorProvider(Color(0xB3C67139), Color(0xB3F6A06B))
internal val Heat3 = ColorProvider(Color(0xFFC67139), Color(0xFFF6A06B))
