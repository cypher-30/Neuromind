package com.alvin.neuromind.domain

import com.alvin.neuromind.data.FeedbackLog
import com.alvin.neuromind.data.Mood
import java.time.Instant
import java.time.ZoneId

sealed class BurnoutState {
    data class LowEnergy(val consecutiveDays: Int) : BurnoutState()
    data class WeekdayStress(val weekday: String) : BurnoutState()
}

object BurnoutAnalyzer {

    private val stressedMoods = setOf(Mood.STRESSED, Mood.TIRED)

    /**
     * Analyses the last 7 days of feedback logs for early burnout signals.
     * Returns null when no warning is warranted.
     *
     * Trigger A: 3+ consecutive days where average energy is <= 2 (on the 1–5 scale).
     * Trigger B: A specific weekday has 2+ STRESSED or TIRED mood entries.
     */
    fun analyze(logs: List<FeedbackLog>): BurnoutState? {
        val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
        val recentLogs = logs.filter { it.date >= sevenDaysAgo }

        if (recentLogs.isEmpty()) return null

        val zone = ZoneId.systemDefault()

        // Group by calendar date
        val logsByDay = recentLogs.groupBy { log ->
            Instant.ofEpochMilli(log.date).atZone(zone).toLocalDate()
        }

        // Trigger A: find the longest run of consecutive low-energy days
        val sortedDays = logsByDay.keys.sorted()
        var streak = 0
        var maxStreak = 0
        for (day in sortedDays) {
            val avgEnergy = logsByDay[day]!!.map { it.energyLevel }.average()
            if (avgEnergy <= 2.0) {
                streak++
                if (streak > maxStreak) maxStreak = streak
            } else {
                streak = 0
            }
        }
        if (maxStreak >= 3) return BurnoutState.LowEnergy(maxStreak)

        // Trigger B: weekday with 2+ stressed/tired entries
        val stressByWeekday = recentLogs
            .filter { it.mood in stressedMoods }
            .groupBy { log ->
                Instant.ofEpochMilli(log.date).atZone(zone).dayOfWeek
            }

        val worst = stressByWeekday.entries.maxByOrNull { it.value.size }
        if (worst != null && worst.value.size >= 2) {
            val dayName = worst.key.name.lowercase().replaceFirstChar { it.uppercase() }
            return BurnoutState.WeekdayStress(dayName)
        }

        return null
    }
}
