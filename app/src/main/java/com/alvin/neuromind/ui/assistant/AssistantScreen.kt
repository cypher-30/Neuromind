package com.alvin.neuromind.ui.assistant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alvin.neuromind.domain.AssistantAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel,
    onNavigateToFocus: (Int) -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToTimetable: () -> Unit,
    onNavigateToInsights: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Handle navigation side-effects
    LaunchedEffect(uiState.pendingNavigation) {
        uiState.pendingNavigation?.let { nav ->
            when (nav.action) {
                AssistantAction.NAVIGATE_FOCUS     -> nav.taskId?.let { onNavigateToFocus(it) }
                AssistantAction.NAVIGATE_TASKS     -> onNavigateToTasks()
                AssistantAction.NAVIGATE_TIMETABLE -> onNavigateToTimetable()
                AssistantAction.NAVIGATE_INSIGHTS  -> onNavigateToInsights()
            }
            viewModel.clearNavigation()
        }
    }

    // Scroll to bottom whenever messages change
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Ask Neuromind") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Chat area
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Example prompt chips on empty state
                if (uiState.messages.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                "Hi! I'm Neuromind. Ask me anything about your schedule or tasks.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Try asking:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            listOf(
                                "What's on today?",
                                "What should I do next?",
                                "Add task: essay due Friday 3pm",
                                "How am I doing?",
                                "When am I free?",
                                "I'm tired"
                            ).forEach { prompt ->
                                SuggestionChip(
                                    onClick = { viewModel.onInputChange(prompt); viewModel.sendMessage() },
                                    label = { Text(prompt) }
                                )
                            }
                        }
                    }
                }

                items(uiState.messages, key = { it.id }) { message ->
                    ChatBubble(message = message)
                }

                if (uiState.isThinking) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp, 12.dp, 12.dp, 4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(end = 64.dp)
                            ) {
                                Text(
                                    "…",
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.inputText,
                    onValueChange = viewModel::onInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask Neuromind…") },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp)
                )
                IconButton(
                    onClick = viewModel::sendMessage,
                    enabled = uiState.inputText.isNotBlank()
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (uiState.inputText.isNotBlank())
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.isUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = if (isUser)
                RoundedCornerShape(12.dp, 4.dp, 12.dp, 12.dp)
            else
                RoundedCornerShape(4.dp, 12.dp, 12.dp, 12.dp),
            color = if (isUser)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surfaceVariant,
            modifier = if (isUser) Modifier.padding(start = 64.dp) else Modifier.padding(end = 64.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
