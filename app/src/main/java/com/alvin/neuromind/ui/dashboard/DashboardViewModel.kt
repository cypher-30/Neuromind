package com.alvin.neuromind.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.alvin.neuromind.data.Mood
import com.alvin.neuromind.data.Priority
import com.alvin.neuromind.data.Task
import com.alvin.neuromind.data.TaskRepository
import com.alvin.neuromind.data.TimetableEntry
import com.alvin.neuromind.domain.Scheduler
import com.alvin.neuromind.domain.TimeSlot
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

data class DashboardUiState(
    val greeting: String = "Welcome!",
    val currentDate: String = "",
    val pendingTaskCount: Int = 0,
    val completedTaskCount: Int = 0,
    val upcomingEvents: List<TimetableEntry> = emptyList(),
    val priorityTasks: List<Task> = emptyList(),
    val todaysPlan: Map<TimeSlot, Task> = emptyMap(),
    val showBurnoutWarning: Boolean = false
)

class DashboardViewModel(repository: TaskRepository, private val scheduler: Scheduler) : ViewModel() {
    val uiState: StateFlow<DashboardUiState> = combine(
        repository.allTasks,
        repository.allTimetableEntries,
        repository.allFeedbackLogs
    ) { tasks, timetable, feedbackLogs ->
        val pending = tasks.filter { !it.isCompleted }
        val completed = tasks.filter { it.isCompleted }
        val today = LocalDate.now().dayOfWeek
        val now = LocalTime.now()
        val overdue = pending.filter { it.isOverdue }
        val highPriority = pending.filter { it.priority == Priority.HIGH && !it.isOverdue }
        val priorityList = (overdue + highPriority).distinctBy { it.id }.take(3)
        val upcoming = timetable.filter { it.dayOfWeek == today && it.endTime > now }.sortedBy { it.startTime }.take(2)
        val recentFeedback = feedbackLogs.take(2)
        val showWarning = if (recentFeedback.size < 2) false else recentFeedback.all { it.mood == Mood.STRESSED || it.mood == Mood.TIRED }
        val freeSlots = scheduler.calculateFreeTimeSlots(today, timetable)
        val plan = scheduler.scheduleTasks(pending, freeSlots)
        DashboardUiState(
            greeting = getGreeting(),
            currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
            pendingTaskCount = pending.size,
            completedTaskCount = completed.size,
            upcomingEvents = upcoming,
            priorityTasks = priorityList,
            todaysPlan = plan,
            showBurnoutWarning = showWarning
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = DashboardUiState()
    )
    private fun getGreeting(): String {
        return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }
}
class DashboardViewModelFactory(private val repository: TaskRepository, private val scheduler: Scheduler) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(repository, scheduler) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}