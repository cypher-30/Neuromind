package com.alvin.neuromind.ui.tasks

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alvin.neuromind.data.Priority
import com.alvin.neuromind.data.Task
import com.alvin.neuromind.ui.components.NeuromindTopBar
import com.alvin.neuromind.ui.theme.priorityColor
import java.text.SimpleDateFormat
import java.util.*

data class TaskListContentState(
    val filter: TaskFilter,
    val isLoading: Boolean,
    val tasks: List<Task>,
    val allTasksForBlocking: List<Task>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskViewModel,
    isRescheduleMode: Boolean,
    onAddTaskClicked: () -> Unit,
    onEditTaskClicked: (Task) -> Unit,
    onFocusTaskClicked: (Task) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            NeuromindTopBar(title = if (isRescheduleMode) "Reschedule Tasks" else "Tasks")
        },
        floatingActionButton = {
            if (!isRescheduleMode) {
                FloatingActionButton(onClick = onAddTaskClicked) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task")
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (!isRescheduleMode) {
                QuickAddTaskField(
                    onAddTask = { input ->
                        viewModel.quickAddTask(input)
                    }
                )
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(TaskFilter.entries) { filter ->
                    FilterChip(
                        selected = uiState.selectedFilter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        label = { Text(filter.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            AnimatedContent(
                targetState = TaskListContentState(uiState.selectedFilter, uiState.isLoading, uiState.displayedTasks, uiState.allTasksForBlocking),
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                label = "task_list_content"
            ) { contentState ->
                when {
                    contentState.isLoading -> TaskSkeletonList()
                    contentState.tasks.isEmpty() -> EmptyState(contentState.filter)
                    else -> TaskItemList(contentState.tasks, contentState.allTasksForBlocking, viewModel, onEditTaskClicked, onFocusTaskClicked)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskItemList(
    tasks: List<Task>,
    allTasksForBlocking: List<Task>,
    viewModel: TaskViewModel,
    onEditTaskClicked: (Task) -> Unit,
    onFocusTaskClicked: (Task) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            viewModel.deleteTask(task)
                            true   // item is removed from the list
                        }
                        else -> false
                    }
                },
                positionalThreshold = { it * 0.4f }
            )

            SwipeToDismissBox(
                state = dismissState,
                modifier = Modifier.clip(MaterialTheme.shapes.medium),
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
    isBlocked: Boolean = false
) {
    val priorityColor = priorityColor(task.priority)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isBlocked) { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                task.isCompleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                isBlocked -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(if (isBlocked) priorityColor.copy(alpha = 0.3f) else priorityColor)
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = if (isBlocked) null else onComplete,
                    enabled = !isBlocked,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isBlocked) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Blocked",
                                modifier = Modifier.size(14.dp).padding(end = 4.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                            color = if (isBlocked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
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
                    if (task.dueDate != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val fmt = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
                        Text(
                            text = fmt.format(Date(task.dueDate)),
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                task.isCompleted -> MaterialTheme.colorScheme.outline
                                task.dueDate < System.currentTimeMillis() -> MaterialTheme.colorScheme.error
                                isBlocked -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                else -> MaterialTheme.colorScheme.outline
                            }
                        )
                    }
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
            }
        }
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(6) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
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
fun EmptyState(filter: TaskFilter = TaskFilter.ALL) {
    val (icon, title, subtitle) = when (filter) {
        TaskFilter.ALL      -> Triple(Icons.Default.CheckCircleOutline, "All clear!", "No pending tasks. Tap + to add one.")
        TaskFilter.TODAY    -> Triple(Icons.Default.Today,              "Free today",       "No tasks due today.")
        TaskFilter.OVERDUE  -> Triple(Icons.Default.TaskAlt,           "You're on track!", "No overdue tasks. Great work!")
        TaskFilter.UPCOMING -> Triple(Icons.Default.EventAvailable,    "Nothing upcoming", "Add tasks with future due dates.")
        TaskFilter.COMPLETED-> Triple(Icons.Default.EmojiEvents,       "No completions yet","Check off tasks to see them here.")
    }
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun QuickAddTaskField(onAddTask: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text  = "Natural language — try \"Essay due Friday 3pm\"",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Add a task…") },
            leadingIcon = {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = "Smart add",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                if (text.isNotBlank()) {
                    IconButton(onClick = {
                        onAddTask(text)
                        text = ""
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Add Task")
                    }
                }
            },
            shape = MaterialTheme.shapes.medium,
            singleLine = true
        )
    }
}
