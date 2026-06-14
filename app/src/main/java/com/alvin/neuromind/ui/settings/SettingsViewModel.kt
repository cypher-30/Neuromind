package com.alvin.neuromind.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alvin.neuromind.data.*
import com.alvin.neuromind.data.preferences.*
import com.alvin.neuromind.domain.NotificationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import kotlin.random.Random

class SettingsViewModel(
    private val userPrefs: UserPreferencesRepository,
    private val repository: TaskRepository
) : ViewModel() {

    val themeSetting: StateFlow<ThemeSetting> = userPrefs.userTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeSetting.SYSTEM)

    val peakStartHour: StateFlow<Int> = userPrefs.peakStartHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 8)

    val peakEndHour: StateFlow<Int> = userPrefs.peakEndHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 11)

    val preferredSessionLength: StateFlow<Int> = userPrefs.focusDuration
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 25)

    val taskStyle: StateFlow<TaskStyle> = userPrefs.taskStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TaskStyle.BALANCED)

    fun updateTheme(theme: ThemeSetting) = viewModelScope.launch {
        userPrefs.saveThemeSetting(theme)
    }

    fun updatePeakStartHour(hour: Int) = viewModelScope.launch {
        userPrefs.savePeakStartHour(hour)
    }

    fun updatePeakEndHour(hour: Int) = viewModelScope.launch {
        userPrefs.savePeakEndHour(hour)
    }

    fun updateSessionLength(minutes: Int) = viewModelScope.launch {
        userPrefs.saveFocusDuration(minutes)
    }

    fun updateTaskStyle(style: TaskStyle) = viewModelScope.launch {
        userPrefs.saveTaskStyle(style)
    }

    fun resetAppData() = viewModelScope.launch {
        repository.allTasks.first().forEach { repository.deleteTask(it) }
        repository.allTimetableEntries.first().forEach { repository.deleteTimetableEntry(it) }
    }

    /**
     * BULK GENERATION: Creates 20 diverse tasks across the next 10 days
     * to test the Task List and Dashboard Priority logic.
     */
    fun generateDemoData() = viewModelScope.launch {
        val subjects = listOf("Math", "Physics", "History", "Coding", "Biology", "Art", "Economics", "Law", "Music")
        val types = listOf("Assignment", "Exam Prep", "Reading", "Project", "Essay", "Research", "Lab Report")

        repeat(20) { index ->
            val randomSubject = subjects.random()
            val randomType = types.random()
            val randomDaysOffset = Random.nextLong(-2, 10)

            val task = Task(
                title = "$randomSubject $randomType",
                description = "Automated test task #$index for $randomSubject. Review specific chapters.",
                dueDate = System.currentTimeMillis() + (randomDaysOffset * 86400000L),
                priority = Priority.entries.random(),
                difficulty = Difficulty.entries.random(),
                durationMinutes = Random.nextInt(30, 180)
            )
            repository.insertTask(task)
        }
    }

    /**
     * BULK GENERATION: Creates a full, busy 7-day academic schedule
     * to test the Timetable Agenda view and AI gaps.
     */
    fun generateBaseTimetable() = viewModelScope.launch {
        val scheduleData = listOf(
            // Monday
            Triple("Mobile App Dev", DayOfWeek.MONDAY, Pair(9, 11)),
            Triple("Linear Algebra", DayOfWeek.MONDAY, Pair(13, 15)),
            // Tuesday
            Triple("Physics Lab", DayOfWeek.TUESDAY, Pair(10, 13)),
            Triple("Gym Session", DayOfWeek.TUESDAY, Pair(17, 18)),
            // Wednesday
            Triple("Database Systems", DayOfWeek.WEDNESDAY, Pair(10, 12)),
            Triple("Study Group", DayOfWeek.WEDNESDAY, Pair(14, 16)),
            // Thursday
            Triple("Web Development", DayOfWeek.THURSDAY, Pair(14, 16)),
            Triple("History Lecture", DayOfWeek.THURSDAY, Pair(11, 12)),
            // Friday
            Triple("Project Meeting", DayOfWeek.FRIDAY, Pair(11, 13)),
            Triple("Economics", DayOfWeek.FRIDAY, Pair(15, 17)),
            // Weekend One-Time Events (Path A test)
            Triple("Library Session", DayOfWeek.SATURDAY, Pair(10, 14)),
            Triple("Laundry/Cleaning", DayOfWeek.SUNDAY, Pair(9, 11))
        )

        scheduleData.forEach { (title, day, times) ->
            repository.insertTimetableEntry(
                TimetableEntry(
                    title = title,
                    dayOfWeek = day,
                    startTime = LocalTime.of(times.first, 0),
                    endTime = LocalTime.of(times.second, 0),
                    venue = "Campus Building ${Random.nextInt(1, 5)}",
                    isRecurring = true
                )
            )
        }
    }

    fun testNotification(context: Context) {
        NotificationHelper(context).showNotification(999, "Neuromind", "Notification system active!")
    }
}

class SettingsViewModelFactory(
    private val userPrefs: UserPreferencesRepository,
    private val repository: TaskRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(userPrefs, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
