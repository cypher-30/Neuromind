package com.alvin.neuromind.data.backup

import com.alvin.neuromind.data.EventReminder
import com.alvin.neuromind.data.TimetableEntry
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * [TimetableEntry] isn't directly @Serializable since its java.time fields have no
 * built-in kotlinx.serialization support. String encoding mirrors Converters.kt
 * (Room's own TypeConverters) so both stay in sync.
 */
@Serializable
data class TimetableEntryDto(
    val id: Int,
    val title: String,
    val dayOfWeek: String,
    val startTime: String,
    val endTime: String,
    val isRecurring: Boolean,
    val date: String?,
    val venue: String?,
    val details: String?,
    // Added in backup schema 2; defaults let schema-1 files restore unchanged.
    val isAllDay: Boolean = false,
    val reminderMode: String? = null
)

fun TimetableEntry.toDto() = TimetableEntryDto(
    id = id,
    title = title,
    dayOfWeek = dayOfWeek.name,
    startTime = startTime.toString(),
    endTime = endTime.toString(),
    isRecurring = isRecurring,
    date = date?.toString(),
    venue = venue,
    details = details,
    isAllDay = isAllDay,
    reminderMode = reminderMode?.name
)

fun TimetableEntryDto.toEntity() = TimetableEntry(
    id = id,
    title = title,
    dayOfWeek = DayOfWeek.valueOf(dayOfWeek),
    startTime = LocalTime.parse(startTime),
    endTime = LocalTime.parse(endTime),
    isRecurring = isRecurring,
    date = date?.let { LocalDate.parse(it) },
    venue = venue,
    details = details,
    isAllDay = isAllDay,
    reminderMode = reminderMode?.let { name -> EventReminder.entries.firstOrNull { it.name == name } }
)
