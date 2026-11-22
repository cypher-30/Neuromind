package com.alvin.neuromind

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
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
import com.alvin.neuromind.domain.AdaptiveAppearance
import com.alvin.neuromind.domain.AppearanceVariant
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
import com.alvin.neuromind.ui.details.DetailsViewModelFactory
import com.alvin.neuromind.ui.details.EntryDetailsScreen
import com.alvin.neuromind.ui.details.EntryDetailsViewModel
import com.alvin.neuromind.ui.details.TaskDetailsScreen
import com.alvin.neuromind.ui.details.TaskDetailsViewModel
import com.alvin.neuromind.ui.events.EventEditorScreen
import com.alvin.neuromind.ui.timetable.TimetableEntryEditorDialog
import com.alvin.neuromind.ui.timetable.TimetableViewModel
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
import com.alvin.neuromind.ui.theme.Dimens
import com.alvin.neuromind.ui.theme.Pill
import com.alvin.neuromind.ui.timetable.TimetableViewModelFactory
import com.alvin.neuromind.ui.splash.SplashScreen

data class NavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon
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

@OptIn(ExperimentalLayoutApi::class)
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

    // Adaptive Organic (opt-in, off by default): purely local, re-evaluated
    // whenever the enabled flag or feedback logs change, and additionally
    // forced to recompute on every app resume and at local midnight — both
    // of which can flip AdaptiveAppearance.resolve()'s result without any
    // feedback-log change (a stale calendar day rolling over, or the app
    // being reopened after the day changed while backgrounded).
    // See domain/AdaptiveAppearance.kt — never reads tone/sentiment/comment text.
    val adaptiveEnabled by userPreferencesRepository.adaptiveAppearanceEnabled.collectAsStateWithLifecycle(initialValue = false)
    val feedbackLogsForAppearance by (if (adaptiveEnabled) repository.allFeedbackLogs else kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsStateWithLifecycle(initialValue = emptyList())
    var adaptiveToday by remember { mutableStateOf(java.time.LocalDate.now()) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                adaptiveToday = java.time.LocalDate.now()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        while (true) {
            val now = java.time.LocalDateTime.now()
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
            val millisUntilMidnight = java.time.Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1000L)
            kotlinx.coroutines.delay(millisUntilMidnight)
            adaptiveToday = java.time.LocalDate.now()
        }
    }
    val adaptiveVariant = if (adaptiveEnabled) {
        AdaptiveAppearance.resolve(feedbackLogsForAppearance, today = adaptiveToday)
    } else {
        AppearanceVariant.BASE
    }

    NeuromindTheme(darkTheme = useDarkTheme, adaptiveVariant = adaptiveVariant) {
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
        // Hidden while the keyboard is up: Scaffold reserves bottom space for
        // this bar regardless of IME visibility, so leaving it "shown" (even
        // though the keyboard now covers it) left a dead reserved gap between
        // input fields and the keyboard on screens like Assistant/Feedback.
        val imeVisible = WindowInsets.isImeVisible
        val showBottomNav = navBackStackEntry?.destination?.route in BOTTOM_NAV_ROUTES && !imeVisible

        LaunchedEffect(pendingRouteRequest) {
            val request = pendingRouteRequest ?: return@LaunchedEffect
            if (request.route == Screen.Timetable.route) {
                // Event reminder tap: open the Timetable, then the event itself.
                navController.navigateToTab("${Screen.TaskList.route}/false?openTimetable=true")
                request.entryId?.let { navController.navigate("${Screen.EntryDetails.route}/$it") }
            } else if (request.route == Screen.Feedback.route) {
                val encodedPrefill = Uri.encode(request.prefillNote ?: "")
                navController.navigate(
                    "${Screen.Feedback.route}?autoVoice=${request.autoStartVoiceCapture}&launchToken=${request.launchToken}&prefill=$encodedPrefill"
                )
            }
            onPendingRouteHandled()
        }

        Scaffold(
            // System bars are applied once, by contentInsets below. Left at the
            // default (safeDrawing), innerPadding carried the status bar too, so
            // every screen's top was pushed down by it twice.
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showBottomNav) {
                    BottomNavBar(navController = navController)
                }
            }
        ) { innerPadding ->
            // When the bottom nav capsule is showing, it already reserves and
            // consumes the navigationBars inset itself (see BottomNavBar's own
            // windowInsetsPadding), and innerPadding.bottom already reflects
            // its full measured height. Applying safeDrawing here too would
            // double-pad the bottom on every bottom-nav screen — most visible
            // as an oversized gap above the keyboard on screens that also use
            // imePadding() (e.g. Assistant).
            val contentInsets = if (showBottomNav) {
                WindowInsets.safeDrawing.exclude(WindowInsets.ime).exclude(WindowInsets.navigationBars)
            } else {
                WindowInsets.safeDrawing.exclude(WindowInsets.ime)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .windowInsetsPadding(contentInsets)
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
                                navController.navigate("${Screen.TaskDetails.route}/$taskId")
                            },
                            onNavigateToFocus = { taskId -> navController.navigate(Screen.FocusMode.route + "/$taskId") },
                            onNavigateToRevision = { navController.navigate(Screen.Revision.route) },
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
                            onTaskClicked = { task ->
                                navController.navigate("${Screen.TaskDetails.route}/${task.id}")
                            },
                            onEntryClicked = { entry ->
                                navController.navigate("${Screen.EntryDetails.route}/${entry.id}")
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
                        route = Screen.TaskDetails.route + "/{taskId}",
                        arguments = listOf(navArgument("taskId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val taskId = backStackEntry.arguments?.getInt("taskId") ?: -1
                        val vm = viewModel<TaskDetailsViewModel>(factory = DetailsViewModelFactory(repository, taskId))
                        TaskDetailsScreen(
                            viewModel = vm,
                            onNavigateUp = { navController.navigateUp() },
                            onEdit = { navController.navigate(Screen.AddEditTask.route + "?taskId=${it.id}") },
                            onFocus = { navController.navigate(Screen.FocusMode.route + "/${it.id}") }
                        )
                    }

                    composable(
                        route = Screen.EntryDetails.route + "/{entryId}",
                        arguments = listOf(navArgument("entryId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val entryId = backStackEntry.arguments?.getInt("entryId") ?: -1
                        val vm = viewModel<EntryDetailsViewModel>(factory = DetailsViewModelFactory(repository, entryId))
                        // Weekly classes edit in the Timetable's class dialog, shown over this screen.
                        val classEditor = viewModel<TimetableViewModel>(factory = timetableViewModelFactory)
                        TimetableEntryEditorDialog(viewModel = classEditor)
                        EntryDetailsScreen(
                            viewModel = vm,
                            onNavigateUp = { navController.navigateUp() },
                            onEditEvent = { navController.navigate(Screen.AddEditTask.route + "?eventId=${it.id}") },
                            onEditClass = { classEditor.openEditEntry(it) }
                        )
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

                    composable(Screen.Revision.route) {
                        val vm = viewModel<com.alvin.neuromind.ui.revision.RevisionViewModel>(
                            factory = com.alvin.neuromind.ui.revision.RevisionViewModelFactory(repository)
                        )
                        com.alvin.neuromind.ui.revision.RevisionScreen(
                            viewModel = vm,
                            onNavigateUp = { navController.navigateUp() },
                            onOpenExam = { navController.navigate("${Screen.EntryDetails.route}/$it") }
                        )
                    }

                    composable(Screen.Insights.route) {
                        val factory = InsightsViewModelFactory(repository, userPreferencesRepository)
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
        NavItem(Screen.Dashboard, "Home", Icons.Outlined.Home, Icons.Filled.Home),
        NavItem(Screen.TaskList, "Tasks", Icons.AutoMirrored.Outlined.List, Icons.AutoMirrored.Filled.List),
        NavItem(Screen.Assistant, "Ask", Icons.AutoMirrored.Outlined.Chat, Icons.AutoMirrored.Filled.Chat),
        NavItem(Screen.Insights, "Insights", Icons.Outlined.BarChart, Icons.Filled.BarChart),
        NavItem(Screen.Settings, "Settings", Icons.Outlined.Settings, Icons.Filled.Settings)
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Calm Nav: an opaque, inset Organic capsule instead of a stock edge-to-edge
    // Material bar — matches the pill language used by PillButton/PillChip/
    // SegmentedToggle elsewhere, with a solid selected-pill indicator instead
    // of Material's default ripple-only selection cue.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Same side insets as the content above (camera cutout in
            // landscape), so the capsule lines up with the cards.
            // Sides and bottom only: the display cutout (punch-hole camera) has a
            // top inset as tall as the status bar, which padded a band of empty
            // space above the capsule on every tab.
            .windowInsetsPadding(
                WindowInsets.navigationBars.union(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
            )
            .padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 10.dp)
            .clip(Pill)
            .background(MaterialTheme.colorScheme.surface)
            .selectableGroup()
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        navItems.forEach { item ->
            val routeToCheck = when (item.screen) {
                Screen.TaskList -> Screen.TaskList.route + "/{isRescheduleMode}?openTimetable={openTimetable}"
                else -> item.screen.route
            }
            val isSelected = currentDestination?.hierarchy?.any { it.route == routeToCheck } == true

            val iconScale by animateFloatAsState(
                targetValue = if (isSelected) 1.1f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "nav_scale_${item.label}"
            )
            val indicatorColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                label = "nav_indicator_${item.label}"
            )
            val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(Pill)
                    .background(indicatorColor)
                    .selectable(
                        selected = isSelected,
                        onClick = {
                            val targetRoute = if (item.screen == Screen.TaskList) Screen.TaskList.withArgs(false) else item.screen.route
                            navController.navigateToTab(targetRoute)
                        },
                        role = Role.Tab
                    )
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    if (isSelected) item.selectedIcon else item.icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp).scale(iconScale)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.label,
                    maxLines = 1,
                    softWrap = false,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor
                )
            }
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
