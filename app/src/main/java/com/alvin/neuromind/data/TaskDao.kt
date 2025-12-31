package com.alvin.neuromind.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

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
    suspend fun getTaskById(id: UUID): Task?

    // --- NEW FUNCTION ---
    // This function will find all sub-tasks that belong to a specific parent.
    @Query("SELECT * FROM tasks WHERE parentId = :parentId")
    suspend fun getSubTasksForParent(parentId: UUID): List<Task>

    // --- NEW TRANSACTION ---
    // A transaction ensures that all database operations within it either
    // all succeed, or all fail together. This prevents orphaned sub-tasks.
    @Transaction
    suspend fun deleteTaskAndSubTasks(task: Task) {
        // First, find all children of the task to be deleted.
        val subTasks = getSubTasksForParent(task.id)
        // Delete all the children.
        subTasks.forEach {
            deleteTask(it)
        }
        // Finally, delete the parent task itself.
        deleteTask(task)
    }
}
