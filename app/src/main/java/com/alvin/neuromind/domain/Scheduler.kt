package com.alvin.neuromind.domain

import com.alvin.neuromind.data.Task
import com.alvin.neuromind.data.TimetableEntry
import java.time.DayOfWeek
import java.time.Duration // FIX: Using the correct java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

data class TimeSlot(val start: LocalTime, val end: LocalTime) {
    // This now correctly uses java.time.Duration
    val durationMinutes: Long get() = Duration.between(start, end).toMinutes()
}

data class ProposedSlot(
    val date: LocalDate,
    val timeSlot: TimeSlot
)

class Scheduler {
    private val dayStartTime: LocalTime = LocalTime.of(8, 0)
    private val dayEndTime: LocalTime = LocalTime.of(23, 0)

    fun calculateFreeTimeSlots(
        day: DayOfWeek,
        timetableEntries: List<TimetableEntry>
    ): List<TimeSlot> {
        // This function now correctly uses the class properties dayStartTime and dayEndTime
        val busySlots = timetableEntries.filter { it.dayOfWeek == day }.sortedBy { it.startTime }
        if (busySlots.isEmpty()) return listOf(TimeSlot(this.dayStartTime, this.dayEndTime))

        val freeSlots = mutableListOf<TimeSlot>()
        var lastBusySlotEnd = this.dayStartTime
        for (busySlot in busySlots) {
            if (busySlot.startTime > lastBusySlotEnd) {
                freeSlots.add(TimeSlot(lastBusySlotEnd, busySlot.startTime))
            }
            lastBusySlotEnd = busySlot.endTime
        }

        if (this.dayEndTime > lastBusySlotEnd) {
            freeSlots.add(TimeSlot(lastBusySlotEnd, this.dayEndTime))
        }
        return freeSlots
    }

    fun scheduleTasks(tasks: List<Task>, freeSlots: List<TimeSlot>): Map<TimeSlot, Task> {
        val sortedTasks = tasks.filter { !it.isCompleted && it.parentId == null }
            .sortedWith(compareBy({ it.priority.ordinal * -1 }, { it.dueDate ?: Long.MAX_VALUE }))
        val scheduledPlan = mutableMapOf<TimeSlot, Task>()
        val remainingSlots = freeSlots.toMutableList()
        for (task in sortedTasks) {
            var slotFoundForTask = false
            val slotsIterator = remainingSlots.iterator()
            while (slotsIterator.hasNext() && !slotFoundForTask) {
                val currentSlot = slotsIterator.next()
                if (task.durationMinutes <= currentSlot.durationMinutes) {
                    val scheduledSlot = TimeSlot(currentSlot.start, currentSlot.start.plusMinutes(task.durationMinutes.toLong()))
                    scheduledPlan[scheduledSlot] = task
                    slotsIterator.remove()
                    val remainingTimeStart = scheduledSlot.end
                    if (remainingTimeStart < currentSlot.end) {
                        remainingSlots.add(TimeSlot(remainingTimeStart, currentSlot.end))
                        remainingSlots.sortBy { it.start }
                    }
                    slotFoundForTask = true
                }
            }
        }
        return scheduledPlan
    }

    fun findNextAvailableSlot(task: Task, allTimetableEntries: List<TimetableEntry>): ProposedSlot? {
        var currentDate = LocalDate.now()

        for (i in 0..14) {
            val searchDate = currentDate.plusDays(i.toLong())
            val dayOfWeek = searchDate.dayOfWeek

            // This now correctly uses the class property dayStartTime
            val effectiveStartTime = if (searchDate.isEqual(LocalDate.now()) && LocalTime.now().isAfter(this.dayStartTime)) {
                LocalTime.now()
            } else {
                this.dayStartTime
            }

            // Create a temporary list of entries for the day, including a dummy entry for the start time
            val dailyEntries = allTimetableEntries.toMutableList()
            dailyEntries.add(TimetableEntry(title="Day Start", dayOfWeek=dayOfWeek, startTime=LocalTime.MIN, endTime=effectiveStartTime))

            val freeSlots = calculateFreeTimeSlots(dayOfWeek, dailyEntries)

            val suitableSlot = freeSlots.firstOrNull { it.durationMinutes >= task.durationMinutes }

            if (suitableSlot != null) {
                val proposedTimeSlot = TimeSlot(suitableSlot.start, suitableSlot.start.plusMinutes(task.durationMinutes.toLong()))
                return ProposedSlot(date = searchDate, timeSlot = proposedTimeSlot)
            }
        }
        return null
    }
}