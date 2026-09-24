package com.alvin.neuromind.domain

import com.alvin.neuromind.data.Task
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class DailyProgressTest {
    private val zone = ZoneId.of("Africa/Nairobi")
    private val today = LocalDate.of(2026, 3, 10)

    private fun due(date: LocalDate, time: LocalTime = LocalTime.NOON) =
        date.atTime(time).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `counts only tasks due today`() {
        val tasks = listOf(
            Task(id = 1, title = "Due today, done", dueDate = due(today), isCompleted = true),
            Task(id = 2, title = "Due today, open", dueDate = due(today, LocalTime.of(23, 59))),
            Task(id = 3, title = "Due at midnight start", dueDate = due(today, LocalTime.MIDNIGHT)),
            Task(id = 4, title = "Overdue from yesterday", dueDate = due(today.minusDays(1))),
            Task(id = 5, title = "Done yesterday", dueDate = due(today.minusDays(1)), isCompleted = true),
            Task(id = 6, title = "Tomorrow", dueDate = due(today.plusDays(1), LocalTime.MIDNIGHT)),
            Task(id = 7, title = "No date")
        )

        val progress = DailyProgressCalculator.forDate(tasks, today, zone)

        assertEquals(3, progress.total)
        assertEquals(1, progress.completed)
        assertEquals(2, progress.pending)
    }

    @Test
    fun `nothing due today is zero of zero`() {
        val tasks = listOf(Task(id = 1, title = "Old", dueDate = due(today.minusDays(3)), isCompleted = true))

        val progress = DailyProgressCalculator.forDate(tasks, today, zone)

        assertEquals(DailyProgress(total = 0, completed = 0), progress)
        assertEquals(0f, progress.fraction)
    }

    @Test
    fun `seven due with none done reads zero of seven`() {
        val tasks = (1..7).map { Task(id = it, title = "T$it", dueDate = due(today)) }

        val progress = DailyProgressCalculator.forDate(tasks, today, zone)

        assertEquals(7, progress.total)
        assertEquals(0, progress.completed)
    }

    @Test
    fun `next day re-evaluates without mutating tasks`() {
        val tasks = listOf(Task(id = 1, title = "Today", dueDate = due(today)))

        assertEquals(1, DailyProgressCalculator.forDate(tasks, today, zone).total)
        assertEquals(0, DailyProgressCalculator.forDate(tasks, today.plusDays(1), zone).total)
    }
}
