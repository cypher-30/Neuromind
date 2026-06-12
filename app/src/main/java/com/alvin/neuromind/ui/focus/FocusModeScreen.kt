package com.alvin.neuromind.ui.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alvin.neuromind.data.Task
import kotlinx.coroutines.delay

private enum class TimerState { Idle, Running, Paused }

@Composable
fun FocusModeScreen(task: Task, onFinish: () -> Unit) {
    val totalSeconds = 25 * 60
    var secondsLeft by rememberSaveable { mutableIntStateOf(totalSeconds) }
    var timerState by rememberSaveable { mutableStateOf(TimerState.Idle) }
    var showDoneDialog by remember { mutableStateOf(false) }

    LaunchedEffect(timerState) {
        if (timerState == TimerState.Running) {
            while (secondsLeft > 0) {
                delay(1000L)
                secondsLeft--
            }
            timerState = TimerState.Idle
            showDoneDialog = true
        }
    }

    if (showDoneDialog) {
        AlertDialog(
            onDismissRequest = { showDoneDialog = false },
            title = { Text("Session Complete!") },
            text = { Text("Great work on \"${task.title}\". Take a short break before the next round.") },
            confirmButton = {
                Button(onClick = { showDoneDialog = false; onFinish() }) { Text("Done") }
            },
            dismissButton = {
                TextButton(onClick = {
                    secondsLeft = totalSeconds
                    showDoneDialog = false
                }) { Text("Another round") }
            }
        )
    }

    val progress = secondsLeft.toFloat() / totalSeconds.toFloat()
    val minutes = secondsLeft / 60
    val seconds = secondsLeft % 60

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Currently Focusing On",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Timer ring
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 10.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "%02d:%02d".format(minutes, seconds),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when (timerState) {
                            TimerState.Idle    -> if (secondsLeft == totalSeconds) "Ready" else "Paused"
                            TimerState.Running -> "Focus"
                            TimerState.Paused  -> "Paused"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Controls
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (secondsLeft < totalSeconds) {
                    OutlinedButton(
                        onClick = {
                            timerState = TimerState.Idle
                            secondsLeft = totalSeconds
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(18.dp))
                    }
                }

                Button(
                    onClick = {
                        timerState = when (timerState) {
                            TimerState.Running -> TimerState.Paused
                            else               -> TimerState.Running
                        }
                    },
                    modifier = Modifier.width(150.dp)
                ) {
                    Icon(
                        imageVector = if (timerState == TimerState.Running) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (timerState == TimerState.Running) "Pause" else "Start",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when (timerState) {
                            TimerState.Running -> "Pause"
                            TimerState.Paused  -> "Resume"
                            TimerState.Idle    -> "Start"
                        }
                    )
                }
            }

            TextButton(onClick = onFinish) { Text("End Session") }
        }
    }
}
