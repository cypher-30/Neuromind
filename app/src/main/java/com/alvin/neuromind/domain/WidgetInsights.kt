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

    /** The next class or timed event that hasn't started yet, looking up to [lookaheadDays] ahead. */
    fun nextUp(entries: List<TimetableEntry>, now: LocalDateTime, lookaheadDays: Int = 7): Occurrence? {
        for (offset in 0..lookaheadDays) {
            val date = now.toLocalDate().plusDays(offset.toLong())
            val next = entries
                .filter { it.occursOn(date) && !it.isAllDay }
                .map { Occurrence(it, date) }
                .filter { it.start.isAfter(now) }
                .minByOrNull { it.start }
            if (next != null) return next
        }
        return null
    }

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
