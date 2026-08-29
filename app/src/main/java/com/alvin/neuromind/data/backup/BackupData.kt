package com.alvin.neuromind.data.backup

import com.alvin.neuromind.data.FeedbackLog
import com.alvin.neuromind.data.FocusSession
import com.alvin.neuromind.data.Task
import kotlinx.serialization.Serializable

/**
 * Full snapshot of user data for export/import. [schemaVersion] is independent of
 * the Room database version — it versions this JSON shape specifically, so a future
 * restore can detect and migrate older backup files.
 */
@Serializable
data class BackupData(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportedAt: Long,
    val tasks: List<Task>,
    val timetable: List<TimetableEntryDto>,
    val feedback: List<FeedbackLog>,
    val focusSessions: List<FocusSession>
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}
