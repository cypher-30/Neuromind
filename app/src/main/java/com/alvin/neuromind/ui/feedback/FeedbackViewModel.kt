package com.alvin.neuromind.ui.feedback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alvin.neuromind.data.FeedbackLog
import com.alvin.neuromind.data.Mood
import com.alvin.neuromind.data.TaskRepository
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.CreationExtras

class FeedbackViewModel(private val repository: com.alvin.neuromind.data.TaskRepository) : ViewModel() {

    /**
     * Creates a new FeedbackLog and saves it to the database.
     */
    fun submitFeedback(
        mood: Mood,
        energyLevel: Int,
        tasksCompleted: Int,
        comment: String?
    ) {
        viewModelScope.launch {
            val newLog = FeedbackLog(
                mood = mood,
                energyLevel = energyLevel,
                tasksCompleted = tasksCompleted,
                comment = comment
            )
            repository.insert(newLog)
        }
    }
}

// The factory is placed in the same file to keep the code organized.
class FeedbackViewModelFactory(
    private val repository: com.alvin.neuromind.data.TaskRepository
) : ViewModelProvider.Factory {
    // --- FIX: Updated the create function signature ---
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(FeedbackViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FeedbackViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}