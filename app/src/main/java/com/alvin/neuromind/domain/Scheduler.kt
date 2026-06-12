package com.alvin.neuromind.domain

import com.alvin.neuromind.data.Task
import com.alvin.neuromind.data.TimetableEntry
import java.time.LocalDate
import java.time.LocalTime

data class TimeSlot(val start: LocalTime, val end: LocalTime)

class Scheduler {
    /**
     * Generates a plan by finding gaps in the timetable for a specific date[cite: 137, 149].
     */
    fun generateSchedule(
        tasks: List<Task>,
        timetable: List<TimetableEntry>,
        date: LocalDate = LocalDate.now()
    ): Map<TimeSlot, Task> {
        val schedule = mutableMapOf<TimeSlot, Task>()

        // 1. Filter and sort tasks by priority[cite: 1114, 1115].
        val todoTasks = tasks.filter { !it.isCompleted }
            .sortedByDescending { it.priority }

        var currentTime = LocalTime.of(8, 0) // Start of work day[cite: 1092].
        val dayEnd = LocalTime.of(22, 0)     // End of work day[cite: 1093].

        for (task in todoTasks) {
            if (currentTime.isAfter(dayEnd)) break

            val taskDuration = task.durationMinutes.toLong()
            val endTime = currentTime.plusMinutes(taskDuration)

            // 2. CHECK FOR CONFLICTS[cite: 1098, 1143].
            // Logic: Is there a class today OR a one-time event on this specific date?
            val conflict = timetable.find { entry ->
                val isRelevantToThisDate = if (entry.isRecurring) {
                    // It's a weekly class: does the Day of Week match?.
                    entry.dayOfWeek == date.dayOfWeek
                } else {
                    // It's a one-time event: does the exact Date match?.
                    entry.date == date
                }

                // Does the time overlap with our proposed task slot?[cite: 1102, 1144].
                isRelevantToThisDate && currentTime.isBefore(entry.endTime) && endTime.isAfter(entry.startTime)
            }

            if (conflict == null && endTime.isBefore(dayEnd)) {
                // Success: No class or appointment[cite: 1119, 1121].
                schedule[TimeSlot(currentTime, endTime)] = task
                currentTime = endTime.plusMinutes(15) // 15-minute buffer/break[cite: 1100, 1104].
            } else if (conflict != null) {
                // Conflict: The AI "jumps" to the end of the class/event[cite: 1106, 1124].
                currentTime = conflict.endTime.plusMinutes(5)
            } else {
                // Day is too full for this specific task
                currentTime = currentTime.plusMinutes(15)
            }
        }
        return schedule
    }
}