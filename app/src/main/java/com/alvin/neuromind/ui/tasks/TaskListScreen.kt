package com.alvin.neuromind.ui.tasks

import com.alvin.neuromind.ui.components.OrganicConfirmDialog
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alvin.neuromind.data.Task
import com.alvin.neuromind.ui.components.CircleIconButton
import com.alvin.neuromind.ui.components.EmptyState
import com.alvin.neuromind.ui.components.OrganicCard
import com.alvin.neuromind.ui.components.PillButton
import com.alvin.neuromind.ui.components.PillChip
import com.alvin.neuromind.ui.components.PillTextField
import com.alvin.neuromind.ui.components.SegmentedToggle
import com.alvin.neuromind.ui.theme.Dimens
import com.alvin.neuromind.ui.theme.Pill
import com.alvin.neuromind.ui.theme.priorityColor
import com.alvin.neuromind.data.TimetableEntry
import com.alvin.neuromind.ui.timetable.TimetableEntryEditorDialog
import com.alvin.neuromind.ui.timetable.TimetableAgendaView
import com.alvin.neuromind.ui.timetable.TimetableViewModel
import com.alvin.neuromind.ui.timetable.TimetableViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

data class TaskListContentState(
    val filter: TaskFilter,
    val isLoading: Boolean,
    val tasks: List<Task>,
    val allTasksForBlocking: List<Task>
)

private enum class TasksSubView { LIST, TIMETABLE }

