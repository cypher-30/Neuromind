package com.alvin.neuromind.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

enum class Mood(val score: Int) {
    STRESSED(1),
    TIRED(2),
    NEUTRAL(3),
    GOOD(4),
    GREAT(5)
}

@Serializable
@Entity(tableName = "feedback_logs")
data class FeedbackLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: Long = System.currentTimeMillis(),
    val mood: Mood,
    val energyLevel: Int,
    val tasksCompleted: Int,
    val comment: String?
)