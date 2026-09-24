package com.alvin.neuromind.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")
    data object Dashboard : Screen("dashboard")
    data object TaskList : Screen("task_list")
    data object AddEditTask : Screen("add_edit_task")
    // No standalone screen — Timetable folds into Tasks as a segmented toggle
    // (see ui/tasks/TaskListScreen.kt). This route is kept only as a deep
    // link that opens Tasks pre-set to the Timetable side, for Settings'
    // "Weekly Timetable" and the Dashboard's "Up next" tap.
    data object Timetable : Screen("timetable")
    data object FocusMode : Screen("focus_mode")
    data object Insights : Screen("insights")
    data object Settings : Screen("settings")
    data object Feedback : Screen("feedback")
    data object Assistant : Screen("assistant")
    data object Previews : Screen("previews")

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
