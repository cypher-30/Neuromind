package com.alvin.neuromind

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.alvin.neuromind.data.Task
import com.alvin.neuromind.data.TaskRepository
import com.alvin.neuromind.data.preferences.ThemeSetting
import com.alvin.neuromind.data.preferences.UserPreferencesRepository
import com.alvin.neuromind.domain.Scheduler
import com.alvin.neuromind.navigation.Screen
import com.alvin.neuromind.ui.assistant.AssistantScreen
import com.alvin.neuromind.ui.assistant.AssistantViewModel
import com.alvin.neuromind.ui.assistant.AssistantViewModelFactory
import com.alvin.neuromind.ui.dashboard.DashboardScreen
import com.alvin.neuromind.ui.dashboard.DashboardViewModel
import com.alvin.neuromind.ui.dashboard.DashboardViewModelFactory
import com.alvin.neuromind.ui.feedback.FeedbackScreen
import com.alvin.neuromind.ui.feedback.FeedbackViewModel
import com.alvin.neuromind.ui.feedback.FeedbackViewModelFactory
import com.alvin.neuromind.ui.focus.FocusModeScreen
import com.alvin.neuromind.ui.focus.FocusViewModel
import com.alvin.neuromind.ui.focus.FocusViewModelFactory
import com.alvin.neuromind.ui.insights.InsightsScreen
import com.alvin.neuromind.ui.insights.InsightsViewModel
import com.alvin.neuromind.ui.insights.InsightsViewModelFactory
import com.alvin.neuromind.ui.settings.SettingsScreen
import com.alvin.neuromind.ui.settings.SettingsViewModel
import com.alvin.neuromind.ui.settings.SettingsViewModelFactory
import com.alvin.neuromind.ui.tasks.*
import com.alvin.neuromind.ui.theme.NeuromindTheme
import com.alvin.neuromind.ui.timetable.TimetableScreen
import com.alvin.neuromind.ui.timetable.TimetableViewModel
import com.alvin.neuromind.ui.splash.SplashScreen
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
    val settingsFactory = SettingsViewModelFactory(userPreferencesRepository, repository)
    val settingsViewModel: SettingsViewModel = viewModel(factory = settingsFactory)

    val themeSetting by settingsViewModel.themeSetting.collectAsStateWithLifecycle()

    val useDarkTheme = when (themeSetting) {
        ThemeSetting.LIGHT -> false
        ThemeSetting.DARK -> true
        else -> isSystemInDarkTheme()
    }

    NeuromindTheme(darkTheme = useDarkTheme) {
        var showSplash by rememberSaveable { mutableStateOf(true) }

        if (showSplash) {
            SplashScreen(onFinished = { showSplash = false })
            return@NeuromindTheme
        }

        val navController = rememberNavController()

        Scaffold(
            bottomBar = { BottomNavBar(navController = navController) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .windowInsetsPadding(WindowInsets.safeDrawing.exclude(WindowInsets.ime))
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Dashboard.route,
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) },
                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) },
                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
                ) {
                    composable(route = Screen.Dashboard.route) {
                        val factory = DashboardViewModelFactory(repository, scheduler, userPreferencesRepository)
                        val vm = viewModel<DashboardViewModel>(factory = factory)
                        DashboardScreen(
                            viewModel = vm,
                            onNavigateToTasks = { navController.navigate(Screen.TaskList.withArgs(true)) },
                            onNavigateToTimetable = { navController.navigate(Screen.Timetable.route) },
                            onNavigateToAssistant = { navController.navigate(Screen.Assistant.route) },
                            onNavigateToTask = { taskId ->
                                navController.navigate(Screen.AddEditTask.route + "?taskId=$taskId")
                            }
                        )
                    }

                    composable(
                        route = Screen.TaskList.route + "/{isRescheduleMode}",
                        arguments = listOf(navArgument("isRescheduleMode") { type = NavType.BoolType; defaultValue = false })
                    ) { backStackEntry ->
                        val isRescheduleMode = backStackEntry.arguments?.getBoolean("isRescheduleMode") ?: false
                        val factory = TaskViewModelFactory(repository)
                        val vm = viewModel<TaskViewModel>(factory = factory)

                        TaskListScreen(
                            viewModel = vm,
                            isRescheduleMode = isRescheduleMode,
                            onAddTaskClicked = { navController.navigate(Screen.AddEditTask.route) },
                            onEditTaskClicked = { task ->
                                navController.navigate(Screen.AddEditTask.route + "?taskId=${task.id}")
                            },
                            onFocusTaskClicked = { task ->
                                navController.navigate(Screen.FocusMode.route + "/${task.id}")
                            }
                        )
                    }

                    composable(
                        route = Screen.AddEditTask.route + "?taskId={taskId}",
                        arguments = listOf(navArgument("taskId") { type = NavType.IntType; defaultValue = -1 })
                    ) { backStackEntry ->
                        val taskId = backStackEntry.arguments?.getInt("taskId") ?: -1
                        val factory = AddEditTaskViewModelFactory(repository)
                        val vm = viewModel<AddEditTaskViewModel>(factory = factory)

                        LaunchedEffect(taskId) {
                            if (taskId != -1) vm.loadTask(taskId)
                        }

                        AddEditTaskScreen(viewModel = vm, onNavigateUp = { navController.navigateUp() })
                    }

                    composable(Screen.Timetable.route) {
                        val factory = TimetableViewModelFactory(repository)
                        val vm = viewModel<TimetableViewModel>(factory = factory)
                        TimetableScreen(
                            viewModel = vm,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = Screen.FocusMode.route + "/{taskId}",
                        arguments = listOf(navArgument("taskId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val taskId = backStackEntry.arguments?.getInt("taskId") ?: return@composable
                        var task by remember { mutableStateOf<Task?>(null) }
                        LaunchedEffect(taskId) { task = repository.getTaskById(taskId) }
                        val focusVm = viewModel<FocusViewModel>(factory = FocusViewModelFactory(repository))

                        task?.let {
                            FocusModeScreen(
                                task = it,
                                viewModel = focusVm,
                                onFinish = { navController.popBackStack() }
                            )
                        }
                    }

                    composable(Screen.Insights.route) {
                        val factory = InsightsViewModelFactory(repository)
                        val vm = viewModel<InsightsViewModel>(factory = factory)
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
                        val vm = viewModel<FeedbackViewModel>(factory = factory)
                        FeedbackScreen(
                            viewModel = vm,
                            onFeedbackSubmitted = { navController.popBackStack() },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.Assistant.route) {
                        val factory = AssistantViewModelFactory(repository, userPreferencesRepository)
                        val vm = viewModel<AssistantViewModel>(factory = factory)
                        AssistantScreen(
                            viewModel = vm,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToFocus = { taskId ->
                                navController.navigate(Screen.FocusMode.route + "/$taskId")
                            },
                            onNavigateToTasks = { navController.navigate(Screen.TaskList.withArgs(false)) },
                            onNavigateToTimetable = { navController.navigate(Screen.Timetable.route) },
                            onNavigateToInsights = { navController.navigate(Screen.Insights.route) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNavBar(navController: NavController) {
    val navItems = listOf(
        NavItem(Screen.Dashboard, "Home", Icons.Default.Dashboard),
        NavItem(Screen.TaskList, "Tasks", Icons.AutoMirrored.Filled.List),
        NavItem(Screen.Insights, "Insights", Icons.Default.BarChart),
        NavItem(Screen.Settings, "Settings", Icons.Default.Settings)
    )
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        navItems.forEach { item ->
            val routeToCheck = when (item.screen) {
                Screen.TaskList -> Screen.TaskList.route + "/{isRescheduleMode}"
                else -> item.screen.route
            }
            val isSelected = currentDestination?.hierarchy?.any { it.route == routeToCheck } == true

            val iconScale by animateFloatAsState(
                targetValue = if (isSelected) 1.15f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "nav_scale_${item.label}"
            )

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    val targetRoute = if (item.screen == Screen.TaskList) Screen.TaskList.withArgs(false) else item.screen.route
                    navController.navigate(targetRoute) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.scale(iconScale)
                    )
                },
                label = {
                    Text(
                        text     = item.label,
                        maxLines = 1,
                        softWrap = false,
                        style    = MaterialTheme.typography.labelMedium
                    )
                }
            )
        }
    }
}
