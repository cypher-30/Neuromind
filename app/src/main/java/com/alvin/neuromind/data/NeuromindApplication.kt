package com.alvin.neuromind.data

import android.app.Application
import com.alvin.neuromind.data.preferences.UserPreferencesRepository
import com.alvin.neuromind.domain.Scheduler

class NeuromindApplication : Application() {
    val database by lazy { NeuromindDatabase.getDatabase(this) }
    val scheduler by lazy { Scheduler() }
    val repository by lazy { TaskRepository(database.taskDao(), database.timetableDao(), database.feedbackLogDao()) }
    val userPreferencesRepository by lazy { UserPreferencesRepository(this) }
}