package com.alvin.neuromind.domain

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
}
