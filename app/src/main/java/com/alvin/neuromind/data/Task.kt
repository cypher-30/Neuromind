package com.alvin.neuromind.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Priority { HIGH, MEDIUM, LOW }
enum class Difficulty { HARD, MEDIUM, EASY }

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0, // FIXED: Must be Int, defaulting to 0 for Auto-Increment
    val title: String,
    val description: String?,
    val dueDate: Long?,
    val priority: Priority,
    val difficulty: Difficulty,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val durationMinutes: Int = 60
) {
    // Helper to check if overdue
    val isOverdue: Boolean
        get() = !isCompleted && dueDate != null && dueDate < System.currentTimeMillis()
}