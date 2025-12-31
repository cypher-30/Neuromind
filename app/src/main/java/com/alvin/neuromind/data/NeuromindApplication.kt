package com.alvin.neuromind.data

import android.app.Application
import com.alvin.neuromind.data.preferences.UserPreferencesRepository
import com.alvin.neuromind.domain.Scheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class NeuromindApplication : Application() {
    // No need to cancel this scope as it'll be torn down with the process
    val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { NeuromindDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { TaskRepository(database.taskDao(), database.timetableDao(), database.feedbackLogDao()) }
    val scheduler by lazy { Scheduler() }
    val userPreferencesRepository by lazy { UserPreferencesRepository(this) }
}