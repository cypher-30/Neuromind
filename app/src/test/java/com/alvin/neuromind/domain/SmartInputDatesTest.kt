package com.alvin.neuromind.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** Date phrases, pinned to Sunday 22 Nov 2026, 10:00. */
class SmartInputDatesTest {
    private val now = LocalDateTime.of(2026, 11, 22, 10, 0)
    private fun due(text: String): LocalDateTime? = SmartInputHelper.parseInput(text, now).dueDate
        ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime() }
    private fun title(text: String) = SmartInputHelper.parseInput(text, now).title

    @Test fun `next week is next Monday`() = assertEquals(LocalDateTime.of(2026, 11, 23, 9, 0), due("revise crypto next week"))
    @Test fun `the 30th`() {
        assertEquals(LocalDateTime.of(2026, 11, 30, 9, 0), due("submit report on the 30th"))
        assertEquals("submit report", title("submit report on the 30th"))
    }
    @Test fun `the 5th rolls to next month`() = assertEquals(LocalDateTime.of(2026, 12, 5, 9, 0), due("pay rent the 5th"))
    @Test fun `tonight`() {
        assertEquals(LocalDateTime.of(2026, 11, 22, 20, 0), due("read chapter 4 tonight"))
        assertEquals("read chapter 4", title("read chapter 4 tonight"))
    }
    @Test fun `in 2 hours`() = assertEquals(LocalDateTime.of(2026, 11, 22, 12, 0), due("call mum in 2 hours"))
    @Test fun `in an hour`() = assertEquals(LocalDateTime.of(2026, 11, 22, 11, 0), due("stretch in an hour"))
    @Test fun `Dec 4 with time`() {
        assertEquals(LocalDateTime.of(2026, 12, 4, 15, 0), due("essay due Dec 4 3pm"))
        assertEquals("essay", title("essay due Dec 4 3pm"))
    }
    @Test fun `4th of December`() = assertEquals(LocalDateTime.of(2026, 12, 4, 9, 0), due("lab on the 4th of December"))
    @Test fun `past month date goes to next year`() = assertEquals(LocalDateTime.of(2027, 1, 10, 9, 0), due("renew id jan 10"))
    @Test fun `time alone that has passed means tomorrow`() = assertEquals(LocalDateTime.of(2026, 11, 23, 8, 0), due("gym at 8am"))
    @Test fun `no date stays undated`() = assertNull(due("tidy desk"))
    @Test fun `errand with month date is on its day`() =
        assertEquals(true, SmartInputHelper.parseInput("go to the bank on Dec 1 at 10am", now).onDueDay)
}
