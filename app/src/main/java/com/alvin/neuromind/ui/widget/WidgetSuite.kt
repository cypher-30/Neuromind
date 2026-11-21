package com.alvin.neuromind.ui.widget

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
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
import com.alvin.neuromind.R
import com.alvin.neuromind.data.NeuromindApplication
import com.alvin.neuromind.data.Task
import com.alvin.neuromind.domain.DailyProgressCalculator
import com.alvin.neuromind.domain.Occurrence
import com.alvin.neuromind.domain.WidgetInsights
import com.alvin.neuromind.navigation.AppRouteIntents
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Six widgets (cut from 17 on 2026-11-22): Today, Next up, Daily Progress,
// Activity Heat, Exam Countdown and Mini Quick Log. Receiver class names of
// the kept ones are unchanged so widgets already on a home screen keep
// working. Each widget is responsive: resizing it on the home screen switches
// between a compact (2x1), medium (3x2) and wide (4x2+) layout.
// Shared palette: WidgetColors.kt. Logic: domain/WidgetInsights.kt.

// --- Sizes the launcher can pick from (Glance uses the largest that fits) ---
private val COMPACT = DpSize(110.dp, 48.dp)
private val MEDIUM = DpSize(170.dp, 100.dp)
private val WIDE = DpSize(250.dp, 100.dp)
private val LARGE = DpSize(250.dp, 170.dp)
private val RESPONSIVE = SizeMode.Responsive(setOf(COMPACT, MEDIUM, WIDE, LARGE))

private enum class Shape { COMPACT, MEDIUM, WIDE, LARGE }

@Composable
private fun shape(): Shape {
    val size = LocalSize.current
    return when {
        size.height < 90.dp -> Shape.COMPACT
        size.width >= 240.dp && size.height >= 160.dp -> Shape.LARGE
        size.width >= 240.dp -> Shape.WIDE
        else -> Shape.MEDIUM
    }
}

private val timeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
private val dayFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())

private fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Today"
    today.plusDays(1) -> "Tomorrow"
    else -> date.format(dayFmt)
}

private fun dueLabel(task: Task, today: LocalDate): String {
    val due = Instant.ofEpochMilli(task.dueDate ?: return "").atZone(ZoneId.systemDefault()).toLocalDateTime()
    return "${dayLabel(due.toLocalDate(), today)} ${due.toLocalTime().format(timeFmt)}"
}

private fun title(size: Int = 11) = TextStyle(color = WidgetAccent, fontWeight = FontWeight.Bold, fontSize = size.sp)
private fun body(size: Int = 14, bold: Boolean = false) =
    TextStyle(color = WidgetText, fontSize = size.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
private fun muted(size: Int = 12) = TextStyle(color = WidgetMuted, fontSize = size.sp)

/** Shell shared by every widget: rounded card, small accent heading, tap action. */
@Composable
private fun WidgetCard(heading: String?, onClick: Action, content: @Composable () -> Unit) {
    val compact = shape() == Shape.COMPACT
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(20.dp)
            .background(WidgetBg)
            .padding(horizontal = 14.dp, vertical = if (compact) 8.dp else 12.dp)
            .clickable(onClick),
        verticalAlignment = if (compact) Alignment.CenterVertically else Alignment.Top
    ) {
        if (heading != null && !compact) {
            Text(heading.uppercase(), style = title())
            Spacer(GlanceModifier.height(6.dp))
        }
        // Own container: Glance drops children past 10 in one Column.
        Column(GlanceModifier.fillMaxWidth().defaultWeight()) { content() }
    }
}

private fun openApp(): Action = actionStartActivity<MainActivity>()

/** Opens the Timetable, with the event's editor on top when it's a dated event. */
private fun openEntry(context: Context, occurrence: Occurrence?): Action {
    val entry = occurrence?.entry ?: return openApp()
    return androidx.glance.appwidget.action.actionStartActivity(
        AppRouteIntents.openEvent(context, if (entry.isRecurring) 0 else entry.id)
    )
}

/** Runs a widget body; on any failure (data or render) shows a plain "open" card instead of a broken widget. */
private suspend fun GlanceAppWidget.safely(name: String, block: suspend () -> Unit) {
    try {
        block()
    } catch (t: Throwable) {
        Log.e("NeuromindWidget", "$name widget failed", t)
        provideContent { WidgetCard(null, openApp()) { Text("Tap to open Neuromind", style = body()) } }
    }
}

/**
 * Centred icon + title + hint, so an empty widget looks intentional instead of
 * a big card with one stray line. Compact sizes get a single icon-and-title row.
 */
