package com.alvin.neuromind.ui.widget

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
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
import com.alvin.neuromind.QuickLogEntryActivity
import com.alvin.neuromind.data.NeuromindApplication
import com.alvin.neuromind.data.Priority
import com.alvin.neuromind.data.Task
import com.alvin.neuromind.data.TaskCategory
import com.alvin.neuromind.data.TimetableEntry
import com.alvin.neuromind.domain.DailyProgress
import com.alvin.neuromind.domain.DailyProgressCalculator
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val WidgetBg = ColorProvider(Color(0xFFF5EAD8), Color(0xFF2E2B25))
private val WidgetSurface = ColorProvider(Color(0xFFEBDDC5), Color(0xFF474238))
private val WidgetText = ColorProvider(Color(0xFF201E1D), Color(0xFFF9F4ED))
private val WidgetMuted = ColorProvider(Color(0x94201E1D), Color(0x94F9F4ED))
private val WidgetAccent = ColorProvider(Color(0xFFC67139), Color(0xFFF6A06B))
private val WidgetDivider = ColorProvider(Color(0x29201E1D), Color(0x1FFFFFFF))

enum class WidgetMode {
    QUICK_ACTIONS,
    FOCUS_SHORTCUTS,
    NEXT_CLASS,
    DAILY_PROGRESS,
    STREAK_WELLBEING,
    SMART_SUGGESTION,
    HEATMAP,
    ACADEMIC,
    MOTIVATOR,
    MINI_QUICK_LOG,
    MINI_STATUS,
    MINI_NEXT
}

data class WidgetSnapshot(
    val tasks: List<Task>,
    val priorities: List<Task>,
    val nextEntry: TimetableEntry?,
    val completedCount: Int,
    val pendingCount: Int,
    /** Tasks due today only — the Daily Progress widget shares Home's definition. */
    val todayProgress: DailyProgress,
    val streakDays: Int,
    val feedbackLast7: List<Int>,
    val academicOpenCount: Int,
    val focusMinutesToday: Int
)

private suspend fun loadWidgetSnapshot(context: Context): WidgetSnapshot {
    val app = context.applicationContext as NeuromindApplication
    val repo = app.repository
    val tasks = repo.allTasks.first()
    val entries = repo.allTimetableEntries.first()
    val feedback = repo.allFeedbackLogs.first()
    val sessions = repo.allFocusSessions.first()
    val now = System.currentTimeMillis()
    val today = LocalDate.now()
    val zone = ZoneId.systemDefault()

    val priorities = tasks
        .filter { !it.isCompleted && (it.priority == Priority.HIGH || (it.dueDate != null && it.dueDate < now)) }
        .sortedWith(compareByDescending<Task> { it.dueDate != null && it.dueDate < now }.thenByDescending { it.priority })
        .take(3)

    val nextEntry = entries
        .filter { entry -> entry.occursOn(today) && !entry.isAllDay }
        .filter { it.startTime.isAfter(java.time.LocalTime.now()) }
        .sortedBy { it.startTime }
        .firstOrNull()

    val loggedDays = feedback.map { Instant.ofEpochMilli(it.date).atZone(zone).toLocalDate() }.toSet()
    var streak = 0
    var cursor = today
    while (cursor in loggedDays) {
        streak++
        cursor = cursor.minusDays(1)
    }

    val feedbackMap = feedback.associateBy { Instant.ofEpochMilli(it.date).atZone(zone).toLocalDate() }
    val feedbackLast7 = (6 downTo 0).map { offset ->
        val day = today.minusDays(offset.toLong())
        feedbackMap[day]?.tasksCompleted ?: 0
    }

    val startOfDay = today.atStartOfDay(zone).toInstant().toEpochMilli()
    val focusMinutesToday = sessions
        .filter { it.completedAt >= startOfDay }
        .sumOf { it.durationMinutes }

    val academicOpenCount = tasks.count { !it.isCompleted && it.category == TaskCategory.ACADEMIC }

    return WidgetSnapshot(
        tasks = tasks,
        priorities = priorities,
        nextEntry = nextEntry,
        completedCount = tasks.count { it.isCompleted },
        pendingCount = tasks.count { !it.isCompleted },
        todayProgress = DailyProgressCalculator.forDate(tasks, today, zone),
        streakDays = streak,
        feedbackLast7 = feedbackLast7,
        academicOpenCount = academicOpenCount,
        focusMinutesToday = focusMinutesToday
    )
}

