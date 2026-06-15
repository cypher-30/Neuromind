package com.alvin.neuromind.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedbackLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedbackLog(log: FeedbackLog)

    @Query("SELECT * FROM feedback_logs ORDER BY date DESC")
    fun getAllLogs(): Flow<List<FeedbackLog>>

    @Query("DELETE FROM feedback_logs")
    suspend fun deleteAllLogs()
}