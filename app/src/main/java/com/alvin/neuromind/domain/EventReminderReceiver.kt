package com.alvin.neuromind.domain

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.alvin.neuromind.data.NeuromindApplication
import com.alvin.neuromind.ui.widget.WidgetRefreshCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.ZoneId
import kotlin.math.abs

class EventReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? NeuromindApplication ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_FIRE -> fire(app, intent)
                    ACTION_MIDNIGHT -> {
                        runCatching { WidgetRefreshCoordinator.updateAllWidgets(app) }
                        EventReminderScheduler(app).scheduleMidnightRefresh()
                    }
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED,
                    AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                        app.repository.rescheduleAllReminders()
                        EventReminderScheduler(app).scheduleMidnightRefresh()
                        runCatching { WidgetRefreshCoordinator.updateAllWidgets(app) }
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun fire(app: NeuromindApplication, intent: Intent) {
        val entryId = intent.getIntExtra(EXTRA_ENTRY_ID, -1)
        val expected = intent.getLongExtra(EXTRA_TRIGGER_AT, -1L)
        val entry = app.repository.getTimetableEntryById(entryId) ?: return
        val trigger = EventReminderPolicy.triggerAt(entry) ?: return
        val triggerMillis = trigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // A stale alarm (event edited after this alarm was set) must not notify.
        if (expected > 0 && abs(triggerMillis - expected) > 60_000L) return
        if (!app.userPreferencesRepository.notificationsEnabled.first()) return
        NotificationHelper(app).showEventReminder(entry)
    }

    companion object {
        const val ACTION_FIRE = "com.alvin.neuromind.action.EVENT_REMINDER"
        const val ACTION_MIDNIGHT = "com.alvin.neuromind.action.MIDNIGHT_REFRESH"
        const val EXTRA_ENTRY_ID = "com.alvin.neuromind.extra.ENTRY_ID"
        const val EXTRA_TRIGGER_AT = "com.alvin.neuromind.extra.TRIGGER_AT"
    }
}
