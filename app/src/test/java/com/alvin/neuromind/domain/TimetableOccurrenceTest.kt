package com.alvin.neuromind.domain

import com.alvin.neuromind.data.TimetableEntry
import com.alvin.neuromind.ui.timetable.TimetableViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class TimetableOccurrenceTest {
    // A Tuesday.
    private val today = LocalDate.of(2026, 3, 10)

    private val weekly = TimetableEntry(
        id = 1, title = "Tuesday lecture", dayOfWeek = DayOfWeek.TUESDAY,
        startTime = LocalTime.of(10, 0), endTime = LocalTime.of(12, 0)
    )
    private fun event(id: Int, date: LocalDate, allDay: Boolean = false) = TimetableEntry(
        id = id, title = "Event $id", dayOfWeek = date.dayOfWeek,
        startTime = if (allDay) LocalTime.MIDNIGHT else LocalTime.of(14, 0),
        endTime = if (allDay) LocalTime.of(23, 59) else LocalTime.of(15, 0),
        isRecurring = false, date = date, isAllDay = allDay
    )

    @Test
    fun `weekly entries occur on their weekday, events only on their date`() {
        assertTrue(weekly.occursOn(today))
        assertTrue(weekly.occursOn(today.plusWeeks(3)))
        assertFalse(weekly.occursOn(today.plusDays(1)))

        val conference = event(2, today.plusDays(9))
        assertTrue(conference.occursOn(today.plusDays(9)))
        assertFalse(conference.occursOn(today.plusDays(2)))
        assertFalse(conference.occursOn(today.plusDays(16)))
    }

    @Test
    fun `all-day events block the whole day`() {
        val allDay = event(3, today, allDay = true)
        assertTrue(allDay.blocks(today, LocalTime.of(6, 0), LocalTime.of(7, 0)))
        assertTrue(allDay.blocks(today, LocalTime.of(22, 0), LocalTime.of(23, 0)))
        assertFalse(allDay.blocks(today.plusDays(1), LocalTime.of(6, 0), LocalTime.of(7, 0)))

        assertTrue(weekly.blocks(today, LocalTime.of(11, 0), LocalTime.of(13, 0)))
        assertFalse(weekly.blocks(today, LocalTime.of(12, 0), LocalTime.of(13, 0)))
    }

    @Test
    fun `time finder skips days with an all-day event`() {
        val slots = TimeFinder.findSlots(
            timetable = listOf(event(4, today.plusDays(1), allDay = true)),
            durationMinutes = 60,
            lookaheadDays = 3,
            maxResults = 50,
            referenceDate = today,
            referenceTime = LocalTime.of(7, 0)
        )
        assertTrue(slots.isNotEmpty())
        assertTrue(slots.none { it.date == today.plusDays(1) })
    }

    @Test
    fun `agenda groups by real date, then later and past events`() {
        val entries = listOf(
            weekly,
            event(5, today.plusDays(1)),
            event(6, today, allDay = true),
            event(7, today.plusDays(30)),
            event(8, today.minusDays(2))
        )

        val sections = TimetableViewModel.buildSections(entries, today)

        assertEquals(listOf("Today", "Tomorrow", "Later", "Past events"), sections.map { it.title })
        // All-day items sort first within a day.
        assertEquals(listOf(6, 1), sections[0].entries.map { it.id })
        assertEquals(listOf(5), sections[1].entries.map { it.id })
        assertEquals(listOf(7), sections[2].entries.map { it.id })
        assertEquals(listOf(8), sections[3].entries.map { it.id })
    }
}
