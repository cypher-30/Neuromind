package com.alvin.neuromind.ui.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.alvin.neuromind.data.DraftKeys
import com.alvin.neuromind.data.DraftSession
import com.alvin.neuromind.data.TaskRepository
import com.alvin.neuromind.data.TimetableDraft
import com.alvin.neuromind.data.TimetableEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** One agenda heading and its entries. [date] is null for the "Later"/"Past" groups. */
data class AgendaSection(
    val title: String,
    val date: LocalDate?,
    val isToday: Boolean,
    val entries: List<TimetableEntry>,
    /** Show each entry's own date (Later/Past groups). */
    val showDates: Boolean = false
)

data class TimetableUiState(
    val entries: List<TimetableEntry> = emptyList()
)

/** Weekly-class editor (the dialog). One-off dated items use the Event editor instead. */
data class TimetableEditorState(
    val entryId: Int = 0,
    val title: String = "",
    val dayOfWeek: DayOfWeek = LocalDate.now().dayOfWeek,
    val startTime: LocalTime = LocalTime.of(9, 0),
    val endTime: LocalTime = LocalTime.of(10, 0),
    val venue: String = "",
    val details: String = "",
    val restoredDraft: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val error: String? = null,
    val isReady: Boolean = false
)

class TimetableViewModel(private val repository: TaskRepository) : ViewModel() {

    val uiState: StateFlow<TimetableUiState> = repository.allTimetableEntries
        .map { TimetableUiState(entries = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = TimetableUiState()
        )

    private val _editor = MutableStateFlow<TimetableEditorState?>(null)
    /** Non-null while the weekly-class dialog is open; lives in the ViewModel so rotation keeps it. */
    val editor: StateFlow<TimetableEditorState?> = _editor.asStateFlow()
    private var editorSession: DraftSession<TimetableDraft>? = null
    private var editorBaseline = TimetableDraft()
    private var editingOriginal: TimetableEntry? = null

    fun openNewEntry() = openEditor(null)

    fun openEditEntry(entry: TimetableEntry) = openEditor(entry)

    private fun openEditor(entry: TimetableEntry?) {
        val session = DraftSession(repository, DraftKeys.of(DraftKeys.TIMETABLE, entry?.id), TimetableDraft.serializer())
        editorSession = session
        editingOriginal = entry
        editorBaseline = entry?.toDraft() ?: TimetableEditorState().toDraft()
        _editor.value = TimetableEditorState(entryId = entry?.id ?: 0).applyDraft(editorBaseline)
        viewModelScope.launch {
            val draft = session.load()?.takeIf { it != editorBaseline }
            if (editorSession !== session) return@launch
            _editor.update { state ->
                state?.applyDraft(draft ?: editorBaseline)?.copy(
                    isReady = true,
                    restoredDraft = draft != null,
                    hasUnsavedChanges = draft != null
                )
            }
        }
    }

    fun editEntry(transform: (TimetableEditorState) -> TimetableEditorState) {
        val current = _editor.value ?: return
        val next = transform(current).copy(error = null)
        val changed = next.toDraft() != editorBaseline
        _editor.value = next.copy(hasUnsavedChanges = changed)
        if (next.isReady) editorSession?.update(next.toDraft().takeIf { changed })
    }

    /** Closes the dialog but keeps any draft for next time. */
    fun closeEditor() {
        editorSession = null
        _editor.value = null
    }

    fun discardEditorDraft() {
        editorSession?.discard()
        _editor.update { it?.applyDraft(editorBaseline)?.copy(restoredDraft = false, hasUnsavedChanges = false, error = null) }
    }

    fun dismissRestoredBanner() = _editor.update { it?.copy(restoredDraft = false) }

    fun saveEditor() {
        val state = _editor.value ?: return
        if (!state.isReady) return
        val error = when {
            state.title.isBlank() -> "Add a title."
            !state.endTime.isAfter(state.startTime) -> "End time must be after the start time."
            else -> null
        }
        if (error != null) {
            _editor.value = state.copy(error = error)
            return
        }
        val session = editorSession
        val base = editingOriginal
        val entry = (base ?: TimetableEntry(title = "", dayOfWeek = state.dayOfWeek, startTime = state.startTime, endTime = state.endTime))
            .copy(
                title = state.title.trim(),
                dayOfWeek = state.dayOfWeek,
                startTime = state.startTime,
                endTime = state.endTime,
                venue = state.venue.takeIf { it.isNotBlank() },
                details = state.details.takeIf { it.isNotBlank() },
                isRecurring = true,
                date = null,
                isAllDay = false
            )
        closeEditor()
        viewModelScope.launch {
            if (entry.id == 0) repository.insertTimetableEntry(entry) else repository.updateTimetableEntry(entry)
            session?.close()
        }
    }

    fun deleteEntry(entry: TimetableEntry) {
        if (_editor.value?.entryId == entry.id && entry.id != 0) closeEditor()
        viewModelScope.launch { repository.deleteTimetableEntry(entry) }
    }

    companion object {
        /** Today plus the next six days (by occurrence), then later dated events, then past ones. */
        fun buildSections(entries: List<TimetableEntry>, today: LocalDate = LocalDate.now()): List<AgendaSection> {
            val order = compareBy<TimetableEntry>({ !it.isAllDay }, { it.startTime })
            val sections = mutableListOf<AgendaSection>()
            for (offset in 0..6) {
                val date = today.plusDays(offset.toLong())
                val onDay = entries.filter { it.occursOn(date) }.sortedWith(order)
                if (onDay.isNotEmpty()) {
                    val title = when (offset) {
                        0 -> "Today"
                        1 -> "Tomorrow"
                        else -> date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault())
                    }
                    sections += AgendaSection(title, date, offset == 0, onDay)
                }
            }
            val weekEnd = today.plusDays(6)
            val later = entries
                .filter { !it.isRecurring && it.date != null && it.date.isAfter(weekEnd) }
                .sortedWith(compareBy<TimetableEntry> { it.date }.then(order))
            if (later.isNotEmpty()) sections += AgendaSection("Later", null, false, later, showDates = true)
            val past = entries
                .filter { !it.isRecurring && (it.date == null || it.date.isBefore(today)) }
                .sortedWith(compareByDescending<TimetableEntry> { it.date }.then(order))
            if (past.isNotEmpty()) sections += AgendaSection("Past events", null, false, past, showDates = true)
            return sections
        }
    }
}

private fun TimetableEntry.toDraft() = TimetableDraft(
    title = title,
    dayOfWeek = dayOfWeek.name,
    startTime = startTime.toString(),
    endTime = endTime.toString(),
    venue = venue ?: "",
    details = details ?: ""
)

private fun TimetableEditorState.toDraft() = TimetableDraft(
    title = title,
    dayOfWeek = dayOfWeek.name,
    startTime = startTime.toString(),
    endTime = endTime.toString(),
    venue = venue,
    details = details
)

private fun TimetableEditorState.applyDraft(d: TimetableDraft) = copy(
    title = d.title,
    dayOfWeek = runCatching { DayOfWeek.valueOf(d.dayOfWeek) }.getOrDefault(dayOfWeek),
    startTime = runCatching { LocalTime.parse(d.startTime) }.getOrDefault(startTime),
    endTime = runCatching { LocalTime.parse(d.endTime) }.getOrDefault(endTime),
    venue = d.venue,
    details = d.details
)

class TimetableViewModelFactory(private val repository: TaskRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(TimetableViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TimetableViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