// Timetable folds into Tasks as a List/Timetable segmented toggle instead of
// a standalone screen (see ui/timetable/TimetableScreen.kt for the agenda
// view itself). `initialTimetableView` lets the Dashboard's "Up next" tap and
// Settings' "Weekly Timetable" deep-link straight into the Timetable side.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskViewModel,
    timetableViewModelFactory: TimetableViewModelFactory,
    isRescheduleMode: Boolean,
    initialTimetableView: Boolean = false,
    onAddTaskClicked: () -> Unit,
    onAddEventClicked: () -> Unit,
    onEditTaskClicked: (Task) -> Unit,
    onEditEventClicked: (TimetableEntry) -> Unit,
    onFocusTaskClicked: (Task) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val timetableViewModel: TimetableViewModel = viewModel(factory = timetableViewModelFactory)
    var subView by rememberSaveable { mutableStateOf(if (initialTimetableView) TasksSubView.TIMETABLE else TasksSubView.LIST) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var showAddMenu by remember { mutableStateOf(false) }
    var taskPendingDeleteId by rememberSaveable { mutableStateOf<Int?>(null) }
    val taskPendingDelete = taskPendingDeleteId?.let { id -> uiState.allTasksForBlocking.find { it.id == id } }

    taskPendingDelete?.let { task ->
        OrganicConfirmDialog(
            title = "Delete task?",
            message = "\"${task.title}\" will be permanently removed. This can't be undone.",
            confirmLabel = "Delete",
            onConfirm = {
                viewModel.deleteTask(task)
                taskPendingDeleteId = null
            },
            onDismiss = { taskPendingDeleteId = null }
        )
    }

    TimetableEntryEditorDialog(viewModel = timetableViewModel)

    Scaffold(
        floatingActionButton = {
            if (!isRescheduleMode) {
                Box {
                    FloatingActionButton(
                        onClick = { if (subView == TasksSubView.LIST) onAddTaskClicked() else showAddMenu = true },
                        shape = Pill,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = if (subView == TasksSubView.LIST) "Add Task" else "Add class or event")
                    }
                    DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Weekly class") },
                            leadingIcon = { Icon(Icons.Default.Repeat, contentDescription = null) },
                            onClick = { showAddMenu = false; timetableViewModel.openNewEntry() }
                        )
                        DropdownMenuItem(
                            text = { Text("Event on a date") },
                            leadingIcon = { Icon(Icons.Default.Event, contentDescription = null) },
                            onClick = { showAddMenu = false; onAddEventClicked() }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = Dimens.ScreenPaddingHorizontal)
        ) {
            Spacer(Modifier.height(Dimens.ScreenPaddingTop - Dimens.CardGap))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isRescheduleMode) "Reschedule tasks" else "Tasks",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f)
                )
                if (!isRescheduleMode) {
                    CircleIconButton(
                        icon = Icons.Default.Search,
                        contentDescription = "Search",
                        onClick = { searchOpen = !searchOpen; if (!searchOpen) viewModel.setSearchQuery("") }
                    )
                }
            }
            Spacer(Modifier.height(Dimens.CardGap))

            if (searchOpen) {
                PillTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    placeholder = "Search tasks and timetable"
                )
                Spacer(Modifier.height(Dimens.CardGap))
            }

            if (!isRescheduleMode) {
                SegmentedToggle(
                    options = listOf("List", "Timetable"),
                    selectedIndex = subView.ordinal,
                    onSelect = { subView = TasksSubView.entries[it] }
                )
                Spacer(Modifier.height(Dimens.CardGap))
            }

            when (subView) {
                TasksSubView.TIMETABLE -> TimetableAgendaView(
                    viewModel = timetableViewModel,
                    searchQuery = uiState.searchQuery,
                    onEditEvent = onEditEventClicked,
                    modifier = Modifier.weight(1f)
                )
                TasksSubView.LIST -> Column(modifier = Modifier.weight(1f)) {
                    if (!isRescheduleMode) {
                        QuickAddTaskField(onAddTask = viewModel::quickAddTask)
                        Spacer(Modifier.height(Dimens.CardGap))
                    }

                    LazyRowFilterChips(uiState.selectedFilter, onSelect = viewModel::setFilter)
                    Spacer(Modifier.height(Dimens.CardGap))

                    AnimatedContent(
                        targetState = TaskListContentState(uiState.selectedFilter, uiState.isLoading, uiState.displayedTasks, uiState.allTasksForBlocking),
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                        label = "task_list_content"
                    ) { contentState ->
                        when {
                            contentState.isLoading -> TaskSkeletonList()
                            contentState.tasks.isEmpty() -> EmptyState(
                                message = emptyMessage(contentState.filter),
                                action = if (contentState.filter == TaskFilter.ALL) {
                                    {
                                        Box(modifier = Modifier.fillMaxWidth(0.6f)) {
                                            PillButton(text = "Add a task", onClick = onAddTaskClicked)
                                        }
                                    }
                                } else null
                            )
                            else -> TaskItemList(
                                tasks = contentState.tasks,
                                allTasksForBlocking = contentState.allTasksForBlocking,
                                viewModel = viewModel,
                                onEditTaskClicked = onEditTaskClicked,
                                onFocusTaskClicked = onFocusTaskClicked,
                                onRequestDelete = { taskPendingDeleteId = it.id }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LazyRowFilterChips(selected: TaskFilter, onSelect: (TaskFilter) -> Unit) {
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(Dimens.ChipGap)
    ) {
        items(TaskFilter.entries) { filter ->
            PillChip(
                label = filter.name.lowercase().replaceFirstChar { it.uppercase() },
                selected = selected == filter,
                onClick = { onSelect(filter) }
            )
        }
    }
}

private fun emptyMessage(filter: TaskFilter): String = when (filter) {
    TaskFilter.ALL -> "Nothing here yet."
    TaskFilter.TODAY -> "No tasks due today."
    TaskFilter.OVERDUE -> "No overdue tasks. Great work!"
    TaskFilter.UPCOMING -> "Add tasks with future due dates."
    TaskFilter.COMPLETED -> "Check off tasks to see them here."
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskItemList(
    tasks: List<Task>,
    allTasksForBlocking: List<Task>,
    viewModel: TaskViewModel,
    onEditTaskClicked: (Task) -> Unit,
    onFocusTaskClicked: (Task) -> Unit,
    onRequestDelete: (Task) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Dimens.ScreenPaddingBottomWithNav),
        verticalArrangement = Arrangement.spacedBy(Dimens.ListGap)
    ) {
        items(items = tasks, key = { it.id }) { task ->
            val isBlocked = task.prerequisiteTaskId?.let { preId ->
                allTasksForBlocking.any { it.id == preId && !it.isCompleted }
            } ?: false

            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    if (isBlocked) return@rememberSwipeToDismissBoxState false
                    when (value) {
                        SwipeToDismissBoxValue.StartToEnd -> {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.onTaskCheckedChange(task, !task.isCompleted)
                            false  // snap back — item stays, state toggles
                        }
                        SwipeToDismissBoxValue.EndToStart -> {
                            onRequestDelete(task)
                            false
                        }
                        else -> false
                    }
                },
                positionalThreshold = { it * 0.4f }
            )

            SwipeToDismissBox(
                state = dismissState,
                modifier = Modifier.clip(MaterialTheme.shapes.large),
                backgroundContent = {
                    val (color, icon, align) = when (dismissState.targetValue) {
                        SwipeToDismissBoxValue.StartToEnd -> Triple(
                            MaterialTheme.colorScheme.primaryContainer,
                            Icons.Default.CheckCircle,
                            Alignment.CenterStart
                        )
                        SwipeToDismissBoxValue.EndToStart -> Triple(
                            MaterialTheme.colorScheme.errorContainer,
                            Icons.Default.Delete,
                            Alignment.CenterEnd
                        )
                        else -> Triple(MaterialTheme.colorScheme.surface, null, Alignment.Center)
                    }
                    Box(
                        Modifier.fillMaxSize().background(color).padding(horizontal = 20.dp),
                        contentAlignment = align
                    ) {
                        icon?.let {
                            Icon(it, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }
                    }
                }
            ) {
                TaskCard(
                    task = task,
                    onClick = { onEditTaskClicked(task) },
                    onComplete = { isChecked ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.onTaskCheckedChange(task, isChecked)
                    },
                    onFocusClick = { onFocusTaskClicked(task) },
                    onDelete = { onRequestDelete(task) },
                    isBlocked = isBlocked
                )
            }
        }
    }
}

@Composable
fun TaskCard(
    task: Task,
    onClick: () -> Unit,
    onComplete: (Boolean) -> Unit,
    onFocusClick: () -> Unit,
    onDelete: () -> Unit = {},
    isBlocked: Boolean = false
) {
    val priorityColor = priorityColor(task.priority)

    OrganicCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isBlocked) { onClick() },
        color = when {
            task.isCompleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            isBlocked -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            else -> MaterialTheme.colorScheme.surface
        }
    ) {
        Row(verticalAlignment = Alignment.Top) {
            val checkColor = if (isBlocked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .then(
                        if (task.isCompleted) Modifier.background(checkColor)
                        else Modifier.border(2.dp, MaterialTheme.colorScheme.outline, androidx.compose.foundation.shape.CircleShape)
                    )
                    .clickable(enabled = !isBlocked) { onComplete(!task.isCompleted) },
                contentAlignment = Alignment.Center
            ) {
                if (task.isCompleted) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isBlocked) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Blocked",
                            modifier = Modifier.size(12.dp).padding(end = 4.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                                else if (isBlocked) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurface
                    )
                }
                if (!task.description.isNullOrBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isBlocked) 0.5f else 1f),
                        maxLines = 2
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                TaskMetaRow(task = task, priorityColor = priorityColor, isBlocked = isBlocked)
            }
            if (!task.isCompleted && !isBlocked) {
                IconButton(onClick = onFocusClick) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = "Start Focus Mode",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskMetaRow(task: Task, priorityColor: androidx.compose.ui.graphics.Color, isBlocked: Boolean) {
    FlowRow(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (task.dueDate != null) {
            val fmt = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
            Text(
                text = fmt.format(Date(task.dueDate)),
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    task.isCompleted -> MaterialTheme.colorScheme.onSurfaceVariant
                    task.dueDate < System.currentTimeMillis() -> MaterialTheme.colorScheme.error
                    isBlocked -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        PriorityPill(task, priorityColor)
        if (!task.subject.isNullOrBlank()) {
            SubjectPill(task.subject)
        }
    }
}

@Composable
private fun PriorityPill(task: Task, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .clip(Pill)
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = task.priority.name.lowercase().replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun SubjectPill(subject: String) {
    Box(
        modifier = Modifier
            .clip(Pill)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = subject,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun TaskSkeletonList() {
    val alpha by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0.15f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "shimmer_alpha"
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Dimens.ScreenPaddingBottomWithNav),
        verticalArrangement = Arrangement.spacedBy(Dimens.ListGap)
    ) {
        items(6) {
            OrganicCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(22.dp).clip(MaterialTheme.shapes.extraSmall)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            Modifier.fillMaxWidth(0.6f).height(15.dp)
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha))
                        )
                        Box(
                            Modifier.fillMaxWidth(0.35f).height(11.dp)
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickAddTaskField(onAddTask: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    PillTextField(
        value = text,
        onValueChange = { text = it },
        placeholder = "Quick add — try \"Essay due Friday 3pm\"",
        leadingIcon = Icons.Default.AutoAwesome,
        trailingIcon = if (text.isNotBlank()) {
            {
                IconButton(onClick = { onAddTask(text); text = "" }) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Add Task")
                }
            }
        } else null,
        onImeAction = { if (text.isNotBlank()) { onAddTask(text); text = "" } }
    )
}