@Composable
private fun EmptyState(icon: Int, headline: String, hint: String) {
    if (shape() == Shape.COMPACT) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(ImageProvider(icon), contentDescription = null, modifier = GlanceModifier.size(18.dp), colorFilter = ColorFilter.tint(WidgetAccent))
            Spacer(GlanceModifier.width(8.dp))
            Text(headline, style = body(13, bold = true), maxLines = 1)
        }
        return
    }
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(ImageProvider(icon), contentDescription = null, modifier = GlanceModifier.size(28.dp), colorFilter = ColorFilter.tint(WidgetAccent))
        Spacer(GlanceModifier.height(6.dp))
        Text(headline, style = body(14, bold = true), maxLines = 1)
        Text(hint, style = muted(12), maxLines = 2)
    }
}

@Composable
private fun Divider() {
    Spacer(GlanceModifier.height(6.dp))
    Spacer(GlanceModifier.fillMaxWidth().height(1.dp).background(WidgetDivider))
    Spacer(GlanceModifier.height(6.dp))
}

/** Title, time line and short venue for one class/event. */
@Composable
private fun OccurrenceBlock(o: Occurrence, today: LocalDate, big: Boolean) {
    Text(o.entry.title.trim(), style = body(if (big) 16 else 13, bold = big), maxLines = if (big) 2 else 1)
    Text(
        "${dayLabel(o.date, today)} · ${o.entry.startTime.format(timeFmt)} – ${o.entry.endTime.format(timeFmt)}",
        style = muted(if (big) 13 else 11), maxLines = 1
    )
    // Online classes keep Teams links in the venue/notes; only show a short, plain venue.
    o.entry.venue?.takeIf { it.isNotBlank() && it.length <= 30 && !it.contains("http") && !it.startsWith("ID:") }?.let {
        Text(it, style = TextStyle(color = WidgetAccent, fontSize = (if (big) 13 else 11).sp), maxLines = 1)
    }
}

// ---------------------------------------------------------------------------
// Today: priorities + next class
// ---------------------------------------------------------------------------
class TodayWidget : GlanceAppWidget() {
    override val sizeMode = RESPONSIVE

