package com.alvin.neuromind.data.backup

import com.alvin.neuromind.data.FeedbackLog
import com.alvin.neuromind.data.FocusSession
import com.alvin.neuromind.data.Task
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * Full snapshot of user data for export/import. [schemaVersion] is independent of
 * the Room database version — it versions this JSON shape specifically, so a future
 * restore can detect and migrate older backup files.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class BackupData(
    // Always written: Json skips default values, so without this the version
    // was missing from every export and the newer-version guard never fired.
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportedAt: Long,
    val tasks: List<Task>,
    val timetable: List<TimetableEntryDto>,
    val feedback: List<FeedbackLog>,
    val focusSessions: List<FocusSession>
) {
    companion object {
        // 2: timetable entries gained isAllDay + reminderMode (dated events).
        // 3: weekly classes gained lastDate (last day of classes).
        const val CURRENT_SCHEMA_VERSION = 3
    }
}
