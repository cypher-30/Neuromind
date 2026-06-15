package com.alvin.neuromind.domain

import com.alvin.neuromind.data.FeedbackLog
import com.alvin.neuromind.data.Task
import com.alvin.neuromind.data.TimetableEntry
import com.alvin.neuromind.data.preferences.CognitiveProfile
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

enum class AssistantIntent {
    SCHEDULE_QUERY,
    TASK_QUERY,
    MOOD_RESPONSE,
    FOCUS_REQUEST,
    STATS_QUERY,
    FREE_TIME_QUERY,
    SUGGESTION_QUERY,
    CREATE_TASK,
    COMPLETE_TASK,
    GREETING,
    HELP,
    UNKNOWN
}

enum class AssistantAction {
    NAVIGATE_FOCUS,
    NAVIGATE_TASKS,
    NAVIGATE_TIMETABLE,
    NAVIGATE_INSIGHTS
}

data class AssistantResponse(
    val message: String,
    val actionType: AssistantAction? = null,
    val taskId: Int? = null
)

object NeuromindAssistant {

    fun classify(input: String): AssistantIntent {
        val lower = input.lowercase().trim()
        return when {
            // Action intents first (most specific)
            lower.containsAny("add task", "create task", "new task", "remind me to", "add a task") ->
                AssistantIntent.CREATE_TASK
            lower.containsAny("mark", "done", "finished", "complete", "i did", "i finished", "completed") &&
                !lower.containsAny("how many", "overdue", "pending", "all complete") ->
                AssistantIntent.COMPLETE_TASK

            // Greeting
            lower.containsAny("hi", "hello", "hey", "thanks", "thank you", "sup", "good morning", "good afternoon", "good evening") ->
                AssistantIntent.GREETING

            // Stats & wellness
            lower.containsAny("how am i doing", "my stats", "wellness", "my progress", "best day", "how have i been", "analytics") ->
                AssistantIntent.STATS_QUERY

            // Free time
            lower.containsAny("free time", "when am i free", "find time", "any gaps", "free slot", "when is a good time") ->
                AssistantIntent.FREE_TIME_QUERY

            // Next suggestion
            lower.containsAny("what should i do", "what next", "what to work on", "suggest", "recommend", "what can i do") ->
                AssistantIntent.SUGGESTION_QUERY

            // Original intents
            lower.containsAny("schedule", "timetable", "class", "today", "tomorrow", "when") ->
                AssistantIntent.SCHEDULE_QUERY
            lower.containsAny("task", "due", "how many", "overdue", "pending", "what do i have") ->
                AssistantIntent.TASK_QUERY
            lower.containsAny("tired", "stressed", "good", "great", "feeling", "energy", "exhausted", "low energy") ->
                AssistantIntent.MOOD_RESPONSE
            lower.containsAny("focus", "pomodoro", "timer", "concentrate", "work on", "start") ->
                AssistantIntent.FOCUS_REQUEST
            lower.containsAny("help", "what can you", "how do", "commands", "what do you") ->
                AssistantIntent.HELP

            else -> AssistantIntent.UNKNOWN
        }
    }

    fun respond(
        intent: AssistantIntent,
        tasks: List<Task>,
        timetable: List<TimetableEntry>,
        profile: CognitiveProfile,
        feedbackLogs: List<FeedbackLog> = emptyList()
    ): AssistantResponse {
        return when (intent) {
            AssistantIntent.SCHEDULE_QUERY  -> scheduleResponse(timetable)
            AssistantIntent.TASK_QUERY      -> taskQueryResponse(tasks)
            AssistantIntent.MOOD_RESPONSE   -> moodResponse(tasks)
            AssistantIntent.FOCUS_REQUEST   -> focusResponse(tasks)
            AssistantIntent.STATS_QUERY     -> statsResponse(tasks, feedbackLogs)
            AssistantIntent.FREE_TIME_QUERY -> freeTimeResponse(timetable)
            AssistantIntent.SUGGESTION_QUERY -> suggestionResponse(tasks, timetable, profile)
            AssistantIntent.GREETING        -> greetingResponse()
            // CREATE_TASK and COMPLETE_TASK are handled in AssistantViewModel (need repository)
            AssistantIntent.CREATE_TASK     -> AssistantResponse("Creating your task now…")
            AssistantIntent.COMPLETE_TASK   -> AssistantResponse("Looking for that task…")
            AssistantIntent.HELP            -> helpResponse()
            AssistantIntent.UNKNOWN         -> AssistantResponse(
                "I'm not quite sure what you mean. Try asking about your schedule, tasks, or say 'help' to see what I can do!"
            )
        }
    }

