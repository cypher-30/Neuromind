package com.alvin.neuromind.ui.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alvin.neuromind.data.FocusSession
import com.alvin.neuromind.data.Task
import com.alvin.neuromind.data.TaskRepository
import kotlinx.coroutines.launch

class FocusViewModel(private val repository: TaskRepository) : ViewModel() {

    fun recordCompletedSession(task: Task, durationMinutes: Int) {
        viewModelScope.launch {
            repository.insertFocusSession(
                FocusSession(
                    taskId = task.id,
                    taskTitle = task.title,
                    durationMinutes = durationMinutes
                )
            )
        }
    }
}

class FocusViewModelFactory(
    private val repository: TaskRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FocusViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FocusViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
