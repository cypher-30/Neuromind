package com.alvin.neuromind.ui.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
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
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.alvin.neuromind.MainActivity
import com.alvin.neuromind.QuickLogEntryActivity
import com.alvin.neuromind.data.NeuromindApplication
import com.alvin.neuromind.domain.DailyProgress
import com.alvin.neuromind.domain.DailyProgressCalculator
import com.alvin.neuromind.domain.Occurrence
import com.alvin.neuromind.domain.UpcomingExam
import com.alvin.neuromind.domain.WidgetInsights
import com.alvin.neuromind.navigation.AppRouteIntents
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// Six widgets (cut from 17 on 2026-11-22): Today (TodayWidget.kt), Next up,
// Daily Progress, Activity Heat, Exam Countdown and Mini Quick Log. The
// receiver class names of the kept ones are unchanged so widgets already on a
// home screen keep working. Shared palette: WidgetColors.kt.

private const val HEAT_DAYS = 28

private val timeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
private val dayFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())

/** Shell shared by every widget: rounded card, small accent title, tap action. */
@Composable
private fun WidgetCard(title: String, onClick: Action, content: @Composable () -> Unit) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(20.dp)
            .background(WidgetBg)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .clickable(onClick)
    ) {
        Text(title.uppercase(), style = TextStyle(color = WidgetAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp))
        Spacer(GlanceModifier.height(6.dp))
        content()
    }
}

private fun openApp(): Action = actionStartActivity<MainActivity>()

/** Opens the Timetable, with the event's editor on top when it's a dated event. */
private fun openEntry(context: Context, occurrence: Occurrence?): Action {
    val entry = occurrence?.entry ?: return openApp()
    val intent: Intent = AppRouteIntents.openEvent(context, if (entry.isRecurring) 0 else entry.id)
    return androidx.glance.appwidget.action.actionStartActivity(intent)
}

/** Runs a widget body; on any failure (data or render) shows a plain "open" card instead of a broken widget. */
private suspend fun GlanceAppWidget.safely(name: String, body: suspend () -> Unit) {
    try {
        body()
    } catch (t: Throwable) {
        Log.e("NeuromindWidget", "$name widget failed", t)
        provideContent { WidgetCard("Neuromind", openApp()) { Text("Tap to open Neuromind", style = TextStyle(color = WidgetText)) } }
    }
}

// --- Next up: the next class or timed event, today or later this week ---
class NextClassWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) = safely(javaClass.simpleName) {
        val entries = (context.applicationContext as NeuromindApplication).repository.allTimetableEntries.first()
        val now = LocalDateTime.now()
        val next = WidgetInsights.nextUp(entries, now)
        provideContent { val ctx = context
            WidgetCard("Next up", openEntry(ctx, next)) {
                if (next == null) {
                    Text("Nothing scheduled this week", style = TextStyle(color = WidgetText, fontSize = 15.sp))
                } else {
                    val whenLabel = when (next.date) {
                        now.toLocalDate() -> "Today"
                        now.toLocalDate().plusDays(1) -> "Tomorrow"
                        else -> next.date.format(dayFmt)
                    }
                    Text(next.entry.title.trim(), style = TextStyle(color = WidgetText, fontWeight = FontWeight.Bold, fontSize = 16.sp), maxLines = 2)
                    Spacer(GlanceModifier.height(4.dp))
                    Text(
                        "$whenLabel · ${next.entry.startTime.format(timeFmt)} – ${next.entry.endTime.format(timeFmt)}",
                        style = TextStyle(color = WidgetMuted, fontSize = 13.sp),
                        maxLines = 1
                    )
                    // Teams links live in the venue/notes for online classes; show only a short venue.
                    next.entry.venue?.takeIf { it.isNotBlank() && it.length <= 30 && !it.contains("http") }?.let {
                        Text(it, style = TextStyle(color = WidgetAccent, fontSize = 13.sp), maxLines = 1)
                    }
                }
            }
        }
    }
}

