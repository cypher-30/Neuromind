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
    val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { NeuromindDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { TaskRepository(database.taskDao(), database.timetableDao(), database.feedbackLogDao()) }
    val scheduler by lazy { Scheduler() }
    val userPreferencesRepository by lazy { UserPreferencesRepository(this) }

    override fun onCreate() {
        super.onCreate()

        // 1. Create the Notification Channel (Required for Android 8+)
        NotificationHelper(this).createNotificationChannel()

        // 2. Schedule the Background Worker
        val workRequest = PeriodicWorkRequestBuilder<TaskCheckWorker>(
            15, TimeUnit.MINUTES // Run every 15 minutes (Minimum allowed by Android)
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "TaskCheckWorker",
            ExistingPeriodicWorkPolicy.KEEP, // Don't replace if already running
            workRequest
        )
    }
}