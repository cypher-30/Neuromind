package com.alvin.neuromind.data

import androidx.room.TypeConverter
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class Converters {
    @TypeConverter fun fromDayOfWeek(day: DayOfWeek?): String? = day?.name
    @TypeConverter fun toDayOfWeek(day: String?): DayOfWeek? = day?.let { DayOfWeek.valueOf(it) }

    @TypeConverter fun fromLocalTime(time: LocalTime?): String? = time?.toString()
    @TypeConverter fun toLocalTime(time: String?): LocalTime? = time?.let { LocalTime.parse(it) }

    @TypeConverter fun fromLocalDate(date: LocalDate?): String? = date?.toString()
    @TypeConverter fun toLocalDate(date: String?): LocalDate? = date?.let { LocalDate.parse(it) }

    @TypeConverter fun fromPriority(priority: Priority): String = priority.name
    @TypeConverter fun toPriority(priority: String): Priority = Priority.valueOf(priority)

    @TypeConverter fun fromDifficulty(difficulty: Difficulty): String = difficulty.name
    @TypeConverter fun toDifficulty(difficulty: String): Difficulty = Difficulty.valueOf(difficulty)

    @TypeConverter fun fromMood(mood: Mood): String = mood.name
    @TypeConverter fun toMood(mood: String): Mood = Mood.valueOf(mood)
}