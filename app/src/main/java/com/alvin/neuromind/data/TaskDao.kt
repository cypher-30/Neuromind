package com.alvin.neuromind.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Int): Task?

    @Query(
        """
        SELECT * FROM tasks
        WHERE isCompleted = 0
          AND (
            priority = 'HIGH'
            OR (dueDate IS NOT NULL AND dueDate < :nowMillis)
          )
        ORDER BY
          CASE WHEN dueDate IS NOT NULL AND dueDate < :nowMillis THEN 0 ELSE 1 END,
          CASE priority
            WHEN 'HIGH' THEN 0
            WHEN 'MEDIUM' THEN 1
            ELSE 2
          END,
          dueDate ASC,
          createdAt DESC
        LIMIT :limit
        """
    )
    suspend fun getWidgetTasks(nowMillis: Long, limit: Int = 3): List<Task>

    @Transaction
    suspend fun deleteTaskAndSubTasks(task: Task) = deleteTask(task)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()
}