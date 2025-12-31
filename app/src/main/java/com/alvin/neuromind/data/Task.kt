package com.alvin.neuromind.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class Difficulty { EASY, MEDIUM, HARD }
enum class Priority { LOW, MEDIUM, HIGH }

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val title: String,
    val description: String?,
    val dueDate: Long?,
    val isRecurring: Boolean = false,
    val recurrencePattern: String? = null,
    val durationMinutes: Int = 60,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val priority: Priority = Priority.LOW,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val parentId: UUID? = null
) {
    val isOverdue: Boolean
        get() = dueDate?.let { it < System.currentTimeMillis() } ?: false && !isCompleted
}