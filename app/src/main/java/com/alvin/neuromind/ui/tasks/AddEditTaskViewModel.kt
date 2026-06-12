package com.alvin.neuromind.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alvin.neuromind.data.*
import kotlinx.coroutines.flow.MutableStateFlow
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

class AddEditTaskViewModel(private val repository: TaskRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AddEditTaskUiState())
    val uiState = _uiState.asStateFlow()
    private var currentTaskId: Int? = null

    fun loadTask(id: Int) {
        if (id == -1 || id == 0) return
        currentTaskId = id
        viewModelScope.launch {
            repository.getTaskById(id)?.let { task ->
                _uiState.update { it.copy(title = task.title, description = task.description ?: "", dueDate = task.dueDate, priority = task.priority, difficulty = task.difficulty) }
            }
        }
    }

    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value) }
    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }
    fun onDueDateChange(value: Long?) = _uiState.update { it.copy(dueDate = value) }
    fun onPriorityChange(value: Priority) = _uiState.update { it.copy(priority = value) }
    fun onDifficultyChange(value: Difficulty) = _uiState.update { it.copy(difficulty = value) }

    fun saveTask() {
        viewModelScope.launch {
            val task = Task(id = currentTaskId ?: 0, title = _uiState.value.title, description = _uiState.value.description, dueDate = _uiState.value.dueDate, priority = _uiState.value.priority, difficulty = _uiState.value.difficulty)
            if (task.id != 0) repository.updateTask(task) else repository.insertTask(task)
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