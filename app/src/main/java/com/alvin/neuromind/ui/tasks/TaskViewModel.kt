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
import java.time.ZoneId
import java.util.UUID
import androidx.lifecycle.viewmodel.CreationExtras

enum class TaskFilter { ALL, UPCOMING, COMPLETED }

data class HierarchicalTask(
    val parent: Task,
    val subTasks: List<Task> = emptyList()
)

data class TaskListUiState(
    val hierarchicalTasks: List<HierarchicalTask> = emptyList(),
    val expandedTaskIds: Set<UUID> = emptySet(),
    val selectedFilter: TaskFilter = TaskFilter.ALL,
    val isRescheduleMode: Boolean = false,
    val isLoading: Boolean = true
)

class TaskViewModel(
    private val repository: TaskRepository,
    private val scheduler: Scheduler
) : ViewModel() {

    private val _mode = MutableStateFlow(false)
    private val _selectedFilter = MutableStateFlow(TaskFilter.ALL)
    private val _expandedTaskIds = MutableStateFlow(emptySet<UUID>())
    private val _proposals = MutableStateFlow<Map<UUID, ProposedSlot>>(emptyMap())

    private val _isFindingProposals = MutableStateFlow(false)
    val isFindingProposals = _isFindingProposals.asStateFlow()

    val uiState: StateFlow<TaskListUiState> = combine(
        repository.allTasks, _mode, _selectedFilter, _expandedTaskIds
    ) { allTasks, isRescheduleMode, filter, expandedIds ->

        val relevantTasks = if (isRescheduleMode) {
            allTasks.filter { it.isOverdue && !it.isCompleted }
        } else { allTasks }

        val filteredTasks = when (filter) {
            TaskFilter.ALL -> relevantTasks.filter { !it.isCompleted }
            TaskFilter.UPCOMING -> relevantTasks.filter { !it.isCompleted && it.dueDate != null }
            TaskFilter.COMPLETED -> relevantTasks.filter { it.isCompleted }
        }

        val subTasksByParentId = filteredTasks.filter { it.parentId != null }.groupBy { it.parentId!! }
        val parentTasks = filteredTasks
            .filter { it.parentId == null }
            .sortedBy { it.dueDate ?: Long.MAX_VALUE }

        val hierarchicalList = parentTasks.map { parent ->
            HierarchicalTask(parent = parent, subTasks = subTasksByParentId[parent.id] ?: emptyList())
        }

        TaskListUiState(
            hierarchicalTasks = hierarchicalList,
            expandedTaskIds = expandedIds,
            selectedFilter = filter,
            isRescheduleMode = isRescheduleMode,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = TaskListUiState()
    )

    val proposals: StateFlow<Map<UUID, ProposedSlot>> = _proposals.asStateFlow()

    fun setMode(isReschedule: Boolean) {
        _mode.value = isReschedule
        if (isReschedule) _selectedFilter.value = TaskFilter.ALL
    }

    fun generateRescheduleProposals() {
        viewModelScope.launch {
            _isFindingProposals.value = true
            val overdueTasks = uiState.value.hierarchicalTasks.map { it.parent }
            val timetable = repository.allTimetableEntries.first()
            val proposalsMap = mutableMapOf<UUID, ProposedSlot>()
            for (task in overdueTasks) {
                scheduler.findNextAvailableSlot(task, timetable)?.let { proposalsMap[task.id] = it }
            }
            _proposals.value = proposalsMap
            _isFindingProposals.value = false
        }
    }

    fun acceptProposals() {
        viewModelScope.launch {
            val acceptedProposals = _proposals.value
            val currentTasks = repository.allTasks.first()
            for ((taskId, proposedSlot) in acceptedProposals) {
                currentTasks.find { it.id == taskId }?.let {
                    val newDueDate = proposedSlot.date.atTime(proposedSlot.timeSlot.end)
                        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    repository.update(it.copy(dueDate = newDueDate, isCompleted = false))
                }
            }
            clearProposals()
            setMode(false)
        }
    }

    fun clearProposals() { _proposals.value = emptyMap() }
    fun setFilter(filter: TaskFilter) { _selectedFilter.value = filter }
    fun toggleTaskExpansion(taskId: UUID) {
        _expandedTaskIds.value = if (taskId in _expandedTaskIds.value) {
            _expandedTaskIds.value - taskId
        } else { _expandedTaskIds.value + taskId }
    }

    fun onTaskCompleted(task: Task, completed: Boolean) {
        viewModelScope.launch {
            repository.update(task.copy(isCompleted = completed))
        }
    }
}

class TaskViewModelFactory(
    private val repository: TaskRepository,
    private val scheduler: Scheduler
) : ViewModelProvider.Factory {
    // --- FIX: Updated the create function signature ---
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(repository, scheduler) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}