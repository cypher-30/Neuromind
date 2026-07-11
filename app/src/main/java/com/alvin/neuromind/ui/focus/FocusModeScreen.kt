package com.alvin.neuromind.ui.focus

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alvin.neuromind.data.Task
import com.alvin.neuromind.ui.components.NeuromindTopBar
import kotlinx.coroutines.delay

private enum class TimerState { Idle, Running, Paused }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusModeScreen(task: Task, viewModel: FocusViewModel, onFinish: () -> Unit) {
    val context = LocalContext.current
    val initialMinutes = task.durationMinutes.coerceIn(5, 120)
    var sessionMinutes by rememberSaveable { mutableIntStateOf(initialMinutes) }
    var secondsLeft by rememberSaveable { mutableIntStateOf(initialMinutes * 60) }
    var timerState by rememberSaveable { mutableStateOf(TimerState.Idle) }
    var showDoneDialog by remember { mutableStateOf(false) }

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // DND Integration
    DisposableEffect(timerState) {
        if (timerState == TimerState.Running) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager.isNotificationPolicyAccessGranted) {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager.isNotificationPolicyAccessGranted) {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }
        }
        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager.isNotificationPolicyAccessGranted) {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }
        }
    }

    LaunchedEffect(timerState) {
        if (timerState == TimerState.Running) {
            while (secondsLeft > 0) {
                delay(1000L)
                secondsLeft--
            }
            timerState = TimerState.Idle
            showDoneDialog = true
            triggerAlert(context)
            viewModel.recordCompletedSession(task, sessionMinutes)
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
                    secondsLeft = sessionMinutes * 60
                    showDoneDialog = false
                }) { Text("Another round") }
            }
        )
    }

    val totalSeconds = sessionMinutes * 60
    val progress = if (totalSeconds > 0) secondsLeft.toFloat() / totalSeconds.toFloat() else 0f
    val minutes = secondsLeft / 60
    val seconds = secondsLeft % 60

    Scaffold(
        topBar = {
            NeuromindTopBar(
                title = "",
                onNavigateBack = onFinish,
                useCloseIcon = true,
                transparent = true
            )
        }
    ) { innerPadding ->
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
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

            // Duration Adjuster (only when Idle/Ready)
            if (timerState == TimerState.Idle && secondsLeft == totalSeconds) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconButton(onClick = { if (sessionMinutes > 5) sessionMinutes -= 5; secondsLeft = sessionMinutes * 60 }) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease time")
                    }
                    Text("${sessionMinutes} min", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { if (sessionMinutes < 120) sessionMinutes += 5; secondsLeft = sessionMinutes * 60 }) {
                        Icon(Icons.Default.Add, contentDescription = "Increase time")
                    }
                }
            }

            // Controls
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (secondsLeft < totalSeconds) {
                    OutlinedButton(
                        onClick = {
                            timerState = TimerState.Idle
                            secondsLeft = sessionMinutes * 60
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
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !notificationManager.isNotificationPolicyAccessGranted) {
                OutlinedButton(
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Grant DND Access")
                }
            }
        }
    }
    }
}

private fun triggerAlert(context: Context) {
    // Vibration
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(1000)
    }

    // Sound - using Notification default
    try {
        val notification = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
        val r = android.media.RingtoneManager.getRingtone(context, notification)
        r.play()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
