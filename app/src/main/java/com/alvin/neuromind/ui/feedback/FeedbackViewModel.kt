package com.alvin.neuromind.ui.feedback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alvin.neuromind.data.FeedbackLog
import com.alvin.neuromind.data.Task
import com.alvin.neuromind.data.TaskCategory
import com.alvin.neuromind.data.TaskRepository
import com.alvin.neuromind.domain.ToneAnalyzer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FeedbackUiState(
    val isSubmitted: Boolean = false,
    val lastToneLabel: String? = null,
    val taskSavedMessage: String? = null
)

class FeedbackViewModel(private val repository: TaskRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(FeedbackUiState())
    val uiState = _uiState.asStateFlow()

    fun submitFeedback(log: FeedbackLog) {
        viewModelScope.launch {
            val analysis = log.comment
                ?.takeIf { it.isNotBlank() }
                ?.let { ToneAnalyzer.analyze(it) }
            repository.insertFeedbackLog(
                log.copy(
                    toneLabel = analysis?.label,
                    sentimentScore = analysis?.sentimentScore
                )
            )
            _uiState.value = FeedbackUiState(
                isSubmitted = true,
                lastToneLabel = analysis?.label?.name
                    ?.lowercase()
                    ?.replaceFirstChar { it.uppercase() }
            )
        }
    }

    fun createTaskFromNote(note: String) {
        val trimmed = note.trim()
        if (trimmed.isBlank()) return
        val title = trimmed
            .replace("\n", " ")
            .split(Regex("\\s+"))
            .take(7)
            .joinToString(" ")
            .let { if (trimmed.split(Regex("\\s+")).size > 7) "$it..." else it }
        viewModelScope.launch {
            repository.insertTask(
                Task(
                    title = title,
                    description = trimmed,
                    category = TaskCategory.PERSONAL
                )
            )
            _uiState.value = _uiState.value.copy(taskSavedMessage = "Saved as task")
        }
    }
}

class FeedbackViewModelFactory(private val repository: TaskRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FeedbackViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FeedbackViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}