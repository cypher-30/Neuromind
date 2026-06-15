package com.alvin.neuromind.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.alvin.neuromind.MainActivity
import com.alvin.neuromind.data.NeuromindApplication
import com.alvin.neuromind.data.Task
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class TodayWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = (context.applicationContext as NeuromindApplication).repository

        val allTasks = repo.allTasks.first()
        val allTimetable = repo.allTimetableEntries.first()

        val today = LocalDate.now()
        val now = LocalTime.now()
        val todayDow: DayOfWeek = today.dayOfWeek

        // Up to 3 incomplete tasks due today or overdue, sorted by due time
        val todayTasks = allTasks
            .filter { task ->
                if (task.isCompleted) return@filter false
                val dueDate = task.dueDate ?: return@filter true
                val dueLocal = Instant.ofEpochMilli(dueDate)
                    .atZone(ZoneId.systemDefault()).toLocalDate()
                !dueLocal.isAfter(today)
            }
            .sortedWith(compareBy(nullsLast()) { it.dueDate })
            .take(3)

        // Next upcoming timetable entry for today
        val nextEntry = allTimetable
            .filter { entry ->
                val relevant = if (entry.isRecurring) {
                    entry.dayOfWeek == todayDow
                } else {
                    entry.date == today
                }
                relevant && entry.startTime.isAfter(now)
            }
            .minByOrNull { it.startTime }

        val taskTitles = todayTasks.map { it.title }
        val nextLabel = nextEntry?.let { entry ->
            val timeStr = entry.startTime
                .format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
            "Next: ${entry.title} at $timeStr"
        }

        provideContent {
            GlanceTheme {
                WidgetContent(
                    taskTitles = taskTitles,
                    nextClassLabel = nextLabel
                )
            }
        }
    }
}

@Composable
private fun WidgetContent(
    taskTitles: List<String>,
    nextClassLabel: String?
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(12),
            verticalAlignment = Alignment.Vertical.Top
        ) {
            Text(
                text = "Today",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.primary
                )
            )

            Spacer(GlanceModifier.height(8))

            if (taskTitles.isEmpty()) {
                Text(
                    text = "All clear — nothing due today!",
                    style = TextStyle(color = GlanceTheme.colors.onSurface)
                )
            } else {
                taskTitles.forEach { title ->
                    Text(
                        text = "• $title",
                        style = TextStyle(color = GlanceTheme.colors.onSurface),
                        maxLines = 1
                    )
                    Spacer(GlanceModifier.height(4))
                }
            }

            if (nextClassLabel != null) {
                Spacer(GlanceModifier.height(8))
                Text(
                    text = nextClassLabel,
                    style = TextStyle(color = GlanceTheme.colors.secondary),
                    maxLines = 1
                )
            }
        }
    }
}
