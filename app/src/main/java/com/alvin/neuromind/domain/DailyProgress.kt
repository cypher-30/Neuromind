package com.alvin.neuromind.domain

import com.alvin.neuromind.data.Task
import java.time.LocalDate
import java.time.ZoneId

data class DailyProgress(val total: Int, val completed: Int) {
    val pending: Int get() = total - completed
    val fraction: Float get() = if (total == 0) 0f else completed.toFloat() / total
}

/**
 * "Today's progress" counts only tasks due on [date] (local calendar day).
 * Overdue, future, and undated tasks are excluded, and a task completed early
 * still counts as done for the day it is due. Nothing is mutated: the count
 * simply re-evaluates against the new date after midnight.
 */
object DailyProgressCalculator {
    fun forDate(tasks: List<Task>, date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): DailyProgress {
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val dueToday = tasks.filter { task -> task.dueDate != null && task.dueDate >= start && task.dueDate < end }
        return DailyProgress(total = dueToday.size, completed = dueToday.count { it.isCompleted })
    }

    /**
     * "All due tasks": every open task with a due date (overdue or upcoming),
     * plus tasks already finished whose due date is today or later. Completed
     * tasks due in the past are history and are left out, so this never turns
     * into a lifetime total.
     */
    fun allDue(tasks: List<Task>, date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): DueOverview {
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val dated = tasks.filter { it.dueDate != null }
        val open = dated.filter { !it.isCompleted }
        val doneAhead = dated.count { it.isCompleted && it.dueDate!! >= start }
        return DueOverview(
            progress = DailyProgress(total = open.size + doneAhead, completed = doneAhead),
            overdue = open.count { it.dueDate!! < start },
            upcoming = open.count { it.dueDate!! >= start }
        )
    }
}

data class DueOverview(val progress: DailyProgress, val overdue: Int, val upcoming: Int)
