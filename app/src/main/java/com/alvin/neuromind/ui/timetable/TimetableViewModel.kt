package com.alvin.neuromind.ui.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.alvin.neuromind.data.TaskRepository
import com.alvin.neuromind.data.TimetableEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime

data class TimetableUiState(
    val entriesByDay: Map<DayOfWeek, List<TimetableEntry>> = emptyMap()
)

class TimetableViewModel(private val repository: TaskRepository) : ViewModel() {

    val uiState: StateFlow<TimetableUiState> = repository.allTimetableEntries
        .map { entries ->
            TimetableUiState(entriesByDay = entries.groupBy { it.dayOfWeek })
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = TimetableUiState()
        )

    fun addEntry(
        title: String,
        dayOfWeek: DayOfWeek,
        startTime: LocalTime,
        endTime: LocalTime,
        venue: String?,
        details: String?
    ) {
        if (title.isBlank() || endTime.isBefore(startTime)) {
            return
        }
        viewModelScope.launch {
            val newEntry = TimetableEntry(
                title = title,
                dayOfWeek = dayOfWeek,
                startTime = startTime,
                endTime = endTime,
                venue = venue?.takeIf { it.isNotBlank() },
                details = details?.takeIf { it.isNotBlank() }
            )
            repository.insert(newEntry)
        }
    }

    fun deleteEntry(entry: TimetableEntry) {
        viewModelScope.launch {
            repository.delete(entry)
        }
    }
}

class TimetableViewModelFactory(
    private val repository: TaskRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(TimetableViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TimetableViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}