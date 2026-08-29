package com.alvin.neuromind.data.backup

import com.alvin.neuromind.data.Difficulty
import com.alvin.neuromind.data.FeedbackLog
import com.alvin.neuromind.data.FocusSession
import com.alvin.neuromind.data.Mood
import com.alvin.neuromind.data.Priority
import com.alvin.neuromind.data.Task
import com.alvin.neuromind.data.TimetableEntry
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class BackupDataTest {

    private fun sampleData() = BackupData(
        exportedAt = 1_700_000_000_000L,
        tasks = listOf(
            Task(
                id = 1, title = "Essay", description = "Draft chapter 2", dueDate = 1_700_100_000_000L,
                priority = Priority.HIGH, difficulty = Difficulty.HARD, isCompleted = false,
                prerequisiteTaskId = null, createdAt = 1_699_000_000_000L, durationMinutes = 90
            ),
            Task(id = 2, title = "Follow-up", isCompleted = true, prerequisiteTaskId = 1)
        ),
        timetable = listOf(
            TimetableEntry(
                id = 1, title = "Lecture", dayOfWeek = DayOfWeek.MONDAY,
                startTime = LocalTime.of(9, 0), endTime = LocalTime.of(11, 0),
                isRecurring = true, date = null, venue = "L01", details = "Bring laptop"
            ).toDto(),
            TimetableEntry(
                id = 2, title = "One-off event", dayOfWeek = DayOfWeek.SATURDAY,
                startTime = LocalTime.of(14, 30), endTime = LocalTime.of(16, 0),
                isRecurring = false, date = LocalDate.of(2026, 8, 1), venue = null, details = null
            ).toDto()
        ),
        feedback = listOf(
            FeedbackLog(id = 1, date = 1_699_900_000_000L, mood = Mood.GOOD, energyLevel = 4, tasksCompleted = 3, comment = "Solid day")
        ),
        focusSessions = listOf(
            FocusSession(id = 1, taskId = 1, taskTitle = "Essay", durationMinutes = 25, completedAt = 1_699_950_000_000L)
        )
    )

    @Test
    fun `round trips through JSON without losing data`() {
        val original = sampleData()

        val json = Json.encodeToString(original)
        val restored = Json.decodeFromString<BackupData>(json)

        assertEquals(original, restored)
    }

    @Test
    fun `round trips timetable entries through the DTO's own conversion`() {
        val entry = TimetableEntry(
            id = 5, title = "Recurring class", dayOfWeek = DayOfWeek.WEDNESDAY,
            startTime = LocalTime.of(8, 15), endTime = LocalTime.of(9, 45),
            isRecurring = true, date = null, venue = "Lab 2", details = null
        )

        val restored = entry.toDto().toEntity()

        assertEquals(entry, restored)
    }

    @Test
    fun `empty backup round trips cleanly`() {
        val empty = BackupData(exportedAt = 0L, tasks = emptyList(), timetable = emptyList(), feedback = emptyList(), focusSessions = emptyList())

        val restored = Json.decodeFromString<BackupData>(Json.encodeToString(empty))

        assertEquals(empty, restored)
    }
}
