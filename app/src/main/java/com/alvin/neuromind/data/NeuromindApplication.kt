package com.alvin.neuromind.data

import android.app.Application
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.alvin.neuromind.data.preferences.UserPreferencesRepository
import com.alvin.neuromind.domain.EventReminderScheduler
import com.alvin.neuromind.domain.NotificationHelper
import com.alvin.neuromind.domain.Scheduler
import com.alvin.neuromind.domain.SuggestionWorker
import com.alvin.neuromind.domain.TaskCheckWorker
import com.alvin.neuromind.domain.WidgetRefreshWorker
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
    val repository by lazy {
        TaskRepository(
            appContext = this,
            database = database,
            taskDao = database.taskDao(),
            timetableDao = database.timetableDao(),
            feedbackLogDao = database.feedbackLogDao(),
            focusSessionDao = database.focusSessionDao(),
            editorDraftDao = database.editorDraftDao(),
            externalScope = applicationScope
        )
    }
    val scheduler by lazy { Scheduler() }
    val userPreferencesRepository by lazy { UserPreferencesRepository(this) }

    override fun onCreate() {
        super.onCreate()

        applicationScope.launch(Dispatchers.IO) {
            NotificationHelper(this@NeuromindApplication).createNotificationChannel()
            repository.refreshWidgetNow()
            // Reconcile event alarms (they don't survive force-stop / some OEM task killers)
            // and arm the post-midnight widget refresh for the daily-progress count.
            runCatching { repository.rescheduleAllReminders() }
            EventReminderScheduler(this@NeuromindApplication).scheduleMidnightRefresh()

            val taskCheckRequest = PeriodicWorkRequestBuilder<TaskCheckWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(this@NeuromindApplication).enqueueUniquePeriodicWork(
                "TaskCheckWorker",
                ExistingPeriodicWorkPolicy.KEEP,
                taskCheckRequest
            )

            // Schedule the daily suggestion worker; reschedule whenever peakStartHour changes
            launch {
                userPreferencesRepository.peakStartHour.collectLatest { peakHour ->
                    scheduleSuggestionWorker(peakHour)
                }
            }

            // Keep widget auto-rotation active even when app data is unchanged.
            launch {
                userPreferencesRepository.widgetStackIntervalMinutes.collectLatest { minutes ->
                    scheduleWidgetRefreshWorker(minutes)
                }
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

    private fun scheduleWidgetRefreshWorker(intervalMinutes: Int) {
        val safe = when (intervalMinutes) {
            15, 30, 60 -> intervalMinutes
            else -> 30
        }
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(safe.toLong(), TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WidgetRefreshWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
