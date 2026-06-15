package com.alvin.neuromind.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val taskId: Int? = null,
    val taskTitle: String,
    val durationMinutes: Int,
    val completedAt: Long = System.currentTimeMillis()
)
