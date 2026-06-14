package com.alvin.neuromind.domain

import com.alvin.neuromind.data.Difficulty
import com.alvin.neuromind.data.Priority
import com.alvin.neuromind.data.Task
import com.alvin.neuromind.data.TimetableEntry
import com.alvin.neuromind.data.preferences.CognitiveProfile
import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class SchedulerTest {

    private val scheduler = Scheduler()
    private val profile = CognitiveProfile(peakStart = 9, peakEnd = 12, sessionLength = 25)

    private fun task(
        id: Int,
        priority: Priority = Priority.MEDIUM,
        difficulty: Difficulty = Difficulty.MEDIUM,
        durationMinutes: Int = 60
    ) = Task(id = id, title = "Task $id", priority = priority, difficulty = difficulty, durationMinutes = durationMinutes)

    // --- Peak-hour scheduling ---

    @Test
    fun `schedule starts at profile peak start hour`() {
        val tasks = listOf(task(1, priority = Priority.HIGH, difficulty = Difficulty.HARD))
        val schedule = scheduler.generateSchedule(tasks, emptyList(), profile = profile)

        assertTrue("Schedule should be non-empty", schedule.isNotEmpty())
        val firstSlot = schedule.keys.minByOrNull { it.start }!!
        assertEquals(
            "First task should start at peakStart (9)",
            LocalTime.of(9, 0),
            firstSlot.start
        )
    }

    @Test
    fun `default profile starts at 8 AM`() {
        val tasks = listOf(task(1))
        val schedule = scheduler.generateSchedule(tasks, emptyList())

        assertTrue(schedule.isNotEmpty())
        val firstSlot = schedule.keys.minByOrNull { it.start }!!
        assertEquals(LocalTime.of(8, 0), firstSlot.start)
    }

    // --- Priority + difficulty ordering ---

    @Test
    fun `high priority task is scheduled before low priority task`() {
        val tasks = listOf(
            task(1, priority = Priority.LOW, difficulty = Difficulty.EASY),
            task(2, priority = Priority.HIGH, difficulty = Difficulty.EASY)
        )
        val schedule = scheduler.generateSchedule(tasks, emptyList(), profile = profile)

        val orderedTasks = schedule.entries.sortedBy { it.key.start }.map { it.value }
        assertEquals("HIGH priority should come first", tasks[1], orderedTasks[0])
        assertEquals("LOW priority should come second", tasks[0], orderedTasks[1])
    }

    @Test
    fun `hard task beats easy task when priority is equal`() {
        val tasks = listOf(
            task(1, priority = Priority.HIGH, difficulty = Difficulty.EASY, durationMinutes = 60),
            task(2, priority = Priority.HIGH, difficulty = Difficulty.HARD, durationMinutes = 60)
        )
        val schedule = scheduler.generateSchedule(tasks, emptyList(), profile = profile)

        val orderedTasks = schedule.entries.sortedBy { it.key.start }.map { it.value }
        assertEquals("HARD should be scheduled before EASY at same priority", tasks[1], orderedTasks[0])
    }

    @Test
    fun `three tasks ordered by priority then difficulty`() {
        val lowEasy  = task(1, priority = Priority.LOW,  difficulty = Difficulty.EASY)
        val highHard = task(2, priority = Priority.HIGH, difficulty = Difficulty.HARD)
        val highEasy = task(3, priority = Priority.HIGH, difficulty = Difficulty.EASY)

        val schedule = scheduler.generateSchedule(
            listOf(lowEasy, highHard, highEasy),
            emptyList(),
            profile = profile
        )

        val ordered = schedule.entries.sortedBy { it.key.start }.map { it.value }
        assertEquals(highHard, ordered[0])
        assertEquals(highEasy, ordered[1])
        assertEquals(lowEasy,  ordered[2])
    }

    // --- Conflict avoidance ---

    @Test
    fun `timetable conflict causes task to be placed after the entry`() {
        val today = LocalDate.now()
        val conflict = TimetableEntry(
            title = "Class",
            dayOfWeek = today.dayOfWeek,
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(10, 0),
            isRecurring = true
        )
        val tasks = listOf(task(1, priority = Priority.HIGH, durationMinutes = 60))
        val schedule = scheduler.generateSchedule(tasks, listOf(conflict), date = today, profile = profile)

        if (schedule.isNotEmpty()) {
            val slot = schedule.keys.first()
            val overlapWithConflict =
                slot.start.isBefore(LocalTime.of(10, 0)) && slot.end.isAfter(LocalTime.of(9, 0))
            assertFalse("Task should not overlap with the timetable conflict", overlapWithConflict)
        }
    }

    @Test
    fun `completed tasks are excluded from schedule`() {
        val completedTask = task(1).copy(isCompleted = true)
        val schedule = scheduler.generateSchedule(listOf(completedTask), emptyList(), profile = profile)
        assertTrue("Completed tasks should not be scheduled", schedule.isEmpty())
    }
}
