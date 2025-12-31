package com.alvin.neuromind.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alvin.neuromind.data.Difficulty
import com.alvin.neuromind.data.Priority
import com.alvin.neuromind.data.Task

// If you are not using this in the new Dashboard, you can actually DELETE this file.
// But here is the fix if you want to keep it.

@Composable
fun ActionableOverdueCard(
    tasks: List<Task>,
    onRescheduleClick: () -> Unit
) {
    if (tasks.isEmpty()) return

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "You have ${tasks.size} overdue tasks!",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onRescheduleClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onErrorContainer,
                    contentColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text("Reschedule Now")
            }
        }
    }
}

@Preview
@Composable
fun PreviewActionableCard() {
    // FIXED: Using Int ID (0) and providing all required fields
    val dummyTasks = listOf(
        Task(
            id = 1,
            title = "Task 1",
            description = null,
            dueDate = 0L,
            priority = Priority.HIGH,
            difficulty = Difficulty.HARD
        ),
        Task(
            id = 2,
            title = "Task 2",
            description = null,
            dueDate = 0L,
            priority = Priority.MEDIUM,
            difficulty = Difficulty.MEDIUM
        )
    )
    ActionableOverdueCard(tasks = dummyTasks, onRescheduleClick = {})
}