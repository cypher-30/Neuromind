package com.alvin.neuromind.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableDao {
    @Query("SELECT * FROM timetable_entries")
    fun getAllEntries(): Flow<List<TimetableEntry>>

    // FIXED: Named 'insert', not 'insertEntry'
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: TimetableEntry)

    // FIXED: Named 'delete'
    @Delete
    suspend fun delete(entry: TimetableEntry)

    @Query("DELETE FROM timetable_entries")
    suspend fun deleteAllEntries()
}