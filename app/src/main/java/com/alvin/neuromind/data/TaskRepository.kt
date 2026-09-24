package com.alvin.neuromind.data

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.alvin.neuromind.data.backup.BackupData
import com.alvin.neuromind.data.backup.toDto
import com.alvin.neuromind.data.backup.toEntity
import com.alvin.neuromind.domain.EventReminderScheduler
import com.alvin.neuromind.ui.widget.WidgetRefreshCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class TaskRepository(
    private val appContext: Context,
    private val database: NeuromindDatabase,
    private val taskDao: TaskDao,
    private val timetableDao: TimetableDao,
    private val feedbackLogDao: FeedbackLogDao,
    private val focusSessionDao: FocusSessionDao,
    private val editorDraftDao: EditorDraftDao,
    /** Outlives screens, so draft writes still land after an editor closes. */
    val externalScope: CoroutineScope
) {
    private val reminderScheduler by lazy { EventReminderScheduler(appContext) }

    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    suspend fun getTaskById(id: Int): Task? = taskDao.getTaskById(id)
    suspend fun insertTask(task: Task) {
        taskDao.insertTask(task)
        refreshTodayWidget()
    }
    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
        refreshTodayWidget()
    }
    suspend fun deleteTask(task: Task) {
        taskDao.deleteTaskAndSubTasks(task)
        editorDraftDao.delete(DraftKeys.of(DraftKeys.TASK, task.id))
        refreshTodayWidget()
    }

    val allTimetableEntries: Flow<List<TimetableEntry>> = timetableDao.getAllEntries()
    suspend fun getTimetableEntryById(id: Int): TimetableEntry? = timetableDao.getEntryById(id)

    /** Returns the saved entry (with its generated id) and how its reminder was scheduled. */
    suspend fun insertTimetableEntry(entry: TimetableEntry): Pair<TimetableEntry, EventReminderScheduler.Status> {
        val id = timetableDao.insertEntry(entry).toInt()
        val saved = entry.copy(id = id)
        val status = reminderScheduler.schedule(saved)
        refreshTodayWidget()
        return saved to status
    }
    suspend fun updateTimetableEntry(entry: TimetableEntry): EventReminderScheduler.Status {
        timetableDao.updateEntry(entry)
        val status = reminderScheduler.schedule(entry)
        refreshTodayWidget()
        return status
    }
    suspend fun deleteTimetableEntry(entry: TimetableEntry) {
        timetableDao.deleteEntry(entry)
        reminderScheduler.cancel(entry.id)
        editorDraftDao.delete(DraftKeys.of(DraftKeys.EVENT, entry.id))
        editorDraftDao.delete(DraftKeys.of(DraftKeys.TIMETABLE, entry.id))
        refreshTodayWidget()
    }

    fun reminderScheduler(): EventReminderScheduler = reminderScheduler

    suspend fun rescheduleAllReminders() {
        reminderScheduler.rescheduleAll(allTimetableEntries.first())
    }

    // --- Editor drafts (local only; never exported) ---
    suspend fun getDraft(key: String): EditorDraft? = editorDraftDao.get(key)
    suspend fun saveDraft(key: String, payload: String) =
        editorDraftDao.upsert(EditorDraft(key = key, payload = payload, updatedAt = System.currentTimeMillis()))
    suspend fun deleteDraft(key: String) = editorDraftDao.delete(key)
    suspend fun deleteAllDrafts() = editorDraftDao.deleteAll()

    val allFeedbackLogs: Flow<List<FeedbackLog>> = feedbackLogDao.getAllLogs()
    suspend fun insertFeedbackLog(log: FeedbackLog) = feedbackLogDao.insertFeedbackLog(log)
    suspend fun deleteAllFeedbackLogs() = feedbackLogDao.deleteAllLogs()

    val allFocusSessions: Flow<List<FocusSession>> = focusSessionDao.getAllSessions()
    suspend fun insertFocusSession(session: FocusSession) = focusSessionDao.insertSession(session)
    suspend fun deleteAllFocusSessions() = focusSessionDao.deleteAllSessions()

    suspend fun getWidgetTasks(nowMillis: Long, limit: Int = 3): List<Task> =
        taskDao.getWidgetTasks(nowMillis = nowMillis, limit = limit)

    suspend fun getWidgetNextEntry(
        dayOfWeek: DayOfWeek,
        date: LocalDate,
        currentTime: LocalTime
    ): TimetableEntry? = timetableDao.getNextEntryForWidget(
        todayDayOfWeek = dayOfWeek.name,
        todayDate = date.toString(),
        currentTime = currentTime.toString()
    )

    suspend fun exportAll(): BackupData = BackupData(
        exportedAt = System.currentTimeMillis(),
        tasks = allTasks.first(),
        timetable = allTimetableEntries.first().map { it.toDto() },
        feedback = allFeedbackLogs.first(),
        focusSessions = allFocusSessions.first()
    )

    /**
     * Replaces all local data with [data]. Original IDs are preserved, so cross-references
     * (e.g. Task.prerequisiteTaskId) survive intact. Editor drafts are cleared too, since
     * they could otherwise point at replaced records.
     */
    suspend fun importAll(data: BackupData) {
        if (data.schemaVersion > BackupData.CURRENT_SCHEMA_VERSION) {
            error("This backup was made by a newer version of Neuromind (format ${data.schemaVersion}).")
        }
        allTimetableEntries.first().forEach { reminderScheduler.cancel(it.id) }
        database.withTransaction {
            taskDao.deleteAllTasks()
            timetableDao.deleteAllEntries()
            feedbackLogDao.deleteAllLogs()
            focusSessionDao.deleteAllSessions()
            editorDraftDao.deleteAll()

            data.tasks.forEach { taskDao.insertTask(it) }
            data.timetable.forEach { timetableDao.insertEntry(it.toEntity()) }
            data.feedback.forEach { feedbackLogDao.insertFeedbackLog(it) }
            data.focusSessions.forEach { focusSessionDao.insertSession(it) }
        }
        rescheduleAllReminders()
        refreshTodayWidget()
    }

    suspend fun refreshWidgetNow() {
        refreshTodayWidget()
    }

    private suspend fun refreshTodayWidget() {
        try {
            WidgetRefreshCoordinator.updateAllWidgets(appContext)
        } catch (t: Throwable) {
            Log.w("TaskRepository", "Widget refresh skipped", t)
        }
    }
}