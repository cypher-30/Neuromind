package com.alvin.neuromind.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alvin.neuromind.data.*
import com.alvin.neuromind.domain.AvailableSlot
import com.alvin.neuromind.domain.StudyPlanGenerator
import com.alvin.neuromind.domain.TimeFinder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId

data class AddEditTaskUiState(
    val title: String = "",
    val description: String = "",
    val dueDate: Long? = null,
    val priority: Priority = Priority.MEDIUM,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val durationMinutes: Int = 60,
    val prerequisiteTaskId: Int? = null,
    val availableTasks: List<Task> = emptyList(),
    val subTaskProposal: List<Task> = emptyList(),
    val availableTimeSlots: List<AvailableSlot> = emptyList(),
    val isTaskSaved: Boolean = false
)

class AddEditTaskViewModel(private val repository: TaskRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AddEditTaskUiState())
    val uiState = _uiState.asStateFlow()
    private var currentTaskId: Int? = null

    init {
        // Always collect so the dependency selector is populated for new tasks too
        viewModelScope.launch {
            repository.allTasks.collect { tasks ->
                _uiState.update { state ->
                    state.copy(availableTasks = tasks.filter { it.id != (currentTaskId ?: -1) })
                }
            }
        }
    }

    fun loadTask(id: Int) {
        if (id <= 0) return
        currentTaskId = id
        viewModelScope.launch {
            repository.getTaskById(id)?.let { task ->
                _uiState.update {
                    it.copy(
                        title = task.title,
                        description = task.description ?: "",
                        dueDate = task.dueDate,
                        priority = task.priority,
                        difficulty = task.difficulty,
                        durationMinutes = task.durationMinutes,
                        prerequisiteTaskId = task.prerequisiteTaskId
                    )
                }
            }
        }
    }

    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value) }
    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }
    fun onDueDateChange(value: Long?) = _uiState.update { it.copy(dueDate = value) }
    fun onPriorityChange(value: Priority) = _uiState.update { it.copy(priority = value) }
    fun onDifficultyChange(value: Difficulty) = _uiState.update { it.copy(difficulty = value) }
    fun onDurationChange(value: Int) = _uiState.update { it.copy(durationMinutes = value) }
    fun onPrerequisiteChange(value: Int?) = _uiState.update { it.copy(prerequisiteTaskId = value) }

    fun saveTask() {
        viewModelScope.launch {
            val state = _uiState.value
            val task = Task(
                id = currentTaskId ?: 0,
                title = state.title,
                description = state.description,
                dueDate = state.dueDate,
                priority = state.priority,
                difficulty = state.difficulty,
                durationMinutes = state.durationMinutes,
                prerequisiteTaskId = state.prerequisiteTaskId
            )
            if (task.id != 0) repository.updateTask(task) else repository.insertTask(task)
            _uiState.update { it.copy(isTaskSaved = true) }
        }
    }

    // --- Pillar 5: Study Plan Generator ---

    fun generateSubTaskProposal() {
        val state = _uiState.value
        val parentTask = Task(
            title = state.title,
            priority = state.priority,
            difficulty = state.difficulty,
            dueDate = state.dueDate,
            durationMinutes = state.durationMinutes
        )
        val proposals = StudyPlanGenerator.generate(parentTask)
        _uiState.update { it.copy(subTaskProposal = proposals) }
    }

    fun confirmSubTaskBreakdown() {
        viewModelScope.launch {
            _uiState.value.subTaskProposal.forEach { repository.insertTask(it) }
            _uiState.update { it.copy(subTaskProposal = emptyList(), isTaskSaved = true) }
        }
    }

    fun dismissSubTaskProposal() = _uiState.update { it.copy(subTaskProposal = emptyList()) }

    // --- Pillar 7: Find a Time ---

    fun findAvailableTime() {
        viewModelScope.launch {
            val timetable = repository.allTimetableEntries.first()
            val slots = TimeFinder.findSlots(
                timetable = timetable,
                durationMinutes = _uiState.value.durationMinutes
            )
            _uiState.update { it.copy(availableTimeSlots = slots) }
        }
    }

    fun applyTimeSlot(slot: AvailableSlot) {
        val epochMilli = slot.date.atTime(slot.startTime)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        _uiState.update { it.copy(dueDate = epochMilli, availableTimeSlots = emptyList()) }
    }

    fun dismissTimeSlots() = _uiState.update { it.copy(availableTimeSlots = emptyList()) }
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
