package com.alvin.neuromind.data

import android.app.Application
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.alvin.neuromind.data.preferences.UserPreferencesRepository
import com.alvin.neuromind.domain.NotificationHelper
import com.alvin.neuromind.domain.Scheduler
import com.alvin.neuromind.domain.SuggestionWorker
import com.alvin.neuromind.domain.TaskCheckWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class NeuromindApplication : Application(), Configuration.Provider {
    val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { NeuromindDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { TaskRepository(database.taskDao(), database.timetableDao(), database.feedbackLogDao(), database.focusSessionDao()) }
    val scheduler by lazy { Scheduler() }
    val userPreferencesRepository by lazy { UserPreferencesRepository(this) }

    override fun onCreate() {
        super.onCreate()

        applicationScope.launch(Dispatchers.IO) {
            NotificationHelper(this@NeuromindApplication).createNotificationChannel()

            val taskCheckRequest = PeriodicWorkRequestBuilder<TaskCheckWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(this@NeuromindApplication).enqueueUniquePeriodicWork(
                "TaskCheckWorker",
                ExistingPeriodicWorkPolicy.KEEP,
                taskCheckRequest
            )

            // Schedule the daily suggestion worker; reschedule whenever peakStartHour changes
            userPreferencesRepository.peakStartHour.collectLatest { peakHour ->
                scheduleSuggestionWorker(peakHour)
            }
        }
    }

    private fun scheduleSuggestionWorker(peakStartHour: Int) {
        val now = LocalDateTime.now()
        val targetToday = now.toLocalDate().atTime(peakStartHour, 0)
        val nextTarget = if (now.isBefore(targetToday)) targetToday else targetToday.plusDays(1)
        val initialDelayMinutes = java.time.Duration.between(now, nextTarget).toMinutes()

        val request = PeriodicWorkRequestBuilder<SuggestionWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SuggestionWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
