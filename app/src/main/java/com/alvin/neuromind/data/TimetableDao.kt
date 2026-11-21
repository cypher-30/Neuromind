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

    /** Sets (or clears, with null) the last day on every weekly class at once. */
    @Query("UPDATE timetable_entries SET lastDate = :lastDate WHERE isRecurring = 1")
    suspend fun setClassesLastDate(lastDate: java.time.LocalDate?)

    @Query("SELECT * FROM timetable_entries WHERE id = :id")
    suspend fun getEntryById(id: Int): TimetableEntry?

    /** Live view of one class/event (null once it's deleted), for the details screen. */
    @Query("SELECT * FROM timetable_entries WHERE id = :id")
    fun observeEntry(id: Int): Flow<TimetableEntry?>

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