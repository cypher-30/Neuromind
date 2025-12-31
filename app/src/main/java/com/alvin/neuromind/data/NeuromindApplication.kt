package com.alvin.neuromind.data

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.alvin.neuromind.data.preferences.UserPreferencesRepository
import com.alvin.neuromind.domain.NotificationHelper
import com.alvin.neuromind.domain.Scheduler
import com.alvin.neuromind.domain.TaskCheckWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.TimeUnit

class NeuromindApplication : Application() {
    // Application scope for database operations
    val applicationScope = CoroutineScope(SupervisorJob())

    // Lazy initialization of Database and Repository
    val database by lazy { NeuromindDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { TaskRepository(database.taskDao(), database.timetableDao(), database.feedbackLogDao()) }

    val scheduler by lazy { Scheduler() }
    val userPreferencesRepository by lazy { UserPreferencesRepository(this) }

    override fun onCreate() {
        super.onCreate()

        // 1. Create Notification Channel
        NotificationHelper(this).createNotificationChannel()

        // 2. Schedule Background Worker (Every 15 mins)
        val workRequest = PeriodicWorkRequestBuilder<TaskCheckWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "TaskCheckWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}