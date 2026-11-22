package com.alvin.neuromind.domain

import com.alvin.neuromind.data.FocusSession
import com.alvin.neuromind.data.Task
import java.time.Instant
import java.time.ZoneId

/** A focus window learned from when the student actually does deep work. */
data class LearnedPeak(val startHour: Int, val endHour: Int, val sessions: Int, val shareOfFocus: Double)

/** "Your [subject] tasks take about [ratio]× your estimate." */
data class EstimateBias(val subject: String, val ratio: Double, val tasks: Int)

/**
 * Learns from the student's own history, offline. Uses Focus sessions, not
 * when a task was ticked off: Focus sessions are logged the moment they end,
 * while tasks are sometimes marked done long after they were finished.
 *
 * Small samples lie, so everything needs a minimum number of data points,
 * uses medians, and is clamped; results are shown as suggestions, never
 * applied silently.
 */
object Learning {

    /**
     * The [width]-hour window (between 6:00 and 23:00) holding the most focus
     * minutes, from at least [minSessions] sessions. Null if there isn't
     * enough data or focus is spread too evenly to call a peak.
     */
    fun peakWindow(
        sessions: List<FocusSession>,
        width: Int = 3,
        minSessions: Int = 5,
        zone: ZoneId = ZoneId.systemDefault()
    ): LearnedPeak? {
        if (sessions.size < minSessions) return null
        val minutesByHour = IntArray(24)
        for (s in sessions) {
            // Spread each session over the hours it covered (it ends at completedAt).
            var end = Instant.ofEpochMilli(s.completedAt).atZone(zone).toLocalDateTime()
            var left = s.durationMinutes.coerceAtLeast(0)
            while (left > 0) {
                val inThisHour = minOf(left, end.minute.takeIf { it > 0 } ?: 60)
                minutesByHour[(if (end.minute == 0) end.minusHours(1) else end).hour] += inThisHour
                left -= inThisHour
                end = end.minusMinutes(inThisHour.toLong())
            }
        }
        val total = minutesByHour.sum().takeIf { it > 0 } ?: return null
        val best = (6..(23 - width)).maxByOrNull { start -> (start until start + width).sumOf { minutesByHour[it] } } ?: return null
        val share = (best until best + width).sumOf { minutesByHour[it] }.toDouble() / total
        // A real peak holds clearly more than an even spread would (width/17 of the day).
        if (share < 0.35) return null
        return LearnedPeak(best, best + width, sessions.size, share)
    }

    /**
     * Per subject: median of (Focus minutes ÷ estimate) over finished tasks that
     * had Focus time, with at least [minTasks] tasks. Only reports a bias that
     * matters (off by 25%+), clamped to 0.5–2×.
     */
    fun estimateBias(
        tasks: List<Task>,
        sessions: List<FocusSession>,
        minTasks: Int = 3
    ): List<EstimateBias> {
        val focusByTask = sessions.filter { it.taskId != null }.groupBy { it.taskId!! }
            .mapValues { (_, s) -> s.sumOf { it.durationMinutes } }
        return tasks
            .filter { it.isCompleted && it.durationMinutes > 0 && (focusByTask[it.id] ?: 0) > 0 && !it.subject.isNullOrBlank() }
            .groupBy { it.subject!!.trim() }
            .mapNotNull { (subject, done) ->
                if (done.size < minTasks) return@mapNotNull null
                val ratios = done.map { focusByTask.getValue(it.id).toDouble() / it.durationMinutes }.sorted()
                val median = if (ratios.size % 2 == 1) ratios[ratios.size / 2]
                    else (ratios[ratios.size / 2 - 1] + ratios[ratios.size / 2]) / 2
                val ratio = median.coerceIn(0.5, 2.0)
                if (ratio in 0.8..1.25) null else EstimateBias(subject, ratio, done.size)
            }
            .sortedByDescending { kotlin.math.abs(it.ratio - 1) }
    }
}
