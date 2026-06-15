package com.alvin.neuromind.domain

import com.alvin.neuromind.data.FocusSession
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class FocusStatsTest {

    private val zone = ZoneId.systemDefault()
    private val today = LocalDate.now()

    private fun session(
        daysAgo: Long,
        durationMinutes: Int,
        title: String = "Study"
    ) = FocusSession(
        taskTitle = title,
        durationMinutes = durationMinutes,
        completedAt = today.minusDays(daysAgo)
            .atStartOfDay(zone).toInstant().toEpochMilli() + 3_600_000L  // 1am same day
    )

    @Test
    fun `empty list returns zeroed summary with no best day`() {
        val summary = FocusStats.summarize(emptyList(), today)
        assertEquals(0, summary.totalMinutesThisWeek)
        assertEquals(0, summary.sessionCountThisWeek)
        assertNull(summary.bestDayLabel)
    }

    @Test
    fun `total minutes and session count cover last 7 days`() {
        val sessions = listOf(
            session(daysAgo = 0, durationMinutes = 25),
            session(daysAgo = 1, durationMinutes = 50),
            session(daysAgo = 6, durationMinutes = 30)
        )
        val summary = FocusStats.summarize(sessions, today)
        assertEquals(105, summary.totalMinutesThisWeek)
        assertEquals(3, summary.sessionCountThisWeek)
    }

    @Test
    fun `session older than 7 days is not counted in weekly total`() {
        val sessions = listOf(
            session(daysAgo = 0, durationMinutes = 25),
            session(daysAgo = 7, durationMinutes = 100)   // exactly 7 days ago = outside the 6-day window
        )
        val summary = FocusStats.summarize(sessions, today)
        // daysAgo=7 maps to weekDays.first() which is 6 days ago; daysAgo=7 is one day further back
        // Our window is today through 6 days ago (7 entries). daysAgo=7 falls outside.
        assertEquals(25, summary.totalMinutesThisWeek)
    }

    @Test
    fun `minutesByDay has exactly 7 entries`() {
        val summary = FocusStats.summarize(emptyList(), today)
        assertEquals(7, summary.minutesByDay.size)
    }

    @Test
    fun `best day label is the day with most focus minutes`() {
        // Put 90 minutes on day 2 (2 days ago) and 25 on today
        val sessions = listOf(
            session(daysAgo = 2, durationMinutes = 45, title = "A"),
            session(daysAgo = 2, durationMinutes = 45, title = "B"),
            session(daysAgo = 0, durationMinutes = 25)
        )
        val summary = FocusStats.summarize(sessions, today)
        assertNotNull(summary.bestDayLabel)
        // The best-day label should match the short day name for (today - 2 days)
        val expectedDay = today.minusDays(2)
        val expectedLabel = expectedDay.dayOfWeek
            .getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())
        assertEquals(expectedLabel, summary.bestDayLabel)
    }

    @Test
    fun `best day is null when all minutes are zero`() {
        // Only session that falls outside the 7-day window
        val sessions = listOf(session(daysAgo = 10, durationMinutes = 50))
        val summary = FocusStats.summarize(sessions, today)
        assertNull(summary.bestDayLabel)
    }
}
