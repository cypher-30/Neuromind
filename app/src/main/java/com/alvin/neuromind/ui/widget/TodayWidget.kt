package com.alvin.neuromind.ui.widget

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.alvin.neuromind.MainActivity
import com.alvin.neuromind.data.NeuromindApplication
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class TodayWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        try {
            val repo = (context.applicationContext as NeuromindApplication).repository

            val today = LocalDate.now()
            val now = LocalTime.now()
            val nowMillis = System.currentTimeMillis()
            val todayDow: DayOfWeek = today.dayOfWeek

            val todayTasks = repo.getWidgetTasks(nowMillis = nowMillis, limit = 3)
            val nextEntry = repo.getWidgetNextEntry(dayOfWeek = todayDow, date = today, currentTime = now)

            val taskTitles = todayTasks.map { it.title }
            val dateLabel = today.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
            val nextLabel = nextEntry?.let { entry ->
                val timeStr = entry.startTime
                    .format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
                "Next: ${entry.title} at $timeStr"
            }

            provideContent {
                WidgetContent(
                    dateLabel = dateLabel,
                    taskTitles = taskTitles,
                    nextClassLabel = nextLabel
                )
            }
        } catch (t: Throwable) {
            Log.e("TodayWidget", "Widget render failed", t)
            provideContent {
                WidgetContent(
                    dateLabel = "Today",
                    taskTitles = emptyList(),
                    nextClassLabel = "Tap to open Neuromind"
                )
            }
        }
    }
}

// Shared widget palette — see WidgetColors.kt.

@Composable
private fun WidgetContent(
    dateLabel: String,
    taskTitles: List<String>,
    nextClassLabel: String?
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetBg)
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(10.dp)
                .background(WidgetSurface)
                .padding(12.dp),
            verticalAlignment = Alignment.Vertical.Top
        ) {
            Text(
                text = "Priorities",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    color = WidgetAccent
                )
            )
            Spacer(GlanceModifier.height(2.dp))
            Text(text = dateLabel, style = TextStyle(color = WidgetMuted))

            Spacer(GlanceModifier.height(8.dp))

            if (taskTitles.isEmpty()) {
                Text(
                    text = "No urgent priorities",
                    style = TextStyle(color = WidgetText)
                )
            } else {
                taskTitles.forEach { title ->
                    Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                        Text(text = "o", style = TextStyle(color = WidgetMuted, fontWeight = FontWeight.Medium))
                        Spacer(GlanceModifier.width(6.dp))
                        Text(text = title, style = TextStyle(color = WidgetText), maxLines = 1)
                    }
                    Spacer(GlanceModifier.height(4.dp))
                }
            }

            Spacer(GlanceModifier.height(4.dp))
            Spacer(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(WidgetDivider)
            )

            if (nextClassLabel != null) {
                Spacer(GlanceModifier.height(8.dp))
                Text(
                    text = nextClassLabel,
                    style = TextStyle(color = WidgetAccent),
                    maxLines = 1
                )
            } else {
                Spacer(GlanceModifier.height(8.dp))
                Text(
                    text = "Next: no upcoming slot",
                    style = TextStyle(color = WidgetMuted),
                    maxLines = 1
                )
            }
        }
    }
}
