package com.alvin.neuromind.domain

import com.alvin.neuromind.data.Difficulty
import com.alvin.neuromind.data.Priority
import com.alvin.neuromind.data.Task
import com.alvin.neuromind.data.TimetableEntry
import com.alvin.neuromind.data.preferences.CognitiveProfile
import java.time.LocalDate
import java.time.LocalTime

data class RebalanceProposal(
    val task: Task,
    val suggestedDate: LocalDate,
    val suggestedTime: LocalTime
)

object TaskRebalancer {

    /**
     * For each overdue task (sorted highest-priority / hardest first), finds the next available
     * time slot and proposes a new due date. HIGH-priority or HARD tasks are given preference for
     * slots that fall within the user's peak-focus window when one is available.
     *
     * No two tasks are assigned the same slot. Tasks with no slot within 7 days are excluded.
     * Existing tests pass `profile = CognitiveProfile()` (the default) implicitly.
     */
    fun rebalance(
        overdueTasks: List<Task>,
        timetable: List<TimetableEntry>,
        today: LocalDate = LocalDate.now(),
        profile: CognitiveProfile = CognitiveProfile()
    ): List<RebalanceProposal> {
        val sorted = overdueTasks.sortedWith(
            compareByDescending<Task> { it.priority.ordinal }
                .thenByDescending { it.difficulty.ordinal }
        )

        val proposals = mutableListOf<RebalanceProposal>()
        val usedSlots = mutableSetOf<Pair<LocalDate, LocalTime>>()

        val peakStart = LocalTime.of(profile.peakStart, 0)
        val peakEnd   = LocalTime.of(profile.peakEnd, 0)

        for (task in sorted) {
            val slots = TimeFinder.findSlots(
                timetable = timetable,
                durationMinutes = task.durationMinutes,
                lookaheadDays = 7,
                maxResults = 10
            ).filter { s -> Pair(s.date, s.startTime) !in usedSlots }

            val isPeakPreferred = task.priority == Priority.HIGH || task.difficulty == Difficulty.HARD
            val slot = if (isPeakPreferred) {
                slots.firstOrNull { s ->
                    !s.startTime.isBefore(peakStart) && s.startTime.isBefore(peakEnd)
                } ?: slots.firstOrNull()
            } else {
                slots.firstOrNull()
            } ?: continue

            usedSlots.add(Pair(slot.date, slot.startTime))
            proposals.add(
                RebalanceProposal(
                    task = task,
                    suggestedDate = slot.date,
                    suggestedTime = slot.startTime
                )
            )
        }

        return proposals
    }
}
