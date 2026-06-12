package com.alvin.neuromind.ui

object SmartInputHelper {
    /**
     * Parses a string like "Remind me to Study Math tomorrow at 5pm"
     */
    fun parseInput(input: String): PartialTask {
        val lower = input.lowercase()
        val title = input.replace(Regex("(?i)remind me to |tomorrow|at \\d+pm"), "").trim()

        // Very basic logic for "tomorrow"
        val date = if (lower.contains("tomorrow")) {
            System.currentTimeMillis() + 86400000
        } else null

        return PartialTask(title = title, dueDate = date)
    }
}
data class PartialTask(val title: String, val dueDate: Long?)