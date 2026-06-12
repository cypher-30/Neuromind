package com.alvin.neuromind.ui.feedback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alvin.neuromind.data.FeedbackLog
import com.alvin.neuromind.data.TaskRepository
import kotlinx.coroutines.launch

class FeedbackViewModel(private val repository: TaskRepository) : ViewModel() {
    fun submitFeedback(log: FeedbackLog) {
        viewModelScope.launch {
            repository.insertFeedbackLog(log)
        }
    }
}

class FeedbackViewModelFactory(private val repository: TaskRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FeedbackViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FeedbackViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}