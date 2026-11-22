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

    /** Live view of one task (null once it's deleted), for the details screen. */
    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeTask(id: Int): kotlinx.coroutines.flow.Flow<Task?>

    @Transaction
    suspend fun deleteTaskAndSubTasks(task: Task) = deleteTask(task)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()
}