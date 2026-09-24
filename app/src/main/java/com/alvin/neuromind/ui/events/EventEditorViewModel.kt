package com.alvin.neuromind.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alvin.neuromind.data.DraftKeys
import com.alvin.neuromind.data.DraftSession
import com.alvin.neuromind.data.EventDraft
import com.alvin.neuromind.data.EventReminder
import com.alvin.neuromind.data.TaskRepository
import com.alvin.neuromind.data.TimetableEntry
import com.alvin.neuromind.domain.EventReminderPolicy
import com.alvin.neuromind.domain.EventReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class EventEditorUiState(
    val title: String = "",
    val date: LocalDate? = null,
    val isAllDay: Boolean = false,
    val startTime: LocalTime = LocalTime.of(9, 0),
    val endTime: LocalTime = LocalTime.of(10, 0),
    val venue: String = "",
    val notes: String = "",
    val reminder: EventReminder = EventReminder.AUTO,
    val isEditing: Boolean = false,
    val isReady: Boolean = false,
    val restoredDraft: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val error: String? = null,
    /** Set once saved or deleted; the screen then shows [resultMessage] and closes. */
    val isFinished: Boolean = false,
    val resultMessage: String? = null
) {
    /** When the reminder would fire with the current input, or null if none. */
    val reminderTime: LocalDateTime?
        get() = date?.let { EventReminderPolicy.triggerAt(it, startTime, isAllDay, reminder) }
}

