package com.alvin.neuromind.ui.feedback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alvin.neuromind.data.FeedbackLog
import com.alvin.neuromind.data.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FeedbackUiState(val isSubmitted: Boolean = false)

class FeedbackViewModel(private val repository: TaskRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(FeedbackUiState())
    val uiState = _uiState.asStateFlow()

    fun submitFeedback(log: FeedbackLog) {
        viewModelScope.launch {
            repository.insertFeedbackLog(log)
            _uiState.value = FeedbackUiState(isSubmitted = true)
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