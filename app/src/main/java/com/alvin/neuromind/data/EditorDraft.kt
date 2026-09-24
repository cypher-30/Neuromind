package com.alvin.neuromind.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * An unfinished editor form (task, event, or weekly timetable entry) kept so an
 * accidental exit doesn't lose typing. Local recovery data only — never exported
 * in backups. [key] is "<kind>:new" or "<kind>:<id>" (see [DraftKeys]).
 */
@Entity(tableName = "editor_drafts")
data class EditorDraft(
    @PrimaryKey val key: String,
    val payload: String,
    val updatedAt: Long
)

@Dao
interface EditorDraftDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(draft: EditorDraft)

    @Query("SELECT * FROM editor_drafts WHERE `key` = :key")
    suspend fun get(key: String): EditorDraft?

    @Query("DELETE FROM editor_drafts WHERE `key` = :key")
    suspend fun delete(key: String)

    @Query("DELETE FROM editor_drafts")
    suspend fun deleteAll()
}

object DraftKeys {
    const val TASK = "task"
    const val EVENT = "event"
    const val TIMETABLE = "timetable"

    fun of(kind: String, id: Int?): String = if (id == null || id <= 0) "$kind:new" else "$kind:$id"
}
