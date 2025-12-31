package com.alvin.neuromind.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alvin.neuromind.data.Task
import com.alvin.neuromind.ui.theme.NeuromindTheme
import java.util.UUID

@Composable
fun ActionableOverdueCard(
    modifier: Modifier = Modifier,
    overdueTasks: List<Task>,
    onRescheduleClick: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // 1. Header Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = "Warning",
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Missed Deadlines",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. List of first few overdue tasks
            Text(
                text = "You have ${overdueTasks.size} overdue task(s):",
                style = MaterialTheme.typography.bodyMedium
            )
            Column(modifier = Modifier.padding(start = 16.dp, top = 4.dp)) {
                overdueTasks.take(3).forEach { task ->
                    Text(
                        text = "• ${task.title}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 3. Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onRescheduleClick) {
                    Text(
                        text = "Reschedule",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun ActionableOverdueCardPreview() {
    val sampleTasks = listOf(
        Task(id = UUID.randomUUID(), title = "Finish Biology Report", description = null, dueDate = System.currentTimeMillis() - 86400000),
        Task(id = UUID.randomUUID(), title = "Review Chapter 4", description = null, dueDate = System.currentTimeMillis() - 172800000),
        Task(id = UUID.randomUUID(), title = "Plan Project Phase 2", description = null, dueDate = System.currentTimeMillis() - 259200000)
    )
    NeuromindTheme {
        ActionableOverdueCard(
            modifier = Modifier.padding(16.dp),
            overdueTasks = sampleTasks,
            onRescheduleClick = {}
        )
    }
}