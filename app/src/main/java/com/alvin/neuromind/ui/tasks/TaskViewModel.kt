package com.alvin.neuromind.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alvin.neuromind.data.Task
import com.alvin.neuromind.data.TaskRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class TaskFilter { ALL, TODAY, UPCOMING, OVERDUE, COMPLETED }

data class TaskListUiState(
    val displayedTasks: List<Task> = emptyList(),
    val selectedFilter: TaskFilter = TaskFilter.ALL,
    val isLoading: Boolean = true
)

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {
    private val _selectedFilter = MutableStateFlow(TaskFilter.ALL)

    val uiState: StateFlow<TaskListUiState> = combine(repository.allTasks, _selectedFilter) { tasks, filter ->
        val now = System.currentTimeMillis()
        val today = LocalDate.now()
        val filtered = when (filter) {
            TaskFilter.ALL -> tasks.filter { !it.isCompleted }.sortedBy { it.dueDate }
            TaskFilter.COMPLETED -> tasks.filter { it.isCompleted }.sortedByDescending { it.dueDate }
            TaskFilter.OVERDUE -> tasks.filter { !it.isCompleted && it.dueDate != null && it.dueDate < now && !isSameDay(it.dueDate, today) }
            TaskFilter.TODAY -> tasks.filter { !it.isCompleted && it.dueDate != null && isSameDay(it.dueDate, today) }
            TaskFilter.UPCOMING -> tasks.filter { !it.isCompleted && (it.dueDate == null || it.dueDate > now) && !isSameDay(it.dueDate ?: 0, today) }
        }
        TaskListUiState(filtered, filter, false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TaskListUiState())

    fun setFilter(filter: TaskFilter) { _selectedFilter.value = filter }

    fun onTaskCheckedChange(task: Task, isChecked: Boolean) = viewModelScope.launch {
        repository.updateTask(task.copy(isCompleted = isChecked))
    }

    fun deleteTask(task: Task) = viewModelScope.launch {
        repository.deleteTask(task)
    }

    private fun isSameDay(millis: Long, date: LocalDate): Boolean =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate() == date
}

class TaskViewModelFactory(private val repository: TaskRepository, private val scheduler: com.alvin.neuromind.domain.Scheduler) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}