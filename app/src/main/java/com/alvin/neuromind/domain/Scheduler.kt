package com.alvin.neuromind.domain

import com.alvin.neuromind.data.Task
import com.alvin.neuromind.data.TimetableEntry
import com.alvin.neuromind.data.preferences.CognitiveProfile
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class TimeSlot(val start: LocalTime, val end: LocalTime)

class Scheduler {
    /**
     * Generates a daily plan by finding timetable gaps and slotting tasks into them.
     *
     * Only [TaskPicker.workable] tasks are planned: not done, not waiting on an
     * unfinished prerequisite, and not "on the day" errands (those happen at
     * their own time, not in a work slot). They're taken most urgent first
     * ([TaskPicker.byUrgency]: overdue, due within 3 days, then priority and
     * difficulty), so the greedy fill from [profile.peakStart] puts the most
     * pressing, hardest work in the sharpest focus window.
     *
     * When [now] is on [date], planning starts no earlier than now (rounded up
     * to the next quarter hour) instead of offering slots that already passed.
     */
    fun generateSchedule(
        tasks: List<Task>,
        timetable: List<TimetableEntry>,
        date: LocalDate = LocalDate.now(),
        profile: CognitiveProfile = CognitiveProfile(),
        now: LocalDateTime? = null,
        /** Minutes already spent in Focus per task id, so only what's left is planned. */
        focusedMinutes: Map<Int, Int> = emptyMap()
    ): Map<TimeSlot, Task> {
        val schedule = mutableMapOf<TimeSlot, Task>()

        val nowMillis = (now ?: LocalDateTime.now()).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val todoTasks = TaskPicker.workQueue(tasks, nowMillis)

        var currentTime = LocalTime.of(profile.peakStart, 0)
        if (now != null && now.toLocalDate() == date) {
            val m = now.toLocalTime().withSecond(0).withNano(0)
            val roundedUp = m.plusMinutes(((15 - m.minute % 15) % 15).toLong())
            // Past midnight-wrap (after 23:45) or past the day's end: nothing left to plan.
            if (roundedUp.isBefore(m) || roundedUp.isAfter(LocalTime.of(22, 0))) return schedule
            if (roundedUp.isAfter(currentTime)) currentTime = roundedUp
        }
        val dayEnd = LocalTime.of(22, 0)

        for (task in todoTasks) {
            if (currentTime.isAfter(dayEnd)) break

            // Big tasks get today's share only (see TaskPicker.todaysShare).
            val taskDuration = TaskPicker.todaysShare(task, date, focusedMinutes[task.id] ?: 0).toLong()
            val endTime = currentTime.plusMinutes(taskDuration)

            val conflict = timetable.find { entry -> entry.blocks(date, currentTime, endTime) }

            if (conflict == null && endTime.isBefore(dayEnd)) {
                schedule[TimeSlot(currentTime, endTime)] = task
                currentTime = endTime.plusMinutes(15) // 15-minute break between tasks
            } else if (conflict != null) {
                // An all-day event reserves the whole day — nothing more fits.
                if (conflict.isAllDay) break
                currentTime = conflict.endTime.plusMinutes(5)
            } else {
                currentTime = currentTime.plusMinutes(15)
            }
        }
        return schedule
    }
}
