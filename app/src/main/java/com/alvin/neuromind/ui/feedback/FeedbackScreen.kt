package com.alvin.neuromind.ui.feedback

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.alvin.neuromind.data.FeedbackLog
import com.alvin.neuromind.data.Mood
import com.alvin.neuromind.ui.components.LabeledField
import com.alvin.neuromind.ui.components.PillButton
import com.alvin.neuromind.ui.components.PillChip
import com.alvin.neuromind.ui.components.ScreenHeader
import com.alvin.neuromind.ui.theme.Dimens

// The design shows a "Logged — thanks" success state inline rather than
// immediately navigating away on submit, so the reviewer sees confirmation
// before choosing to go back — this screen no longer auto-pops on submit.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeedbackScreen(
    viewModel: FeedbackViewModel,
    onNavigateBack: () -> Unit,
    autoStartVoiceCapture: Boolean = false,
    autoStartToken: Long = 0L,
    initialPrefillNote: String? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedMood by remember { mutableStateOf<Mood?>(null) }
    var energyLevel by remember { mutableFloatStateOf(3f) }
    var tasksCompleted by remember { mutableStateOf("") }
    var comment by remember(initialPrefillNote) { mutableStateOf(initialPrefillNote ?: "") }
    var voiceError by remember { mutableStateOf<String?>(null) }
    var autoStartHandled by remember(autoStartToken) { mutableStateOf(false) }

    val voiceCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
            if (!spoken.isNullOrBlank()) {
                comment = if (comment.isBlank()) spoken else "$comment\n$spoken"
            }
        }
    }

    val startVoiceCapture = {
        voiceError = null
        val voiceIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your journal note")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        try {
            voiceCaptureLauncher.launch(voiceIntent)
        } catch (_: Throwable) {
            voiceError = "Voice input is not available on this device yet."
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startVoiceCapture()
        } else {
            voiceError = "Microphone permission is needed for voice logging."
        }
    }

    fun requestVoiceCapture() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) startVoiceCapture() else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    DisposableEffect(autoStartVoiceCapture, autoStartToken, lifecycleOwner) {
        if (!autoStartVoiceCapture || autoStartHandled || !initialPrefillNote.isNullOrBlank()) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME && !autoStartHandled) {
                    autoStartHandled = true
                    requestVoiceCapture()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.ScreenPaddingHorizontal)
            .padding(bottom = Dimens.ScreenPaddingBottomNoNav)
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(Dimens.ScreenPaddingTop))
        ScreenHeader(title = "How was today?", onBack = onNavigateBack)

        if (uiState.isSubmitted) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Logged — thanks", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(10.dp))
                Text(
                    "This feeds tomorrow's plan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                uiState.lastToneLabel?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Detected tone: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            return@Column
        }

        Column(verticalArrangement = Arrangement.spacedBy(Dimens.CardGap)) {
            Text("Mood", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Mood.entries.forEach { mood ->
                    PillChip(
                        label = mood.name.lowercase().replaceFirstChar { it.uppercase() },
                        selected = selectedMood == mood,
                        onClick = { selectedMood = mood }
                    )
                }
            }

            Text(
                "Energy — ${energyLevel.toInt()}/5",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = energyLevel,
                onValueChange = { energyLevel = it },
                valueRange = 1f..5f,
                steps = 3,
                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
            )

            LabeledField(
                label = "Tasks Completed Today",
                value = tasksCompleted,
                onValueChange = { tasksCompleted = it }
            )

            LabeledField(
                label = "Anything on your mind?",
                value = comment,
                onValueChange = { comment = it },
                placeholder = "Optional",
                singleLine = false,
                minLines = 3
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { requestVoiceCapture() }) {
                    Text("Voice note")
                }
                OutlinedButton(
                    onClick = { viewModel.createTaskFromNote(comment) },
                    enabled = comment.isNotBlank()
                ) {
                    Text("Save as task")
                }
            }

            voiceError?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            uiState.taskSavedMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(4.dp))
            PillButton(
                text = "Submit",
                onClick = {
                    viewModel.submitFeedback(
                        FeedbackLog(
                            mood = selectedMood ?: Mood.NEUTRAL,
                            energyLevel = energyLevel.toInt(),
                            tasksCompleted = tasksCompleted.toIntOrNull() ?: 0,
                            comment = comment.takeIf { it.isNotBlank() }
                        )
                    )
                }
            )
        }
    }
}
