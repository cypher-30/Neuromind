package com.alvin.neuromind.ui.previews

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alvin.neuromind.ui.components.OrganicCard
import com.alvin.neuromind.ui.components.ScreenHeader
import com.alvin.neuromind.ui.components.SectionKicker
import com.alvin.neuromind.ui.theme.Dimens

// Reached from Settings > "Widget & notifications". Presentational only — a
// static showcase of what the home-screen widget and system notifications
// look like in the Organic palette, matching Neuromind.dc.html's mock (no
// live task data, no notifications actually posted).
@Composable
fun PreviewsScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.ScreenPaddingHorizontal)
            .padding(bottom = Dimens.ScreenPaddingBottomNoNav)
    ) {
        Spacer(Modifier.height(Dimens.ScreenPaddingTop))
        ScreenHeader(title = "Widget & notifications", onBack = onBack)
        SectionKicker("Home-screen widget", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        WidgetPreview()
        Spacer(Modifier.height(10.dp))
        WidgetCatalogHelp()

        Spacer(Modifier.height(Dimens.SectionGap))
        SectionKicker("Notifications", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.CardGap)) {
            DueSoonNotificationPreview()
            TodaysFocusNotificationPreview()
        }
    }
}

@Composable
private fun WidgetCatalogHelp() {
    OrganicCard(modifier = Modifier.fillMaxWidth()) {
        Text("Auto Stack setup", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        Text("1) Add 'Auto Stack (Configurable)' from the widget picker.", style = MaterialTheme.typography.bodySmall)
        Text("2) In Settings, set 'Widget Stack Interval' to 15/30/60 min.", style = MaterialTheme.typography.bodySmall)
        Text("3) The same widget rotates cards automatically.", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(10.dp))
        Text("Available sizes", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        Text("- Mini Status (1x1)", style = MaterialTheme.typography.bodySmall)
        Text("- Mini Next (2x1)", style = MaterialTheme.typography.bodySmall)
        Text("- Mini Quick Log (1x1)", style = MaterialTheme.typography.bodySmall)
        Text("- Standard widgets (about 2x2 and up)", style = MaterialTheme.typography.bodySmall)

        Spacer(Modifier.height(10.dp))
        Text("Mini Quick Log setup", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        Text("1) Long-press home screen and open widget picker.", style = MaterialTheme.typography.bodySmall)
        Text("2) Add Neuromind > Mini Quick Log (1x1).", style = MaterialTheme.typography.bodySmall)
        Text("3) Tap it to start voice logging immediately.", style = MaterialTheme.typography.bodySmall)

        Spacer(Modifier.height(10.dp))
        Text("Quick Voice Log tile", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        Text("1) Swipe down twice, then tap Edit (pencil).", style = MaterialTheme.typography.bodySmall)
        Text("2) Drag 'Quick Voice Log' into active tiles.", style = MaterialTheme.typography.bodySmall)
        Text("3) Tap tile any time for one-swipe voice capture.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun WidgetPreview() {
    OrganicCard(modifier = Modifier.widthIn(max = 280.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Today", style = MaterialTheme.typography.titleSmall)
            Text("Wed, Aug 30", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(10.dp))
        listOf("Finish chem lab report", "Gym — leg day", "Draft slides for presentation").forEach { title ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(14.dp)
                        .height(14.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(8.dp))
        }
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        Text(
            "Next: Organic Chemistry — Lecture Hall 3 · 9:00 AM",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DueSoonNotificationPreview() {
    OrganicCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(18.dp).height(18.dp).clip(CircleShape).then(
                Modifier.background(MaterialTheme.colorScheme.primary)
            ))
            Spacer(Modifier.width(8.dp))
            Text("Neuromind · now", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        Text("Due in 15 minutes", style = MaterialTheme.typography.titleSmall)
        Text("Chem lab report is due at 5:00 PM.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NotificationActionPill("Focus now", filled = true)
            NotificationActionPill("Snooze", filled = false)
        }
    }
}

@Composable
private fun TodaysFocusNotificationPreview() {
    OrganicCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(18.dp).height(18.dp).clip(CircleShape).then(
                Modifier.background(MaterialTheme.colorScheme.secondary)
            ))
            Spacer(Modifier.width(8.dp))
            Text("Neuromind · 8:00 AM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        Text("Today's focus", style = MaterialTheme.typography.titleSmall)
        Text(
            "Your energy's usually highest right now — good time for the chem report.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NotificationActionPill(label: String, filled: Boolean) {
    Box(
        modifier = Modifier
            .clip(com.alvin.neuromind.ui.theme.Pill)
            .then(
                if (filled) Modifier.background(MaterialTheme.colorScheme.primary)
                else Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
            )
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    }
}
