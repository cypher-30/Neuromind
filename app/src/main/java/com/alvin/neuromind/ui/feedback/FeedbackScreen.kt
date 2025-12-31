package com.alvin.neuromind.ui.feedback

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alvin.neuromind.data.Mood

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    viewModel: FeedbackViewModel,
    // A function to call when feedback is submitted, to navigate back.
    onFeedbackSubmitted: () -> Unit
) {
    var selectedMood by remember { mutableStateOf(Mood.NEUTRAL) }
    var energyLevel by remember { mutableStateOf(3f) } // Use a float for the slider
    var comment by remember { mutableStateOf("") }

    // This would eventually come from the ViewModel, but is hardcoded for now.
    val tasksCompletedToday = 5

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("End-of-Day Review", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 24.dp))

        // Mood Selection
        Text("How did you feel today?", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        SegmentedButtonRow(
            options = Mood.values().toList(),
            selectedOption = selectedMood,
            onOptionSelect = { selectedMood = it }
        )
        Spacer(Modifier.height(24.dp))

        // Energy Level Slider
        Text("What was your energy level?", style = MaterialTheme.typography.titleMedium)
        Slider(
            value = energyLevel,
            onValueChange = { energyLevel = it },
            valueRange = 1f..5f,
            steps = 3 // This creates 5 discrete steps (1, 2, 3, 4, 5)
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Low", style = MaterialTheme.typography.bodySmall)
            Text("High", style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(24.dp))

        // Comments Section
        Text("Any additional comments?", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it },
            label = { Text("Optional comments...") },
            modifier = Modifier.fillMaxWidth().height(120.dp)
        )
        Spacer(Modifier.height(32.dp))

        // Submit Button
        Button(
            onClick = {
                viewModel.submitFeedback(
                    mood = selectedMood,
                    energyLevel = energyLevel.toInt(),
                    tasksCompleted = tasksCompletedToday,
                    comment = comment.takeIf { it.isNotBlank() }
                )
                // Navigate back after submitting
                onFeedbackSubmitted()
            },
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Submit Review")
        }
    }
}

// A generic Composable for our segmented buttons, reused from TaskListScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SegmentedButtonRow(
    options: List<T>,
    selectedOption: T,
    onOptionSelect: (T) -> Unit
) where T : Enum<T> {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                onClick = { onOptionSelect(option) },
                selected = (option == selectedOption)
            ) {
                Text(option.name.lowercase().replaceFirstChar { it.titlecase() })
            }
        }
    }
}
