package com.alvin.neuromind.domain

import com.alvin.neuromind.data.Task
import com.alvin.neuromind.data.TimetableEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class WorkloadTest {
    private val zone = ZoneId.systemDefault()
    private val sunday = LocalDate.of(2026, 11, 22)
    private fun millis(t: LocalDateTime) = t.atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `warns when a deadline needs more than the free time before it`() {
        // Sunday 7 pm, 8 h of work due tomorrow 10 am: only ~2 h (Sun eve) + 2 h (Mon morning) free.
        val now = sunday.atTime(19, 0)
        val big = Task(id = 1, title = "Notebook", durationMinutes = 480, dueDate = millis(sunday.plusDays(1).atTime(10, 0)))
        val w = Workload.warnings(listOf(big), emptyList(), now)
        assertEquals(1, w.size)
        assertEquals(480, w[0].neededMinutes)
        assertEquals(120 + 120, w[0].freeMinutes)
    }

    @Test
    fun `no warning when it fits, and classes reduce free time`() {
        val now = sunday.atTime(8, 0)
        val task = Task(id = 1, title = "Report", durationMinutes = 120, dueDate = millis(sunday.plusDays(1).atTime(12, 0)))
        assertTrue(Workload.warnings(listOf(task), emptyList(), now).isEmpty())

        val mondayClasses = TimetableEntry(title = "Lectures", dayOfWeek = DayOfWeek.MONDAY,
            startTime = LocalTime.of(8, 0), endTime = LocalTime.of(12, 0))
        val free = Workload.freeMinutes(listOf(mondayClasses), sunday.plusDays(1).atTime(8, 0),
            sunday.plusDays(1).atTime(21, 0), LocalTime.of(8, 0), LocalTime.of(21, 0), 360)
        assertEquals(360, free) // 9 h free, capped at 6 h
    }

    @Test
    fun `focus time already spent counts as done`() {
        val now = sunday.atTime(19, 0)
        val big = Task(id = 1, title = "Notebook", durationMinutes = 480, dueDate = millis(sunday.plusDays(1).atTime(10, 0)))
        assertTrue(Workload.warnings(listOf(big), emptyList(), now, focusedMinutes = mapOf(1 to 300)).isEmpty())
    }
}
