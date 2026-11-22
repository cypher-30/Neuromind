package com.alvin.neuromind.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class SmartInputHelperTest {

    @Test
    fun `parseInput handles tomorrow`() {
        val input = "study tomorrow"
        val parsed = SmartInputHelper.parseInput(input)
        
        assertEquals("study", parsed.title)
        assertNotNull(parsed.dueDate)
    }

    @Test
    fun `parseInput handles next monday`() {
        val input = "next monday at 2pm gym"
        val parsed = SmartInputHelper.parseInput(input)
        
        assertEquals("gym", parsed.title)
        assertNotNull(parsed.dueDate)
    }

    @Test
    fun `parseInput handles in 3 days`() {
        val input = "remind me to call mom in 3 days"
        val parsed = SmartInputHelper.parseInput(input)
        
        assertEquals("call mom", parsed.title)
        assertNotNull(parsed.dueDate)
    }

    @Test
    fun `parseInput handles time at 5pm`() {
        val input = "buy milk at 5pm"
        val parsed = SmartInputHelper.parseInput(input)
        
        assertEquals("buy milk", parsed.title)
        assertNotNull(parsed.dueDate)
    }

    @Test
    fun `parseInput cleans up prefixes`() {
        val input = "remind me to add task write code"
        val parsed = SmartInputHelper.parseInput(input)
        
        assertEquals("write code", parsed.title)
    }

    @Test
    fun `errands with a day are flagged for that day`() {
        val p = SmartInputHelper.parseInput("remind me to go to Upperhill Medical Centre on Monday at 2:45pm")
        assertEquals("go to Upperhill Medical Centre", p.title)
        assertTrue(p.onDueDay)
        assertTrue(SmartInputHelper.parseInput("I need to sign attendance with K Ouma tomorrow").onDueDay)
        assertTrue(SmartInputHelper.parseInput("dentist appointment friday at 10am").onDueDay)
    }

    @Test
    fun `work and undated errands are not flagged`() {
        assertFalse(SmartInputHelper.parseInput("add task: essay due Friday 3pm").onDueDay)
        assertFalse(SmartInputHelper.parseInput("revise chapters 4 and 5 tomorrow").onDueDay)
        assertFalse(SmartInputHelper.parseInput("go to the library").onDueDay) // no day given
    }

    @Test
    fun `need-to sentences with a day create tasks, others don't`() {
        assertEquals(AssistantIntent.CREATE_TASK, NeuromindAssistant.classify("I need to go to Parklands on Monday"))
        assertEquals(AssistantIntent.FOCUS_REQUEST, NeuromindAssistant.classify("I need to focus"))
    }
}
