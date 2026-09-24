package com.alvin.neuromind.domain

import com.alvin.neuromind.data.EventReminder
import com.alvin.neuromind.data.TimetableEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class EventReminderPolicyTest {
    private val date = LocalDate.of(2026, 5, 20)

    private fun trigger(start: LocalTime, mode: EventReminder?, allDay: Boolean = false) =
        EventReminderPolicy.triggerAt(date, start, allDay, mode)

    @Test
    fun `default is ten minutes before a daytime event`() {
        assertEquals(date.atTime(13, 50), trigger(LocalTime.of(14, 0), EventReminder.AUTO))
        assertEquals(date.atTime(8, 50), trigger(LocalTime.of(9, 0), EventReminder.AUTO))
    }

    @Test
    fun `default is previous evening for events before 9 AM`() {
        assertEquals(date.minusDays(1).atTime(21, 0), trigger(LocalTime.of(8, 30), EventReminder.AUTO))
        assertEquals(date.minusDays(1).atTime(21, 0), trigger(LocalTime.of(0, 30), EventReminder.AUTO))
    }

    @Test
    fun `default for all-day events is 9 AM on the day`() {
        assertEquals(date.atTime(9, 0), trigger(LocalTime.MIDNIGHT, EventReminder.AUTO, allDay = true))
    }

    @Test
    fun `explicit choices override the default`() {
        val start = LocalTime.of(7, 0)
        assertEquals(date.atTime(7, 0), trigger(start, EventReminder.AT_START))
        assertEquals(date.atTime(6, 50), trigger(start, EventReminder.MINUTES_10))
        assertEquals(date.atTime(6, 30), trigger(start, EventReminder.MINUTES_30))
        assertEquals(date.minusDays(1).atTime(7, 0), trigger(start, EventReminder.DAY_BEFORE))
    }

    @Test
    fun `all-day explicit choices count back from 9 AM`() {
        assertEquals(date.atTime(8, 30), trigger(LocalTime.MIDNIGHT, EventReminder.MINUTES_30, allDay = true))
        assertEquals(date.minusDays(1).atTime(9, 0), trigger(LocalTime.MIDNIGHT, EventReminder.DAY_BEFORE, allDay = true))
    }

    @Test
    fun `off and legacy entries get no alarm`() {
        assertNull(trigger(LocalTime.NOON, EventReminder.OFF))
        assertNull(trigger(LocalTime.NOON, null))
    }

    @Test
    fun `recurring classes never get event alarms`() {
        val weekly = TimetableEntry(
            title = "Lecture", dayOfWeek = date.dayOfWeek, startTime = LocalTime.of(10, 0), endTime = LocalTime.of(11, 0),
            isRecurring = true, date = date, reminderMode = EventReminder.AUTO
        )
        assertNull(EventReminderPolicy.triggerAt(weekly))
        assertEquals(date.atTime(9, 50), EventReminderPolicy.triggerAt(weekly.copy(isRecurring = false)))
    }
}
