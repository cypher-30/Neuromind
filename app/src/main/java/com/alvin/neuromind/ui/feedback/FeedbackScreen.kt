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
    onNavigateBack: () -> Unit // *** NEW BACK BUTTON PARAMETER ***
) {
    var selectedMood by remember { mutableStateOf<Mood?>(null) }
    var energyLevel by remember { mutableFloatStateOf(5f) }
    var comment by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("End-of-Day Review") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
            // MOOD SECTION
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("How was your day?", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Mood.entries.forEach { mood ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val isSelected = selectedMood == mood
                                    InputChip(
                                        selected = isSelected,
                                        onClick = { selectedMood = mood },
                                        label = { Text(mood.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                        colors = InputChipDefaults.inputChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ENERGY SECTION
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Energy Level", style = MaterialTheme.typography.titleMedium)
                            Text("${energyLevel.toInt()}/10", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = energyLevel,
                            onValueChange = { energyLevel = it },
                            valueRange = 1f..10f,
                            steps = 8
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Exhausted", style = MaterialTheme.typography.labelSmall)
                            Text("Energetic", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // NOTES SECTION
            item {
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Daily Reflection") },
                    placeholder = { Text("What went well? What didn't?") },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    shape = MaterialTheme.shapes.medium
                )
            }

            // SAVE BUTTON
            item {
                Button(
                    onClick = {
                        if (selectedMood != null) {
                            viewModel.submitFeedback(selectedMood!!, energyLevel.toInt(), 0, comment)
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = selectedMood != null
                ) {
                    Text("Save Review")
                }
            }
        }
    }
}