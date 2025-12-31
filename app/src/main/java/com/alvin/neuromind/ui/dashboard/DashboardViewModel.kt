package com.alvin.neuromind.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alvin.neuromind.data.Priority
import com.alvin.neuromind.data.Task
import com.alvin.neuromind.data.TaskRepository
import com.alvin.neuromind.data.TimetableEntry
import com.alvin.neuromind.domain.Scheduler
import com.alvin.neuromind.domain.TimeSlot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
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
    val isLoading: Boolean = true
)

class DashboardViewModel(
    private val repository: TaskRepository,
    private val scheduler: Scheduler
) : ViewModel() {

    // We use flowOn(Dispatchers.Default) to move calculations off the main thread
    val uiState: StateFlow<DashboardUiState> = combine(
        repository.allTasks,
        repository.allTimetableEntries
    ) { tasks, timetableEntries ->
        // This block now runs in the background
        val today = LocalDate.now()
        val greeting = getGreeting()
        val dateStr = today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))

        val pending = tasks.count { !it.isCompleted }
        val completed = tasks.count { it.isCompleted }

        // Priorities: Overdue or High Priority, not done
        val priorities = tasks.filter { !it.isCompleted && (it.isOverdue || it.priority == Priority.HIGH) }
            .sortedBy { it.dueDate }
            .take(3)

        // Events: Today, sorted by time
        val todayEvents = timetableEntries
            .filter { it.dayOfWeek == today.dayOfWeek }
            .sortedBy { it.startTime }

        // AI Scheduling (The Heavy Math)
        val freeSlots = scheduler.calculateFreeTimeSlots(today.dayOfWeek, todayEvents)
        val plan = scheduler.scheduleTasks(tasks, freeSlots)

        DashboardUiState(
            greeting = greeting,
            currentDate = dateStr,
            pendingTaskCount = pending,
            completedTaskCount = completed,
            priorityTasks = priorities,
            upcomingEvents = todayEvents.take(2),
            todaysPlan = plan,
            isLoading = false
        )
    }
        .flowOn(Dispatchers.Default) // <--- CRITICAL FIX: Moves calculation to background
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState(isLoading = true)
        )

    private fun getGreeting(): String {
        val hour = LocalTime.now().hour
        return when (hour) {
            in 5..11 -> "Good Morning"
            in 12..17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }
}

class DashboardViewModelFactory(
    private val repository: TaskRepository,
    private val scheduler: Scheduler
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(repository, scheduler) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}