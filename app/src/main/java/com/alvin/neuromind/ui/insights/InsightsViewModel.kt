package com.alvin.neuromind.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
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

data class InsightsUiState(
    val completionData: List<Pair<String, Int>> = emptyList(),
    val averageMood: String = "N/A",
    val averageEnergy: Int = 0,
    val wellnessScore: Float = 0.0f,
    val isLoading: Boolean = true
)

class InsightsViewModel(private val repository: TaskRepository) : ViewModel() {

    val uiState: StateFlow<InsightsUiState> = combine(
        repository.allTasks,
        repository.allFeedbackLogs
    ) { tasks, feedbackLogs ->

        // 1. Weekly Completion Logic
        val today = LocalDate.now()
        val weekDays = (0..6).map { today.minusDays((6 - it).toLong()) }

        val completionsByDay = weekDays.map { day ->
            val dayLabel = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            val count = tasks.count { task ->
                // FIXED: Used 'createdAt' instead of 'updatedAt'
                val taskDate = Instant.ofEpochMilli(task.createdAt)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                task.isCompleted && taskDate == day
            }
            dayLabel to count
        }

        // 2. Averages & Wellness Score
        val recentFeedback = feedbackLogs.take(14)
        val wellnessScore: Float
        val avgMoodStr: String
        val avgEnergy: Int

        if (recentFeedback.isEmpty()) {
            wellnessScore = 0f
            avgMoodStr = "No Data"
            avgEnergy = 0
        } else {
            val totalScore = recentFeedback.sumOf { it.mood.score + it.energyLevel }
            val maxPossibleScore = recentFeedback.size * 15
            wellnessScore = (totalScore.toFloat() / maxPossibleScore.toFloat()).coerceIn(0f, 1f)

            avgEnergy = (recentFeedback.sumOf { it.energyLevel } / recentFeedback.size)

            val avgMoodScore = recentFeedback.sumOf { it.mood.score } / recentFeedback.size
            avgMoodStr = Mood.entries
                .minByOrNull { kotlin.math.abs(it.score - avgMoodScore) }
                ?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Neutral"
        }

        InsightsUiState(
            completionData = completionsByDay,
            averageMood = avgMoodStr,
            averageEnergy = avgEnergy,
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
    private val repository: TaskRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(InsightsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InsightsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}