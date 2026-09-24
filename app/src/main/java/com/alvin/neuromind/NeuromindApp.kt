package com.alvin.neuromind

import android.net.Uri
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
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
import kotlinx.coroutines.flow.first
import com.alvin.neuromind.data.Task
import com.alvin.neuromind.data.TaskRepository
import com.alvin.neuromind.data.preferences.ThemeSetting
import com.alvin.neuromind.data.preferences.UserPreferencesRepository
import com.alvin.neuromind.domain.Scheduler
import com.alvin.neuromind.navigation.Screen
import com.alvin.neuromind.navigation.AppRouteRequest
import com.alvin.neuromind.ui.assistant.AssistantScreen
import com.alvin.neuromind.ui.assistant.AssistantViewModel
import com.alvin.neuromind.ui.assistant.AssistantViewModelFactory
import com.alvin.neuromind.ui.components.SegmentedToggle
import com.alvin.neuromind.ui.dashboard.DashboardScreen
import com.alvin.neuromind.ui.dashboard.DashboardViewModel
import com.alvin.neuromind.ui.dashboard.DashboardViewModelFactory
import com.alvin.neuromind.ui.events.EventEditorScreen
import com.alvin.neuromind.ui.events.EventEditorViewModel
import com.alvin.neuromind.ui.events.EventEditorViewModelFactory
import com.alvin.neuromind.ui.feedback.FeedbackScreen
import com.alvin.neuromind.ui.feedback.FeedbackViewModel
import com.alvin.neuromind.ui.feedback.FeedbackViewModelFactory
import com.alvin.neuromind.ui.focus.FocusModeScreen
import com.alvin.neuromind.ui.focus.FocusViewModel
import com.alvin.neuromind.ui.focus.FocusViewModelFactory
import com.alvin.neuromind.ui.insights.InsightsScreen
import com.alvin.neuromind.ui.insights.InsightsViewModel
import com.alvin.neuromind.ui.insights.InsightsViewModelFactory
import com.alvin.neuromind.ui.onboarding.OnboardingScreen
import com.alvin.neuromind.ui.onboarding.OnboardingViewModel
import com.alvin.neuromind.ui.onboarding.OnboardingViewModelFactory
import com.alvin.neuromind.ui.previews.PreviewsScreen
import com.alvin.neuromind.ui.settings.SettingsScreen
import com.alvin.neuromind.ui.settings.SettingsViewModel
import com.alvin.neuromind.ui.settings.SettingsViewModelFactory
import com.alvin.neuromind.ui.tasks.*
import com.alvin.neuromind.ui.theme.NeuromindTheme
import com.alvin.neuromind.ui.timetable.TimetableViewModelFactory
import com.alvin.neuromind.ui.splash.SplashScreen

data class NavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

// Bottom-nav-visible top-level destinations. AddEditTask, FocusMode and
// Feedback are pushed screens and deliberately excluded — the bar used to
// stay visible there, which read as a bug.
private val BOTTOM_NAV_ROUTES = setOf(
    Screen.Dashboard.route,
    Screen.TaskList.route + "/{isRescheduleMode}?openTimetable={openTimetable}",
    Screen.Assistant.route,
    Screen.Insights.route,
    Screen.Settings.route
)

