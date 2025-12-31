package com.alvin.neuromind.data

import kotlinx.coroutines.flow.Flow
import java.util.UUID

class TaskRepository(
    private val taskDao: TaskDao,
    private val timetableDao: TimetableDao,
    private val feedbackLogDao: FeedbackLogDao
) {
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    suspend fun insert(task: Task) = taskDao.insertTask(task)
    suspend fun update(task: Task) = taskDao.updateTask(task)
    suspend fun delete(task: Task) = taskDao.deleteTaskAndSubTasks(task)

    val allTimetableEntries: Flow<List<TimetableEntry>> = timetableDao.getAllEntries()
    suspend fun insert(entry: TimetableEntry) = timetableDao.insertEntry(entry)
    suspend fun delete(entry: TimetableEntry) = timetableDao.deleteEntry(entry)

    val allFeedbackLogs: Flow<List<FeedbackLog>> = feedbackLogDao.getAllLogs()
    suspend fun insert(log: FeedbackLog) = feedbackLogDao.insertLog(log)
}