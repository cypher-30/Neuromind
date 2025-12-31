package com.alvin.neuromind.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

@Entity(tableName = "timetable_entries")
data class TimetableEntry(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val title: String,
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
    // --- NEW FIELDS ADDED ---
    val venue: String? = null,
    val details: String? = null
)