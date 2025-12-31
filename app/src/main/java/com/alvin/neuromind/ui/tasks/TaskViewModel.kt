package com.alvin.neuromind.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alvin.neuromind.data.Task
import com.alvin.neuromind.data.TaskRepository
import com.alvin.neuromind.domain.ProposedSlot
import com.alvin.neuromind.domain.Scheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

// Enums and Helper Classes
enum class TaskFilter { ALL, TODAY, UPCOMING, OVERDUE }

data class HierarchicalTask(
    val parent: Task,
    val subTasks: List<Task>
)

data class TaskListUiState(
    val hierarchicalTasks: List<HierarchicalTask> = emptyList(),
    val selectedFilter: TaskFilter = TaskFilter.ALL,
    val isRescheduleMode: Boolean = false,
    val isLoading: Boolean = false,
    val expandedTaskIds: Set<UUID> = emptySet()
)

// The Main ViewModel
class TaskViewModel(
    private val repository: TaskRepository,
    private val scheduler: Scheduler
) : ViewModel() {

    private val _isRescheduleMode = MutableStateFlow(false)
    private val _filter = MutableStateFlow(TaskFilter.ALL)
    private val _expandedTaskIds = MutableStateFlow<Set<UUID>>(emptySet())

    // Proposals for rescheduling (Smart AI feature)
    private val _proposals = MutableStateFlow<Map<UUID, ProposedSlot>>(emptyMap())
    val proposals: StateFlow<Map<UUID, ProposedSlot>> = _proposals.asStateFlow()

    val uiState: StateFlow<TaskListUiState> = combine(
        repository.allTasks,
        _filter,
        _isRescheduleMode,
        _expandedTaskIds
    ) { tasks, filter, isRescheduleMode, expandedIds ->
        // 1. Organize tasks into Parent -> Subtasks
        val parentTasks = tasks.filter { it.parentId == null }
        val subTasksMap = tasks.filter { it.parentId != null }.groupBy { it.parentId }

        val hierarchical = parentTasks.map { parent ->
            HierarchicalTask(parent, subTasksMap[parent.id] ?: emptyList())
        }

        // 2. Filter logic
        val filtered = if (isRescheduleMode) {
            // In reschedule mode, only show active, overdue tasks
            hierarchical.filter { it.parent.isOverdue && !it.parent.isCompleted }
        } else {
            when (filter) {
                TaskFilter.ALL -> hierarchical.filter { !it.parent.isCompleted }
                TaskFilter.TODAY -> hierarchical.filter { !it.parent.isCompleted /* Add actual date logic here later */ }
                TaskFilter.UPCOMING -> hierarchical.filter { !it.parent.isCompleted && !it.parent.isOverdue }
                TaskFilter.OVERDUE -> hierarchical.filter { it.parent.isOverdue && !it.parent.isCompleted }
            }
        }

        TaskListUiState(
            hierarchicalTasks = filtered,
            selectedFilter = filter,
            isRescheduleMode = isRescheduleMode,
            expandedTaskIds = expandedIds
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TaskListUiState())

    // --- Actions ---

    fun setMode(isReschedule: Boolean) {
        _isRescheduleMode.value = isReschedule
    }

    fun setFilter(filter: TaskFilter) {
        _filter.value = filter
    }

    fun toggleTaskExpansion(taskId: UUID) {
        _expandedTaskIds.update { current ->
            if (current.contains(taskId)) current - taskId else current + taskId
        }
    }

    fun onTaskCompleted(task: Task, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.update(task.copy(isCompleted = isCompleted))
        }
    }

    // --- Smart Reschedule Logic ---

    fun acceptProposals() {
        viewModelScope.launch {
            val currentProposals = _proposals.value
            currentProposals.forEach { (taskId, proposal) ->
                val task = repository.getTaskById(taskId) // You might need to add getTaskById to Repository if missing
                if (task != null) {
                    // Update task with new time
                    // For now, we just mock this update or assume Repository handles it
                    // Ideally: repository.update(task.copy(dueDate = proposal.date...))
                }
            }
            _proposals.value = emptyMap()
        }
    }

    fun clearProposals() {
        _proposals.value = emptyMap()
    }
}

class TaskViewModelFactory(
    private val repository: TaskRepository,
    private val scheduler: Scheduler
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(repository, scheduler) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}