package com.alvin.neuromind.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alvin.neuromind.data.Difficulty
import com.alvin.neuromind.data.Priority
import com.alvin.neuromind.data.Task
import com.alvin.neuromind.data.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddEditTaskUiState(
    val title: String = "",
    val description: String = "",
    val dueDate: Long? = null,
    val priority: Priority = Priority.MEDIUM,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val isTaskSaved: Boolean = false
)

class AddEditTaskViewModel(
    private val repository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditTaskUiState())
    val uiState: StateFlow<AddEditTaskUiState> = _uiState.asStateFlow()

    private var currentTaskId: Int? = null

    // Call this from UI to load existing data
    fun loadTask(id: Int) {
        if (id == -1) return
        currentTaskId = id
        viewModelScope.launch {
            val task = repository.getTaskById(id)
            if (task != null) {
                _uiState.update {
                    it.copy(
                        title = task.title,
                        description = task.description ?: "",
                        dueDate = task.dueDate,
                        priority = task.priority,
                        difficulty = task.difficulty
                    )
                }
            }
        }
    }

    fun onTitleChange(newTitle: String) { _uiState.update { it.copy(title = newTitle) } }
    fun onDescriptionChange(newDesc: String) { _uiState.update { it.copy(description = newDesc) } }
    fun onDueDateChange(newDate: Long?) { _uiState.update { it.copy(dueDate = newDate) } }
    fun onPriorityChange(newPriority: Priority) { _uiState.update { it.copy(priority = newPriority) } }
    fun onDifficultyChange(newDiff: Difficulty) { _uiState.update { it.copy(difficulty = newDiff) } }

    fun saveTask() {
        viewModelScope.launch {
            val task = Task(
                id = currentTaskId ?: 0,
                title = _uiState.value.title,
                description = _uiState.value.description,
                dueDate = _uiState.value.dueDate,
                priority = _uiState.value.priority,
                difficulty = _uiState.value.difficulty,
                createdAt = System.currentTimeMillis(),
                isCompleted = false
            )

            if (currentTaskId != null && currentTaskId != 0) {
                repository.update(task)
            } else {
                repository.insert(task)
            }
            _uiState.update { it.copy(isTaskSaved = true) }
        }
    }
}

class AddEditTaskViewModelFactory(private val repository: TaskRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddEditTaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddEditTaskViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}