package com.alvin.neuromind.domain

import com.alvin.neuromind.data.Difficulty
import com.alvin.neuromind.data.Priority
import com.alvin.neuromind.data.Task
import com.alvin.neuromind.data.TimetableEntry
import com.alvin.neuromind.data.preferences.CognitiveProfile
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class SuggestionEngineTest {

    private val defaultProfile = CognitiveProfile(peakStart = 9, peakEnd = 12)
    private val peakTime = LocalDateTime.now().withHour(10).withMinute(0)
    // 21:00 → ninetyMinLater = 22:30; TimeFinder starts from 21:30; a 60-min slot would
    // end at 22:30 which is past dayEnd (22:00), so no today-slots are found → guarantees
    // FREE_SLOT_NUDGE (priority 2) does NOT fire, allowing ENERGY_MATCH / OVERDUE_ALERT tests
    // to be deterministic regardless of real system time.
    private val offPeakTime = LocalDateTime.now().withHour(21).withMinute(0)

    private fun task(
        id: Int = 1,
        title: String = "Test Task",
        priority: Priority = Priority.MEDIUM,
        difficulty: Difficulty = Difficulty.MEDIUM,
        isCompleted: Boolean = false,
        dueDate: Long? = null
    ) = Task(id = id, title = title, priority = priority, difficulty = difficulty, isCompleted = isCompleted, dueDate = dueDate)

    private val emptyTimetable = emptyList<TimetableEntry>()

    @Test
    fun `peak hour nudge fires when inside peak window and hard task exists`() {
        val tasks = listOf(
            task(id = 1, difficulty = Difficulty.HARD, priority = Priority.HIGH)
        )
        val suggestion = SuggestionEngine.suggest(tasks, emptyTimetable, defaultProfile, peakTime)
        assertNotNull(suggestion)
        assertEquals(SuggestionType.PEAK_HOUR_NUDGE, suggestion!!.type)
    }

    @Test
    fun `peak hour nudge does not fire when all hard-high tasks are completed`() {
        val tasks = listOf(
            task(id = 1, difficulty = Difficulty.HARD, priority = Priority.HIGH, isCompleted = true),
            task(id = 2, difficulty = Difficulty.EASY, priority = Priority.LOW)
        )
        val suggestion = SuggestionEngine.suggest(tasks, emptyTimetable, defaultProfile, peakTime)
        // Peak nudge should not fire — may fire ENERGY_MATCH or similar
        assertNotEquals(SuggestionType.PEAK_HOUR_NUDGE, suggestion?.type)
    }

    @Test
    fun `energy match fires outside peak hours when easy tasks remain`() {
        val tasks = listOf(
            task(id = 1, difficulty = Difficulty.EASY)
        )
        val suggestion = SuggestionEngine.suggest(tasks, emptyTimetable, defaultProfile, offPeakTime)
        assertNotNull(suggestion)
        assertEquals(SuggestionType.ENERGY_MATCH, suggestion!!.type)
        assertEquals(1, suggestion.taskId)
    }

    @Test
    fun `overdue alert fires for 1 or 2 overdue tasks but not for 0`() {
        val now = System.currentTimeMillis()
        val pastDue = now - 86_400_000L

        val zeroOverdue = listOf(task(id = 1, difficulty = Difficulty.HARD))
        val oneOverdue = listOf(task(id = 1, dueDate = pastDue))
        val twoOverdue = listOf(task(id = 1, dueDate = pastDue), task(id = 2, dueDate = pastDue))

        val noAlert = SuggestionEngine.suggest(zeroOverdue, emptyTimetable, defaultProfile, offPeakTime)
        assertNotEquals("No overdue alert for 0 overdue", SuggestionType.OVERDUE_ALERT, noAlert?.type)

        val alert1 = SuggestionEngine.suggest(oneOverdue, emptyTimetable, defaultProfile, offPeakTime)
        assertEquals("Overdue alert for 1 overdue", SuggestionType.OVERDUE_ALERT, alert1?.type)

        val alert2 = SuggestionEngine.suggest(twoOverdue, emptyTimetable, defaultProfile, offPeakTime)
        assertEquals("Overdue alert for 2 overdue", SuggestionType.OVERDUE_ALERT, alert2?.type)
    }

    @Test
    fun `returns null when there are no incomplete tasks`() {
        val tasks = listOf(
            task(id = 1, isCompleted = true),
            task(id = 2, isCompleted = true)
        )
        val suggestion = SuggestionEngine.suggest(tasks, emptyTimetable, defaultProfile, peakTime)
        assertNull("Should return null with no incomplete tasks", suggestion)
    }
}
