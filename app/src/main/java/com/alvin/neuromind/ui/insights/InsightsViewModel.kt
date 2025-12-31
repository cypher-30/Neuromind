package com.alvin.neuromind.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alvin.neuromind.data.FeedbackLog
import com.alvin.neuromind.data.Mood
import com.alvin.neuromind.data.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import androidx.lifecycle.viewmodel.CreationExtras

data class InsightsUiState(
    val weeklyCompletionData: List<Float> = List(7) { 0f },
    val weekDayLabels: List<String> = emptyList(),
    val wellnessScore: Float = 0.0f,
    val isLoading: Boolean = true
)

class InsightsViewModel(private val repository: com.alvin.neuromind.data.TaskRepository) : ViewModel() {

    val uiState: StateFlow<InsightsUiState> = combine(
        repository.allTasks,
        repository.allFeedbackLogs
    ) { tasks, feedbackLogs ->

        // --- Weekly Completion Logic ---
        val today = LocalDate.now()
        val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1) // Assuming week starts on Monday
        val weekDays = (0..6).map { startOfWeek.plusDays(it.toLong()) }

        val completionsByDay = weekDays.map { day ->
            tasks.count { task ->
                val completedDate = Instant.ofEpochMilli(task.updatedAt)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                task.isCompleted && completedDate == day
            }.toFloat()
        }

        val dayLabels = weekDays.map { it.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }

        // --- Wellness Score Logic ---
        val recentFeedback = feedbackLogs.take(7) // Average of last 7 days
        val wellnessScore = if (recentFeedback.isEmpty()) {
            0f // Default if no feedback
        } else {
            val totalScore = recentFeedback.sumOf { it.mood.score + it.energyLevel }
            val maxPossibleScore = recentFeedback.size * (Mood.GREAT.score + 10) // 10 is max energy
            (totalScore.toFloat() / maxPossibleScore.toFloat()).coerceIn(0f, 1f)
        }

        InsightsUiState(
            weeklyCompletionData = completionsByDay,
            weekDayLabels = dayLabels,
            wellnessScore = wellnessScore,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = InsightsUiState()
    )
}

class InsightsViewModelFactory(
    private val repository: com.alvin.neuromind.data.TaskRepository
) : ViewModelProvider.Factory {
    // --- FIX: Updated the create function signature ---
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(InsightsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InsightsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}