package com.alvin.neuromind.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alvin.neuromind.data.Difficulty
import com.alvin.neuromind.data.Priority
import com.alvin.neuromind.data.Task
import com.alvin.neuromind.data.TaskRepository
import com.alvin.neuromind.data.TimetableEntry
import com.alvin.neuromind.data.preferences.ThemeSetting
import com.alvin.neuromind.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val taskRepository: TaskRepository // Added Repository access
) : ViewModel() {

    val themeSetting: StateFlow<ThemeSetting> = userPreferencesRepository.userTheme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeSetting.SYSTEM
        )

    fun updateTheme(theme: ThemeSetting) {
        viewModelScope.launch {
            userPreferencesRepository.saveThemeSetting(theme)
        }
    }

    // *** NEW FEATURE: DEMO DATA GENERATOR ***
    fun generateDemoData() {
        viewModelScope.launch {
            // 1. Clear existing sample spam if needed (Optional, keeping it additive for now)

            // 2. Add Timetable Entries
            val entries = listOf(
                TimetableEntry(title = "Mobile App Dev", dayOfWeek = DayOfWeek.MONDAY, startTime = LocalTime.of(9, 0), endTime = LocalTime.of(11, 0), venue = "Lab 3", details = "Jetpack Compose"),
                TimetableEntry(title = "Linear Algebra", dayOfWeek = DayOfWeek.MONDAY, startTime = LocalTime.of(13, 0), endTime = LocalTime.of(14, 30), venue = "Hall A", details = "Matrices"),
                TimetableEntry(title = "Gym", dayOfWeek = DayOfWeek.TUESDAY, startTime = LocalTime.of(17, 0), endTime = LocalTime.of(18, 30), venue = "Campus Gym", details = "Leg Day"),
                TimetableEntry(title = "Database Systems", dayOfWeek = DayOfWeek.WEDNESDAY, startTime = LocalTime.of(10, 0), endTime = LocalTime.of(12, 0), venue = "Room 404", details = "SQL & Normalization"),
                TimetableEntry(title = "Project Meeting", dayOfWeek = DayOfWeek.FRIDAY, startTime = LocalTime.of(14, 0), endTime = LocalTime.of(15, 0), venue = "Library", details = "Final Year Project")
            )
            entries.forEach { taskRepository.insert(it) }

            // 3. Add Tasks
            val tasks = listOf(
                Task(title = "Finish Neuromind UI", description = "Implement the new timetable design.", dueDate = System.currentTimeMillis() + 86400000, priority = Priority.HIGH, difficulty = Difficulty.HARD),
                Task(title = "Buy Groceries", description = "Milk, Eggs, Coffee", dueDate = System.currentTimeMillis() + 172800000, priority = Priority.MEDIUM, difficulty = Difficulty.EASY),
                Task(title = "Submit Assignment", description = "Math homework due soon.", dueDate = System.currentTimeMillis() - 3600000, priority = Priority.HIGH, difficulty = Difficulty.MEDIUM) // Overdue
            )
            tasks.forEach { taskRepository.insert(it) }
        }
    }
}

class SettingsViewModelFactory(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val taskRepository: TaskRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(userPreferencesRepository, taskRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}