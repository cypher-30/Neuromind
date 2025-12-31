package com.alvin.neuromind.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object TaskList : Screen("task_list")
    data object AddEditTask : Screen("add_edit_task")
    data object Timetable : Screen("timetable")
    data object Insights : Screen("insights")
    data object Settings : Screen("settings")
    data object Feedback : Screen("feedback")

    // Helper to pass arguments safely
    fun withArgs(vararg args: Any): String {
        return buildString {
            append(route)
            args.forEach { arg ->
                append("/$arg")
            }
        }
    }
}