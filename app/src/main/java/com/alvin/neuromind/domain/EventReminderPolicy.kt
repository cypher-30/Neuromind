package com.alvin.neuromind.domain

import com.alvin.neuromind.data.EventReminder
import com.alvin.neuromind.data.TimetableEntry
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * When an event's reminder should fire.
 *
 * Automatic (the default):
 *  - all-day event            -> 9:00 AM on the event date
 *  - timed event before 9 AM  -> 9:00 PM the previous evening
 *  - any other timed event    -> 10 minutes before it starts
 *
 * An explicit choice overrides the automatic rule. All-day events have no start
 * time, so explicit offsets count back from the same 9:00 AM anchor.
 */
object EventReminderPolicy {
    val ALL_DAY_ANCHOR: LocalTime = LocalTime.of(9, 0)
    val EARLY_EVENT_CUTOFF: LocalTime = LocalTime.of(9, 0)
    val PREVIOUS_EVENING: LocalTime = LocalTime.of(21, 0)

    fun triggerAt(
        date: LocalDate,
        startTime: LocalTime,
        isAllDay: Boolean,
        mode: EventReminder?
    ): LocalDateTime? {
        val anchor = if (isAllDay) date.atTime(ALL_DAY_ANCHOR) else date.atTime(startTime)
        return when (mode) {
            null, EventReminder.OFF -> null
            EventReminder.AUTO -> when {
                isAllDay -> anchor
                startTime.isBefore(EARLY_EVENT_CUTOFF) -> date.minusDays(1).atTime(PREVIOUS_EVENING)
                else -> anchor.minusMinutes(10)
            }
            EventReminder.AT_START -> anchor
            EventReminder.MINUTES_10 -> anchor.minusMinutes(10)
            EventReminder.MINUTES_30 -> anchor.minusMinutes(30)
            EventReminder.DAY_BEFORE -> anchor.minusDays(1)
        }
    }

    /** Only dated (one-time) events with a non-legacy reminder get exact alarms. */
    fun triggerAt(entry: TimetableEntry): LocalDateTime? {
        val date = entry.date ?: return null
        if (entry.isRecurring) return null
        return triggerAt(date, entry.startTime, entry.isAllDay, entry.reminderMode)
    }

    fun label(mode: EventReminder): String = when (mode) {
        EventReminder.AUTO -> "Default"
        EventReminder.OFF -> "Off"
        EventReminder.AT_START -> "At start"
        EventReminder.MINUTES_10 -> "10 min before"
        EventReminder.MINUTES_30 -> "30 min before"
        EventReminder.DAY_BEFORE -> "1 day before"
    }
}
