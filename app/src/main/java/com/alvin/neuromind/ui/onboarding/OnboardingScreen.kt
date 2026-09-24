package com.alvin.neuromind.ui.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alvin.neuromind.data.preferences.PeakWindow
import com.alvin.neuromind.ui.components.OrganicCard
import com.alvin.neuromind.ui.components.OrganicToggle
import com.alvin.neuromind.ui.components.PillButton
import com.alvin.neuromind.ui.components.PillChip

// First-run only, gated on UserPreferencesRepository.isOnboarded (see
// NeuromindApp.kt). Two questions — peak focus window and whether to allow
// gentle reminders — because that is all the Cognitive Profile needs to start
// producing a sensible plan; everything else is refinable later from Settings.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onFinished: () -> Unit
) {
    val selectedWindow by viewModel.selectedWindow.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 40.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Welcome to Neuromind", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(10.dp))
            Text(
                "A couple of quick things so your plan fits how you actually work.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(28.dp))

            Text(
                "When are you sharpest?",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PeakWindow.entries.forEach { window ->
                    PillChip(
                        label = window.label,
                        selected = selectedWindow == window,
                        onClick = { viewModel.selectWindow(window) }
                    )
                }
            }
            Spacer(Modifier.height(28.dp))

            OrganicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.toggleNotifications() }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Gentle reminders", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Due-soon nudges and a daily suggestion",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OrganicToggle(checked = notificationsEnabled, onCheckedChange = { viewModel.toggleNotifications() })
                }
            }
        }

        PillButton(text = "Get started", onClick = { viewModel.finish(onFinished) })
    }
}
