package com.alvin.neuromind.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedbackLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedbackLog(log: FeedbackLog) // Updated to match Repository

    @Query("SELECT * FROM feedback_logs ORDER BY date DESC")
    fun getAllLogs(): Flow<List<FeedbackLog>>
}