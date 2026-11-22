package com.alvin.neuromind.domain

import com.alvin.neuromind.data.Difficulty
import com.alvin.neuromind.data.Priority
import com.alvin.neuromind.data.Task
import com.alvin.neuromind.data.preferences.CognitiveProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class TaskPickerTest {

    private val zone = ZoneId.systemDefault()
    private val sunday = LocalDate.of(2026, 11, 22)
    private val monday = sunday.plusDays(1)
    private val sundayMorning = LocalDateTime.of(sunday, java.time.LocalTime.of(9, 0))
    private fun millis(t: LocalDateTime) = t.atZone(zone).toInstant().toEpochMilli()

    // Monday's errands (high priority, on the day) and real work.
    private val parklands = Task(id = 1, title = "Go to Parklands", priority = Priority.HIGH, onDueDay = true,
        dueDate = millis(monday.atTime(13, 30)))
    private val upperhill = Task(id = 2, title = "Go to Upperhill", priority = Priority.HIGH, onDueDay = true,
        dueDate = millis(monday.atTime(14, 45)), prerequisiteTaskId = 1)
    private val signCrypto = Task(id = 3, title = "Sign Crypto attendance", priority = Priority.HIGH, difficulty = Difficulty.HARD,
        onDueDay = true, dueDate = millis(monday.atTime(17, 15)))
    private val aiDeliverable = Task(id = 4, title = "AI Deliverable 3", priority = Priority.HIGH, difficulty = Difficulty.HARD,
        dueDate = millis(LocalDate.of(2026, 11, 25).atTime(23, 55)))
    private val essayLater = Task(id = 5, title = "Essay", priority = Priority.MEDIUM,
        dueDate = millis(LocalDate.of(2026, 12, 20).atTime(12, 0)))
    private val all = listOf(parklands, upperhill, signCrypto, aiDeliverable, essayLater)

    @Test
    fun `errands stay out of priorities before their day`() {
        val ids = TaskPicker.priorities(all, sunday, millis(sundayMorning)).map { it.id }
        assertEquals(listOf(4), ids) // only the HIGH work task
    }

    @Test
    fun `errands show in priorities on their day, ahead of work`() {
        val mondayMorning = millis(monday.atTime(8, 0))
        val ids = TaskPicker.priorities(all, monday, mondayMorning).map { it.id }
        assertEquals(listOf(1, 2, 3, 4), ids)
    }

    @Test
    fun `overdue errand stays a priority but is never rebalanced`() {
        val tuesday = millis(monday.plusDays(1).atTime(9, 0))
        assertTrue(TaskPicker.priorities(all, monday.plusDays(1), tuesday).any { it.id == 1 })
        val overdueParklands = parklands.copy(dueDate = millis(sundayMorning.minusDays(1)))
        assertTrue(TaskPicker.rebalanceable(listOf(overdueParklands)).isEmpty())
    }

    @Test
    fun `errands and blocked tasks are never workable`() {
        val blockedWork = Task(id = 6, title = "Write up", prerequisiteTaskId = 4)
        val ids = TaskPicker.workable(all + blockedWork).map { it.id }
        assertEquals(listOf(4, 5), ids)
    }

    @Test
    fun `work queue puts what's due soon ahead of higher priority later work`() {
        val dueSoonMedium = Task(id = 7, title = "Lab report", priority = Priority.MEDIUM,
            dueDate = millis(sunday.plusDays(1).atTime(9, 0)))
        val highLater = Task(id = 8, title = "Project", priority = Priority.HIGH,
            dueDate = millis(sunday.plusDays(20).atTime(9, 0)))
        val queue = TaskPicker.workQueue(listOf(highLater, dueSoonMedium), millis(sundayMorning))
        assertEquals(listOf(7, 8), queue.map { it.id })
    }

    @Test
    fun `peak-hour suggestion never offers an errand for focus`() {
        val peak = sunday.atTime(10, 0)
        val suggestion = SuggestionEngine.suggest(listOf(signCrypto, aiDeliverable), emptyList(), CognitiveProfile(), now = peak)
        assertNotEquals(3, suggestion?.taskId)
        assertEquals(4, suggestion?.taskId)
    }

    @Test
    fun `no suggestion when only errands are left`() {
        val suggestion = SuggestionEngine.suggest(listOf(parklands, signCrypto), emptyList(), CognitiveProfile(), now = sunday.atTime(10, 0))
        assertEquals(null, suggestion)
    }

    @Test
    fun `plan skips errands and starts no earlier than now`() {
        val now = sunday.atTime(15, 7)
        val plan = Scheduler().generateSchedule(all, emptyList(), date = sunday, profile = CognitiveProfile(), now = now)
        assertFalse(plan.values.any { it.onDueDay })
        val firstStart = plan.keys.minOf { it.start }
        assertEquals(java.time.LocalTime.of(15, 15), firstStart)
    }

    @Test
    fun `plan is empty late at night`() {
        val plan = Scheduler().generateSchedule(all, emptyList(), date = sunday, profile = CognitiveProfile(), now = sunday.atTime(23, 50))
        assertTrue(plan.isEmpty())
    }

    @Test
    fun `priorities only look a few days ahead`() {
        val highFarAway = Task(id = 9, title = "Final project", priority = Priority.HIGH,
            dueDate = millis(sunday.plusDays(20).atTime(9, 0)))
        val lowDueSoon = Task(id = 10, title = "Reading", priority = Priority.LOW,
            dueDate = millis(sunday.plusDays(2).atTime(9, 0)))
        val highNoDate = Task(id = 11, title = "Someday", priority = Priority.HIGH)
        val ids = TaskPicker.priorities(listOf(highFarAway, lowDueSoon, highNoDate), sunday, millis(sundayMorning)).map { it.id }
        assertEquals(listOf(10), ids)
    }

    @Test
    fun `big work is spread over the days before its deadline`() {
        // 4 h due Wednesday, planned on Sunday: Sun..Wed = 4 days -> 60 min today.
        val big = Task(id = 12, title = "Notebook", durationMinutes = 240,
            dueDate = millis(LocalDate.of(2026, 11, 25).atTime(23, 55)))
        assertEquals(60, TaskPicker.todaysShare(big, sunday))
        // 2 h already done in Focus: 120 left over 4 days -> 30.
        assertEquals(30, TaskPicker.todaysShare(big, sunday, focusedMinutes = 120))
        // Due today or overdue: everything that's left.
        assertEquals(240, TaskPicker.todaysShare(big, LocalDate.of(2026, 11, 25)))
        // Small tasks stay whole.
        assertEquals(45, TaskPicker.todaysShare(big.copy(durationMinutes = 45), sunday))
    }

    @Test
    fun `plan gives a big task only today's share`() {
        val big = Task(id = 12, title = "Notebook", durationMinutes = 240, priority = Priority.HIGH,
            dueDate = millis(LocalDate.of(2026, 11, 25).atTime(23, 55)))
        val plan = Scheduler().generateSchedule(listOf(big), emptyList(), date = sunday, profile = CognitiveProfile(), now = sundayMorning)
        val slot = plan.keys.single()
        assertEquals(60L, java.time.Duration.between(slot.start, slot.end).toMinutes())
    }
}
