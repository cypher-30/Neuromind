package com.alvin.neuromind.domain

import com.alvin.neuromind.data.Task
import com.alvin.neuromind.data.TimetableEntry
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** One upcoming occurrence of a class or event. */
data class Occurrence(val entry: TimetableEntry, val date: LocalDate) {
    val start: LocalDateTime get() = date.atTime(entry.startTime)
}

/** An exam with a cleaned-up name and whole days remaining. */
data class UpcomingExam(val entry: TimetableEntry, val date: LocalDate, val name: String, val daysLeft: Long)

/** Pure helpers behind the home-screen widgets (kept out of Glance code so they can be tested). */
object WidgetInsights {

    /** The next [count] classes or timed events that haven't started yet, up to [lookaheadDays] ahead. */
    fun upcoming(entries: List<TimetableEntry>, now: LocalDateTime, count: Int, lookaheadDays: Int = 7): List<Occurrence> =
        (0..lookaheadDays).asSequence()
            .map { now.toLocalDate().plusDays(it.toLong()) }
            .flatMap { date ->
                entries.filter { it.occursOn(date) && !it.isAllDay }
                    .map { Occurrence(it, date) }
                    .sortedBy { it.start }
            }
            .filter { it.start.isAfter(now) }
            .take(count)
            .toList()

    /** The next class or timed event that hasn't started yet. */
    fun nextUp(entries: List<TimetableEntry>, now: LocalDateTime, lookaheadDays: Int = 7): Occurrence? =
        upcoming(entries, now, 1, lookaheadDays).firstOrNull()

    /** Open tasks with a due time still ahead, soonest first. */
    fun nextDue(tasks: List<Task>, nowMillis: Long, count: Int): List<Task> =
        tasks.filter { !it.isCompleted && it.dueDate != null && it.dueDate >= nowMillis }
            .sortedBy { it.dueDate }
            .take(count)

    private val examPrefix = Regex("""^\s*exam\s*[:\-–]\s*""", RegexOption.IGNORE_CASE)
    private val unitCode = Regex("""^[A-Z]{3}\s?\d{4}\s+""")

    /** Dated events whose title mentions "exam", still to come, soonest first. */
    fun upcomingExams(entries: List<TimetableEntry>, now: LocalDateTime): List<UpcomingExam> =
        entries
            .filter { !it.isRecurring && it.date != null && it.title.contains("exam", ignoreCase = true) }
            .filter { it.date!!.atTime(it.endTime).isAfter(now) }
            .sortedWith(compareBy({ it.date }, { it.startTime }))
            .map { entry ->
                val name = entry.title.replace(examPrefix, "").replace(unitCode, "").trim().ifEmpty { entry.title }
                UpcomingExam(entry, entry.date!!, name, ChronoUnit.DAYS.between(now.toLocalDate(), entry.date))
            }

    /** Tasks completed on each of the last [days] days (oldest first), from Task.completedAt. */
    fun completionsByDay(tasks: List<Task>, today: LocalDate, days: Int, zone: ZoneId = ZoneId.systemDefault()): List<Int> {
        val counts = tasks
            .mapNotNull { it.completedAt }
            .map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
            .groupingBy { it }
            .eachCount()
        return (days - 1 downTo 0).map { counts[today.minusDays(it.toLong())] ?: 0 }
    }

    /** "Today", "Tomorrow" or "in N days". */
    fun daysLabel(daysLeft: Long): String = when (daysLeft) {
        0L -> "Today"
        1L -> "Tomorrow"
        else -> "in $daysLeft days"
    }
}
