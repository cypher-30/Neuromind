package com.alvin.neuromind.ui.feedback

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
    onFeedbackSubmitted: () -> Unit
) {
    var selectedMood by remember { mutableStateOf<Mood?>(null) }
    var energyLevel by remember { mutableFloatStateOf(5f) }
    var comment by remember { mutableStateOf("") }

    // Check if the callback is effectively "go back" (from settings) or "finish" (from dashboard)
    // For UI purposes, we assume we always want a back button if we are in a full screen flow.

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Review") },
                navigationIcon = {
                    IconButton(onClick = onFeedbackSubmitted) { // Acts as Back
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text("How are you feeling?", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Mood.entries.forEach { mood ->
                        FilterChip(
                            selected = selectedMood == mood,
                            onClick = { selectedMood = mood },
                            label = { Text(mood.name.take(1) + mood.name.drop(1).lowercase()) }
                        )
                    }
                }
            }

            item {
                Text("Energy Level: ${energyLevel.toInt()}/10", style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = energyLevel,
                    onValueChange = { energyLevel = it },
                    valueRange = 1f..10f,
                    steps = 8
                )
            }

            item {
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Notes on your day") },
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                )
            }

            item {
                Button(
                    onClick = {
                        if (selectedMood != null) {
                            // FIXED: Added '0' as the third argument for tasksCompleted
                            viewModel.submitFeedback(selectedMood!!, energyLevel.toInt(), 0, comment)
                            onFeedbackSubmitted()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedMood != null
                ) {
                    Text("Save Review")
                }
            }
        }
    }
}