package com.alvin.neuromind.domain

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.alvin.neuromind.data.NeuromindApplication
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class TaskCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val application = applicationContext as NeuromindApplication
        val repository = application.repository
        val notificationHelper = NotificationHelper(applicationContext)

        // Respect the in-app Notifications switch (Settings / onboarding).
        if (!application.userPreferencesRepository.notificationsEnabled.first()) return Result.success()

        // 1. Check Tasks Due Soon
        // We look ahead 45 minutes to ensure we catch tasks even if the worker runs slightly late
        val nowMillis = System.currentTimeMillis()
        val checkWindowMillis = nowMillis + (45 * 60 * 1000)

        val allTasks = repository.allTasks.first()

        allTasks.forEach { task ->
            if (!task.isCompleted && task.dueDate != null) {
                // Check if due date is in the future AND within our check window
                if (task.dueDate > nowMillis && task.dueDate <= checkWindowMillis) {

                    // Calculate exact minutes remaining for "Accuracy" perception
                    val dueDateTime = java.time.Instant.ofEpochMilli(task.dueDate).atZone(ZoneId.systemDefault())
                    val nowDateTime = java.time.Instant.now().atZone(ZoneId.systemDefault())
                    val minutesRemaining = ChronoUnit.MINUTES.between(nowDateTime, dueDateTime)
                    val timeString = dueDateTime.format(DateTimeFormatter.ofPattern("h:mm a"))

                    notificationHelper.showNotification(
                        id = task.id.hashCode(),
                        title = "Task Due: ${task.title}",
                        message = "Due at $timeString (in ~$minutesRemaining mins)"
                    )
                }
            }
        }

        // 2. Check Timetable Events (Classes)
        val today = LocalDate.now()
        val timeNow = LocalTime.now()
        val checkWindowTime = timeNow.plusMinutes(45)

        val allEntries = repository.allTimetableEntries.first()
        // Dated events with a reminder setting get exact alarms (EventReminderScheduler);
        // only legacy entries (reminderMode == null) are still polled here.
        val todaysEntries = allEntries.filter { it.occursOn(today) && !it.isAllDay && it.reminderMode == null }

        todaysEntries.forEach { entry ->
            // Check if start time is in the future AND within check window
            if (entry.startTime.isAfter(timeNow) && entry.startTime.isBefore(checkWindowTime)) {

                val minutesUntilClass = ChronoUnit.MINUTES.between(timeNow, entry.startTime)
                val timeString = entry.startTime.format(DateTimeFormatter.ofPattern("h:mm a"))

                notificationHelper.showNotification(
                    id = NotificationHelper.TIMETABLE_NOTIFICATION_BASE + entry.id,
                    title = "Upcoming: ${entry.title}",
                    message = "Starts at $timeString (in ~$minutesUntilClass mins) @ ${entry.venue ?: "Unknown Venue"}"
                )
            }
        }

        return Result.success()
    }
}