package com.alvin.neuromind.domain

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.alvin.neuromind.MainActivity
import com.alvin.neuromind.R
import com.alvin.neuromind.navigation.AppRouteIntents

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "neuromind_alerts"
        const val CHANNEL_NAME = "Task & Schedule Alerts"
        const val NOTIFICATION_ID_BASE = 1000
        // Separate ID ranges so task, legacy-timetable and event alerts never overwrite each other.
        const val TIMETABLE_NOTIFICATION_BASE = 100_000
        const val EVENT_NOTIFICATION_BASE = 300_000
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for upcoming tasks and classes"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showEventReminder(entry: com.alvin.neuromind.data.TimetableEntry) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return

        val dateFormat = java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d")
        val timeFormat = java.time.format.DateTimeFormatter.ofPattern("h:mm a")
        val datePart = entry.date?.let { d ->
            when (d) {
                java.time.LocalDate.now() -> "Today"
                java.time.LocalDate.now().plusDays(1) -> "Tomorrow"
                else -> d.format(dateFormat)
            }
        } ?: ""
        val whenPart = if (entry.isAllDay) "$datePart · All day" else "$datePart at ${entry.startTime.format(timeFormat)}"
        val message = listOfNotNull(whenPart, entry.venue?.takeIf { it.isNotBlank() }).joinToString(" · ")

        val openIntent = AppRouteIntents.openEvent(context, entry.id)
        val pendingIntent = PendingIntent.getActivity(
            context,
            EVENT_NOTIFICATION_BASE + entry.id,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Coming up: ${entry.title}")
            .setContentText(message)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(EVENT_NOTIFICATION_BASE + entry.id, notification)
    }

    fun showNotification(id: Int, title: String, message: String) {
        // Create an intent to open the app when tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val quickLogIntent = AppRouteIntents.openFeedback(context, autoStartVoiceCapture = true)
        val quickLogPendingIntent = PendingIntent.getActivity(
            context,
            1,
            quickLogIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Check permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                return // Don't crash if permission not granted
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Uses app icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .addAction(0, "Quick log", quickLogPendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(id, builder.build())
        }
    }
}