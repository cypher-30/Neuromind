package com.alvin.neuromind.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSession)

    @Query("SELECT * FROM focus_sessions ORDER BY completedAt DESC")
    fun getAllSessions(): Flow<List<FocusSession>>

    @Query("DELETE FROM focus_sessions")
    suspend fun deleteAllSessions()
}