    // ─── Helper: strip verb prefixes to extract the target task title ───────────
    fun extractTaskTarget(input: String): String {
        val lower = input.lowercase()
        return lower
            .replace(Regex("(?i)^(mark|complete|finish|did|finished|i did|i finished|i completed)\\s+"), "")
            .replace(Regex("(?i)\\s+(done|complete|completed|as done|as complete|as finished)\\s*$"), "")
            .trim()
            .ifBlank { lower }
    }

    // ─── Private responders ──────────────────────────────────────────────────────

    private fun scheduleResponse(timetable: List<TimetableEntry>): AssistantResponse {
        val today = LocalDate.now()
        val todayEntries = timetable
            .filter { it.dayOfWeek == today.dayOfWeek }
            .sortedBy { it.startTime }

        return if (todayEntries.isEmpty()) {
            AssistantResponse(
                "You have no classes or events scheduled for today. A great day to focus on your tasks!",
                actionType = AssistantAction.NAVIGATE_TIMETABLE
            )
        } else {
            val formatter = DateTimeFormatter.ofPattern("h:mm a")
            val list = todayEntries.joinToString("\n") { entry ->
                val venue = entry.venue?.let { " @ $it" } ?: ""
                "• ${entry.title} at ${entry.startTime.format(formatter)}$venue"
            }
            AssistantResponse(
                "Here's your schedule for today:\n\n$list",
                actionType = AssistantAction.NAVIGATE_TIMETABLE
            )
        }
    }

    private fun taskQueryResponse(tasks: List<Task>): AssistantResponse {
        val incompleteTasks = tasks.filter { !it.isCompleted }
        val overdueTasks = incompleteTasks.filter { it.isOverdue }.sortedBy { it.dueDate }

        return if (overdueTasks.isEmpty()) {
            AssistantResponse(
                "You have ${incompleteTasks.size} pending task${if (incompleteTasks.size != 1) "s" else ""} and none are overdue. Keep it up!",
                actionType = AssistantAction.NAVIGATE_TASKS
            )
        } else {
            val oldest = overdueTasks.first()
            val formatter = SimpleDateFormat("MMM d", Locale.getDefault())
            val overdueDate = oldest.dueDate?.let { formatter.format(Date(it)) } ?: "unknown date"
            AssistantResponse(
                "You have ${overdueTasks.size} overdue task${if (overdueTasks.size != 1) "s" else ""}. " +
                "The oldest is '${oldest.title}', due $overdueDate.",
                actionType = AssistantAction.NAVIGATE_TASKS
            )
        }
    }

    private fun moodResponse(tasks: List<Task>): AssistantResponse {
        val incompleteTasks = tasks.filter { !it.isCompleted }
        val easyTask = incompleteTasks.minByOrNull { it.difficulty.ordinal }

        return if (easyTask == null) {
            AssistantResponse("Nothing on your task list right now. Take a well-earned break!")
        } else {
            AssistantResponse(
                "When energy is low, start small. How about '${easyTask.title}'? It's one of your lighter tasks.",
                actionType = AssistantAction.NAVIGATE_TASKS,
                taskId = easyTask.id
            )
        }
    }

    private fun focusResponse(tasks: List<Task>): AssistantResponse {
        val topTask = tasks
            .filter { !it.isCompleted }
            .maxWithOrNull(compareBy<Task> { it.priority }.thenBy { it.difficulty })

        return if (topTask == null) {
            AssistantResponse("No pending tasks to focus on. Enjoy your free time!")
        } else {
            AssistantResponse(
                "Starting a focus session for '${topTask.title}'. You've got this!",
                actionType = AssistantAction.NAVIGATE_FOCUS,
                taskId = topTask.id
            )
        }
    }

