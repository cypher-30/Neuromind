package com.alvin.neuromind.domain

object SmartInputHelper {
    fun parseInput(input: String): ParsedTask {
        val lower = input.lowercase()
        val title = input.replace(Regex("(?i)remind me to |tomorrow|at \\d+pm"), "").trim()

        val date = if (lower.contains("tomorrow")) {
            System.currentTimeMillis() + 86400000
        } else null

        return ParsedTask(title = if (title.isEmpty()) "New Task" else title, dueDate = date)
    }
}
data class ParsedTask(val title: String, val dueDate: Long?)