    override suspend fun provideGlance(context: Context, id: GlanceId) = safely("Today") {
        val repo = (context.applicationContext as NeuromindApplication).repository
        val now = LocalDateTime.now()
        val priorities = repo.getWidgetTasks(nowMillis = System.currentTimeMillis(), limit = 5)
        val next = WidgetInsights.nextUp(repo.allTimetableEntries.first(), now)
        provideContent {
            val today = now.toLocalDate()
            WidgetCard("Priorities", openApp()) {
                if (priorities.isEmpty() && next == null) {
                    EmptyState(R.drawable.ic_widget_check, "All clear", "No urgent tasks or classes coming up")
                } else when (shape()) {
                    Shape.COMPACT -> Text(priorities.firstOrNull()?.title ?: "No urgent priorities", style = body(14, bold = true), maxLines = 2)
                    Shape.WIDE, Shape.LARGE -> Row(GlanceModifier.fillMaxSize()) {
                        Column(GlanceModifier.defaultWeight()) {
                            PriorityList(priorities, if (shape() == Shape.LARGE) 5 else 3)
                        }
                        Spacer(GlanceModifier.width(12.dp))
                        Column(GlanceModifier.defaultWeight()) {
                            Text("NEXT", style = title(10))
                            if (next != null) OccurrenceBlock(next, today, big = false)
                            else Text("Nothing this week", style = muted())
                        }
                    }
                    Shape.MEDIUM -> {
                        PriorityList(priorities, 3)
                        Divider()
                        Text(
                            next?.let { "Next: ${it.entry.title.trim()} · ${dayLabel(it.date, today)} ${it.entry.startTime.format(timeFmt)}" }
                                ?: "Next: nothing this week",
                            style = TextStyle(color = WidgetAccent, fontSize = 12.sp), maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PriorityList(tasks: List<Task>, max: Int) {
    if (tasks.isEmpty()) {
        Text("No urgent priorities", style = body())
        return
    }
    Column {
        tasks.take(max).forEach { task ->
            Row(GlanceModifier.padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(GlanceModifier.size(6.dp).cornerRadius(3.dp).background(if (task.isOverdue) WidgetDanger else WidgetAccent)) {}
                Spacer(GlanceModifier.width(8.dp))
                Text(task.title, style = body(13), maxLines = 1)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Next up: the next class or timed event (and the one after, when wide)
// ---------------------------------------------------------------------------
class NextClassWidget : GlanceAppWidget() {
    override val sizeMode = RESPONSIVE

    override suspend fun provideGlance(context: Context, id: GlanceId) = safely("Next up") {
        val entries = (context.applicationContext as NeuromindApplication).repository.allTimetableEntries.first()
        val now = LocalDateTime.now()
        val coming = WidgetInsights.upcoming(entries, now, count = 3)
        provideContent {
            val today = now.toLocalDate()
            val next = coming.firstOrNull()
            WidgetCard("Next up", openEntry(context, next)) {
                when {
                    next == null -> EmptyState(R.drawable.ic_widget_calendar, "Nothing scheduled", "No classes or events in the next week")
                    shape() == Shape.COMPACT -> {
                        Text(next.entry.title.trim(), style = body(14, bold = true), maxLines = 1)
                        Text("${dayLabel(next.date, today)} · ${next.entry.startTime.format(timeFmt)}", style = muted(), maxLines = 1)
                    }
                    shape() == Shape.MEDIUM -> OccurrenceBlock(next, today, big = true)
                    else -> Row(GlanceModifier.fillMaxSize()) {
                        Column(GlanceModifier.defaultWeight()) { OccurrenceBlock(next, today, big = true) }
                        Spacer(GlanceModifier.width(12.dp))
                        Column(GlanceModifier.defaultWeight()) {
                            Text("THEN", style = title(10))
                            Spacer(GlanceModifier.height(2.dp))
                            val later = coming.drop(1).take(if (shape() == Shape.LARGE) 2 else 1)
                            if (later.isEmpty()) Text("Nothing else this week", style = muted())
                            later.forEach { OccurrenceBlock(it, today, big = false); Spacer(GlanceModifier.height(4.dp)) }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Daily Progress: today's due tasks with a bar; shows what's next when empty
// ---------------------------------------------------------------------------
class DailyProgressWidget : GlanceAppWidget() {
    override val sizeMode = RESPONSIVE

    override suspend fun provideGlance(context: Context, id: GlanceId) = safely("Progress") {
        val tasks = (context.applicationContext as NeuromindApplication).repository.allTasks.first()
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val progress = DailyProgressCalculator.forDate(tasks, today)
        val startOfToday = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val startOfTomorrow = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val openToday = tasks.filter { !it.isCompleted && it.dueDate != null && it.dueDate in startOfToday until startOfTomorrow }
            .sortedBy { it.dueDate }
        val nextDue = WidgetInsights.nextDue(tasks, System.currentTimeMillis(), 3)
        provideContent {
            WidgetCard("Today's progress", openApp()) {
                val s = shape()
                if (progress.total == 0 && nextDue.isEmpty()) {
                    EmptyState(R.drawable.ic_widget_check, "Nothing due today", "Tasks with a due date show up here")
                } else if (progress.total == 0) {
                    Text("No tasks due today", style = body(if (s == Shape.COMPACT) 13 else 15, bold = s != Shape.COMPACT))
                    if (s != Shape.COMPACT && nextDue.isNotEmpty()) {
                        Divider()
                        Text("COMING UP", style = title(10))
                        nextDue.take(if (s == Shape.MEDIUM) 2 else 3).forEach { task ->
                            Text("${task.title} · ${dueLabel(task, today)}", style = body(12), maxLines = 1)
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${progress.completed}", style = body(if (s == Shape.COMPACT) 20 else 28, bold = true))
                        Text(" of ${progress.total} done", style = muted(14))
                    }
                    Spacer(GlanceModifier.height(if (s == Shape.COMPACT) 4.dp else 8.dp))
                    LinearProgressIndicator(
                        progress = progress.completed.toFloat() / progress.total,
                        modifier = GlanceModifier.fillMaxWidth().height(8.dp),
                        color = WidgetAccent,
                        backgroundColor = WidgetDivider
                    )
                    if (s != Shape.COMPACT) {
                        Spacer(GlanceModifier.height(6.dp))
                        if (openToday.isEmpty()) Text("All done for today", style = muted())
                        openToday.take(if (s == Shape.LARGE) 4 else if (s == Shape.WIDE) 2 else 1).forEach { task ->
                            Text("${task.title} · ${dueLabel(task, today).substringAfter(' ')}", style = body(12), maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Activity Heat: tasks finished per day; the grid grows with the widget
// ---------------------------------------------------------------------------
class HeatmapWidget : GlanceAppWidget() {
    override val sizeMode = RESPONSIVE

    override suspend fun provideGlance(context: Context, id: GlanceId) = safely("Activity") {
        val tasks = (context.applicationContext as NeuromindApplication).repository.allTasks.first()
        val today = LocalDate.now()
        // Enough history for the largest layout; each layout shows its tail.
        val counts = WidgetInsights.completionsByDay(tasks, today, 7 * 16)
        provideContent {
            val s = shape()
            // Rows are weeks (7 days each, today bottom-right). Wide layouts show 2 weeks per row.
            val perRow = if (s == Shape.WIDE || s == Shape.LARGE) 14 else 7
            val rows = when (s) { Shape.COMPACT -> 2; Shape.MEDIUM, Shape.WIDE -> 4; Shape.LARGE -> 8 }
            val cell = if (perRow == 14) 11.dp else 14.dp
            val gap = if (perRow == 14) 3.dp else 4.dp
            val shown = counts.takeLast(perRow * rows)
            val weeks = shown.size / 7
            WidgetCard("Activity · $weeks weeks", openApp()) {
                // Glance drops children past 10 per Row/Column, so gaps are padding
                // (not Spacers) and a 14-day row is two 7-day rows side by side.
                if (shown.sum() == 0) {
                    EmptyState(R.drawable.ic_widget_spark, "No activity yet", "Tick off tasks and this fills in day by day")
                } else Column {
                    shown.chunked(perRow).forEach { row ->
                        Row(GlanceModifier.padding(bottom = gap)) {
                            row.chunked(7).forEach { week ->
                                Row {
                                    week.forEach { count ->
                                        Box(GlanceModifier.padding(end = gap)) {
                                            Box(GlanceModifier.size(cell).cornerRadius(3.dp).background(heatColor(count))) {}
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (s != Shape.COMPACT && shown.sum() > 0) {
                    Text("${shown.sum()} tasks done", style = muted(), maxLines = 1)
                }
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

// ---------------------------------------------------------------------------
// Exam Countdown: next exam large, the ones after it small
// ---------------------------------------------------------------------------
class ExamCountdownWidget : GlanceAppWidget() {
    override val sizeMode = RESPONSIVE

    override suspend fun provideGlance(context: Context, id: GlanceId) = safely("Exams") {
        val entries = (context.applicationContext as NeuromindApplication).repository.allTimetableEntries.first()
        val exams = WidgetInsights.upcomingExams(entries, LocalDateTime.now())
        provideContent {
            val first = exams.firstOrNull()
            WidgetCard("Next exam", openEntry(context, first?.let { Occurrence(it.entry, it.date) })) {
                val s = shape()
                when {
                    first == null -> EmptyState(R.drawable.ic_widget_calendar, "No exams coming up", "Add an event with \"exam\" in its title")
                    s == Shape.COMPACT -> {
                        Text(first.name, style = body(14, bold = true), maxLines = 1)
                        Text(WidgetInsights.daysLabel(first.daysLeft), style = TextStyle(color = WidgetAccent, fontSize = 12.sp), maxLines = 1)
                    }
                    s == Shape.MEDIUM -> {
                        ExamHeadline(first)
                        exams.drop(1).take(2).forEach { exam ->
                            Spacer(GlanceModifier.height(3.dp))
                            Text("${exam.name} · ${exam.date.format(dayFmt)}", style = body(12), maxLines = 1)
                        }
                    }
                    else -> Row(GlanceModifier.fillMaxSize()) {
                        // Big day count on the left, the rest of the run on the right.
                        Column(GlanceModifier.width(76.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                if (first.daysLeft == 0L) "Today" else "${first.daysLeft}",
                                style = TextStyle(color = WidgetAccent, fontWeight = FontWeight.Bold, fontSize = if (first.daysLeft == 0L) 22.sp else 40.sp)
                            )
                            if (first.daysLeft > 0) Text(if (first.daysLeft == 1L) "day" else "days", style = muted())
                        }
                        Spacer(GlanceModifier.width(10.dp))
                        Column(GlanceModifier.defaultWeight()) {
                            Text(first.name, style = body(15, bold = true), maxLines = 1)
                            Text("${first.date.format(dayFmt)}, ${first.entry.startTime.format(timeFmt)}", style = muted(), maxLines = 1)
                            first.entry.venue?.takeIf { it.isNotBlank() }?.let { Text(it, style = TextStyle(color = WidgetAccent, fontSize = 12.sp), maxLines = 1) }
                            Spacer(GlanceModifier.height(4.dp))
                            exams.drop(1).take(if (s == Shape.LARGE) 5 else 2).forEach { exam ->
                                Text("${exam.name} · ${exam.date.format(dayFmt)}", style = body(12), maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExamHeadline(exam: com.alvin.neuromind.domain.UpcomingExam) {
    Text(exam.name, style = body(16, bold = true), maxLines = 1)
    Text(
        "${WidgetInsights.daysLabel(exam.daysLeft)} · ${exam.date.format(dayFmt)}, ${exam.entry.startTime.format(timeFmt)}",
        style = TextStyle(color = WidgetAccent, fontSize = 13.sp, fontWeight = FontWeight.Medium), maxLines = 1
    )
    exam.entry.venue?.takeIf { it.isNotBlank() }?.let { Text(it, style = muted(), maxLines = 1) }
}

// ---------------------------------------------------------------------------
// Mini Quick Log (1x1): one tap into voice logging
// ---------------------------------------------------------------------------
class MiniQuickLogWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) = safely("Log") {
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
                    Text("Log", style = title(16))
                    Text("voice", style = muted(11))
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
