package com.alvin.neuromind

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.alvin.neuromind.data.TaskRepository
import com.alvin.neuromind.data.preferences.ThemeSetting
import com.alvin.neuromind.data.preferences.UserPreferencesRepository
import com.alvin.neuromind.domain.Scheduler
import com.alvin.neuromind.navigation.Screen
import com.alvin.neuromind.ui.dashboard.DashboardScreen
import com.alvin.neuromind.ui.dashboard.DashboardViewModelFactory
import com.alvin.neuromind.ui.feedback.FeedbackScreen
import com.alvin.neuromind.ui.feedback.FeedbackViewModelFactory
import com.alvin.neuromind.ui.insights.InsightsScreen
import com.alvin.neuromind.ui.insights.InsightsViewModelFactory
import com.alvin.neuromind.ui.settings.SettingsScreen
import com.alvin.neuromind.ui.settings.SettingsViewModel
import com.alvin.neuromind.ui.settings.SettingsViewModelFactory
import com.alvin.neuromind.ui.tasks.*
import com.alvin.neuromind.ui.theme.NeuromindTheme
import com.alvin.neuromind.ui.timetable.TimetableScreen
import com.alvin.neuromind.ui.timetable.TimetableViewModelFactory

data class NavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

@Composable
fun NeuromindApp(
    repository: TaskRepository,
    scheduler: Scheduler,
    userPreferencesRepository: UserPreferencesRepository
) {
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(userPreferencesRepository))
    val themeSetting by settingsViewModel.themeSetting.collectAsState()
    val useDarkTheme = when (themeSetting) {
        ThemeSetting.LIGHT -> false
        ThemeSetting.DARK -> true
        ThemeSetting.SYSTEM -> isSystemInDarkTheme()
    }

    NeuromindTheme(darkTheme = useDarkTheme) {
        val navController = rememberNavController()
        Scaffold(
            bottomBar = { BottomNavBar(navController = navController) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Dashboard.route
                ) {
                    composable(Screen.Dashboard.route) {
                        val factory = DashboardViewModelFactory(repository, scheduler)
                        val vm = viewModel<com.alvin.neuromind.ui.dashboard.DashboardViewModel>(factory = factory)
                        DashboardScreen(
                            viewModel = vm,
                            onNavigateToTasks = { navController.navigate(Screen.TaskList.withArgs(true)) },
                            onNavigateToTimetable = { navController.navigate(Screen.Timetable.route) },
                            onNavigateToFeedback = { navController.navigate(Screen.Feedback.route) }
                        )
                    }
                    composable(
                        route = Screen.TaskList.route,
                        arguments = listOf(navArgument("isRescheduleMode") { defaultValue = false })
                    ) { backStackEntry ->
                        val isRescheduleMode = backStackEntry.arguments?.getBoolean("isRescheduleMode") ?: false
                        val factory = TaskViewModelFactory(repository, scheduler)
                        val vm = viewModel<com.alvin.neuromind.ui.tasks.TaskViewModel>(factory = factory)
                        TaskListScreen(
                            viewModel = vm,
                            isRescheduleMode = isRescheduleMode,
                            onAddTaskClicked = { navController.navigate(Screen.AddEditTask.route) }
                        )
                    }
                    composable(Screen.AddEditTask.route) {
                        val factory = AddEditTaskViewModelFactory(repository)
                        val vm = viewModel<AddEditTaskViewModel>(factory = factory)
                        AddEditTaskScreen(viewModel = vm, onNavigateUp = { navController.navigateUp() })
                    }
                    composable(Screen.Timetable.route) {
                        val factory = TimetableViewModelFactory(repository)
                        val vm = viewModel<com.alvin.neuromind.ui.timetable.TimetableViewModel>(factory = factory)
                        TimetableScreen(viewModel = vm)
                    }
                    composable(Screen.Insights.route) {
                        val factory = InsightsViewModelFactory(repository)
                        val vm = viewModel<com.alvin.neuromind.ui.insights.InsightsViewModel>(factory = factory)
                        InsightsScreen(viewModel = vm)
                    }
                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onNavigateToTimetable = { navController.navigate(Screen.Timetable.route) },
                            onNavigateToFeedback = { navController.navigate(Screen.Feedback.route) }
                        )
                    }
                    composable(Screen.Feedback.route) {
                        val factory = FeedbackViewModelFactory(repository)
                        val vm = viewModel<com.alvin.neuromind.ui.feedback.FeedbackViewModel>(factory = factory)
                        FeedbackScreen(viewModel = vm, onFeedbackSubmitted = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNavBar(navController: NavController) {
    val navItems = listOf(
        NavItem(Screen.Dashboard, "Dashboard", Icons.Default.Dashboard),
        NavItem(Screen.TaskList, "Tasks", Icons.AutoMirrored.Filled.List),
        NavItem(Screen.Insights, "Insights", Icons.Default.BarChart),
        NavItem(Screen.Settings, "Settings", Icons.Default.Settings)
    )
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        navItems.forEach { item ->
            NavigationBarItem(
                selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true,
                onClick = {
                    navController.navigate(item.screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}