package com.alvin.neuromind.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.alvin.neuromind.data.Difficulty
import com.alvin.neuromind.data.Priority
import com.alvin.neuromind.data.Task
import com.alvin.neuromind.data.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// This is now a data class for stable state updates
data class AddEditTaskUiState(
    val title: String = "",
    val description: String = "",
    val dueDate: Long? = null,
    val priority: Priority = Priority.MEDIUM,
    val difficulty: Difficulty = Difficulty.MEDIUM
)

class AddEditTaskViewModel(private val repository: TaskRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AddEditTaskUiState())
    val uiState = _uiState.asStateFlow()

    fun onTitleChange(newTitle: String) {
        _uiState.update { it.copy(title = newTitle) }
    }

    fun onDescriptionChange(newDescription: String) {
        _uiState.update { it.copy(description = newDescription) }
    }

    fun onDueDateChange(newDate: Long?) {
        _uiState.update { it.copy(dueDate = newDate) }
    }

    fun onPriorityChange(newPriority: Priority) {
        _uiState.update { it.copy(priority = newPriority) }
    }

    fun onDifficultyChange(newDifficulty: Difficulty) {
        _uiState.update { it.copy(difficulty = newDifficulty) }
    }

    fun saveTask() {
        if (uiState.value.title.isBlank()) {
            return
        }
        viewModelScope.launch {
            val currentState = uiState.value
            val newTask = Task(
                title = currentState.title.trim(),
                description = currentState.description.trim(),
                dueDate = currentState.dueDate,
                priority = currentState.priority,
                difficulty = currentState.difficulty
            )
            repository.insert(newTask)
        }
    }
}

class AddEditTaskViewModelFactory(
    private val repository: TaskRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(AddEditTaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddEditTaskViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}