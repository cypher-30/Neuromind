package com.alvin.neuromind.domain

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.*

object SmartInputHelper {
    fun parseInput(input: String): ParsedTask {
        val lower = input.lowercase()
        var title = input
        var dueDate: LocalDateTime? = null

        // 1. Detect "tomorrow"
        if (lower.contains("tomorrow")) {
            dueDate = LocalDateTime.now().plusDays(1).with(LocalTime.of(9, 0))
            title = title.replace(Regex("(?i)\\s*tomorrow\\s*"), " ").trim()
        }

        // 2. Detect "next [weekday]"
        val nextWeekdayRegex = Regex("(?i)next\\s+(monday|tuesday|wednesday|thursday|friday|saturday|sunday)")
        nextWeekdayRegex.find(lower)?.let { match ->
            val dayName = match.groupValues[1]
            val dayOfWeek = DayOfWeek.valueOf(dayName.uppercase())
            dueDate = LocalDateTime.now().with(TemporalAdjusters.next(dayOfWeek)).with(LocalTime.of(9, 0))
            title = title.replace(Regex("(?i)\\s*next\\s+$dayName\\s*"), " ").trim()
        }

        // 3. Detect "[weekday]" (this week)
        if (dueDate == null) {
            val weekdayRegex = Regex("(?i)\\b(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b")
            weekdayRegex.find(lower)?.let { match ->
                val dayName = match.groupValues[1]
                val dayOfWeek = DayOfWeek.valueOf(dayName.uppercase())
                dueDate = LocalDateTime.now().with(TemporalAdjusters.nextOrSame(dayOfWeek)).with(LocalTime.of(9, 0))
                title = title.replace(Regex("(?i)\\b$dayName\\b"), " ").trim()
            }
        }

        // 4. Detect "in X days"
        val inXDaysRegex = Regex("(?i)in\\s+(\\d+)\\s+days?")
        inXDaysRegex.find(lower)?.let { match ->
            val days = match.groupValues[1].toLong()
            dueDate = LocalDateTime.now().plusDays(days).with(LocalTime.of(9, 0))
            title = title.replace(Regex("(?i)\\s*in\\s+$days\\s+days?\\s*"), " ").trim()
        }

        // 5. Detect Time (at Xpm, at Xam, at HH:mm)
        val timeRegex = Regex("(?i)at\\s+(\\d+)(?::(\\d+))?\\s*(am|pm)?")
        timeRegex.find(lower)?.let { match ->
            var hour = match.groupValues[1].toInt()
            val minute = if (match.groupValues[2].isNotEmpty()) match.groupValues[2].toInt() else 0
            val amPm = match.groupValues[3].lowercase()

            if (amPm == "pm" && hour < 12) hour += 12
            if (amPm == "am" && hour == 12) hour = 0

            val targetTime = LocalTime.of(hour, minute)
            dueDate = (dueDate ?: LocalDateTime.now()).with(targetTime)
            title = title.replace(match.value, " ").trim()
        }

        // Clean up common prefixes
        title = title.replace(Regex("(?i)^(remind me to |add task |task )+"), "").trim()

        val finalDueDate = dueDate?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()

        return ParsedTask(
            title = if (title.isBlank()) "New Task" else title.replace(Regex("\\s+"), " "),
            dueDate = finalDueDate
        )
    }
}

data class ParsedTask(val title: String, val dueDate: Long?)
