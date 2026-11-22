package com.alvin.neuromind.domain

import com.alvin.neuromind.data.Task
import com.alvin.neuromind.data.TimetableEntry
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** "This deadline needs more time than you have before it." */
data class OverloadWarning(
    val task: Task,
    val due: LocalDateTime,
    /** Work (and revision) due by then, minutes. */
    val neededMinutes: Int,
    /** Free time between now and then, minutes. */
    val freeMinutes: Int
)

/**
 * Early warning for crunches: for each open task due in the next [horizonDays],
 * in deadline order, compare everything due by then (remaining work, errands,
 * revision planned in that time) with the free time before it.
 *
 * Free time = the [dayStart]–[dayEnd] window around classes and events, from
 * now on, at most [studyMinutesPerDay] a day (nobody works every free minute).
 * Errands are deadlines, not fixed slots: their time counts against the day,
 * but they aren't pinned to a time.
 */
object Workload {

    fun warnings(
        tasks: List<Task>,
        timetable: List<TimetableEntry>,
        now: LocalDateTime,
        revision: RevisionPlan? = null,
        focusedMinutes: Map<Int, Int> = emptyMap(),
        horizonDays: Long = 3,
        dayStart: LocalTime = LocalTime.of(8, 0),
        dayEnd: LocalTime = LocalTime.of(21, 0),
        studyMinutesPerDay: Int = 360,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<OverloadWarning> {
        val horizonEnd = now.toLocalDate().plusDays(horizonDays).atTime(LocalTime.MAX)
        val due = tasks
            .filter { !it.isCompleted && it.dueDate != null }
            .map { it to Instant.ofEpochMilli(it.dueDate!!).atZone(zone).toLocalDateTime() }
            .filter { (_, at) -> at.isAfter(now) && !at.isAfter(horizonEnd) }
            .sortedBy { it.second }

        var needed = 0
        val out = mutableListOf<OverloadWarning>()
        for ((task, at) in due) {
            needed += (task.durationMinutes - (focusedMinutes[task.id] ?: 0)).coerceAtLeast(0)
            val revisionBefore = revision?.sessions
                ?.filter { !it.date.isBefore(now.toLocalDate()) && it.date.isBefore(at.toLocalDate()) }
                ?.sumOf { it.minutes } ?: 0
            val free = freeMinutes(timetable, now, at, dayStart, dayEnd, studyMinutesPerDay)
            if (needed + revisionBefore > free) {
                out += OverloadWarning(task, at, needed + revisionBefore, free)
            }
        }
        // One warning per crunch is enough: keep the earliest deadline that overflows.
        return out.take(1)
    }

    /** Usable free minutes from [from] to [to], per day capped at [cap]. */
    fun freeMinutes(
        timetable: List<TimetableEntry>,
        from: LocalDateTime,
        to: LocalDateTime,
        dayStart: LocalTime,
        dayEnd: LocalTime,
        cap: Int
    ): Int {
        var total = 0
        var date = from.toLocalDate()
        while (!date.isAfter(to.toLocalDate())) {
            val start = if (date == from.toLocalDate()) maxOf(dayStart, from.toLocalTime()) else dayStart
            val end = if (date == to.toLocalDate()) minOf(dayEnd, to.toLocalTime()) else dayEnd
            if (end.isAfter(start)) {
                val day = date
                val entries = timetable.filter { it.occursOn(day) }
                if (entries.none { it.isAllDay && !it.isRecurring }) {
                    val busy = entries.filter { !it.isAllDay }.map { it.startTime to it.endTime }
                    total += minOf(cap, free(start, end, busy))
                }
            }
            date = date.plusDays(1)
        }
        return total
    }

    private fun free(from: LocalTime, to: LocalTime, busy: List<Pair<LocalTime, LocalTime>>): Int {
        var taken = 0L
        var cursor = from
        for ((a, b) in busy.sortedBy { it.first }) {
            val s = maxOf(a, cursor)
            val e = minOf(b, to)
            if (e.isAfter(s)) { taken += ChronoUnit.MINUTES.between(s, e); cursor = e }
        }
        return (ChronoUnit.MINUTES.between(from, to) - taken).toInt().coerceAtLeast(0)
    }
}
