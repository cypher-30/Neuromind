package com.alvin.neuromind.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alvin.neuromind.data.FeedbackLog
import com.alvin.neuromind.data.Priority
import com.alvin.neuromind.data.Task
import com.alvin.neuromind.data.TaskRepository
import com.alvin.neuromind.data.TimetableEntry
import com.alvin.neuromind.data.preferences.UserPreferencesRepository
import com.alvin.neuromind.domain.BurnoutAnalyzer
import com.alvin.neuromind.domain.BurnoutState
import com.alvin.neuromind.domain.DailyProgress
import com.alvin.neuromind.domain.DailyProgressCalculator
import com.alvin.neuromind.domain.DueOverview
import com.alvin.neuromind.domain.RebalanceProposal
import com.alvin.neuromind.domain.Scheduler
import com.alvin.neuromind.domain.Suggestion
import com.alvin.neuromind.domain.SuggestionEngine
import com.alvin.neuromind.domain.TaskRebalancer
import com.alvin.neuromind.domain.TimeSlot
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class DashboardUiState(
    val greeting: String = "",
    val currentDate: String = "",
    /** Today's due tasks only (see DailyProgressCalculator). */
    val todayProgress: DailyProgress = DailyProgress(total = 0, completed = 0),
    /** Every open dated task plus ones finished ahead of time (second face of the progress card). */
    val allDue: DueOverview = DueOverview(DailyProgress(0, 0), overdue = 0, upcoming = 0),
    val priorityTasks: List<Task> = emptyList(),
    /** Classes/events in progress right now (all-day events count for the whole day). */
    val happeningNow: List<TimetableEntry> = emptyList(),
    /** Later today, soonest first. */
    val upcomingEvents: List<TimetableEntry> = emptyList(),
    val todaysPlan: Map<TimeSlot, Task> = emptyMap(),
    val burnoutState: BurnoutState? = null,
    val rebalanceProposals: List<RebalanceProposal> = emptyList(),
    val suggestion: Suggestion? = null,
    val currentStreak: Int = 0,
    val isLoading: Boolean = true
)

class DashboardViewModel(
    private val repository: TaskRepository,
    private val scheduler: Scheduler,
    private val userPrefs: UserPreferencesRepository
) : ViewModel() {

    private val _rebalanceDismissed = MutableStateFlow(false)

    // Re-evaluates "now", "up next" and today's progress every minute (and so
    // across midnight) even when no database row changes.
    private val clock = flow {
        while (true) {
            emit(LocalDateTime.now().withSecond(0).withNano(0))
            val now = LocalDateTime.now()
            val msToNextMinute = 60_000L - (now.second * 1000L + now.nano / 1_000_000L)
            delay(msToNextMinute.coerceAtLeast(1_000L))
        }
    }

    private val settings = combine(userPrefs.cognitiveProfile, _rebalanceDismissed, clock) { profile, dismissed, now ->
        Triple(profile, dismissed, now)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.allTasks,
        repository.allTimetableEntries,
        repository.allFeedbackLogs,
        settings
    ) { tasks, timetable, feedbackLogs, (profile, rebalanceDismissed, clockNow) ->

        val now = System.currentTimeMillis()
        val today = clockNow.toLocalDate()
        val nowTime = clockNow.toLocalTime()

        val priorityList = tasks
            .filter { task -> !task.isCompleted && (task.priority == Priority.HIGH || (task.dueDate != null && task.dueDate < now)) }
            .sortedWith(compareByDescending<Task> { task -> task.dueDate != null && task.dueDate < now }.thenByDescending { it.priority })
            .take(5)

        val todaysEntries = timetable.filter { it.occursOn(today) }
        val happeningNow = todaysEntries
            .filter { it.isAllDay || (!nowTime.isBefore(it.startTime) && nowTime.isBefore(it.endTime)) }
            .sortedWith(compareBy<TimetableEntry> { it.isAllDay }.thenBy { it.startTime })
        val eventsToday = todaysEntries
            .filter { !it.isAllDay && it.startTime.isAfter(nowTime) }
            .sortedBy { it.startTime }
            .take(3)

        val plan = scheduler.generateSchedule(tasks, timetable, date = today, profile = profile)
        val burnout = BurnoutAnalyzer.analyze(feedbackLogs)

        val overdueTasks = tasks.filter { it.isOverdue && !it.isCompleted }
        val rebalanceProposals = if (overdueTasks.size >= 3 && !rebalanceDismissed) {
            TaskRebalancer.rebalance(overdueTasks, timetable, profile = profile)
        } else {
            emptyList()
        }

        // Suggestion is lower priority than burnout and rebalance — only compute when neither is active
        val suggestion = if (burnout == null && rebalanceProposals.isEmpty()) {
            SuggestionEngine.suggest(tasks, timetable, profile, feedbackLogs)
        } else {
            null
        }

        DashboardUiState(
            greeting = getGreeting(),
            currentDate = today.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
            todayProgress = DailyProgressCalculator.forDate(tasks, today),
            allDue = DailyProgressCalculator.allDue(tasks, today),
            priorityTasks = priorityList,
            happeningNow = happeningNow,
            upcomingEvents = eventsToday,
            todaysPlan = plan,
            burnoutState = burnout,
            rebalanceProposals = rebalanceProposals,
            suggestion = suggestion,
            currentStreak = currentStreak(feedbackLogs, today),
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun confirmRebalance() {
        val proposals = uiState.value.rebalanceProposals
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            proposals.forEach { proposal ->
                val epochMillis = proposal.suggestedDate
                    .atTime(proposal.suggestedTime)
                    .atZone(zone)
                    .toInstant()
                    .toEpochMilli()
                repository.updateTask(proposal.task.copy(dueDate = epochMillis))
            }
            _rebalanceDismissed.value = false
        }
    }

    fun dismissRebalance() {
        _rebalanceDismissed.value = true
    }

    // Task has no completedAt, so the streak's only honest source is
    // consecutive calendar days with a FeedbackLog. A day is "current" only
    // if the most recent log is today or yesterday — otherwise the streak
    // has already lapsed and reads as 0 (hidden by the UI) rather than a
    // stale count.
    private fun currentStreak(logs: List<FeedbackLog>, today: LocalDate): Int {
        val zone = ZoneId.systemDefault()
        val loggedDays = logs.map { java.time.Instant.ofEpochMilli(it.date).atZone(zone).toLocalDate() }.toSet()
        if (loggedDays.isEmpty()) return 0

        var cursor = if (today in loggedDays) today
                     else if (today.minusDays(1) in loggedDays) today.minusDays(1)
                     else return 0

        var streak = 0
        while (cursor in loggedDays) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

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