class EventEditorViewModel(private val repository: TaskRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(EventEditorUiState())
    val uiState = _uiState.asStateFlow()

    private var original: TimetableEntry? = null
    private var baseline = EventDraft()
    private var draftSession: DraftSession<EventDraft>? = null

    /** Call once with the entry id to edit, or -1 for a new event. Restores any unsaved draft. */
    fun start(eventId: Int) {
        if (draftSession != null) return
        val id = eventId.takeIf { it > 0 }
        val session = DraftSession(repository, DraftKeys.of(DraftKeys.EVENT, id), EventDraft.serializer())
        draftSession = session
        viewModelScope.launch {
            val entry = id?.let { repository.getTimetableEntryById(it) }
            original = entry
            if (entry != null) baseline = entry.toDraft()
            val draft = session.load()?.takeIf { it != baseline }
            _uiState.update {
                it.applyDraft(draft ?: baseline).copy(
                    isEditing = entry != null,
                    isReady = true,
                    restoredDraft = draft != null,
                    hasUnsavedChanges = draft != null
                )
            }
        }
    }

    private fun edit(transform: (EventEditorUiState) -> EventEditorUiState) {
        _uiState.update { transform(it).copy(error = null) }
        val state = _uiState.value
        if (!state.isReady || state.isFinished) return
        val draft = state.toDraft()
        val changed = draft != baseline
        _uiState.update { it.copy(hasUnsavedChanges = changed) }
        draftSession?.update(draft.takeIf { changed })
    }

    fun onTitleChange(value: String) = edit { it.copy(title = value) }
    fun onDateChange(value: LocalDate) = edit { it.copy(date = value) }
    fun onAllDayChange(value: Boolean) = edit { it.copy(isAllDay = value) }
    fun onStartTimeChange(value: LocalTime) = edit { state ->
        // Keep the event's length when the start moves, like calendar apps do.
        val length = Duration.between(state.startTime, state.endTime)
        val newEnd = if (length.isNegative || length.isZero) value.plusHours(1) else value.plus(length)
        state.copy(startTime = value, endTime = if (newEnd.isAfter(value)) newEnd else LocalTime.of(23, 59))
    }
    fun onEndTimeChange(value: LocalTime) = edit { it.copy(endTime = value) }
    fun onVenueChange(value: String) = edit { it.copy(venue = value) }
    fun onNotesChange(value: String) = edit { it.copy(notes = value) }
    fun onReminderChange(value: EventReminder) = edit { it.copy(reminder = value) }

    fun dismissRestoredBanner() = _uiState.update { it.copy(restoredDraft = false) }

    fun discardDraft() {
        draftSession?.discard()
        _uiState.update { it.applyDraft(baseline).copy(restoredDraft = false, hasUnsavedChanges = false, error = null) }
    }

    fun save() {
        val state = _uiState.value
        if (!state.isReady || state.isFinished) return
        val date = state.date
        val error = when {
            state.title.isBlank() -> "Give the event a title."
            date == null -> "Pick the date of the event."
            !state.isAllDay && !state.endTime.isAfter(state.startTime) -> "End time must be after the start time."
            else -> null
        }
        if (error != null || date == null) {
            _uiState.update { it.copy(error = error) }
            return
        }
        val start = if (state.isAllDay) LocalTime.MIDNIGHT else state.startTime
        val end = if (state.isAllDay) LocalTime.of(23, 59) else state.endTime
        val base = original ?: TimetableEntry(title = "", dayOfWeek = date.dayOfWeek, startTime = start, endTime = end)
        val entry = base.copy(
            title = state.title.trim(),
            dayOfWeek = date.dayOfWeek,
            startTime = start,
            endTime = end,
            isRecurring = false,
            date = date,
            venue = state.venue.trim().takeIf { it.isNotBlank() },
            details = state.notes.trim().takeIf { it.isNotBlank() },
            isAllDay = state.isAllDay,
            reminderMode = state.reminder
        )
        _uiState.update { it.copy(isFinished = true) }
        viewModelScope.launch {
            val status = if (entry.id == 0) repository.insertTimetableEntry(entry).second
            else repository.updateTimetableEntry(entry)
            draftSession?.close()
            _uiState.update { it.copy(resultMessage = savedMessage(status)) }
        }
    }

    fun delete() {
        val entry = original ?: return
        _uiState.update { it.copy(isFinished = true) }
        viewModelScope.launch {
            repository.deleteTimetableEntry(entry)
            draftSession?.close()
            _uiState.update { it.copy(resultMessage = "Event deleted") }
        }
    }

    private fun savedMessage(status: EventReminderScheduler.Status): String = when (status) {
        EventReminderScheduler.Status.SCHEDULED_EXACT -> "Event saved — reminder set"
        EventReminderScheduler.Status.SCHEDULED_INEXACT ->
            "Event saved — the reminder may be a few minutes late until you allow Alarms & reminders"
        EventReminderScheduler.Status.IN_PAST -> "Event saved — its reminder time has already passed"
        EventReminderScheduler.Status.NONE -> "Event saved"
    }
}

private fun TimetableEntry.toDraft() = EventDraft(
    title = title,
    date = date?.toString(),
    isAllDay = isAllDay,
    startTime = startTime.toString(),
    endTime = endTime.toString(),
    venue = venue ?: "",
    notes = details ?: "",
    // Legacy one-time entries have no mode; editing one opts it into the default reminder.
    reminder = (reminderMode ?: EventReminder.AUTO).name
)

private fun EventEditorUiState.toDraft() = EventDraft(
    title = title,
    date = date?.toString(),
    isAllDay = isAllDay,
    startTime = startTime.toString(),
    endTime = endTime.toString(),
    venue = venue,
    notes = notes,
    reminder = reminder.name
)

private fun EventEditorUiState.applyDraft(d: EventDraft) = copy(
    title = d.title,
    date = d.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    isAllDay = d.isAllDay,
    startTime = runCatching { LocalTime.parse(d.startTime) }.getOrDefault(startTime),
    endTime = runCatching { LocalTime.parse(d.endTime) }.getOrDefault(endTime),
    venue = d.venue,
    notes = d.notes,
    reminder = runCatching { EventReminder.valueOf(d.reminder) }.getOrDefault(EventReminder.AUTO)
)

class EventEditorViewModelFactory(private val repository: TaskRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EventEditorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EventEditorViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