// --- Daily Progress: today's due tasks with a bar ---
class DailyProgressWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) = safely(javaClass.simpleName) {
        val tasks = (context.applicationContext as NeuromindApplication).repository.allTasks.first()
        val progress: DailyProgress = DailyProgressCalculator.forDate(tasks, LocalDate.now())
        provideContent {
            WidgetCard("Today's progress", openApp()) {
                if (progress.total == 0) {
                    Text("No tasks due today", style = TextStyle(color = WidgetText, fontSize = 15.sp))
                } else {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${progress.completed}", style = TextStyle(color = WidgetText, fontWeight = FontWeight.Bold, fontSize = 28.sp))
                        Text(" of ${progress.total} done", style = TextStyle(color = WidgetMuted, fontSize = 14.sp))
                    }
                    Spacer(GlanceModifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = progress.completed.toFloat() / progress.total,
                        modifier = GlanceModifier.fillMaxWidth().height(8.dp),
                        color = WidgetAccent,
                        backgroundColor = WidgetDivider
                    )
                    Spacer(GlanceModifier.height(6.dp))
                    Text(
                        if (progress.pending == 0) "All done for today" else "${progress.pending} left",
                        style = TextStyle(color = WidgetMuted, fontSize = 12.sp)
                    )
                }
            }
        }
    }
}

// --- Activity Heat: tasks finished per day, last 4 weeks, today bottom-right ---
class HeatmapWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) = safely(javaClass.simpleName) {
        val tasks = (context.applicationContext as NeuromindApplication).repository.allTasks.first()
        val counts = WidgetInsights.completionsByDay(tasks, LocalDate.now(), HEAT_DAYS)
        val total = counts.sum()
        provideContent {
            WidgetCard("Activity · 4 weeks", openApp()) {
                Column {
                    counts.chunked(7).forEach { week ->
                        Row {
                            week.forEach { count ->
                                Box(GlanceModifier.size(14.dp).cornerRadius(3.dp).background(heatColor(count))) {}
                                Spacer(GlanceModifier.width(4.dp))
                            }
                        }
                        Spacer(GlanceModifier.height(4.dp))
                    }
                }
                Text(
                    if (total == 0) "Fills in as you tick off tasks" else "$total tasks done",
                    style = TextStyle(color = WidgetMuted, fontSize = 12.sp),
                    maxLines = 1
                )
            }
        }
    }
}

private fun heatColor(count: Int): ColorProvider = when {
    count >= 4 -> Heat3
    count >= 2 -> Heat2
    count >= 1 -> Heat1
    else -> Heat0
}

// --- Exam Countdown: next exam large, the two after it small ---
class ExamCountdownWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) = safely(javaClass.simpleName) {
        val entries = (context.applicationContext as NeuromindApplication).repository.allTimetableEntries.first()
        val exams: List<UpcomingExam> = WidgetInsights.upcomingExams(entries, LocalDateTime.now())
        provideContent { val ctx = context
            val first = exams.firstOrNull()
            WidgetCard("Next exam", openEntry(ctx, first?.let { Occurrence(it.entry, it.date) })) {
                if (first == null) {
                    Text("No exams coming up", style = TextStyle(color = WidgetText, fontSize = 15.sp))
                    Text("Add an event with \"exam\" in its title", style = TextStyle(color = WidgetMuted, fontSize = 12.sp))
                } else {
                    Text(first.name, style = TextStyle(color = WidgetText, fontWeight = FontWeight.Bold, fontSize = 16.sp), maxLines = 1)
                    Text(
                        "${WidgetInsights.daysLabel(first.daysLeft)} · ${first.date.format(dayFmt)}, ${first.entry.startTime.format(timeFmt)}",
                        style = TextStyle(color = WidgetAccent, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                        maxLines = 1
                    )
                    first.entry.venue?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = TextStyle(color = WidgetMuted, fontSize = 12.sp), maxLines = 1)
                    }
                    exams.drop(1).take(2).forEach { exam ->
                        Spacer(GlanceModifier.height(4.dp))
                        Text(
                            "${exam.name} · ${exam.date.format(dayFmt)}",
                            style = TextStyle(color = WidgetText, fontSize = 12.sp),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// --- Mini Quick Log (1x1): one tap into voice logging ---
class MiniQuickLogWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) = safely(javaClass.simpleName) {
        provideContent {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(20.dp)
                    .background(WidgetBg)
                    .clickable(actionStartActivity<QuickLogEntryActivity>()),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Log", style = TextStyle(color = WidgetAccent, fontWeight = FontWeight.Bold, fontSize = 16.sp))
                    Text("voice", style = TextStyle(color = WidgetMuted, fontSize = 11.sp))
                }
            }
        }
    }
}

object WidgetRefreshCoordinator {
    suspend fun updateAllWidgets(context: Context) {
        val appContext = context.applicationContext
        TodayWidget().updateAll(appContext)
        NextClassWidget().updateAll(appContext)
        DailyProgressWidget().updateAll(appContext)
        HeatmapWidget().updateAll(appContext)
        ExamCountdownWidget().updateAll(appContext)
        MiniQuickLogWidget().updateAll(appContext)
    }
}
