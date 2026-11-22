package com.alvin.neuromind.ui.assistant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alvin.neuromind.domain.AssistantAction
import com.alvin.neuromind.ui.components.CircleIconButton
import com.alvin.neuromind.ui.components.OrganicCard
import com.alvin.neuromind.ui.components.PillChip
import com.alvin.neuromind.ui.components.PillTextField
import com.alvin.neuromind.ui.theme.Dimens
import com.alvin.neuromind.ui.theme.Elevation

@OptIn(ExperimentalLayoutApi::class)
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
    val onAction: (PendingNav) -> Unit = { nav ->
        when (nav.action) {
            AssistantAction.NAVIGATE_FOCUS     -> nav.taskId?.let { onNavigateToFocus(it) }
            AssistantAction.NAVIGATE_TASKS     -> onNavigateToTasks()
            AssistantAction.NAVIGATE_TIMETABLE -> onNavigateToTimetable()
            AssistantAction.NAVIGATE_INSIGHTS  -> onNavigateToInsights()
        }
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.ScreenPaddingHorizontal)
            .padding(top = Dimens.ScreenPaddingTop)
            .imePadding()
    ) {
        Text("Ask Neuromind", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "What's on today?",
                                "What should I do next?",
                                "Add task: essay due Friday 3pm",
                                "How am I doing?",
                                "When am I free?",
                                "I'm tired"
                            ).forEach { prompt ->
                                PillChip(
                                    label = prompt,
                                    selected = false,
                                    onClick = { viewModel.onInputChange(prompt); viewModel.sendMessage() }
                                )
                            }
                        }
                    }
                }
            }

            items(uiState.messages, key = { it.id }) { message ->
                ChatBubble(message = message, onAction = onAction)
            }

            if (uiState.isThinking) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        OrganicCard(
                            modifier = Modifier.padding(end = 64.dp),
                            elevation = Elevation.Small,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text("…", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        ComposerBar(
            text = uiState.inputText,
            onTextChange = viewModel::onInputChange,
            onSend = { if (uiState.inputText.isNotBlank()) viewModel.sendMessage() },
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
        )
    }
}

/**
 * Message bar: one rounded surface holding the text and the send button, like
 * a chat app. Grows to 4 lines; Send is muted until there's something to send.
 */
@Composable
private fun ComposerBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canSend = text.isNotBlank()
    androidx.compose.material3.Surface(
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(start = 20.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 11.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                maxLines = 4,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Send
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSend = { onSend() }),
                decorationBox = { inner ->
                    Box {
                        if (text.isEmpty()) {
                            Text(
                                "Message Neuromind",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        inner()
                    }
                }
            )
            Spacer(Modifier.width(8.dp))
            CircleIconButton(
                icon = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                onClick = onSend,
                size = Dimens.CircleButtonSizeLarge,
                background = if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                tint = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, onAction: (PendingNav) -> Unit) {
    val isUser = message.isUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        OrganicCard(
            shape = MaterialTheme.shapes.medium,
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            elevation = 0.dp,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            modifier = if (isUser) Modifier.padding(start = 64.dp) else Modifier.padding(end = 64.dp)
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
            message.action?.let { nav ->
                Spacer(Modifier.height(10.dp))
                PillChip(
                    label = actionLabel(nav),
                    selected = true,
                    onClick = { onAction(nav) }
                )
            }
        }
    }
}

private fun actionLabel(nav: PendingNav): String = when (nav.action) {
    AssistantAction.NAVIGATE_TIMETABLE -> "Open Timetable"
    AssistantAction.NAVIGATE_TASKS     -> "Open Tasks"
    AssistantAction.NAVIGATE_FOCUS     -> "Start focus"
    AssistantAction.NAVIGATE_INSIGHTS  -> "Open Insights"
}
