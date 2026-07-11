package com.alvin.neuromind.ui.feedback

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alvin.neuromind.data.FeedbackLog
import com.alvin.neuromind.data.Mood
import com.alvin.neuromind.ui.components.NeuromindTopBar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FeedbackScreen(
    viewModel: FeedbackViewModel,
    onFeedbackSubmitted: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedMood by remember { mutableStateOf(Mood.NEUTRAL) }
    var energyLevel by remember { mutableFloatStateOf(3f) }
    var tasksCompleted by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }

    LaunchedEffect(uiState.isSubmitted) {
        if (uiState.isSubmitted) onFeedbackSubmitted()
    }

    Scaffold(
        topBar = {
            NeuromindTopBar(title = "Daily Review", onNavigateBack = onNavigateBack)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("How are you feeling?", style = MaterialTheme.typography.titleMedium)

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Mood.entries.forEach { mood ->
                    FilterChip(
                        selected = selectedMood == mood,
                        onClick = { selectedMood = mood },
                        label = { Text(mood.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Text("Energy Level: ${energyLevel.toInt()}/5", style = MaterialTheme.typography.titleMedium)
            Slider(value = energyLevel, onValueChange = { energyLevel = it }, valueRange = 1f..5f, steps = 3)

            OutlinedTextField(
                value = tasksCompleted,
                onValueChange = { tasksCompleted = it },
                label = { Text("Tasks Completed Today") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text("Additional Thoughts") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Button(
                onClick = {
                    if (!uiState.isSubmitted) {
                        viewModel.submitFeedback(
                            FeedbackLog(
                                mood = selectedMood,
                                energyLevel = energyLevel.toInt(),
                                tasksCompleted = tasksCompleted.toIntOrNull() ?: 0,
                                comment = comment.takeIf { it.isNotBlank() }
                            )
                        )
                    }
                },
                enabled = !uiState.isSubmitted,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Submit Review")
            }
        }
    }
}