package com.alvin.neuromind.domain

import com.alvin.neuromind.data.TimetableEntry
import java.time.LocalDate
import java.time.LocalTime

data class AvailableSlot(
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime
)

object TimeFinder {

    /**
     * Finds up to [maxResults] free time slots of [durationMinutes] within the next
     * [lookaheadDays] days, avoiding all timetable conflicts.
     *
     * Search starts from the next 30-minute mark after [referenceTime] for today,
     * or from 8 AM for future days. Both [referenceDate] and [referenceTime] default
     * to the real current date/time; pass explicit values for deterministic testing.
     */
    fun findSlots(
        timetable: List<TimetableEntry>,
        durationMinutes: Int,
        lookaheadDays: Int = 7,
        maxResults: Int = 5,
        referenceDate: LocalDate = LocalDate.now(),
        referenceTime: LocalTime = LocalTime.now()
    ): List<AvailableSlot> {
        val slots = mutableListOf<AvailableSlot>()
        val dayStart = LocalTime.of(8, 0)
        val dayEnd = LocalTime.of(22, 0)

        for (dayOffset in 0 until lookaheadDays) {
            if (slots.size >= maxResults) break
            val date = referenceDate.plusDays(dayOffset.toLong())

            var current = if (dayOffset == 0) {
                when {
                    referenceTime.isBefore(dayStart) -> dayStart
                    else -> referenceTime.withSecond(0).withNano(0).plusMinutes(30)
                }
            } else {
                dayStart
            }

            while (slots.size < maxResults) {
                val end = current.plusMinutes(durationMinutes.toLong())
                if (!end.isBefore(dayEnd)) break

                val conflict = timetable.firstOrNull { entry ->
                    val relevant = if (entry.isRecurring) {
                        entry.dayOfWeek == date.dayOfWeek
                    } else {
                        entry.date == date
                    }
                    relevant && current.isBefore(entry.endTime) && end.isAfter(entry.startTime)
                }

                if (conflict == null) {
                    slots.add(AvailableSlot(date, current, end))
                    current = end.plusMinutes(15)
                } else {
                    current = conflict.endTime.plusMinutes(5)
                }
            }
        }

        return slots
    }
}
