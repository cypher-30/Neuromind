package com.alvin.neuromind.domain

import com.alvin.neuromind.data.FeedbackLog
import com.alvin.neuromind.data.Mood
import org.junit.Assert.*
import org.junit.Test

class BurnoutAnalyzerTest {

    private fun log(
        daysAgo: Long,
        energyLevel: Int,
        mood: Mood = Mood.NEUTRAL,
        comment: String? = null
    ) = FeedbackLog(
        date = System.currentTimeMillis() - (daysAgo * 24 * 60 * 60 * 1000),
        mood = mood,
        energyLevel = energyLevel,
        tasksCompleted = 0,
        comment = comment
    )

    // --- Trigger A: 3+ consecutive low-energy days ---

    @Test
    fun `three consecutive low energy days triggers LowEnergy warning`() {
        val logs = listOf(
            log(daysAgo = 2, energyLevel = 1),
            log(daysAgo = 1, energyLevel = 2),
            log(daysAgo = 0, energyLevel = 1)
        )
        val result = BurnoutAnalyzer.analyze(logs)
        assertTrue("Expected LowEnergy but got $result", result is BurnoutState.LowEnergy)
        assertEquals(3, (result as BurnoutState.LowEnergy).consecutiveDays)
    }

    @Test
    fun `two consecutive low energy days does not trigger`() {
        val logs = listOf(
            log(daysAgo = 1, energyLevel = 2),
            log(daysAgo = 0, energyLevel = 1)
        )
        // Not three in a row — should not trigger Trigger A
        // (may still trigger B if stressed, but not in this test)
        val result = BurnoutAnalyzer.analyze(logs)
        assertFalse(result is BurnoutState.LowEnergy)
    }

    @Test
    fun `healthy energy levels return null`() {
        val logs = listOf(
            log(daysAgo = 2, energyLevel = 4, mood = Mood.GOOD),
            log(daysAgo = 1, energyLevel = 5, mood = Mood.GREAT),
            log(daysAgo = 0, energyLevel = 3, mood = Mood.NEUTRAL)
        )
        assertNull(BurnoutAnalyzer.analyze(logs))
    }

    @Test
    fun `empty log list returns null`() {
        assertNull(BurnoutAnalyzer.analyze(emptyList()))
    }

    @Test
    fun `low energy streak broken by good day resets counter`() {
        // Days 4, 3 are low; day 2 is good; days 1, 0 are low — max streak = 2
        val logs = listOf(
            log(daysAgo = 4, energyLevel = 1),
            log(daysAgo = 3, energyLevel = 2),
            log(daysAgo = 2, energyLevel = 4),
            log(daysAgo = 1, energyLevel = 1),
            log(daysAgo = 0, energyLevel = 2)
        )
        val result = BurnoutAnalyzer.analyze(logs)
        assertFalse("Broken streak should not trigger LowEnergy", result is BurnoutState.LowEnergy)
    }

    // --- Trigger B: repeated stress on a specific weekday ---

    @Test
    fun `two stressed entries on same weekday triggers WeekdayStress`() {
        // Both logs today (same weekday), with STRESSED mood
        val logs = listOf(
            log(daysAgo = 0, energyLevel = 3, mood = Mood.STRESSED),
            log(daysAgo = 0, energyLevel = 4, mood = Mood.STRESSED)
        )
        val result = BurnoutAnalyzer.analyze(logs)
        assertTrue("Expected WeekdayStress but got $result", result is BurnoutState.WeekdayStress)
    }

    @Test
    fun `two tired entries on same weekday triggers WeekdayStress`() {
        val logs = listOf(
            log(daysAgo = 0, energyLevel = 3, mood = Mood.TIRED),
            log(daysAgo = 0, energyLevel = 2, mood = Mood.TIRED)
        )
        val result = BurnoutAnalyzer.analyze(logs)
        assertTrue(result is BurnoutState.WeekdayStress)
    }

    @Test
    fun `single stressed entry does not trigger WeekdayStress`() {
        val logs = listOf(
            log(daysAgo = 0, energyLevel = 3, mood = Mood.STRESSED),
            log(daysAgo = 1, energyLevel = 4, mood = Mood.GOOD)
        )
        // Only 1 stressed entry on the given weekday
        val result = BurnoutAnalyzer.analyze(logs)
        assertFalse(result is BurnoutState.WeekdayStress)
    }

    // --- Trigger A takes priority over B ---

    @Test
    fun `LowEnergy is returned before WeekdayStress when both conditions met`() {
        // 3 consecutive low-energy days AND 2 stressed on the same day
        val logs = listOf(
            log(daysAgo = 2, energyLevel = 1, mood = Mood.STRESSED),
            log(daysAgo = 2, energyLevel = 1, mood = Mood.STRESSED),
            log(daysAgo = 1, energyLevel = 2),
            log(daysAgo = 0, energyLevel = 1)
        )
        val result = BurnoutAnalyzer.analyze(logs)
        assertTrue("LowEnergy should take priority", result is BurnoutState.LowEnergy)
    }
}
