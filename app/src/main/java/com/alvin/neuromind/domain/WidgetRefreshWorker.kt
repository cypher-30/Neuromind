package com.alvin.neuromind.domain

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.alvin.neuromind.ui.widget.WidgetRefreshCoordinator

class WidgetRefreshWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            WidgetRefreshCoordinator.updateAllWidgets(applicationContext)
            Result.success()
        } catch (_: Throwable) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "WidgetRefreshWorker"
    }
}

