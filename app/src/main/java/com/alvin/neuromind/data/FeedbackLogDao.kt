package com.alvin.neuromind.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedbackLogDao {

    /**
     * Inserts a new feedback log into the database. If a log with the same ID
     * already exists, it will be replaced.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: FeedbackLog)

    /**
     * Retrieves all feedback logs from the database, ordered by date descending.
     * @return A Flow that emits a new list of logs whenever the data changes.
     */
    @Query("SELECT * FROM feedback_logs ORDER BY date DESC")
    fun getAllLogs(): Flow<List<FeedbackLog>>

}