open class SimpleInfoWidget(private val mode: WidgetMode) : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        try {
            val snapshot = loadWidgetSnapshot(context)
            provideContent {
                WidgetFrame(titleFor(mode), mode) {
                    renderMode(mode, snapshot)
                }
            }
        } catch (t: Throwable) {
            Log.e("SimpleInfoWidget", "Widget render failed", t)
            provideContent {
                WidgetFrame("Neuromind", mode) {
                    Text("Tap to open Neuromind", style = TextStyle(color = WidgetText))
                }
            }
        }
    }
}

class QuickActionsWidget : SimpleInfoWidget(WidgetMode.QUICK_ACTIONS)
class FocusShortcutsWidget : SimpleInfoWidget(WidgetMode.FOCUS_SHORTCUTS)
class NextClassWidget : SimpleInfoWidget(WidgetMode.NEXT_CLASS)
class DailyProgressWidget : SimpleInfoWidget(WidgetMode.DAILY_PROGRESS)
class StreakWellbeingWidget : SimpleInfoWidget(WidgetMode.STREAK_WELLBEING)
class SmartSuggestionWidget : SimpleInfoWidget(WidgetMode.SMART_SUGGESTION)
class HeatmapWidget : SimpleInfoWidget(WidgetMode.HEATMAP)
class AcademicWidget : SimpleInfoWidget(WidgetMode.ACADEMIC)
class MotivatorWidget : SimpleInfoWidget(WidgetMode.MOTIVATOR)
class MiniQuickLogWidget : SimpleInfoWidget(WidgetMode.MINI_QUICK_LOG)
class MiniStatusWidget : SimpleInfoWidget(WidgetMode.MINI_STATUS)
class MiniNextWidget : SimpleInfoWidget(WidgetMode.MINI_NEXT)

open class RotatingStackWidget(private val intervalMinutes: Long) : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        try {
            val snapshot = loadWidgetSnapshot(context)
            val rotatingModes = listOf(
                WidgetMode.DAILY_PROGRESS,
                WidgetMode.NEXT_CLASS,
                WidgetMode.SMART_SUGGESTION,
                WidgetMode.STREAK_WELLBEING,
                WidgetMode.ACADEMIC,
                WidgetMode.MOTIVATOR
            )
            val slot = ((System.currentTimeMillis() / (intervalMinutes * 60_000L)) % rotatingModes.size).toInt()
            val active = rotatingModes[slot]
            provideContent {
                WidgetFrame("Stack ${intervalMinutes}m", active) {
                    renderMode(active, snapshot)
                }
            }
        } catch (t: Throwable) {
            Log.e("RotatingStackWidget", "Widget render failed", t)
            provideContent {
                WidgetFrame("Stack", WidgetMode.DAILY_PROGRESS) {
                    Text("Open Neuromind", style = TextStyle(color = WidgetText))
                }
            }
        }
    }
}

class RotatingStack15Widget : RotatingStackWidget(15)
class RotatingStack30Widget : RotatingStackWidget(30)
class RotatingStack60Widget : RotatingStackWidget(60)

class ConfigurableStackWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        try {
            val app = context.applicationContext as NeuromindApplication
            val interval = app.userPreferencesRepository.widgetStackIntervalMinutes.first()
            val snapshot = loadWidgetSnapshot(context)
            val rotatingModes = listOf(
                WidgetMode.DAILY_PROGRESS,
                WidgetMode.NEXT_CLASS,
                WidgetMode.SMART_SUGGESTION,
                WidgetMode.STREAK_WELLBEING,
                WidgetMode.ACADEMIC,
                WidgetMode.MOTIVATOR
            )
            val slot = ((System.currentTimeMillis() / (interval * 60_000L)) % rotatingModes.size).toInt()
            val active = rotatingModes[slot]
            provideContent {
                WidgetFrame("Auto Stack ${interval}m", active) {
                    renderMode(active, snapshot)
                }
            }
        } catch (t: Throwable) {
            Log.e("ConfigurableStackWidget", "Widget render failed", t)
            provideContent {
                WidgetFrame("Auto Stack", WidgetMode.DAILY_PROGRESS) {
                    Text("Open Neuromind", style = TextStyle(color = WidgetText))
                }
            }
        }
    }
}

