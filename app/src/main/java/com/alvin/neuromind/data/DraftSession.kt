package com.alvin.neuromind.data

import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Auto-saves one editor's in-progress input under [key]. Writes run on the
 * repository's application scope so they still land when the screen is
 * closed mid-edit (Back, dismiss, process death after the write).
 */
class DraftSession<T>(
    private val repository: TaskRepository,
    val key: String,
    private val serializer: KSerializer<T>
) {
    private val mutex = Mutex()
    @Volatile private var closed = false
    @Volatile private var latest: T? = null

    suspend fun load(): T? {
        val stored = repository.getDraft(key) ?: return null
        return runCatching { DraftJson.decodeFromString(serializer, stored.payload) }.getOrNull()
    }

    /** Saves [payload]; null means the form matches its baseline, so the draft is removed. */
    fun update(payload: T?) {
        if (closed) return
        latest = payload
        repository.externalScope.launch {
            mutex.withLock {
                if (closed) return@withLock
                val current = latest
                if (current == null) {
                    repository.deleteDraft(key)
                } else {
                    repository.saveDraft(key, DraftJson.encodeToString(serializer, current))
                }
            }
        }
    }

    /** User explicitly threw the draft away; the editor stays open. */
    fun discard() {
        latest = null
        repository.externalScope.launch { mutex.withLock { repository.deleteDraft(key) } }
    }

    /** The item was saved or deleted — stop writing and remove the draft. */
    fun close() {
        closed = true
        latest = null
        repository.externalScope.launch { mutex.withLock { repository.deleteDraft(key) } }
    }
}

internal val DraftJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

@Serializable
data class TaskDraft(
    val title: String = "",
    val description: String = "",
    val dueDate: Long? = null,
    val priority: String = Priority.MEDIUM.name,
    val difficulty: String = Difficulty.MEDIUM.name,
    val durationMinutes: Int = 60,
    val prerequisiteTaskId: Int? = null,
    val subject: String = "",
    val category: String = TaskCategory.ACADEMIC.name
)

@Serializable
data class EventDraft(
    val title: String = "",
    val date: String? = null,
    val isAllDay: Boolean = false,
    val startTime: String = "09:00",
    val endTime: String = "10:00",
    val venue: String = "",
    val notes: String = "",
    val reminder: String = EventReminder.AUTO.name
)

@Serializable
data class TimetableDraft(
    val title: String = "",
    val dayOfWeek: String = "MONDAY",
    val startTime: String = "09:00",
    val endTime: String = "10:00",
    val venue: String = "",
    val details: String = ""
)
