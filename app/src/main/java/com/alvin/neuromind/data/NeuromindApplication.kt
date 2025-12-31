package com.alvin.neuromind.data

import android.app.Application
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.alvin.neuromind.data.preferences.UserPreferencesRepository
import com.alvin.neuromind.domain.NotificationHelper
import com.alvin.neuromind.domain.Scheduler
import com.alvin.neuromind.domain.TaskCheckWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class NeuromindApplication : Application(), Configuration.Provider {
    val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { NeuromindDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { TaskRepository(database.taskDao(), database.timetableDao(), database.feedbackLogDao()) }
    val scheduler by lazy { Scheduler() }
    val userPreferencesRepository by lazy { UserPreferencesRepository(this) }

    override fun onCreate() {
        super.onCreate()

        // Fix for Startup Lag: Run heavy initialization in background
        applicationScope.launch(Dispatchers.IO) {
            NotificationHelper(this@NeuromindApplication).createNotificationChannel()

            val workRequest = PeriodicWorkRequestBuilder<TaskCheckWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(this@NeuromindApplication).enqueueUniquePeriodicWork(
                "TaskCheckWorker",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}