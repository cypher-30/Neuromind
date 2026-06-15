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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatMessage(
    val id: Int,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class PendingNav(val action: AssistantAction, val taskId: Int? = null)

data class AssistantUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isThinking: Boolean = false,
    val pendingNavigation: PendingNav? = null
)

class AssistantViewModel(
    private val repository: TaskRepository,
    private val userPrefs: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    private var nextId = 0

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

            val intent = NeuromindAssistant.classify(text)

            when (intent) {
                AssistantIntent.CREATE_TASK   -> handleCreateTask(text)
                AssistantIntent.COMPLETE_TASK -> handleCompleteTask(text, tasks)
                else -> {
                    val response = NeuromindAssistant.respond(intent, tasks, timetable, profile, feedbackLogs)
                    val botMsg   = ChatMessage(id = nextId++, text = response.message, isUser = false)
                    val pending  = response.actionType?.let { PendingNav(it, response.taskId) }
                    _uiState.update { it.copy(messages = it.messages + botMsg, isThinking = false, pendingNavigation = pending) }
                }
            }
        }
    }

    private fun handleCreateTask(text: String) {
        val parsed  = SmartInputHelper.parseInput(text)
        val newTask = Task(title = parsed.title, dueDate = parsed.dueDate, priority = Priority.MEDIUM)

        viewModelScope.launch {
            repository.insertTask(newTask)

            val duePart = if (parsed.dueDate != null) {
                val fmt = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                " due ${fmt.format(Date(parsed.dueDate))}"
            } else ""

            val botMsg = ChatMessage(
                id     = nextId++,
                text   = "Added \"${parsed.title}\"$duePart to your tasks! ✅",
                isUser = false
            )
            _uiState.update { it.copy(messages = it.messages + botMsg, isThinking = false) }
        }
    }

    private fun handleCompleteTask(text: String, tasks: List<Task>) {
        val target  = NeuromindAssistant.extractTaskTarget(text)
        val matches = tasks.filter { !it.isCompleted && it.title.contains(target, ignoreCase = true) }

        val reply = when {
            matches.isEmpty() -> {
                "I couldn't find a task matching \"$target\". Check your task list?"
            }
            matches.size > 1 -> {
                val names = matches.take(3).joinToString(", ") { "\"${it.title}\"" }
                "I found a few matches: $names. Can you be more specific?"
            }
            else -> {
                val task = matches.first()
                viewModelScope.launch { repository.updateTask(task.copy(isCompleted = true)) }
                "Done! ✅ Marked \"${task.title}\" as complete. Nice work!"
            }
        }

        val botMsg = ChatMessage(id = nextId++, text = reply, isUser = false)
        _uiState.update { it.copy(messages = it.messages + botMsg, isThinking = false) }
    }

    fun clearNavigation() {
        _uiState.update { it.copy(pendingNavigation = null) }
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
