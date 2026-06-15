package com.alvin.neuromind.domain

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.alvin.neuromind.data.NeuromindApplication
import kotlinx.coroutines.flow.first

class SuggestionWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val application = applicationContext as NeuromindApplication
        val repository = application.repository
        val userPrefs = application.userPreferencesRepository

        val tasks = repository.allTasks.first()
        val timetable = repository.allTimetableEntries.first()
        val profile = userPrefs.cognitiveProfile.first()

        val suggestion = SuggestionEngine.suggest(tasks, timetable, profile) ?: return Result.success()

        NotificationHelper(applicationContext).showNotification(
            id = SUGGESTION_NOTIFICATION_ID,
            title = "Neuromind Suggests",
            message = suggestion.message
        )

        return Result.success()
    }

    companion object {
        const val SUGGESTION_NOTIFICATION_ID = 2000
        const val WORK_NAME = "SuggestionWorker"
    }
}