    private fun statsResponse(tasks: List<Task>, feedbackLogs: List<FeedbackLog>): AssistantResponse {
        val retro = RetroAnalyzer.analyze(tasks, feedbackLogs)
        if (retro == null) {
            return AssistantResponse(
                "Keep logging daily reviews — I need at least 7 days of data to show your full stats. " +
                "Check your current Insights in the meantime!",
                actionType = AssistantAction.NAVIGATE_INSIGHTS
            )
        }
        val onTrackPct = (retro.onTrackRate * 100).toInt()
        val bestDayStr = retro.bestDay?.let { dayName(it.day) } ?: "—"
        val recentFeedback = feedbackLogs.take(14)
        val avgEnergy = if (recentFeedback.isEmpty()) 0
                        else recentFeedback.sumOf { it.energyLevel } / recentFeedback.size
        val avgMoodScore = if (recentFeedback.isEmpty()) 0
                           else recentFeedback.sumOf { it.mood.score } / recentFeedback.size

        return AssistantResponse(
            "📊 Your stats snapshot:\n\n" +
            "✅ On-track rate: $onTrackPct%\n" +
            "🏆 Best day: $bestDayStr\n" +
            "⚡ Avg energy: $avgEnergy/5\n" +
            "😊 Avg mood: $avgMoodScore/5\n\n" +
            "Head to Insights for the full breakdown!",
            actionType = AssistantAction.NAVIGATE_INSIGHTS
        )
    }

    private fun freeTimeResponse(timetable: List<TimetableEntry>): AssistantResponse {
        val slots = TimeFinder.findSlots(
            timetable,
            durationMinutes = 60,
            lookaheadDays = 2,
            maxResults = 3
        )
        if (slots.isEmpty()) {
            return AssistantResponse(
                "You look pretty packed! Your timetable is full — check it to plan around your classes.",
                actionType = AssistantAction.NAVIGATE_TIMETABLE
            )
        }
        val formatter = DateTimeFormatter.ofPattern("h:mm a")
        val today = LocalDate.now()
        val slotList = slots.joinToString("\n") { slot ->
            val dayLabel = if (slot.date == today) "Today" else slot.date.dayOfWeek.name.lowercase()
                .replaceFirstChar { it.uppercase() }
            "• $dayLabel ${slot.startTime.format(formatter)} – ${slot.endTime.format(formatter)}"
        }
        return AssistantResponse(
            "Here are your next free slots (1+ hr):\n\n$slotList",
            actionType = AssistantAction.NAVIGATE_TIMETABLE
        )
    }

    private fun suggestionResponse(
        tasks: List<Task>,
        timetable: List<TimetableEntry>,
        profile: CognitiveProfile
    ): AssistantResponse {
        val suggestion = SuggestionEngine.suggest(tasks, timetable, profile)
        return if (suggestion != null) {
            AssistantResponse(
                message = suggestion.message,
                actionType = if (suggestion.taskId != null) AssistantAction.NAVIGATE_TASKS else null,
                taskId = suggestion.taskId
            )
        } else {
            val topTask = tasks.filter { !it.isCompleted }
                .maxWithOrNull(compareBy<Task> { it.priority }.thenBy { it.difficulty })
            if (topTask != null) {
                AssistantResponse(
                    "Your top priority right now is '${topTask.title}'. Give it your attention!",
                    actionType = AssistantAction.NAVIGATE_TASKS,
                    taskId = topTask.id
                )
            } else {
                AssistantResponse("You're all caught up! Enjoy the downtime or add new tasks when ready. 🎉")
            }
        }
    }

    private fun greetingResponse(): AssistantResponse {
        val greetings = listOf(
            "Hey! Ready to tackle the day? Ask me about your schedule, tasks, or how you're doing 😊",
            "Hi there! I'm here to help you stay on top of things. What would you like to know?",
            "Hello! Your productivity partner is ready. Ask me anything about your day!",
            "Hey! You've got this. What can I help you with today? 💪"
        )
        return AssistantResponse(greetings.random())
    }

    private fun helpResponse() = AssistantResponse(
        "Here's what I can help you with:\n\n" +
        "📅 \"What's on today?\" — see your schedule\n" +
        "✅ \"What tasks are overdue?\" — check pending tasks\n" +
        "😴 \"I'm tired\" — find an easy task\n" +
        "🎯 \"Start a focus session\" — launch the timer\n" +
        "📊 \"How am I doing?\" — see your stats\n" +
        "⏰ \"When am I free?\" — find open time slots\n" +
        "💡 \"What should I do next?\" — get a smart suggestion\n" +
        "➕ \"Add task: essay due Friday 3pm\" — create a task\n" +
        "☑️ \"Mark essay done\" — complete a task\n\n" +
        "Just ask me anything about your day!"
    )

    private fun dayName(day: DayOfWeek): String =
        day.name.lowercase().replaceFirstChar { it.uppercase() }

    private fun String.containsAny(vararg keywords: String): Boolean =
        keywords.any { this.contains(it) }
}
