package com.alvin.neuromind.ui.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alvin.neuromind.data.Priority
import com.alvin.neuromind.data.Task
import com.alvin.neuromind.data.TaskRepository
import com.alvin.neuromind.data.preferences.UserPreferencesRepository
import com.alvin.neuromind.domain.AssistantAction
import com.alvin.neuromind.domain.AssistantIntent
import com.alvin.neuromind.domain.NeuromindAssistant
import com.alvin.neuromind.domain.SmartInputHelper
import com.alvin.neuromind.domain.TaskMatch
import com.alvin.neuromind.domain.TaskMatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PendingNav(val action: AssistantAction, val taskId: Int? = null)

data class ChatMessage(
    val id: Int,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    /** Optional follow-up the user can tap; replies never navigate on their own. */
    val action: PendingNav? = null
)

data class AssistantUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isThinking: Boolean = false
)

class AssistantViewModel(
    private val repository: TaskRepository,
    private val userPrefs: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    private var nextId = 0

    // Conversation context, so follow-ups work: "mark it done", "yes", "and tomorrow?".
    private var lastTask: Task? = null
    private var pendingComplete: Task? = null
    private var lastIntent: AssistantIntent? = null

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) return

        val userMsg = ChatMessage(id = nextId++, text = text, isUser = true)
        _uiState.update { it.copy(messages = it.messages + userMsg, inputText = "", isThinking = true) }

        viewModelScope.launch {
            val tasks        = repository.allTasks.first()
            val timetable    = repository.allTimetableEntries.first()
            val feedbackLogs = repository.allFeedbackLogs.first()
            val profile      = userPrefs.cognitiveProfile.first()

            val lower = text.lowercase().trim().trimEnd('?', '!', '.')

            // "yes" / "no" to "Did you mean …?"
            pendingComplete?.let { task ->
                pendingComplete = null
                if (Regex("^(yes|yeah|yep|yup|sure|correct|that one|ok|okay)\\b").containsMatchIn(lower)) {
                    complete(task); return@launch
                }
                if (Regex("^(no|nope|nah)\\b").containsMatchIn(lower)) {
                    reply("Okay, I left it. Try a few more words from the task's name."); return@launch
                }
            }

            var intent = NeuromindAssistant.classify(text)
            // "and tomorrow?" / "what about Friday?": the last question, another day.
            val day = NeuromindAssistant.dayIn(text)
            val shortFollowUp = day != null && lower.split(Regex("\\s+")).size <= 4 &&
                Regex("^(and|what about|how about|ok and)\\b").containsMatchIn(lower)
            if (shortFollowUp) {
                intent = if (lastIntent == AssistantIntent.TASK_QUERY) AssistantIntent.TASK_QUERY else AssistantIntent.SCHEDULE_QUERY
            }
            lastIntent = intent

            when {
                intent == AssistantIntent.CREATE_TASK -> handleCreateTask(text)
                intent == AssistantIntent.COMPLETE_TASK -> handleCompleteTask(text, tasks)
                intent == AssistantIntent.TASK_QUERY && day != null ->
                    reply(NeuromindAssistant.tasksDueOnResponse(tasks, day).message)
                else -> {
                    val response = NeuromindAssistant.respond(intent, tasks, timetable, profile, feedbackLogs, input = text)
                    val action   = response.actionType?.let { PendingNav(it, response.taskId) }
                    val botMsg   = ChatMessage(id = nextId++, text = response.message, isUser = false, action = action)
                    _uiState.update { it.copy(messages = it.messages + botMsg, isThinking = false) }
                }
            }
        }
    }

    private fun handleCreateTask(text: String) {
        val parsed  = SmartInputHelper.parseInput(text)
        val newTask = Task(title = parsed.title, dueDate = parsed.dueDate, priority = Priority.MEDIUM, onDueDay = parsed.onDueDay)

        viewModelScope.launch {
            repository.insertTask(newTask)
            // Remember it for "mark it done" (insertTask doesn't return the id).
            lastTask = repository.allTasks.first().lastOrNull { it.title == newTask.title && !it.isCompleted }

            val duePart = if (parsed.dueDate != null) {
                val fmt = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                " due ${fmt.format(Date(parsed.dueDate))}"
            } else ""

            val botMsg = ChatMessage(
                id     = nextId++,
                text   = "Added \"${parsed.title}\"$duePart to your tasks! ✅" +
                    (if (parsed.onDueDay) "\nIt's an errand for that day, so it stays out of your plan until then." else ""),
                isUser = false
            )
            _uiState.update { it.copy(messages = it.messages + botMsg, isThinking = false) }
        }
    }

    private fun handleCompleteTask(text: String, tasks: List<Task>) {
        val target = NeuromindAssistant.extractTaskTarget(text)
        // "mark it done" / "that's done": the task we just talked about.
        val rest = target.replace(Regex("\\b(done|finished|complete|completed|as|mark)\\b"), "").trim()
        val last = lastTask
        if (rest.isEmpty() || rest in setOf("it", "that", "this", "that one", "this one", "that's", "it's")) {
            val current = last?.let { l -> tasks.firstOrNull { it.id == l.id } }
            if (current != null && !current.isCompleted) complete(current)
            else reply("Which task? Tell me a few words from its name.")
            return
        }
        when (val m = TaskMatcher.match(target, tasks)) {
            is TaskMatch.Exact -> complete(m.task)
            is TaskMatch.Likely -> {
                pendingComplete = m.task
                lastTask = m.task
                reply("Did you mean \"${m.task.title}\"? Say yes and I'll mark it done.")
            }
            is TaskMatch.Several -> {
                val names = m.tasks.take(3).joinToString(", ") { "\"${it.title}\"" }
                reply("I found a few matches: $names. Which one?")
            }
            TaskMatch.None -> reply("I couldn't find a task matching \"$target\". Check your task list?")
        }
    }

    private fun reply(text: String) {
        val botMsg = ChatMessage(id = nextId++, text = text, isUser = false)
        _uiState.update { it.copy(messages = it.messages + botMsg, isThinking = false) }
    }

    private fun complete(task: Task) {
        lastTask = task
        viewModelScope.launch { repository.updateTask(task.copy(isCompleted = true)) }
        reply("Done! ✅ Marked \"${task.title}\" as complete. Nice work!")
    }
}

class AssistantViewModelFactory(
    private val repository: TaskRepository,
    private val userPrefs: UserPreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AssistantViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AssistantViewModel(repository, userPrefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
