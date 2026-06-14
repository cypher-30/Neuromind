package com.alvin.neuromind.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alvin.neuromind.data.Task
import com.alvin.neuromind.data.TaskRepository
import com.alvin.neuromind.data.TimetableEntry
import com.alvin.neuromind.data.preferences.UserPreferencesRepository
import com.alvin.neuromind.domain.BurnoutAnalyzer
import com.alvin.neuromind.domain.BurnoutState
import com.alvin.neuromind.domain.Scheduler
import com.alvin.neuromind.domain.TimeSlot
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class DashboardUiState(
    val greeting: String = "",
    val currentDate: String = "",
    val pendingTaskCount: Int = 0,
    val completedTaskCount: Int = 0,
    val priorityTasks: List<Task> = emptyList(),
    val upcomingEvents: List<TimetableEntry> = emptyList(),
    val todaysPlan: Map<TimeSlot, Task> = emptyMap(),
    val burnoutState: BurnoutState? = null,
    val isLoading: Boolean = true
)

class DashboardViewModel(
    private val repository: TaskRepository,
    private val scheduler: Scheduler,
    private val userPrefs: UserPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.allTasks,
        repository.allTimetableEntries,
        repository.allFeedbackLogs,
        userPrefs.cognitiveProfile
    ) { tasks, timetable, feedbackLogs, profile ->

        val pendingCount = tasks.count { !it.isCompleted }
        val completedCount = tasks.count { it.isCompleted }

        val priorityList = tasks.filter { !it.isCompleted }
            .sortedByDescending { it.priority }
            .take(3)

        val today = LocalDate.now()
        val nowTime = LocalTime.now()
        val eventsToday = timetable.filter { it.dayOfWeek == today.dayOfWeek }
            .filter { it.startTime.isAfter(nowTime) }
            .sortedBy { it.startTime }
            .take(3)

        val plan = scheduler.generateSchedule(tasks, timetable, profile = profile)
        val burnout = BurnoutAnalyzer.analyze(feedbackLogs)

        DashboardUiState(
            greeting = getGreeting(),
            currentDate = today.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
            pendingTaskCount = pendingCount,
            completedTaskCount = completedCount,
            priorityTasks = priorityList,
            upcomingEvents = eventsToday,
            todaysPlan = plan,
            burnoutState = burnout,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    private fun getGreeting(): String {
        val hour = LocalTime.now().hour
        return when (hour) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }
}

class DashboardViewModelFactory(
    private val repository: TaskRepository,
    private val scheduler: Scheduler,
    private val userPrefs: UserPreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(repository, scheduler, userPrefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
