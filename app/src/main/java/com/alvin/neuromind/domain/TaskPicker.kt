package com.alvin.neuromind.domain

import com.alvin.neuromind.data.Task
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The one place that decides which tasks the "smart" surfaces may pick:
 * the Home plan, Today's priorities (app + widget), suggestions (Home, Ask,
 * notifications), Ask's focus/mood replies and the overdue rebalancer.
 *
 * Two kinds of task:
 *  - Work (default): done any time before the due date, so it can be planned
 *    and suggested for Focus.
 *  - On the day ([Task.onDueDay]): an errand or appointment that happens on its
 *    due date. Invisible to these surfaces before that day; on the day (or once
 *    overdue) it shows in priorities, but it's never planned or offered as Focus
 *    work, and never auto-rescheduled.
 *
 * Tasks waiting on an unfinished prerequisite are never planned or suggested.
 */
object TaskPicker {

    fun dueDay(task: Task, zone: ZoneId = ZoneId.systemDefault()): LocalDate? =
        task.dueDate?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }

    fun isBlocked(task: Task, all: List<Task>): Boolean =
        task.prerequisiteTaskId != null && all.any { it.id == task.prerequisiteTaskId && !it.isCompleted }

    /** On-the-day tasks only exist from their due day onward; work tasks always. */
    fun isRelevant(task: Task, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        if (!task.onDueDay) return true
        val day = dueDay(task, zone) ?: return true
        return !day.isAfter(today)
    }

    /** Work that can be scheduled or suggested for Focus right now. */
    fun workable(all: List<Task>): List<Task> =
        all.filter { !it.isCompleted && !it.onDueDay && !isBlocked(it, all) }

    /**
     * Most urgent first: overdue, then due within [soonDays], then priority,
     * difficulty, and earliest due date. Tasks with no due date sort last in
     * their priority band.
     */
    fun byUrgency(nowMillis: Long, soonDays: Long = 3): Comparator<Task> {
        val soon = nowMillis + soonDays * 24 * 60 * 60 * 1000
        return compareByDescending<Task> { it.dueDate != null && it.dueDate < nowMillis }
            .thenByDescending { it.dueDate != null && it.dueDate < soon }
            .thenByDescending { it.priority }
            .thenByDescending { it.difficulty }
            .thenBy { it.dueDate ?: Long.MAX_VALUE }
    }

    /** Work tasks in the order the planner and suggestions should consider them. */
    fun workQueue(all: List<Task>, nowMillis: Long): List<Task> =
        workable(all).sortedWith(byUrgency(nowMillis))

    /**
     * Today's priorities: overdue tasks, today's on-the-day errands, and
     * HIGH-priority work. On-the-day tasks for later days stay hidden.
     */
    fun priorities(
        all: List<Task>,
        today: LocalDate,
        nowMillis: Long,
        limit: Int = 5,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<Task> =
        all.asSequence()
            .filter { !it.isCompleted && isRelevant(it, today, zone) }
            .filter { task ->
                val overdue = task.dueDate != null && task.dueDate < nowMillis
                val errandToday = task.onDueDay && dueDay(task, zone) == today
                overdue || errandToday || task.priority == com.alvin.neuromind.data.Priority.HIGH
            }
            .sortedWith(
                compareByDescending<Task> { it.dueDate != null && it.dueDate < nowMillis }
                    .thenByDescending { it.onDueDay }
                    .thenByDescending { it.priority }
                    .thenBy { it.dueDate ?: Long.MAX_VALUE }
            )
            .take(limit)
            .toList()

    /** Overdue work the rebalancer may move; on-the-day errands are never auto-rescheduled. */
    fun rebalanceable(all: List<Task>): List<Task> =
        all.filter { it.isOverdue && !it.isCompleted && !it.onDueDay }
}