@Composable
fun NeuromindApp(
    repository: TaskRepository,
    scheduler: Scheduler,
    userPreferencesRepository: UserPreferencesRepository,
    showComposeSplash: Boolean = true,
    pendingRouteRequest: AppRouteRequest? = null,
    onPendingRouteHandled: () -> Unit = {}
) {
    val settingsFactory = SettingsViewModelFactory(userPreferencesRepository, repository)
    val settingsViewModel: SettingsViewModel = viewModel(factory = settingsFactory)
    val timetableViewModelFactory = TimetableViewModelFactory(repository)

    val themeSetting by settingsViewModel.themeSetting.collectAsStateWithLifecycle()

    val useDarkTheme = when (themeSetting) {
        ThemeSetting.LIGHT -> false
        ThemeSetting.DARK -> true
        else -> isSystemInDarkTheme()
    }

    NeuromindTheme(darkTheme = useDarkTheme) {
        var showSplash by rememberSaveable { mutableStateOf(showComposeSplash) }
        // Defaults to true (assume onboarded) so an existing user never sees a
        // one-frame onboarding flash while the DataStore value loads; the
        // cost is a new user's first frame briefly assuming they're onboarded
        // too, which self-corrects as soon as the real value arrives.
        val isOnboarded by userPreferencesRepository.isOnboarded.collectAsStateWithLifecycle(initialValue = true)
        var onboardingDone by rememberSaveable { mutableStateOf(false) }

        if (showSplash) {
            SplashScreen(onFinished = { showSplash = false })
            return@NeuromindTheme
        }

        if (!isOnboarded && !onboardingDone) {
            val factory = OnboardingViewModelFactory(userPreferencesRepository)
            val vm = viewModel<OnboardingViewModel>(factory = factory)
            OnboardingScreen(viewModel = vm, onFinished = { onboardingDone = true })
            return@NeuromindTheme
        }

        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val showBottomNav = navBackStackEntry?.destination?.route in BOTTOM_NAV_ROUTES

        LaunchedEffect(pendingRouteRequest) {
            val request = pendingRouteRequest ?: return@LaunchedEffect
            if (request.route == Screen.Timetable.route) {
                // Event reminder tap: open the Timetable, then the event itself.
                navController.navigateToTab("${Screen.TaskList.route}/false?openTimetable=true")
                request.entryId?.let { navController.navigate("${Screen.AddEditTask.route}?eventId=$it") }
            } else if (request.route == Screen.Feedback.route) {
                val encodedPrefill = Uri.encode(request.prefillNote ?: "")
                navController.navigate(
                    "${Screen.Feedback.route}?autoVoice=${request.autoStartVoiceCapture}&launchToken=${request.launchToken}&prefill=$encodedPrefill"
                )
            }
            onPendingRouteHandled()
        }

        Scaffold(
            bottomBar = {
                if (showBottomNav) {
                    Column {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        BottomNavBar(navController = navController)
                    }
                }
            }
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
                            },
                            isDarkTheme = useDarkTheme,
                            onToggleTheme = {
                                settingsViewModel.updateTheme(
                                    if (useDarkTheme) ThemeSetting.LIGHT else ThemeSetting.DARK
                                )
                            }
                        )
                    }

                    composable(
                        route = Screen.TaskList.route + "/{isRescheduleMode}?openTimetable={openTimetable}",
                        arguments = listOf(
                            navArgument("isRescheduleMode") { type = NavType.BoolType; defaultValue = false },
                            navArgument("openTimetable") { type = NavType.BoolType; defaultValue = false }
                        )
                    ) { backStackEntry ->
                        val isRescheduleMode = backStackEntry.arguments?.getBoolean("isRescheduleMode") ?: false
                        val openTimetable = backStackEntry.arguments?.getBoolean("openTimetable") ?: false
                        val factory = TaskViewModelFactory(repository)
                        val vm = viewModel<TaskViewModel>(factory = factory)

                        TaskListScreen(
                            viewModel = vm,
                            timetableViewModelFactory = timetableViewModelFactory,
                            isRescheduleMode = isRescheduleMode,
                            initialTimetableView = openTimetable,
                            onAddTaskClicked = { navController.navigate(Screen.AddEditTask.route) },
                            onAddEventClicked = { navController.navigate(Screen.AddEditTask.route + "?kind=event") },
                            onEditTaskClicked = { task ->
                                navController.navigate(Screen.AddEditTask.route + "?taskId=${task.id}")
                            },
                            onEditEventClicked = { entry ->
                                navController.navigate(Screen.AddEditTask.route + "?eventId=${entry.id}")
                            },
                            onFocusTaskClicked = { task ->
                                navController.navigate(Screen.FocusMode.route + "/${task.id}")
                            }
                        )
                    }

                    composable(Screen.Timetable.route) {
                        // Compatibility route: timetable is now the Tasks screen's
                        // segmented Timetable view.
                        LaunchedEffect(Unit) {
                            navController.navigate("${Screen.TaskList.route}/false?openTimetable=true") {
                                popUpTo(Screen.Timetable.route) { inclusive = true }
                            }
                        }
                    }

                    composable(
                        route = Screen.AddEditTask.route + "?taskId={taskId}&eventId={eventId}&kind={kind}",
                        arguments = listOf(
                            navArgument("taskId") { type = NavType.IntType; defaultValue = -1 },
                            navArgument("eventId") { type = NavType.IntType; defaultValue = -1 },
                            navArgument("kind") { type = NavType.StringType; defaultValue = "task" }
                        )
                    ) { backStackEntry ->
                        val taskId = backStackEntry.arguments?.getInt("taskId") ?: -1
                        val eventId = backStackEntry.arguments?.getInt("eventId") ?: -1
                        val isNew = taskId == -1 && eventId == -1
                        var kind by rememberSaveable {
                            mutableStateOf(if (eventId != -1) "event" else backStackEntry.arguments?.getString("kind") ?: "task")
                        }
                        // New items choose Task or Event at the top; editing keeps the item's own kind.
                        val selector: @Composable () -> Unit = {
                            if (isNew) {
                                SegmentedToggle(
                                    options = listOf("Task", "Event"),
                                    selectedIndex = if (kind == "event") 1 else 0,
                                    onSelect = { kind = if (it == 1) "event" else "task" }
                                )
                            }
                        }

                        if (kind == "event") {
                            val vm = viewModel<EventEditorViewModel>(factory = EventEditorViewModelFactory(repository))
                            LaunchedEffect(vm) { vm.start(eventId) }
                            val notificationsOn by userPreferencesRepository.notificationsEnabled
                                .collectAsStateWithLifecycle(initialValue = true)
                            EventEditorScreen(
                                viewModel = vm,
                                reminderScheduler = repository.reminderScheduler(),
                                appNotificationsEnabled = notificationsOn,
                                onNavigateUp = { navController.navigateUp() },
                                topContent = selector
                            )
                        } else {
                            val vm = viewModel<AddEditTaskViewModel>(factory = AddEditTaskViewModelFactory(repository))
                            LaunchedEffect(vm) { vm.start(taskId) }
                            AddEditTaskScreen(
                                viewModel = vm,
                                onNavigateUp = { navController.navigateUp() },
                                topContent = selector
                            )
                        }
                    }

                    composable(
                        route = Screen.FocusMode.route + "/{taskId}",
                        arguments = listOf(navArgument("taskId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val taskId = backStackEntry.arguments?.getInt("taskId") ?: return@composable
                        var task by androidx.compose.runtime.remember { mutableStateOf<Task?>(null) }
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
                            onNavigateToFeedback = { navController.navigate(Screen.Feedback.route) },
                            onNavigateToPreviews = { navController.navigate(Screen.Previews.route) },
                            onReplaySplash = { showSplash = true }
                        )
                    }

                    composable(Screen.Previews.route) {
                        PreviewsScreen(onBack = { navController.popBackStack() })
                    }

                    composable(
                        route = "${Screen.Feedback.route}?autoVoice={autoVoice}&launchToken={launchToken}&prefill={prefill}",
                        arguments = listOf(
                            navArgument("autoVoice") { type = NavType.BoolType; defaultValue = false },
                            navArgument("launchToken") { type = NavType.LongType; defaultValue = 0L },
                            navArgument("prefill") { type = NavType.StringType; defaultValue = "" }
                        )
                    ) { backStackEntry ->
                        val autoVoice = backStackEntry.arguments?.getBoolean("autoVoice") ?: false
                        val launchToken = backStackEntry.arguments?.getLong("launchToken") ?: 0L
                        val prefill = backStackEntry.arguments?.getString("prefill")?.ifBlank { null }
                        val factory = FeedbackViewModelFactory(repository)
                        val vm = viewModel<FeedbackViewModel>(factory = factory)
                        FeedbackScreen(
                            viewModel = vm,
                            onNavigateBack = { navController.popBackStack() },
                            autoStartVoiceCapture = autoVoice,
                            autoStartToken = launchToken,
                            initialPrefillNote = prefill
                        )
                    }

                    composable(Screen.Assistant.route) {
                        val factory = AssistantViewModelFactory(repository, userPreferencesRepository)
                        val vm = viewModel<AssistantViewModel>(factory = factory)
                        AssistantScreen(
                            viewModel = vm,
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
        NavItem(Screen.Dashboard, "Home", Icons.Outlined.Home),
        NavItem(Screen.TaskList, "Tasks", Icons.AutoMirrored.Outlined.List),
        NavItem(Screen.Assistant, "Ask", Icons.AutoMirrored.Outlined.Chat),
        NavItem(Screen.Insights, "Insights", Icons.Outlined.BarChart),
        NavItem(Screen.Settings, "Settings", Icons.Outlined.Settings)
    )
    NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        navItems.forEach { item ->
            val routeToCheck = when (item.screen) {
                Screen.TaskList -> Screen.TaskList.route + "/{isRescheduleMode}?openTimetable={openTimetable}"
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
                    navController.navigateToTab(targetRoute)
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
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        }
    }
}

/**
 * Bottom-nav tabs always land on their own root, whatever was opened on top.
 * The old saveState/restoreState pattern restored the stack saved under Home
 * (e.g. Tasks → Timetable), so tapping Home appeared to do nothing.
 */
private fun NavController.navigateToTab(route: String) {
    val startId = graph.findStartDestination().id
    if (route == Screen.Dashboard.route) {
        if (!popBackStack(startId, inclusive = false)) {
            navigate(route) { launchSingleTop = true }
        }
        return
    }
    navigate(route) {
        popUpTo(startId)
        launchSingleTop = true
    }
}