@Composable
private fun WidgetFrame(title: String, mode: WidgetMode, content: @Composable () -> Unit) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetBg)
            .clickable(
                if (mode == WidgetMode.MINI_QUICK_LOG) {
                    actionStartActivity<QuickLogEntryActivity>()
                } else {
                    actionStartActivity<MainActivity>()
                }
            )
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(10.dp)
                .background(WidgetSurface)
                .padding(12.dp)
        ) {
            Text(title, style = TextStyle(color = WidgetAccent, fontWeight = FontWeight.Bold))
            Spacer(GlanceModifier.height(6.dp))
            content()
        }
    }
}

@Composable
private fun renderMode(mode: WidgetMode, snapshot: WidgetSnapshot) {
    when (mode) {
        WidgetMode.QUICK_ACTIONS -> {
            Text("- Add Task", style = TextStyle(color = WidgetText))
            Text("- Start Focus", style = TextStyle(color = WidgetText))
            Text("- Open Timetable", style = TextStyle(color = WidgetText))
            Text("- Ask Neuromind", style = TextStyle(color = WidgetText))
        }
        WidgetMode.FOCUS_SHORTCUTS -> {
            Text("25m  |  45m  |  60m", style = TextStyle(color = WidgetText))
            Spacer(GlanceModifier.height(4.dp))
            Text("Focused today: ${snapshot.focusMinutesToday} min", style = TextStyle(color = WidgetMuted))
        }
        WidgetMode.NEXT_CLASS -> {
            val fmt = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
            if (snapshot.nextEntry != null) {
                Text(snapshot.nextEntry.title, style = TextStyle(color = WidgetText, fontWeight = FontWeight.Medium), maxLines = 1)
                Text("${snapshot.nextEntry.startTime.format(fmt)} - ${snapshot.nextEntry.endTime.format(fmt)}", style = TextStyle(color = WidgetMuted))
                if (!snapshot.nextEntry.venue.isNullOrBlank()) {
                    Text(snapshot.nextEntry.venue, style = TextStyle(color = WidgetAccent), maxLines = 1)
                }
            } else {
                Text("No more classes today", style = TextStyle(color = WidgetText))
            }
        }
        WidgetMode.DAILY_PROGRESS -> {
            val progress = snapshot.todayProgress
            if (progress.total == 0) {
                Text("Done 0/0", style = TextStyle(color = WidgetText))
                Spacer(GlanceModifier.height(4.dp))
                Text("No tasks due today", style = TextStyle(color = WidgetMuted))
            } else {
                Text("Done ${progress.completed}/${progress.total}", style = TextStyle(color = WidgetText))
                Spacer(GlanceModifier.height(4.dp))
                Text("Pending today: ${progress.pending}", style = TextStyle(color = WidgetMuted))
            }
        }
        WidgetMode.STREAK_WELLBEING -> {
            Text("Streak: ${snapshot.streakDays} days", style = TextStyle(color = WidgetText))
            val avg = if (snapshot.feedbackLast7.isEmpty()) 0 else snapshot.feedbackLast7.average().toInt()
            Text("Avg tasks/day (7d): $avg", style = TextStyle(color = WidgetMuted))
        }
        WidgetMode.SMART_SUGGESTION -> {
            val message = when {
                snapshot.priorities.isNotEmpty() -> "Start: ${snapshot.priorities.first().title}"
                snapshot.pendingCount > 0 -> "Pick one small task to gain momentum"
                else -> "Great pace. Plan tomorrow in 5 min"
            }
            Text(message, style = TextStyle(color = WidgetText), maxLines = 2)
        }
        WidgetMode.HEATMAP -> {
            val heat = snapshot.feedbackLast7.joinToString(" ") { count ->
                when {
                    count >= 5 -> "#"
                    count >= 3 -> "*"
                    count >= 1 -> "+"
                    else -> "."
                }
            }
            Text("7d activity", style = TextStyle(color = WidgetMuted))
            Text(heat, style = TextStyle(color = WidgetText))
            Text("Legend: . + * #", style = TextStyle(color = WidgetMuted))
        }
        WidgetMode.ACADEMIC -> {
            Text("Open academic tasks", style = TextStyle(color = WidgetMuted))
            Text(snapshot.academicOpenCount.toString(), style = TextStyle(color = WidgetAccent, fontWeight = FontWeight.Bold))
            snapshot.tasks.filter { !it.isCompleted && it.category == TaskCategory.ACADEMIC }.take(1).forEach {
                Text(it.title, style = TextStyle(color = WidgetText), maxLines = 1)
            }
        }
        WidgetMode.MOTIVATOR -> {
            val quotes = listOf(
                "Tiny progress still counts.",
                "One focused block beats multitasking.",
                "Start now. Perfect later.",
                "Do the next useful thing."
            )
            val idx = (System.currentTimeMillis() / 3_600_000L % quotes.size).toInt()
            Text(quotes[idx], style = TextStyle(color = WidgetText), maxLines = 2)
        }
        WidgetMode.MINI_QUICK_LOG -> {
            Text("Log", style = TextStyle(color = WidgetMuted))
            Text("Voice", style = TextStyle(color = WidgetAccent, fontWeight = FontWeight.Bold))
        }
        WidgetMode.MINI_STATUS -> {
            Text("Open", style = TextStyle(color = WidgetMuted))
            Text(snapshot.pendingCount.toString(), style = TextStyle(color = WidgetAccent, fontWeight = FontWeight.Bold))
        }
        WidgetMode.MINI_NEXT -> {
            if (snapshot.nextEntry == null) {
                Text("No class", style = TextStyle(color = WidgetText))
            } else {
                val fmt = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
                Text(snapshot.nextEntry.startTime.format(fmt), style = TextStyle(color = WidgetAccent, fontWeight = FontWeight.Bold))
                Text(snapshot.nextEntry.title, style = TextStyle(color = WidgetText), maxLines = 1)
            }
        }
    }

    if (mode != WidgetMode.MINI_QUICK_LOG && mode != WidgetMode.MINI_STATUS && mode != WidgetMode.MINI_NEXT) {
        Spacer(GlanceModifier.height(6.dp))
        Spacer(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(1.dp)
                .background(WidgetDivider)
        )
        Spacer(GlanceModifier.height(4.dp))
        Row {
            Text(LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())), style = TextStyle(color = WidgetMuted))
            Spacer(GlanceModifier.width(8.dp))
            Text("Tap to open", style = TextStyle(color = WidgetAccent))
        }
    }
}

