package com.alvin.neuromind.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao,
    private val timetableDao: TimetableDao,
    private val feedbackLogDao: FeedbackLogDao
) {
    // --- TASKS ---
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()

    suspend fun getTaskById(id: Int): Task? {
        return taskDao.getTaskById(id)
    }

    suspend fun insert(task: Task) {
        taskDao.insert(task)
    }

    suspend fun update(task: Task) {
        taskDao.update(task)
    }

    suspend fun delete(task: Task) {
        taskDao.delete(task)
    }

    // --- TIMETABLE ---
    val allTimetableEntries: Flow<List<TimetableEntry>> = timetableDao.getAllEntries()

    suspend fun insert(entry: TimetableEntry) {
        timetableDao.insert(entry)
    }

    suspend fun delete(entry: TimetableEntry) {
        timetableDao.delete(entry)
    }

    // --- FEEDBACK ---
    val allFeedbackLogs: Flow<List<FeedbackLog>> = feedbackLogDao.getAllLogs()

    suspend fun insert(log: FeedbackLog) {
        feedbackLogDao.insert(log)
    }
}