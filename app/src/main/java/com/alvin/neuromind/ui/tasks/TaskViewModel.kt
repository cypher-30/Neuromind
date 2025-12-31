package com.alvin.neuromind.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alvin.neuromind.data.Task
import com.alvin.neuromind.data.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class TaskListUiState(
    val overdueTasks: List<Task> = emptyList(),
    val todayTasks: List<Task> = emptyList(),
    val upcomingTasks: List<Task> = emptyList(),
    val completedTasks: List<Task> = emptyList(),
    val isLoading: Boolean = true
)

class TaskViewModel(
    private val repository: TaskRepository
) : ViewModel() {

    val uiState: StateFlow<TaskListUiState> = repository.allTasks
        .map { tasks ->
            val now = System.currentTimeMillis()
            val todayDate = LocalDate.now()

            // 1. Filter & Sort
            val (completed, active) = tasks.partition { it.isCompleted }

            val overdue = active.filter {
                it.dueDate != null && it.dueDate < now && !isSameDay(it.dueDate, todayDate)
            }.sortedBy { it.dueDate }

            val today = active.filter {
                it.dueDate != null && isSameDay(it.dueDate, todayDate)
            }.sortedByDescending { it.priority } // High priority first for today

            val upcoming = active.filter {
                it.dueDate == null || (it.dueDate > now && !isSameDay(it.dueDate, todayDate))
            }.sortedBy { it.dueDate ?: Long.MAX_VALUE }

            TaskListUiState(
                overdueTasks = overdue,
                todayTasks = today,
                upcomingTasks = upcoming,
                completedTasks = completed,
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TaskListUiState()
        )

    fun onTaskCheckedChange(task: Task, isChecked: Boolean) {
        viewModelScope.launch {
            repository.update(task.copy(isCompleted = isChecked))
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.delete(task)
        }
    }

    private fun isSameDay(millis: Long, date: LocalDate): Boolean {
        val taskDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        return taskDate == date
    }
}

class TaskViewModelFactory(
    private val repository: TaskRepository,
    private val scheduler: com.alvin.neuromind.domain.Scheduler // Kept for app compatibility
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}