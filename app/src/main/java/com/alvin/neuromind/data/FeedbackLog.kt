package com.alvin.neuromind.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class Mood(val score: Int) {
    STRESSED(1),
    TIRED(2),
    NEUTRAL(3),
    GOOD(4),
    GREAT(5)
}

@Entity(tableName = "feedback_logs")
data class FeedbackLog(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val date: Long = System.currentTimeMillis(),
    val mood: Mood,
    val energyLevel: Int,
    val tasksCompleted: Int,
    val comment: String?
)