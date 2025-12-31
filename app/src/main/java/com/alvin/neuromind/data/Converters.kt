package com.alvin.neuromind.data

import androidx.room.TypeConverter
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

class Converters {
    @TypeConverter fun fromUUID(uuid: UUID?): String? = uuid?.toString()
    @TypeConverter fun toUUID(uuid: String?): UUID? = uuid?.let { UUID.fromString(it) }
    @TypeConverter fun fromDifficulty(difficulty: Difficulty): String = difficulty.name
    @TypeConverter fun toDifficulty(difficulty: String): Difficulty = Difficulty.valueOf(difficulty)
    @TypeConverter fun fromPriority(priority: Priority): String = priority.name
    @TypeConverter fun toPriority(priority: String): Priority = Priority.valueOf(priority)
    @TypeConverter fun fromDayOfWeek(day: DayOfWeek?): String? = day?.name
    @TypeConverter fun toDayOfWeek(day: String?): DayOfWeek? = day?.let { DayOfWeek.valueOf(it) }
    @TypeConverter fun fromLocalTime(time: LocalTime?): String? = time?.toString()
    @TypeConverter fun toLocalTime(time: String?): LocalTime? = time?.let { LocalTime.parse(it) }
    @TypeConverter fun fromMood(mood: Mood): String = mood.name
    @TypeConverter fun toMood(mood: String): Mood = Mood.valueOf(mood)
}