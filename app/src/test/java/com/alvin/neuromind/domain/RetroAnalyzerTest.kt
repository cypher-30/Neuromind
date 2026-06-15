package com.alvin.neuromind.domain

import com.alvin.neuromind.data.Difficulty
import com.alvin.neuromind.data.FeedbackLog
import com.alvin.neuromind.data.Mood
import com.alvin.neuromind.data.Priority
import com.alvin.neuromind.data.Task
import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

class RetroAnalyzerTest {

    private val zone = ZoneId.systemDefault()

    private fun task(
        id: Int = 0,
        isCompleted: Boolean = false,
        dueDate: Long? = null,
        createdDaysAgo: Long = 0,
        priority: Priority = Priority.MEDIUM,
        difficulty: Difficulty = Difficulty.MEDIUM
    ) = Task(
        id = id,
        title = "Task $id",
        isCompleted = isCompleted,
        dueDate = dueDate,
        priority = priority,
        difficulty = difficulty,
        createdAt = System.currentTimeMillis() - createdDaysAgo * 86_400_000L
    )

    private fun log(
        daysAgo: Long,
        mood: Mood = Mood.NEUTRAL,
        energy: Int = 3,
        tasksCompleted: Int = 2
    ) = FeedbackLog(
        date = System.currentTimeMillis() - daysAgo * 86_400_000L,
        mood = mood,
        energyLevel = energy,
        tasksCompleted = tasksCompleted,
        comment = null
    )

    private val today = LocalDate.now()

    @Test
    fun `returns null when fewer than 7 days of logs exist`() {
        val logs = listOf(log(daysAgo = 0), log(daysAgo = 1))
        // tasks are present but logs span only 2 days — should return null
        val result = RetroAnalyzer.analyze(emptyList(), logs, today)
        assertNull("Expected null with under-7-day data", result)
    }

    @Test
    fun `4-week trend returns correct completion percentages`() {
        // Week W4 (current week): 2 tasks, 1 completed
        // All created "today"
        val tasks = listOf(
            task(id = 1, isCompleted = true, createdDaysAgo = 0),
            task(id = 2, isCompleted = false, createdDaysAgo = 0)
        )
        val logs = (0..7L).map { log(daysAgo = it) }

        val result = RetroAnalyzer.analyze(tasks, logs, today)
        assertNotNull(result)

        // W4 is the current week (index 3 in the list)
        val w4 = result!!.weeklyTrend.last()
        assertEquals("W4 label", "W4", w4.weekLabel)
        assertEquals("W4 completion pct", 0.5f, w4.completionPct, 0.01f)

        // Older weeks have no tasks — should be 0%
        val w1 = result.weeklyTrend.first()
        assertEquals("W1 completion pct", 0f, w1.completionPct, 0.01f)
    }

    @Test
    fun `best day tie is resolved to earlier weekday`() {
        // Create 2 completed tasks on Monday and 2 on Wednesday
        // Monday < Wednesday in DayOfWeek ordinal (1 vs 3) — Monday should win
        val monday = today.with(DayOfWeek.MONDAY)
        val wednesday = today.with(DayOfWeek.WEDNESDAY)

        // Only use dates within the last 30 days
        val mondayDate = if (monday.isBefore(today) || monday == today) monday else monday.minusWeeks(1)
        val wednesdayDate = if (wednesday.isBefore(today) || wednesday == today) wednesday else wednesday.minusWeeks(1)

        val mondayMillis = mondayDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val wedMillis = wednesdayDate.atStartOfDay(zone).toInstant().toEpochMilli()

        val tasks = listOf(
            task(id = 1, isCompleted = true).copy(createdAt = mondayMillis),
            task(id = 2, isCompleted = true).copy(createdAt = mondayMillis),
            task(id = 3, isCompleted = true).copy(createdAt = wedMillis),
            task(id = 4, isCompleted = true).copy(createdAt = wedMillis)
        )
        val logs = (0..7L).map { log(daysAgo = it) }

        val result = RetroAnalyzer.analyze(tasks, logs, today)
        assertNotNull(result)
        val bestDay = result!!.bestDay
        assertNotNull("bestDay should not be null", bestDay)
        assertEquals("Monday should win tie", DayOfWeek.MONDAY, bestDay!!.day)
    }

    @Test
    fun `on-track rate excludes tasks without a due date`() {
        val now = System.currentTimeMillis()
        val past = now - 2 * 86_400_000L  // 2 days ago

        val tasks = listOf(
            task(id = 1, isCompleted = true, dueDate = past),   // on track
            task(id = 2, isCompleted = false, dueDate = past),  // off track (overdue)
            task(id = 3, isCompleted = true, dueDate = null)    // no dueDate — excluded
        )
        val logs = (0..7L).map { log(daysAgo = it) }

        val result = RetroAnalyzer.analyze(tasks, logs, today)
        assertNotNull(result)
        // 1 on-track out of 2 tasks with a past dueDate = 50%
        assertEquals("On-track rate", 0.5f, result!!.onTrackRate, 0.01f)
    }
}
