package com.alvin.neuromind.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alvin.neuromind.ui.theme.NeuromindTheme

// A simple data class to hold the state for this card, making it clean and reusable.
data class ProgressState(
    val completed: Int,
    val total: Int
)

@Composable
fun TodayProgressCard(
    modifier: Modifier = Modifier,
    progressState: ProgressState
) {
    val progress = if (progressState.total > 0) {
        progressState.completed.toFloat() / progressState.total.toFloat()
    } else {
        0f
    }

    // Animate the progress value for a smoother visual effect.
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        label = "ProgressAnimation"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Box to overlay the progress text on the indicator
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(80.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 8.dp,
                    strokeCap = StrokeCap.Round
                )
                Text(
                    text = "${progressState.completed}/${progressState.total}",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            // Text content next to the progress indicator
            Column {
                Text(
                    text = "Tasks Complete",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "A clear plan for a focused day.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TodayProgressCardPreview() {
    NeuromindTheme {
        TodayProgressCard(
            modifier = Modifier.padding(16.dp),
            progressState = ProgressState(completed = 4, total = 7)
        )
    }
}