package com.alvin.neuromind.domain

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.alvin.neuromind.data.NeuromindApplication
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class TaskCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val application = applicationContext as NeuromindApplication
        val repository = application.repository
        val notificationHelper = NotificationHelper(applicationContext)

        // 1. Check Tasks Due Soon (in the next 30 minutes)
        val now = System.currentTimeMillis()
        val thirtyMinutesFromNow = now + (30 * 60 * 1000)

        // We fetch all tasks once (efficient enough for local DB)
        val allTasks = repository.allTasks.first()

        allTasks.forEach { task ->
            if (!task.isCompleted && task.dueDate != null) {
                if (task.dueDate in now..thirtyMinutesFromNow) {
                    notificationHelper.showNotification(
                        id = task.id.hashCode(),
                        title = "Task Due Soon!",
                        message = "${task.title} is due in less than 30 mins."
                    )
                }
            }
        }

        // 2. Check Classes/Events Starting Soon (in the next 20 minutes)
        val today = LocalDate.now()
        val timeNow = LocalTime.now()
        val twentyMinutesLater = timeNow.plusMinutes(20)

        val allEntries = repository.allTimetableEntries.first()
        val todaysEntries = allEntries.filter { it.dayOfWeek == today.dayOfWeek }

        todaysEntries.forEach { entry ->
            // Check if start time is between now and +20 mins
            if (entry.startTime.isAfter(timeNow) && entry.startTime.isBefore(twentyMinutesLater)) {
                notificationHelper.showNotification(
                    id = entry.id.hashCode(),
                    title = "Upcoming Event",
                    message = "${entry.title} starts at ${entry.startTime}."
                )
            }
        }

        return Result.success()
    }
}