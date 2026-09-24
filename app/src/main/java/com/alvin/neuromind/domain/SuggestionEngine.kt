package com.alvin.neuromind.domain

import com.alvin.neuromind.data.Difficulty
import com.alvin.neuromind.data.FeedbackLog
import com.alvin.neuromind.data.Priority
import com.alvin.neuromind.data.Task
import com.alvin.neuromind.data.TimetableEntry
import com.alvin.neuromind.data.preferences.CognitiveProfile
import java.time.LocalDateTime

enum class SuggestionType { PEAK_HOUR_NUDGE, FREE_SLOT_NUDGE, ENERGY_MATCH, OVERDUE_ALERT }
enum class EnergyState { LOW, MEDIUM, HIGH }

data class Suggestion(
    val type: SuggestionType,
    val message: String,
    val taskId: Int? = null,
    val actionLabel: String? = null
)

object SuggestionEngine {

    /**
     * Returns a single, timely suggestion based on the current time, cognitive profile,
     * and task list. Returns null when nothing relevant applies.
     *
     * Priority order (first match wins):
     * 1. PEAK_HOUR_NUDGE — in peak window + incomplete HIGH/HARD task
     * 2. FREE_SLOT_NUDGE — free slot starting within 90 min matches an upcoming task's duration
     * 3. ENERGY_MATCH — outside peak hours + EASY incomplete task
     * 4. OVERDUE_ALERT — 1–2 overdue tasks (≥ 3 is the rebalancer's job)
     */
    fun suggest(
        tasks: List<Task>,
        timetable: List<TimetableEntry>,
        profile: CognitiveProfile,
        feedbackLogs: List<FeedbackLog> = emptyList(),
        now: LocalDateTime = LocalDateTime.now()
    ): Suggestion? {
        val incompleteTasks = tasks.filter { !it.isCompleted }
        if (incompleteTasks.isEmpty()) return null

        val hour = now.hour
        val inPeakHour = hour in profile.peakStart until profile.peakEnd

        // 1. PEAK_HOUR_NUDGE
        if (inPeakHour) {
            val hardTask = incompleteTasks.firstOrNull {
                it.priority == Priority.HIGH || it.difficulty == Difficulty.HARD
            }
            if (hardTask != null) {
                return Suggestion(
                    type = SuggestionType.PEAK_HOUR_NUDGE,
                    message = "It's your peak hour - tackle your toughest task: ${hardTask.title}",
                    taskId = hardTask.id,
                    actionLabel = "Focus now"
                )
            }
        }

        // 2. FREE_SLOT_NUDGE
        val nowDate = now.toLocalDate()
        val nowTime = now.toLocalTime()
        val ninetyMinLater = nowTime.plusMinutes(90)

        val slotTask = incompleteTasks.firstOrNull { task ->
            val slots = TimeFinder.findSlots(
                timetable, task.durationMinutes,
                lookaheadDays = 1, maxResults = 1,
                referenceDate = nowDate, referenceTime = nowTime
            )
            slots.isNotEmpty() && !slots[0].startTime.isAfter(ninetyMinLater)
        }
        if (slotTask != null) {
            val slot = TimeFinder.findSlots(
                timetable, slotTask.durationMinutes,
                lookaheadDays = 1, maxResults = 1,
                referenceDate = nowDate, referenceTime = nowTime
            )[0]
            val nextEvent = timetable
                .filter { it.occursOn(nowDate) && !it.isAllDay && it.startTime.isAfter(nowTime) }
                .minByOrNull { it.startTime }
            val mins = slotTask.durationMinutes
            val message = if (nextEvent != null)
                "You have $mins mins free before ${nextEvent.title} - ${slotTask.title} fits perfectly"
            else
                "You have a free $mins-min window - ${slotTask.title} fits perfectly"
            return Suggestion(
                type = SuggestionType.FREE_SLOT_NUDGE,
                message = message,
                taskId = slotTask.id,
                actionLabel = "Start task"
            )
        }

        // 3. ENERGY_MATCH (explicit LOW/MEDIUM/HIGH mapping from recent logs)
        val energyState = resolveEnergyState(feedbackLogs)
        val nonOverdueTasks = incompleteTasks.filterNot { it.isOverdue }
        val energyMatchedTask = pickTaskForEnergyState(nonOverdueTasks, energyState)
        if (energyMatchedTask != null) {
            val message = when (energyState) {
                EnergyState.LOW -> "Low energy right now - try this light task: ${energyMatchedTask.title}"
                EnergyState.MEDIUM -> "Steady energy window - this balanced task fits: ${energyMatchedTask.title}"
                EnergyState.HIGH -> "High energy detected - lean into this challenge: ${energyMatchedTask.title}"
            }
            return Suggestion(
                type = SuggestionType.ENERGY_MATCH,
                message = message,
                taskId = energyMatchedTask.id,
                actionLabel = "Start task"
            )
        }

        // 4. OVERDUE_ALERT (1–2 overdue; ≥ 3 is handled by the rebalance card)
        val overdueCount = incompleteTasks.count { it.isOverdue }
        if (overdueCount in 1..2) {
            val oldest = incompleteTasks.filter { it.isOverdue }.minByOrNull { it.dueDate ?: Long.MAX_VALUE }
            return Suggestion(
                type = SuggestionType.OVERDUE_ALERT,
                message = "You have $overdueCount overdue task${if (overdueCount > 1) "s" else ""} - tackle one now?",
                taskId = oldest?.id,
                actionLabel = if (oldest != null) "View task" else null
            )
        }

        return null
    }

    private fun resolveEnergyState(feedbackLogs: List<FeedbackLog>): EnergyState {
        val recent = feedbackLogs.take(3)
        if (recent.isEmpty()) return EnergyState.MEDIUM
        val avg = recent.map { it.energyLevel.coerceIn(1, 5) }.average()
        return when {
            avg < 2.5 -> EnergyState.LOW
            avg < 3.8 -> EnergyState.MEDIUM
            else -> EnergyState.HIGH
        }
    }

    private fun pickTaskForEnergyState(tasks: List<Task>, energyState: EnergyState): Task? {
        return when (energyState) {
            EnergyState.LOW -> tasks.firstOrNull {
                it.difficulty == Difficulty.EASY || it.priority == Priority.LOW || it.durationMinutes <= 30
            }
            EnergyState.MEDIUM -> tasks.firstOrNull {
                it.difficulty == Difficulty.MEDIUM || it.priority == Priority.MEDIUM
            } ?: tasks.firstOrNull { it.difficulty == Difficulty.EASY }
            EnergyState.HIGH -> tasks.firstOrNull {
                it.priority == Priority.HIGH || it.difficulty == Difficulty.HARD || it.durationMinutes >= 60
            }
        }
    }
}
