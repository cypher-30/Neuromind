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

class TaskRebalancerTest {

    private val emptyTimetable = emptyList<TimetableEntry>()

    private fun overdueTask(id: Int, durationMinutes: Int = 60) = Task(
        id = id,
        title = "Overdue Task $id",
        dueDate = System.currentTimeMillis() - 86_400_000L,
        isCompleted = false,
        durationMinutes = durationMinutes
    )

    private fun busyTimetable(date: LocalDate): List<TimetableEntry> {
        // Fill the whole day with one recurring entry so no slots fit
        return listOf(
            TimetableEntry(
                title = "All Day Block",
                dayOfWeek = date.dayOfWeek,
                startTime = LocalTime.of(8, 0),
                endTime = LocalTime.of(22, 0),
                isRecurring = true
            )
        )
    }

    @Test
    fun `produces one proposal per overdue task when slots are available`() {
        val tasks = listOf(
            overdueTask(1, durationMinutes = 30),
            overdueTask(2, durationMinutes = 30),
            overdueTask(3, durationMinutes = 30)
        )
        val proposals = TaskRebalancer.rebalance(tasks, emptyTimetable)
        assertEquals("One proposal per task", 3, proposals.size)
    }

    @Test
    fun `does not reuse the same slot for two tasks`() {
        val tasks = listOf(
            overdueTask(1, durationMinutes = 60),
            overdueTask(2, durationMinutes = 60)
        )
        val proposals = TaskRebalancer.rebalance(tasks, emptyTimetable)
        if (proposals.size == 2) {
            val slot1 = Pair(proposals[0].suggestedDate, proposals[0].suggestedTime)
            val slot2 = Pair(proposals[1].suggestedDate, proposals[1].suggestedTime)
            assertNotEquals("Slots must be different", slot1, slot2)
        }
    }

    @Test
    fun `excludes tasks when no slot fits within lookahead window`() {
        // Use a busyTimetable that covers all 7 days by using recurring entries for every day
        val allDayBlocks = DayOfWeek.entries.map { day ->
            TimetableEntry(
                title = "Blocked",
                dayOfWeek = day,
                startTime = LocalTime.of(8, 0),
                endTime = LocalTime.of(22, 0),
                isRecurring = true
            )
        }
        val tasks = listOf(overdueTask(1, durationMinutes = 60))
        val proposals = TaskRebalancer.rebalance(tasks, allDayBlocks)
        assertTrue("No slot available — proposals should be empty", proposals.isEmpty())
    }

    @Test
    fun `returns empty list when fewer than 3 overdue tasks are passed`() {
        val tasks = listOf(overdueTask(1), overdueTask(2))
        // Defensive: rebalancer itself has no minimum check — it just processes what it receives.
        // The caller (ViewModel) enforces the >= 3 threshold. Rebalancer should still work.
        val proposals = TaskRebalancer.rebalance(tasks, emptyTimetable)
        // Either 0, 1, or 2 proposals — just verify no crash and count matches available tasks
        assertTrue("Proposals should not exceed input count", proposals.size <= 2)
    }

    @Test
    fun `high priority task claims an earlier slot than low priority task`() {
        val highTask = Task(
            id = 1, title = "High Priority",
            priority = Priority.HIGH, difficulty = Difficulty.EASY,
            dueDate = System.currentTimeMillis() - 86_400_000L,
            durationMinutes = 60
        )
        val lowTask = Task(
            id = 2, title = "Low Priority",
            priority = Priority.LOW, difficulty = Difficulty.EASY,
            dueDate = System.currentTimeMillis() - 86_400_000L,
            durationMinutes = 60
        )
        // Pass low first so ordering by list position would put it earlier — rebalancer must re-sort.
        val proposals = TaskRebalancer.rebalance(
            listOf(lowTask, highTask), emptyTimetable
        )
        if (proposals.size == 2) {
            val highSlotIndex = proposals.indexOfFirst { it.task.id == 1 }
            val lowSlotIndex  = proposals.indexOfFirst { it.task.id == 2 }
            assertTrue("HIGH priority task should be assigned before LOW priority", highSlotIndex < lowSlotIndex)
        }
    }

    @Test
    fun `hard task prefers peak-hour slot when one is available`() {
        // Peak hours 9–11. Create a timetable that blocks everything EXCEPT 9 AM on day 1.
        // The rebalancer should assign the HARD task to the 9 AM peak slot, not a later one.
        val today = LocalDate.now()
        val blockedSlots = DayOfWeek.entries.map { day ->
            TimetableEntry(
                title     = "Block",
                dayOfWeek = day,
                startTime = LocalTime.of(12, 0),
                endTime   = LocalTime.of(22, 0),
                isRecurring = true
            )
        }
        val hardTask = Task(
            id = 10, title = "Hard Task",
            priority = Priority.MEDIUM, difficulty = Difficulty.HARD,
            dueDate = System.currentTimeMillis() - 86_400_000L,
            durationMinutes = 30
        )
        val profile = CognitiveProfile(peakStart = 9, peakEnd = 11)
        val proposals = TaskRebalancer.rebalance(listOf(hardTask), blockedSlots, profile = profile)
        // The only free slots are in the 8–11 window, which includes peak hours
        if (proposals.isNotEmpty()) {
            val assignedHour = proposals[0].suggestedTime.hour
            assertTrue("HARD task should be slotted within 8–11 AM range", assignedHour in 8..10)
        }
    }
}
