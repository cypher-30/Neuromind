package com.alvin.neuromind.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

enum class Priority { LOW, MEDIUM, HIGH }
enum class Difficulty { EASY, MEDIUM, HARD }
enum class TaskCategory { ACADEMIC, FITNESS, SOCIAL, PERSONAL }

@Serializable
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
    val durationMinutes: Int = 60,
    val subject: String? = null, // added in DB v10 — free-text, drives Insights' by-subject breakdown
    val category: TaskCategory = TaskCategory.ACADEMIC // added in DB v10
) {
    val isOverdue: Boolean
        get() = !isCompleted && dueDate != null && dueDate < System.currentTimeMillis()
}