package com.alvin.neuromind.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Priority { LOW, MEDIUM, HIGH }
enum class Difficulty { EASY, MEDIUM, HARD }

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String? = null,
    val dueDate: Long? = null,
    val priority: Priority = Priority.MEDIUM,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val isCompleted: Boolean = false,
    val prerequisiteTaskId: Int? = null, // Pillar 2: Dependencies
    val createdAt: Long = System.currentTimeMillis(),
    val durationMinutes: Int = 60
) {
    val isOverdue: Boolean
        get() = !isCompleted && dueDate != null && dueDate < System.currentTimeMillis()
}