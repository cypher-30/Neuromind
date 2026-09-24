package com.alvin.neuromind.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: TimetableEntry): Long

    @Update
    suspend fun updateEntry(entry: TimetableEntry)

    @Delete
    suspend fun deleteEntry(entry: TimetableEntry)

    @Query("SELECT * FROM timetable_entries WHERE id = :id")
    suspend fun getEntryById(id: Int): TimetableEntry?

    @Query("SELECT * FROM timetable_entries ORDER BY dayOfWeek, startTime")
    fun getAllEntries(): Flow<List<TimetableEntry>>

    @Query(
        """
        SELECT * FROM timetable_entries
        WHERE startTime > :currentTime
          AND isAllDay = 0
          AND (
              (isRecurring = 1 AND dayOfWeek = :todayDayOfWeek)
              OR (isRecurring = 0 AND date = :todayDate)
          )
        ORDER BY startTime ASC
        LIMIT 1
        """
    )
    suspend fun getNextEntryForWidget(
        todayDayOfWeek: String,
        todayDate: String,
        currentTime: String
    ): TimetableEntry?

    @Query("DELETE FROM timetable_entries")
    suspend fun deleteAllEntries()
}