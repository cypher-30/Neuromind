package com.alvin.neuromind.domain

import com.alvin.neuromind.data.Task
import com.alvin.neuromind.data.TimetableEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class WidgetInsightsTest {
    private val zone = ZoneId.of("Africa/Nairobi")
    // Sunday 22 Nov 2026, 9 PM.
    private val now = LocalDateTime.of(2026, 11, 22, 21, 0)

    private fun event(id: Int, title: String, date: LocalDate, start: Int, end: Int) = TimetableEntry(
        id = id, title = title, dayOfWeek = date.dayOfWeek,
        startTime = LocalTime.of(start, 0), endTime = LocalTime.of(end, 0),
        isRecurring = false, date = date
    )

    @Test
    fun `exams are cleaned up, ordered and counted down`() {
        val entries = listOf(
            event(2, "EXAM: HLE 3204 Spanish IV", LocalDate.of(2026, 12, 4), 13, 15),
            event(1, "EXAM: ICS 3102 Mobile App Dev", LocalDate.of(2026, 12, 2), 16, 18),
            event(3, "Meeting with mentor", LocalDate.of(2026, 11, 23), 15, 16),
            event(4, "EXAM: ICS 9999 Already sat", LocalDate.of(2026, 11, 20), 9, 11)
        )
        val exams = WidgetInsights.upcomingExams(entries, now)
        assertEquals(listOf("Mobile App Dev", "Spanish IV"), exams.map { it.name })
        assertEquals(10L, exams.first().daysLeft)
        assertEquals("in 10 days", WidgetInsights.daysLabel(exams.first().daysLeft))
        assertEquals("Tomorrow", WidgetInsights.daysLabel(1))
    }

    @Test
    fun `next up skips finished slots and ended classes`() {
        val monday = TimetableEntry(
            id = 10, title = "Mobile", dayOfWeek = DayOfWeek.MONDAY,
            startTime = LocalTime.of(8, 15), endTime = LocalTime.of(10, 15)
        )
        assertEquals(LocalDate.of(2026, 11, 23), WidgetInsights.nextUp(listOf(monday), now)?.date)
        // Classes that ended before Monday no longer come up.
        assertNull(WidgetInsights.nextUp(listOf(monday.copy(lastDate = LocalDate.of(2026, 11, 20))), now))
    }

    @Test
    fun `completions are counted per day, oldest first`() {
        val today = now.toLocalDate()
        fun doneAt(date: LocalDate) = Task(title = "t", isCompleted = true,
            completedAt = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli())
        val tasks = listOf(doneAt(today), doneAt(today), doneAt(today.minusDays(2)), Task(title = "open"))
        assertEquals(listOf(1, 0, 2), WidgetInsights.completionsByDay(tasks, today, 3, zone))
    }
}
