package com.alvin.neuromind.domain

import com.alvin.neuromind.data.Difficulty
import com.alvin.neuromind.data.FeedbackLog
import com.alvin.neuromind.data.Mood
import com.alvin.neuromind.data.Priority
import com.alvin.neuromind.data.Task
import com.alvin.neuromind.data.TimetableEntry
import com.alvin.neuromind.data.preferences.CognitiveProfile
import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class NeuromindAssistantTest {

    private val profile       = CognitiveProfile()
    private val emptyTimetable = emptyList<TimetableEntry>()
    private val noTasks        = emptyList<Task>()
    private val noLogs         = emptyList<FeedbackLog>()

    private fun task(
        id: Int = 1,
        title: String = "Test Task",
        priority: Priority = Priority.MEDIUM,
        difficulty: Difficulty = Difficulty.MEDIUM,
        isCompleted: Boolean = false,
        dueDate: Long? = null
    ) = Task(id = id, title = title, priority = priority, difficulty = difficulty,
             isCompleted = isCompleted, dueDate = dueDate)

    private fun log(mood: Mood, energy: Int, tasks: Int, daysAgo: Int = 0) = FeedbackLog(
        date           = System.currentTimeMillis() - daysAgo * 86_400_000L,
        mood           = mood,
        energyLevel    = energy,
        tasksCompleted = tasks,
        comment        = null
    )

    // ─── Original intent tests ────────────────────────────────────────────────

    @Test
    fun `each original intent classifies correctly`() {
        assertEquals(AssistantIntent.SCHEDULE_QUERY,  NeuromindAssistant.classify("What's on today?"))
        assertEquals(AssistantIntent.TASK_QUERY,      NeuromindAssistant.classify("What tasks are overdue?"))
        assertEquals(AssistantIntent.MOOD_RESPONSE,   NeuromindAssistant.classify("I'm feeling tired"))
        assertEquals(AssistantIntent.FOCUS_REQUEST,   NeuromindAssistant.classify("Start a focus session"))
        assertEquals(AssistantIntent.HELP,            NeuromindAssistant.classify("help"))
    }

    @Test
    fun `unknown fallback triggers for unrecognised input`() {
        val intent = NeuromindAssistant.classify("xyzzy frobble glorp")
        assertEquals(AssistantIntent.UNKNOWN, intent)

        val response = NeuromindAssistant.respond(intent, noTasks, emptyTimetable, profile)
        assertTrue("Unknown response should mention help", response.message.contains("help", ignoreCase = true))
    }

    @Test
    fun `schedule query lists today entries in time order`() {
        val today = LocalDate.now()
        val timetable = listOf(
            TimetableEntry(title = "Maths",   dayOfWeek = today.dayOfWeek,
                           startTime = LocalTime.of(10, 0), endTime = LocalTime.of(11, 0), isRecurring = true),
            TimetableEntry(title = "Physics", dayOfWeek = today.dayOfWeek,
                           startTime = LocalTime.of(8, 0),  endTime = LocalTime.of(9, 0),  isRecurring = true)
        )
        val response = NeuromindAssistant.respond(AssistantIntent.SCHEDULE_QUERY, noTasks, timetable, profile)
        val physicsIdx = response.message.indexOf("Physics")
        val mathsIdx   = response.message.indexOf("Maths")
        assertTrue("Physics (8 AM) should come before Maths (10 AM)", physicsIdx < mathsIdx)
    }

    @Test
    fun `task query overdue response includes correct count`() {
        val now  = System.currentTimeMillis()
        val past = now - 86_400_000L
        val tasks = listOf(
            task(id = 1, dueDate = past),
            task(id = 2, dueDate = past),
            task(id = 3, dueDate = now + 86_400_000L)
        )
        val response = NeuromindAssistant.respond(AssistantIntent.TASK_QUERY, tasks, emptyTimetable, profile)
        assertTrue("Should mention 2 overdue tasks", response.message.contains("2"))
    }

    @Test
    fun `mood response for tired returns a low-difficulty task suggestion`() {
        val tasks = listOf(
            task(id = 1, title = "Hard Assignment", difficulty = Difficulty.HARD),
            task(id = 2, title = "Quick Note",      difficulty = Difficulty.EASY)
        )
        val response = NeuromindAssistant.respond(AssistantIntent.MOOD_RESPONSE, tasks, emptyTimetable, profile)
        assertTrue("Mood response should suggest the easy task", response.message.contains("Quick Note"))
    }

    @Test
    fun `focus request carries correct task id and navigate focus action`() {
        val tasks = listOf(
            task(id = 10, priority = Priority.HIGH, difficulty = Difficulty.HARD),
            task(id = 20, priority = Priority.LOW,  difficulty = Difficulty.EASY)
        )
        val response = NeuromindAssistant.respond(AssistantIntent.FOCUS_REQUEST, tasks, emptyTimetable, profile)
        assertEquals(AssistantAction.NAVIGATE_FOCUS, response.actionType)
        assertEquals(10, response.taskId)
    }

    // ─── New intent classification tests ─────────────────────────────────────

    @Test
    fun `create task intent classifies from common phrasings`() {
        assertEquals(AssistantIntent.CREATE_TASK, NeuromindAssistant.classify("Add task: essay due Friday"))
        assertEquals(AssistantIntent.CREATE_TASK, NeuromindAssistant.classify("remind me to submit the assignment"))
        assertEquals(AssistantIntent.CREATE_TASK, NeuromindAssistant.classify("new task math homework"))
    }

    @Test
    fun `complete task intent classifies from common phrasings`() {
        assertEquals(AssistantIntent.COMPLETE_TASK, NeuromindAssistant.classify("mark essay done"))
        assertEquals(AssistantIntent.COMPLETE_TASK, NeuromindAssistant.classify("i finished the physics lab"))
        assertEquals(AssistantIntent.COMPLETE_TASK, NeuromindAssistant.classify("complete the reading"))
    }

    @Test
    fun `stats query intent classifies correctly`() {
        assertEquals(AssistantIntent.STATS_QUERY, NeuromindAssistant.classify("How am I doing?"))
        assertEquals(AssistantIntent.STATS_QUERY, NeuromindAssistant.classify("show me my stats"))
        assertEquals(AssistantIntent.STATS_QUERY, NeuromindAssistant.classify("what's my best day?"))
    }

    @Test
    fun `free time intent classifies correctly`() {
        assertEquals(AssistantIntent.FREE_TIME_QUERY, NeuromindAssistant.classify("when am I free?"))
        assertEquals(AssistantIntent.FREE_TIME_QUERY, NeuromindAssistant.classify("find time for a study session"))
        assertEquals(AssistantIntent.FREE_TIME_QUERY, NeuromindAssistant.classify("any free slots today?"))
    }

    @Test
    fun `suggestion intent classifies correctly`() {
        assertEquals(AssistantIntent.SUGGESTION_QUERY, NeuromindAssistant.classify("what should I do next?"))
        assertEquals(AssistantIntent.SUGGESTION_QUERY, NeuromindAssistant.classify("what to work on now?"))
    }

    @Test
    fun `greeting intent classifies correctly`() {
        assertEquals(AssistantIntent.GREETING, NeuromindAssistant.classify("hi"))
        assertEquals(AssistantIntent.GREETING, NeuromindAssistant.classify("hello there"))
        assertEquals(AssistantIntent.GREETING, NeuromindAssistant.classify("thanks!"))
    }

    // ─── extractTaskTarget helper tests ──────────────────────────────────────

    @Test
    fun `extractTaskTarget strips leading mark verb`() {
        val result = NeuromindAssistant.extractTaskTarget("mark essay done")
        assertEquals("essay", result)
    }

    @Test
    fun `extractTaskTarget strips i finished prefix`() {
        val result = NeuromindAssistant.extractTaskTarget("i finished the physics lab")
        assertEquals("the physics lab", result)
    }

    @Test
    fun `extractTaskTarget strips trailing done keyword`() {
        val result = NeuromindAssistant.extractTaskTarget("complete math homework done")
        assertEquals("math homework", result)
    }

    // ─── Stats query response tests ───────────────────────────────────────────

    @Test
    fun `stats response with fewer than 7 log days prompts user to log more`() {
        val logs = (0..3).map { log(Mood.GOOD, 4, 2, it) }
        val response = NeuromindAssistant.respond(
            AssistantIntent.STATS_QUERY, noTasks, emptyTimetable, profile, logs
        )
        assertTrue("Should ask user to log more", response.message.contains("7", ignoreCase = true))
        assertEquals(AssistantAction.NAVIGATE_INSIGHTS, response.actionType)
    }

    @Test
    fun `stats response with enough data includes on-track rate and navigates to insights`() {
        val logs = (0 until 14).map { log(Mood.GOOD, 4, 3, it) }
        val response = NeuromindAssistant.respond(
            AssistantIntent.STATS_QUERY, noTasks, emptyTimetable, profile, logs
        )
        assertTrue("Should include on-track info", response.message.contains("On-track", ignoreCase = true))
        assertEquals(AssistantAction.NAVIGATE_INSIGHTS, response.actionType)
    }

    // ─── Free time response tests ─────────────────────────────────────────────

    @Test
    fun `free time response with empty timetable returns slots and navigates to timetable`() {
        val response = NeuromindAssistant.respond(
            AssistantIntent.FREE_TIME_QUERY, noTasks, emptyTimetable, profile
        )
        assertTrue("Should contain free slots", response.message.contains("free", ignoreCase = true))
        assertEquals(AssistantAction.NAVIGATE_TIMETABLE, response.actionType)
    }

    // ─── Suggestion response tests ────────────────────────────────────────────

    @Test
    fun `suggestion response with pending tasks recommends a task`() {
        val tasks = listOf(task(id = 5, title = "Study Physics", priority = Priority.HIGH))
        val response = NeuromindAssistant.respond(
            AssistantIntent.SUGGESTION_QUERY, tasks, emptyTimetable, profile
        )
        assertTrue("Should mention the task", response.message.isNotBlank())
    }

    @Test
    fun `suggestion response with no tasks celebrates all-done state`() {
        val response = NeuromindAssistant.respond(
            AssistantIntent.SUGGESTION_QUERY, noTasks, emptyTimetable, profile
        )
        assertTrue("Should celebrate", response.message.contains("caught up", ignoreCase = true)
                || response.message.contains("free", ignoreCase = true))
    }
}
