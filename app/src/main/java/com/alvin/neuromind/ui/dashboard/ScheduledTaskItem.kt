package com.alvin.neuromind.ui.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alvin.neuromind.data.Difficulty
import com.alvin.neuromind.data.Task
import com.alvin.neuromind.domain.TimeSlot
import com.alvin.neuromind.ui.theme.NeuromindTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Composable
fun ScheduledTaskItem(
    modifier: Modifier = Modifier,
    slot: TimeSlot,
    task: Task,
    onClick: () -> Unit = {}
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }

    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Column for Time
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(70.dp)
            ) {
                Text(
                    text = slot.start.format(timeFormatter),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "|",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = slot.end.format(timeFormatter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            // Vertical Divider
            HorizontalDivider(
                modifier = Modifier
                    .height(40.dp)
                    .width(1.dp)
            )

            // Column for Task Details
            Column(
                modifier = Modifier.padding(start = 16.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                task.description?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun ScheduledTaskItemPreview() {
    val sampleTask = Task(
        id = UUID.randomUUID(),
        title = "Draft Chapter 1 of Thesis",
        description = "Focus on the introduction and literature review sections.",
        dueDate = null, // <-- FIX: Added missing parameter
        difficulty = Difficulty.HARD
    )
    val sampleSlot = TimeSlot(
        start = LocalTime.of(14, 0),
        end = LocalTime.of(15, 30)
    )

    NeuromindTheme {
        ScheduledTaskItem(
            modifier = Modifier.padding(16.dp),
            slot = sampleSlot,
            task = sampleTask
        )
    }
}