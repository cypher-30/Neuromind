package com.alvin.neuromind.domain

import com.alvin.neuromind.data.FocusSession
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

data class FocusSummary(
    val totalMinutesThisWeek: Int,
    val sessionCountThisWeek: Int,
    val minutesByDay: List<Pair<String, Int>>,   // last 7 days, short day labels
    val bestDayLabel: String?
)

object FocusStats {

    /**
     * Summarises completed focus sessions over the last 7 days relative to [referenceDate].
     * Pass an explicit [referenceDate] in tests for deterministic results.
     */
    fun summarize(
        sessions: List<FocusSession>,
        referenceDate: LocalDate = LocalDate.now()
    ): FocusSummary {
        val weekDays = (0..6).map { referenceDate.minusDays((6 - it).toLong()) }

        val minutesByDay = weekDays.map { day ->
            val label = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            val mins = sessions.filter { session ->
                val sessionDate = Instant.ofEpochMilli(session.completedAt)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                sessionDate == day
            }.sumOf { it.durationMinutes }
            label to mins
        }

        val totalMinutes = minutesByDay.sumOf { it.second }
        val sessionCount = sessions.count { session ->
            val sessionDate = Instant.ofEpochMilli(session.completedAt)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            !sessionDate.isBefore(weekDays.first()) && !sessionDate.isAfter(referenceDate)
        }

        val bestDayLabel = minutesByDay.maxByOrNull { it.second }
            ?.takeIf { it.second > 0 }
            ?.first

        return FocusSummary(
            totalMinutesThisWeek = totalMinutes,
            sessionCountThisWeek = sessionCount,
            minutesByDay = minutesByDay,
            bestDayLabel = bestDayLabel
        )
    }
}