private fun titleFor(mode: WidgetMode): String = when (mode) {
    WidgetMode.QUICK_ACTIONS -> "Quick Actions"
    WidgetMode.FOCUS_SHORTCUTS -> "Focus"
    WidgetMode.NEXT_CLASS -> "Next Class"
    WidgetMode.DAILY_PROGRESS -> "Progress"
    WidgetMode.STREAK_WELLBEING -> "Streak"
    WidgetMode.SMART_SUGGESTION -> "Smart Suggestion"
    WidgetMode.HEATMAP -> "Activity Heat"
    WidgetMode.ACADEMIC -> "Academic"
    WidgetMode.MOTIVATOR -> "Motivator"
    WidgetMode.MINI_QUICK_LOG -> "Mini Quick Log"
    WidgetMode.MINI_STATUS -> "Mini Status"
    WidgetMode.MINI_NEXT -> "Mini Next"
}

object WidgetRefreshCoordinator {
    suspend fun updateAllWidgets(context: Context) {
        val appContext = context.applicationContext
        TodayWidget().updateAll(appContext)
        QuickActionsWidget().updateAll(appContext)
        FocusShortcutsWidget().updateAll(appContext)
        NextClassWidget().updateAll(appContext)
        DailyProgressWidget().updateAll(appContext)
        StreakWellbeingWidget().updateAll(appContext)
        SmartSuggestionWidget().updateAll(appContext)
        HeatmapWidget().updateAll(appContext)
        AcademicWidget().updateAll(appContext)
        MotivatorWidget().updateAll(appContext)
        MiniQuickLogWidget().updateAll(appContext)
        MiniStatusWidget().updateAll(appContext)
        MiniNextWidget().updateAll(appContext)
        ConfigurableStackWidget().updateAll(appContext)
        RotatingStack15Widget().updateAll(appContext)
        RotatingStack30Widget().updateAll(appContext)
        RotatingStack60Widget().updateAll(appContext)
    }
}




