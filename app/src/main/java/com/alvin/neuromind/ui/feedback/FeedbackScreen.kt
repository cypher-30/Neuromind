package com.alvin.neuromind.ui.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvin.neuromind.data.Mood

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    viewModel: FeedbackViewModel,
    onFeedbackSubmitted: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var selectedMood by remember { mutableStateOf<Mood?>(null) }
    var energyLevel by remember { mutableFloatStateOf(3f) }
    var comment by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Reflection") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp), // Increased outer padding
            verticalArrangement = Arrangement.spacedBy(32.dp) // More breathing room between sections
        ) {
            // Header Text
            Text(
                "Let's wrap up the day.",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // 1. MOOD SECTION
            Column {
                SectionLabel("How are you feeling?", Icons.Default.SentimentSatisfiedAlt)
                Spacer(Modifier.height(16.dp))

                // Custom Emoji-like Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Mood.entries.forEach { mood ->
                        MoodItem(
                            mood = mood,
                            isSelected = selectedMood == mood,
                            onSelect = { selectedMood = mood }
                        )
                    }
                }
            }

            // 2. ENERGY SECTION
            Column {
                SectionLabel("Energy Level", Icons.Default.Bolt)
                Spacer(Modifier.height(16.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Drained", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "${energyLevel.toInt()}/5",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("Charged", style = MaterialTheme.typography.bodyMedium)
                        }
                        Slider(
                            value = energyLevel,
                            onValueChange = { energyLevel = it },
                            valueRange = 1f..5f,
                            steps = 3,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            // 3. NOTES SECTION
            Column {
                SectionLabel("Quick Notes", null)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    placeholder = { Text("What went well? Any blockers?") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            // SUBMIT BUTTON
            Button(
                onClick = {
                    if (selectedMood != null) {
                        viewModel.submitFeedback(selectedMood!!, energyLevel.toInt(), 0, comment)
                        onFeedbackSubmitted()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = selectedMood != null
            ) {
                Text("Save Check-in", fontSize = 18.sp)
            }

            Spacer(Modifier.height(24.dp)) // Bottom padding
        }
    }
}

@Composable
fun SectionLabel(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun MoodItem(mood: Mood, isSelected: Boolean, onSelect: () -> Unit) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer

    // Fix: Explicitly handle the click without relying on implicit Indication
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onSelect) // Moved clickable AFTER background
            .padding(12.dp)
    ) {
        Text(
            text = when(mood) {
                Mood.GREAT -> "🤩"
                Mood.GOOD -> "🙂"
                Mood.NEUTRAL -> "😐"
                Mood.TIRED -> "😴"
                Mood.STRESSED -> "😫"
            },
            fontSize = 32.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = mood.name.lowercase().replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}