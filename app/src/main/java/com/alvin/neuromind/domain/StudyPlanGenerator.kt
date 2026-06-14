package com.alvin.neuromind.domain

import com.alvin.neuromind.data.Task

object StudyPlanGenerator {

    /**
     * Breaks a task into focused sub-tasks.
     *
     * If the title mentions specific chapters ("chapters 4, 5, 8"), one sub-task is
     * created per chapter. Otherwise the task is split into three standard phases:
     * Plan, Execute, Review.
     */
    fun generate(parentTask: Task): List<Task> {
        val base = Task(
            title = "",
            priority = parentTask.priority,
            difficulty = parentTask.difficulty,
            dueDate = parentTask.dueDate
        )

        val chapterRegex = Regex("(?i)chapters?\\s+([0-9][0-9,\\s&]+)")
        val match = chapterRegex.find(parentTask.title)

        if (match != null) {
            val chapters = match.groupValues[1]
                .split(Regex("[,\\s&]+"))
                .mapNotNull { it.trim().toIntOrNull() }
                .distinct()

            if (chapters.isNotEmpty()) {
                val baseTitle = parentTask.title
                    .replaceFirst(match.value, "")
                    .replace(Regex("(?i)revise\\s*|review\\s*|study\\s*"), "")
                    .trim()
                    .ifBlank { parentTask.title }
                val subDuration = (parentTask.durationMinutes / chapters.size).coerceAtLeast(30)
                return chapters.map { ch ->
                    base.copy(title = "Chapter $ch — $baseTitle", durationMinutes = subDuration)
                }
            }
        }

        val phases = listOf("Plan & Research", "Execute", "Review & Polish")
        val subDuration = (parentTask.durationMinutes / 3).coerceAtLeast(30)
        return phases.map { phase ->
            base.copy(title = "${parentTask.title}: $phase", durationMinutes = subDuration)
        }
    }
}
