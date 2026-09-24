package com.alvin.neuromind.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * Reminder choice for dated events. `null` on an entry means "legacy": the
 * entry predates event reminders and keeps the old polling notification
 * (TaskCheckWorker) instead of an exact alarm.
 */
enum class EventReminder { AUTO, OFF, AT_START, MINUTES_10, MINUTES_30, DAY_BEFORE }

@Entity(tableName = "timetable_entries")
data class TimetableEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val isRecurring: Boolean = true,
    val date: LocalDate? = null,
    val venue: String? = null,
    val details: String? = null,
    val isAllDay: Boolean = false, // added in DB v12
    val reminderMode: EventReminder? = null // added in DB v12
) {
    /** Weekly entries occur on their weekday; one-time entries only on their exact date. */
    fun occursOn(target: LocalDate): Boolean =
        if (isRecurring) dayOfWeek == target.dayOfWeek else date == target

    /** Whether this entry blocks [start, end) on [target] — all-day entries block the whole day. */
    fun blocks(target: LocalDate, start: LocalTime, end: LocalTime): Boolean =
        occursOn(target) && (isAllDay || (start.isBefore(endTime) && end.isAfter(startTime)))
}