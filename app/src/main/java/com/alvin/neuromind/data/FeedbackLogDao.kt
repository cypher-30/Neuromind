package com.alvin.neuromind.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedbackLogDao {
    @Query("SELECT * FROM feedback_logs ORDER BY date DESC")
    fun getAllLogs(): Flow<List<FeedbackLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: FeedbackLog) // This was missing
}