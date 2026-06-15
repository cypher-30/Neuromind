package com.alvin.neuromind.domain

import com.alvin.neuromind.data.FeedbackLog
import com.alvin.neuromind.data.Mood
import com.alvin.neuromind.data.Task
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class WeekStat(val weekLabel: String, val completionPct: Float)
data class BestDayStat(val day: DayOfWeek, val completedCount: Int)
data class MoodStat(val mood: Mood, val avgTasksCompleted: Float)

data class RetroInsights(
    val weeklyTrend: List<WeekStat>,
    val bestDay: BestDayStat?,
    val moodProductivity: List<MoodStat>,
    val onTrackRate: Float
)

object RetroAnalyzer {

    fun analyze(
        tasks: List<Task>,
        logs: List<FeedbackLog>,
        today: LocalDate = LocalDate.now()
    ): RetroInsights? {
        val zone = ZoneId.systemDefault()

        val logDates = logs.map { Instant.ofEpochMilli(it.date).atZone(zone).toLocalDate() }.toSet()
        if (logDates.size < 7) return null

        // 4-week completion trend (chronological: oldest first)
        val weeklyTrend = (3 downTo 0).map { weeksAgo ->
            val weekEnd = today.minusWeeks(weeksAgo.toLong())
            val weekStart = weekEnd.minusDays(6)
            val weekTasks = tasks.filter { task ->
                val taskDate = Instant.ofEpochMilli(task.createdAt).atZone(zone).toLocalDate()
                taskDate in weekStart..weekEnd
            }
            val completed = weekTasks.count { it.isCompleted }
            val total = weekTasks.size
            val pct = if (total == 0) 0f else completed.toFloat() / total.toFloat()
            WeekStat(weekLabel = "W${4 - weeksAgo}", completionPct = pct)
        }

        // Best day of week over last 30 days — ties broken by earliest weekday
        val thirtyDaysAgo = today.minusDays(30)
        val recentCompletedTasks = tasks.filter { task ->
            task.isCompleted &&
            Instant.ofEpochMilli(task.createdAt).atZone(zone).toLocalDate() >= thirtyDaysAgo
        }
        val countByDay = recentCompletedTasks.groupBy { task ->
            Instant.ofEpochMilli(task.createdAt).atZone(zone).toLocalDate().dayOfWeek
        }.mapValues { it.value.size }

        val bestDay = countByDay.entries
            .maxWithOrNull(compareByDescending<Map.Entry<DayOfWeek, Int>> { it.value }
                .thenByDescending { it.key.value })
            ?.let { BestDayStat(it.key, it.value) }

        // Mood × productivity: average tasks completed per mood level
        val moodProductivity = Mood.entries.map { mood ->
            val moodLogs = logs.filter { it.mood == mood }
            val avg = if (moodLogs.isEmpty()) 0f
            else moodLogs.sumOf { it.tasksCompleted }.toFloat() / moodLogs.size
            MoodStat(mood = mood, avgTasksCompleted = avg)
        }

        // On-track rate: of past-due tasks (last 30 days), fraction completed
        val now = System.currentTimeMillis()
        val thirtyDaysAgoMillis = thirtyDaysAgo.atStartOfDay(zone).toInstant().toEpochMilli()
        val pastDueTasks = tasks.filter { task ->
            task.dueDate != null &&
            task.dueDate < now &&
            task.dueDate >= thirtyDaysAgoMillis
        }
        val onTrackRate = if (pastDueTasks.isEmpty()) 0f
        else pastDueTasks.count { it.isCompleted }.toFloat() / pastDueTasks.size

        return RetroInsights(
            weeklyTrend = weeklyTrend,
            bestDay = bestDay,
            moodProductivity = moodProductivity,
            onTrackRate = onTrackRate
        )
    }
}
