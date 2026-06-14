package com.alvin.neuromind.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
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

data class InsightsUiState(
    val completionData: List<Pair<String, Int>> = emptyList(),
    val averageMood: String = "N/A",
    val averageEnergy: Int = 0,
    val wellnessScore: Float = 0.0f,
    val recentNotes: List<FeedbackLog> = emptyList(),
    val isLoading: Boolean = true
)

class InsightsViewModel(private val repository: TaskRepository) : ViewModel() {

    val uiState: StateFlow<InsightsUiState> = combine(
        repository.allTasks,
        repository.allFeedbackLogs
    ) { tasks, feedbackLogs ->

        // Weekly completion chart
        val today = LocalDate.now()
        val weekDays = (0..6).map { today.minusDays((6 - it).toLong()) }

        val completionsByDay = weekDays.map { day ->
            val dayLabel = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            val count = tasks.count { task ->
                val taskDate = Instant.ofEpochMilli(task.createdAt)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                task.isCompleted && taskDate == day
            }
            dayLabel to count
        }

        // Wellness score — max 10 per log (mood max 5 + energy max 5)
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
            val maxPossibleScore = recentFeedback.size * 10
            wellnessScore = (totalScore.toFloat() / maxPossibleScore.toFloat()).coerceIn(0f, 1f)

            avgEnergy = (recentFeedback.sumOf { it.energyLevel } / recentFeedback.size)

            val avgMoodScore = recentFeedback.sumOf { it.mood.score } / recentFeedback.size
            avgMoodStr = Mood.entries
                .minByOrNull { kotlin.math.abs(it.score - avgMoodScore) }
                ?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Neutral"
        }

        // Recent journal notes (non-blank comments, newest first, capped at 5)
        val recentNotes = feedbackLogs.filter { !it.comment.isNullOrBlank() }.take(5)

        InsightsUiState(
            completionData = completionsByDay,
            averageMood = avgMoodStr,
            averageEnergy = avgEnergy,
            wellnessScore = wellnessScore,
            recentNotes = recentNotes,
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
