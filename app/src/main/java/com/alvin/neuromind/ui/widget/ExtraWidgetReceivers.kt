package com.alvin.neuromind.ui.widget

import androidx.glance.appwidget.GlanceAppWidgetReceiver

// Receiver class names are kept from the original 17-widget suite so widgets
// already placed on a home screen keep working after the cut to six.
class NextClassWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = NextClassWidget()
}
class DailyProgressWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = DailyProgressWidget()
}
class HeatmapWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = HeatmapWidget()
}
class ExamCountdownWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ExamCountdownWidget()
}
class MiniQuickLogWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = MiniQuickLogWidget()
}
