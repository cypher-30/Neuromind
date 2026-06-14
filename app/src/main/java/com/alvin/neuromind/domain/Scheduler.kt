package com.alvin.neuromind.domain

import com.alvin.neuromind.data.Task
import com.alvin.neuromind.data.TimetableEntry
import com.alvin.neuromind.data.preferences.CognitiveProfile
import java.time.LocalDate
import java.time.LocalTime

data class TimeSlot(val start: LocalTime, val end: LocalTime)

class Scheduler {
    /**
     * Generates a daily plan by finding timetable gaps and slotting tasks into them.
     *
     * Ordering: HIGH-priority tasks land first; within the same priority, HARD tasks
     * are placed before MEDIUM and EASY so the most demanding work happens during
     * the user's cognitive peak window.
     *
     * The greedy fill starts at [profile.peakStart] so the top-priority, hardest
     * tasks naturally fall inside the sharpest focus window.
     */
    fun generateSchedule(
        tasks: List<Task>,
        timetable: List<TimetableEntry>,
        date: LocalDate = LocalDate.now(),
        profile: CognitiveProfile = CognitiveProfile()
    ): Map<TimeSlot, Task> {
        val schedule = mutableMapOf<TimeSlot, Task>()

        val todoTasks = tasks.filter { !it.isCompleted }
            .sortedWith(compareByDescending<Task> { it.priority }.thenByDescending { it.difficulty })

        var currentTime = LocalTime.of(profile.peakStart, 0)
        val dayEnd = LocalTime.of(22, 0)

        for (task in todoTasks) {
            if (currentTime.isAfter(dayEnd)) break

            val taskDuration = task.durationMinutes.toLong()
            val endTime = currentTime.plusMinutes(taskDuration)

            val conflict = timetable.find { entry ->
                val isRelevantToThisDate = if (entry.isRecurring) {
                    entry.dayOfWeek == date.dayOfWeek
                } else {
                    entry.date == date
                }
                isRelevantToThisDate && currentTime.isBefore(entry.endTime) && endTime.isAfter(entry.startTime)
            }

            if (conflict == null && endTime.isBefore(dayEnd)) {
                schedule[TimeSlot(currentTime, endTime)] = task
                currentTime = endTime.plusMinutes(15) // 15-minute break between tasks
            } else if (conflict != null) {
                currentTime = conflict.endTime.plusMinutes(5)
            } else {
                currentTime = currentTime.plusMinutes(15)
            }
        }
        return schedule
    }
}
