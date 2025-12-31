package com.alvin.neuromind.domain

import com.alvin.neuromind.data.Task
import com.alvin.neuromind.data.TimetableEntry
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

data class TimeSlot(
    val start: LocalTime,
    val end: LocalTime
)

class Scheduler {

    fun generateSchedule(
        tasks: List<Task>,
        timetable: List<TimetableEntry>,
        date: LocalDate = LocalDate.now()
    ): Map<TimeSlot, Task> {
        val schedule = mutableMapOf<TimeSlot, Task>()

        // 1. Filter tasks that are relevant (not completed)
        val todoTasks = tasks.filter { !it.isCompleted }
            .sortedByDescending { it.priority } // High priority first

        // 2. Simple blocking: Schedule tasks in free time slots
        // This is a simplified logic to get it compiling and running.
        // It assumes a 9-5 work day for simplicity in this version.

        var currentTime = LocalTime.of(9, 0)
        val dayEnd = LocalTime.of(17, 0)

        for (task in todoTasks) {
            if (currentTime.isAfter(dayEnd)) break

            val taskDuration = 60L // Assume 1 hour per task for now
            val endTime = currentTime.plusMinutes(taskDuration)

            // Check if this slots overlaps with any fixed class/event
            val isBlocked = timetable.any { entry ->
                // Check if entry is for today
                if (entry.dayOfWeek == date.dayOfWeek) {
                    val entryStart = entry.startTime
                    val entryEnd = entry.endTime
                    // Simple overlap check
                    currentTime.isBefore(entryEnd) && endTime.isAfter(entryStart)
                } else {
                    false
                }
            }

            if (!isBlocked && endTime.isBefore(dayEnd)) {
                schedule[TimeSlot(currentTime, endTime)] = task
                currentTime = endTime.plusMinutes(15) // 15 min break
            } else {
                // If blocked, just skip 30 mins and try again (naive algorithm)
                currentTime = currentTime.plusMinutes(30)
            }
        }

        return schedule
    }
}