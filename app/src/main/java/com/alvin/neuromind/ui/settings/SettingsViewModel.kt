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
            val allTasks = taskRepository.allTasks.first()
            allTasks.forEach { taskRepository.delete(it) }

            val allEntries = taskRepository.allTimetableEntries.first()
            allEntries.forEach { taskRepository.delete(it) }
        }
    }

    // --- RANDOM DEMO DATA (BULK) ---
    fun generateDemoData() {
        viewModelScope.launch {
            val subjects = listOf("Math", "Physics", "History", "Coding", "Biology", "Art", "Economics")
            val types = listOf("Assignment", "Exam", "Reading", "Project", "Essay")

            repeat(5) {
                val randomSubject = subjects.random()
                val randomType = types.random()
                val randomDaysForward = Random.nextLong(0, 7)

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
    }

    // --- FULL WEEK TIMETABLE ---
    fun generateBaseTimetable() {
        viewModelScope.launch {
            val entries = listOf(
                // Monday
                TimetableEntry(title = "Mobile App Dev", dayOfWeek = DayOfWeek.MONDAY, startTime = LocalTime.of(9, 0), endTime = LocalTime.of(11, 0), venue = "Lab 3", details = "Jetpack Compose"),
                TimetableEntry(title = "Linear Algebra", dayOfWeek = DayOfWeek.MONDAY, startTime = LocalTime.of(13, 0), endTime = LocalTime.of(14, 30), venue = "Hall A", details = "Matrices"),
                // Tuesday
                TimetableEntry(title = "Gym", dayOfWeek = DayOfWeek.TUESDAY, startTime = LocalTime.of(7, 0), endTime = LocalTime.of(8, 30), venue = "Campus Gym", details = "Cardio"),
                TimetableEntry(title = "Physics Lab", dayOfWeek = DayOfWeek.TUESDAY, startTime = LocalTime.of(10, 0), endTime = LocalTime.of(12, 0), venue = "Sci Block", details = "Optics"),
                // Wednesday
                TimetableEntry(title = "Database Systems", dayOfWeek = DayOfWeek.WEDNESDAY, startTime = LocalTime.of(10, 0), endTime = LocalTime.of(12, 0), venue = "Room 404", details = "SQL"),
                // Thursday
                TimetableEntry(title = "Web Development", dayOfWeek = DayOfWeek.THURSDAY, startTime = LocalTime.of(14, 0), endTime = LocalTime.of(16, 0), venue = "Lab 1", details = "React/Node"),
                // Friday
                TimetableEntry(title = "Project Meeting", dayOfWeek = DayOfWeek.FRIDAY, startTime = LocalTime.of(11, 0), endTime = LocalTime.of(12, 0), venue = "Library", details = "Group A")
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