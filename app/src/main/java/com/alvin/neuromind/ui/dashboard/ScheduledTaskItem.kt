package com.alvin.neuromind.ui.dashboard

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alvin.neuromind.data.Difficulty
import com.alvin.neuromind.data.Priority
import com.alvin.neuromind.data.Task
import com.alvin.neuromind.domain.TimeSlot
import java.time.LocalTime

@Composable
fun ScheduledTaskItem(
    timeSlot: TimeSlot,
    task: Task
) {
    Card(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(modifier = Modifier.padding(12.dp)) {
            Text(
                "${timeSlot.start} - ${timeSlot.end}",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(task.title)
        }
    }
}

@Preview
@Composable
fun PreviewScheduledItem() {
    // FIXED: Using Int ID (0) and providing all required fields
    val task = Task(
        id = 1,
        title = "Study Math",
        description = "Algebra",
        dueDate = System.currentTimeMillis(),
        priority = Priority.HIGH,
        difficulty = Difficulty.MEDIUM
    )
    val slot = TimeSlot(LocalTime.of(9, 0), LocalTime.of(10, 0))
    ScheduledTaskItem(timeSlot = slot, task = task)
}