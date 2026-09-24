package com.alvin.neuromind.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.alvin.neuromind.ui.theme.Dimens

// One stat-tile shape for every "label above a big Caprasimo number" card —
// Dashboard's Pending/Done, Insights' On-track rate/Best day. Previously each
// screen hardcoded its own height (110dp/130dp/130dp) and layout for the same
// idea; this is the single version.
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    OrganicCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(Dimens.CardPaddingCompact)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = valueColor
            )
        }
    }
}
