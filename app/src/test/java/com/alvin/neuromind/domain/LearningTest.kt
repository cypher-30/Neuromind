package com.alvin.neuromind.domain

import com.alvin.neuromind.data.FocusSession
import com.alvin.neuromind.data.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class LearningTest {
    private val zone = ZoneId.systemDefault()
    private fun at(day: Int, h: Int, m: Int = 0) =
        LocalDateTime.of(LocalDate.of(2026, 11, day), java.time.LocalTime.of(h, m)).atZone(zone).toInstant().toEpochMilli()
    private fun session(day: Int, endH: Int, minutes: Int, taskId: Int? = null) =
        FocusSession(taskId = taskId, taskTitle = "x", durationMinutes = minutes, completedAt = at(day, endH))

    @Test
    fun `evening focus is learned as the peak`() {
        // Six evening sessions ending 9-10 pm, one morning session.
        val sessions = (1..6).map { session(it, 21 + it % 2, 50) } + session(7, 9, 50)
        val peak = Learning.peakWindow(sessions)!!
        assertTrue("start ${peak.startHour}", peak.startHour in 18..20)
        assertEquals(peak.startHour + 3, peak.endHour)
    }

    @Test
    fun `too little or too scattered data gives no suggestion`() {
        assertNull(Learning.peakWindow(listOf(session(1, 21, 50), session(2, 21, 50))))
        val scattered = (6..22 step 2).mapIndexed { i, h -> session(i + 1, h, 25) }
        assertNull(Learning.peakWindow(scattered))
    }

    @Test
    fun `estimate bias uses the median and needs three tasks`() {
        val tasks = (1..4).map { Task(id = it, title = "Crypto $it", subject = "ICS 3201 Crypto", durationMinutes = 60, isCompleted = true) }
        // Focus: 90, 90, 100, and one outlier 300 -> median 95 / 60 = ~1.58.
        val sessions = listOf(90, 90, 100, 300).mapIndexed { i, m -> session(i + 1, 20, m, taskId = i + 1) }
        val bias = Learning.estimateBias(tasks, sessions).single()
        assertEquals("ICS 3201 Crypto", bias.subject)
        assertEquals(1.58, bias.ratio, 0.01)
        assertTrue(Learning.estimateBias(tasks.take(2), sessions).isEmpty())
    }
}
