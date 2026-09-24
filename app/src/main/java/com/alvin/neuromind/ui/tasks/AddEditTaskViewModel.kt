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
    val subject: String = "",
    val category: TaskCategory = TaskCategory.ACADEMIC,
    val isEditing: Boolean = false,
    val availableTasks: List<Task> = emptyList(),
    val subTaskProposal: List<Task> = emptyList(),
    val availableTimeSlots: List<AvailableSlot> = emptyList(),
    val isTaskSaved: Boolean = false,
    /** True once the original task and any draft are loaded; edits before then are not persisted. */
    val isReady: Boolean = false,
    /** Input was restored from an unsaved draft. */
    val restoredDraft: Boolean = false,
    /** Current input differs from the saved task (or an empty form) and is being auto-saved. */
    val hasUnsavedChanges: Boolean = false
)

class AddEditTaskViewModel(private val repository: TaskRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AddEditTaskUiState())
    val uiState = _uiState.asStateFlow()
    private var currentTaskId: Int? = null
    private var original: Task? = null
    private var baseline = TaskDraft()
    private var draftSession: DraftSession<TaskDraft>? = null

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

    /** Call once per screen with the task id, or -1 for a new task. Restores any unsaved draft. */
    fun start(id: Int) {
        if (draftSession != null) return
        val taskId = id.takeIf { it > 0 }
        currentTaskId = taskId
        val session = DraftSession(repository, DraftKeys.of(DraftKeys.TASK, taskId), TaskDraft.serializer())
        draftSession = session
        viewModelScope.launch {
            val task = taskId?.let { repository.getTaskById(it) }
            original = task
            if (task != null) baseline = task.toDraft()
            val draft = session.load()?.takeIf { it != baseline }
            _uiState.update { state ->
                state.applyDraft(draft ?: baseline).copy(
                    isEditing = task != null,
                    isReady = true,
                    restoredDraft = draft != null,
                    hasUnsavedChanges = draft != null,
                    availableTasks = state.availableTasks.filter { it.id != (taskId ?: -1) }
                )
            }
        }
    }

    private fun edit(transform: (AddEditTaskUiState) -> AddEditTaskUiState) {
        _uiState.update(transform)
        val state = _uiState.value
        if (!state.isReady || state.isTaskSaved) return
        val draft = state.toDraft()
        val changed = draft != baseline
        _uiState.update { it.copy(hasUnsavedChanges = changed) }
        draftSession?.update(draft.takeIf { changed })
    }

    fun onTitleChange(value: String) = edit { it.copy(title = value) }
    fun onDescriptionChange(value: String) = edit { it.copy(description = value) }
    fun onDueDateChange(value: Long?) = edit { it.copy(dueDate = value) }
    fun onPriorityChange(value: Priority) = edit { it.copy(priority = value) }
    fun onDifficultyChange(value: Difficulty) = edit { it.copy(difficulty = value) }
    fun onDurationChange(value: Int) = edit { it.copy(durationMinutes = value) }
    fun onPrerequisiteChange(value: Int?) = edit { it.copy(prerequisiteTaskId = value) }
    fun onSubjectChange(value: String) = edit { it.copy(subject = value) }
    fun onCategoryChange(value: TaskCategory) = edit { it.copy(category = value) }

    /** Throws away the unsaved draft and resets the form to the saved task (or blank). */
    fun discardDraft() {
        draftSession?.discard()
        _uiState.update { it.applyDraft(baseline).copy(restoredDraft = false, hasUnsavedChanges = false) }
    }

    fun dismissRestoredBanner() = _uiState.update { it.copy(restoredDraft = false) }

    fun saveTask() {
        val state = _uiState.value
        if (state.title.isBlank() || state.isTaskSaved || !state.isReady) return
        _uiState.update { it.copy(isTaskSaved = true) }
        viewModelScope.launch {
            val fields = { base: Task ->
                base.copy(
                    title = state.title.trim(),
                    description = state.description,
                    dueDate = state.dueDate,
                    priority = state.priority,
                    difficulty = state.difficulty,
                    durationMinutes = state.durationMinutes,
                    prerequisiteTaskId = state.prerequisiteTaskId,
                    subject = state.subject.trim().takeIf { it.isNotBlank() },
                    category = state.category
                )
            }
            // Editing keeps isCompleted/createdAt from the stored task.
            val existing = original
            if (existing != null) repository.updateTask(fields(existing))
            else repository.insertTask(fields(Task(title = state.title)))
            draftSession?.close()
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
            draftSession?.close()
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
        edit { it.copy(dueDate = epochMilli, availableTimeSlots = emptyList()) }
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

private fun Task.toDraft() = TaskDraft(
    title = title,
    description = description ?: "",
    dueDate = dueDate,
    priority = priority.name,
    difficulty = difficulty.name,
    durationMinutes = durationMinutes,
    prerequisiteTaskId = prerequisiteTaskId,
    subject = subject ?: "",
    category = category.name
)

private fun AddEditTaskUiState.toDraft() = TaskDraft(
    title = title,
    description = description,
    dueDate = dueDate,
    priority = priority.name,
    difficulty = difficulty.name,
    durationMinutes = durationMinutes,
    prerequisiteTaskId = prerequisiteTaskId,
    subject = subject,
    category = category.name
)

private fun AddEditTaskUiState.applyDraft(d: TaskDraft) = copy(
    title = d.title,
    description = d.description,
    dueDate = d.dueDate,
    priority = runCatching { Priority.valueOf(d.priority) }.getOrDefault(Priority.MEDIUM),
    difficulty = runCatching { Difficulty.valueOf(d.difficulty) }.getOrDefault(Difficulty.MEDIUM),
    durationMinutes = d.durationMinutes,
    prerequisiteTaskId = d.prerequisiteTaskId,
    subject = d.subject,
    category = runCatching { TaskCategory.valueOf(d.category) }.getOrDefault(TaskCategory.ACADEMIC)
)