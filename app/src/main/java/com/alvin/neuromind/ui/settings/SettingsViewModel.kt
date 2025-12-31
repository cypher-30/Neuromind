package com.alvin.neuromind.ui.settings

import android.content.Context
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
import com.alvin.neuromind.domain.NotificationHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import kotlin.random.Random

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val taskRepository: TaskRepository
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

    // --- RESET DATA ---
    fun resetAppData() {
        viewModelScope.launch {
            // Since we don't have a deleteAll() in DAO yet, we iterate and delete.
            // In a production app, you would add @Query("DELETE FROM tasks") to DAO.
            val allTasks = taskRepository.allTasks.first()
            allTasks.forEach { taskRepository.delete(it) }

            val allEntries = taskRepository.allTimetableEntries.first()
            allEntries.forEach { taskRepository.delete(it) }
        }
    }

    // --- RANDOM DEMO DATA ---
    fun generateDemoData() {
        viewModelScope.launch {
            val subjects = listOf("Math", "Physics", "History", "Coding", "Biology", "Art", "Economics")
            val types = listOf("Assignment", "Exam", "Reading", "Project", "Essay")

            val randomSubject = subjects.random()
            val randomType = types.random()

            val randomDaysForward = Random.nextLong(0, 7) // 0 to 7 days from now

            val task = Task(
                title = "$randomSubject $randomType",
                description = "Prepare for the upcoming $randomSubject session. Review chapter ${Random.nextInt(1, 10)}.",
                dueDate = System.currentTimeMillis() + (randomDaysForward * 86400000L),
                priority = Priority.entries.random(),
                difficulty = Difficulty.entries.random(),
                durationMinutes = Random.nextInt(30, 120)
            )
            taskRepository.insert(task)
        }
    }

    // Add basic timetable data if empty
    fun generateBaseTimetable() {
        viewModelScope.launch {
            val entries = listOf(
                TimetableEntry(title = "Mobile App Dev", dayOfWeek = DayOfWeek.MONDAY, startTime = LocalTime.of(9, 0), endTime = LocalTime.of(11, 0), venue = "Lab 3", details = "Jetpack Compose"),
                TimetableEntry(title = "Gym", dayOfWeek = DayOfWeek.TUESDAY, startTime = LocalTime.of(17, 0), endTime = LocalTime.of(18, 30), venue = "Campus Gym", details = "Cardio"),
                TimetableEntry(title = "Database Systems", dayOfWeek = DayOfWeek.WEDNESDAY, startTime = LocalTime.of(10, 0), endTime = LocalTime.of(12, 0), venue = "Room 404", details = "SQL")
            )
            entries.forEach { taskRepository.insert(it) }
        }
    }

    // --- TEST NOTIFICATION ---
    fun testNotification(context: Context) {
        val helper = NotificationHelper(context)
        helper.showNotification(
            id = 999,
            title = "Neuromind Test",
            message = "If you see this, notifications are working!"
        )
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