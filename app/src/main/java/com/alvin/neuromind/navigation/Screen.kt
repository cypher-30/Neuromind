package com.alvin.neuromind.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object TaskList : Screen("task_list")
    object AddEditTask : Screen("add_edit_task")
    object Timetable : Screen("timetable")
    object Insights : Screen("insights")
    object Settings : Screen("settings")
    object Feedback : Screen("feedback")

    fun withArgs(vararg args: Any): String {
        return buildString {
            append(route)
            args.forEach { arg ->
                append("/$arg")
            }
        }
    }
}