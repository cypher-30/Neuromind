package com.alvin.neuromind.data

import androidx.room.withTransaction
import com.alvin.neuromind.data.backup.BackupData
import com.alvin.neuromind.data.backup.toDto
import com.alvin.neuromind.data.backup.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class TaskRepository(
    private val database: NeuromindDatabase,
    private val taskDao: TaskDao,
    private val timetableDao: TimetableDao,
    private val feedbackLogDao: FeedbackLogDao,
    private val focusSessionDao: FocusSessionDao
) {
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    suspend fun getTaskById(id: Int): Task? = taskDao.getTaskById(id)
    suspend fun insertTask(task: Task) = taskDao.insertTask(task)
    suspend fun updateTask(task: Task) = taskDao.updateTask(task)
    suspend fun deleteTask(task: Task) = taskDao.deleteTaskAndSubTasks(task)

    val allTimetableEntries: Flow<List<TimetableEntry>> = timetableDao.getAllEntries()
    suspend fun insertTimetableEntry(entry: TimetableEntry) = timetableDao.insertEntry(entry)
    suspend fun updateTimetableEntry(entry: TimetableEntry) = timetableDao.updateEntry(entry)
    suspend fun deleteTimetableEntry(entry: TimetableEntry) = timetableDao.deleteEntry(entry)

    val allFeedbackLogs: Flow<List<FeedbackLog>> = feedbackLogDao.getAllLogs()
    suspend fun insertFeedbackLog(log: FeedbackLog) = feedbackLogDao.insertFeedbackLog(log)
    suspend fun deleteAllFeedbackLogs() = feedbackLogDao.deleteAllLogs()

    val allFocusSessions: Flow<List<FocusSession>> = focusSessionDao.getAllSessions()
    suspend fun insertFocusSession(session: FocusSession) = focusSessionDao.insertSession(session)
    suspend fun deleteAllFocusSessions() = focusSessionDao.deleteAllSessions()

    suspend fun exportAll(): BackupData = BackupData(
        exportedAt = System.currentTimeMillis(),
        tasks = allTasks.first(),
        timetable = allTimetableEntries.first().map { it.toDto() },
        feedback = allFeedbackLogs.first(),
        focusSessions = allFocusSessions.first()
    )

    /** Replaces all local data with [data]. Original IDs are preserved, so cross-references (e.g. Task.prerequisiteTaskId) survive intact. */
    suspend fun importAll(data: BackupData) {
        database.withTransaction {
            taskDao.deleteAllTasks()
            timetableDao.deleteAllEntries()
            feedbackLogDao.deleteAllLogs()
            focusSessionDao.deleteAllSessions()

            data.tasks.forEach { taskDao.insertTask(it) }
            data.timetable.forEach { timetableDao.insertEntry(it.toEntity()) }
            data.feedback.forEach { feedbackLogDao.insertFeedbackLog(it) }
            data.focusSessions.forEach { focusSessionDao.insertSession(it) }
        }
    }
}