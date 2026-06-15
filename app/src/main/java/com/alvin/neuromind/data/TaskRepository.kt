package com.alvin.neuromind.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(
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
}