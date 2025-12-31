package com.alvin.neuromind.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object TaskList : Screen("taskList?isRescheduleMode={isRescheduleMode}") {
        fun withArgs(isRescheduleMode: Boolean) = "taskList?isRescheduleMode=$isRescheduleMode"
    }
    data object AddEditTask : Screen("addEditTask")
    data object Timetable : Screen("timetable")
    data object Feedback : Screen("feedback")
    data object Insights : Screen("insights")
    data object Settings : Screen("settings")
}