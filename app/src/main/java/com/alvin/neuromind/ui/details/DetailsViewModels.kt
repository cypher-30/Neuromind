package com.alvin.neuromind.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.alvin.neuromind.data.Task
import com.alvin.neuromind.data.TaskRepository
import com.alvin.neuromind.data.TimetableEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Loading until the first read; Missing once the item is gone (deleted here or in its editor). */
sealed interface Details<out T> {
    data object Loading : Details<Nothing>
    data object Missing : Details<Nothing>
    data class Loaded<T>(val value: T) : Details<T>
}

/** A task plus the task it waits on, if any. */
data class TaskDetails(val task: Task, val prerequisite: Task?) {
    val isBlocked: Boolean get() = prerequisite != null && !prerequisite.isCompleted
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TaskDetailsViewModel(private val repository: TaskRepository, taskId: Int) : ViewModel() {

    val state: StateFlow<Details<TaskDetails>> = repository.observeTask(taskId)
        .flatMapLatest { task ->
            when {
                task == null -> flowOf(Details.Missing)
                task.prerequisiteTaskId == null -> flowOf(Details.Loaded(TaskDetails(task, null)))
                else -> repository.observeTask(task.prerequisiteTaskId).map { Details.Loaded(TaskDetails(task, it)) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Details.Loading)

    /** [finishedAt]: when it was really finished (marked late); null = now. */
    fun setCompleted(done: Boolean, finishedAt: Long? = null) {
        val details = (state.value as? Details.Loaded)?.value ?: return
        if (done && details.isBlocked) return
        viewModelScope.launch {
            repository.updateTask(details.task.copy(isCompleted = done, completedAt = if (done) finishedAt else null))
        }
    }

    fun delete() {
        val task = (state.value as? Details.Loaded)?.value?.task ?: return
        viewModelScope.launch { repository.deleteTask(task) }
    }
}

class EntryDetailsViewModel(private val repository: TaskRepository, entryId: Int) : ViewModel() {

    val state: StateFlow<Details<TimetableEntry>> = repository.observeTimetableEntry(entryId)
        .map { if (it == null) Details.Missing else Details.Loaded(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Details.Loading)

    fun delete() {
        val entry = (state.value as? Details.Loaded)?.value ?: return
        viewModelScope.launch { repository.deleteTimetableEntry(entry) }
    }
}

class DetailsViewModelFactory(private val repository: TaskRepository, private val id: Int) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        @Suppress("UNCHECKED_CAST")
        return when {
            modelClass.isAssignableFrom(TaskDetailsViewModel::class.java) -> TaskDetailsViewModel(repository, id)
            modelClass.isAssignableFrom(EntryDetailsViewModel::class.java) -> EntryDetailsViewModel(repository, id)
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        } as T
    }
}
