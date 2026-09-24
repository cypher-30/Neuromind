package com.alvin.neuromind.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

enum class ThemeSetting { SYSTEM, LIGHT, DARK }
enum class TaskStyle { ANALYTICAL, CREATIVE, BALANCED }

// The Organic redesign's Cognitive Profile screen offers four preset chips
// instead of an hour-range stepper. Each maps onto the existing
// peakStartHour/peakEndHour ints so no new storage or Scheduler change is
// needed — see PeakWindow.forHours for the reverse lookup used to highlight
// the selected chip.
enum class PeakWindow(val label: String, val startHour: Int, val endHour: Int) {
    MORNING("Morning", 6, 9),
    LATE_MORNING("Late morning", 9, 12),
    AFTERNOON("Afternoon", 12, 17),
    EVENING("Evening", 17, 22);

    companion object {
        fun forHours(start: Int, end: Int): PeakWindow =
            entries.find { it.startHour == start && it.endHour == end } ?: LATE_MORNING
    }
}

data class CognitiveProfile(
    val peakStart: Int = 8,
    val peakEnd: Int = 11,
    val sessionLength: Int = 25,
    val style: TaskStyle = TaskStyle.BALANCED
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(context: Context) {
    private val dataStore = context.dataStore

    private object PreferenceKeys {
        val THEME_SETTING = stringPreferencesKey("theme_setting")
        val FOCUS_DURATION = intPreferencesKey("focus_duration")
        val PEAK_START_HOUR = intPreferencesKey("peak_start_hour")
        val PEAK_END_HOUR = intPreferencesKey("peak_end_hour")
        val TASK_STYLE = stringPreferencesKey("task_style")
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val WIDGET_STACK_INTERVAL = intPreferencesKey("widget_stack_interval")
    }

    val userTheme: Flow<ThemeSetting> = dataStore.data
        .map { preferences ->
            val themeName = preferences[PreferenceKeys.THEME_SETTING] ?: ThemeSetting.SYSTEM.name
            ThemeSetting.valueOf(themeName)
        }

    val focusDuration: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[PreferenceKeys.FOCUS_DURATION] ?: 25
        }

    val peakStartHour: Flow<Int> = dataStore.data
        .map { preferences -> preferences[PreferenceKeys.PEAK_START_HOUR] ?: 8 }

    val peakEndHour: Flow<Int> = dataStore.data
        .map { preferences -> preferences[PreferenceKeys.PEAK_END_HOUR] ?: 11 }

    val taskStyle: Flow<TaskStyle> = dataStore.data
        .map { preferences ->
            val name = preferences[PreferenceKeys.TASK_STYLE] ?: TaskStyle.BALANCED.name
            TaskStyle.valueOf(name)
        }

    val cognitiveProfile: Flow<CognitiveProfile> = combine(
        peakStartHour, peakEndHour, focusDuration, taskStyle
    ) { start, end, session, style ->
        CognitiveProfile(peakStart = start, peakEnd = end, sessionLength = session, style = style)
    }

    val isOnboarded: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[PreferenceKeys.ONBOARDED] ?: false }

    val notificationsEnabled: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[PreferenceKeys.NOTIFICATIONS_ENABLED] ?: true }

    val widgetStackIntervalMinutes: Flow<Int> = dataStore.data
        .map { preferences -> preferences[PreferenceKeys.WIDGET_STACK_INTERVAL] ?: 30 }

    suspend fun saveThemeSetting(theme: ThemeSetting) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.THEME_SETTING] = theme.name
        }
    }

    suspend fun saveFocusDuration(duration: Int) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.FOCUS_DURATION] = duration
        }
    }

    suspend fun savePeakStartHour(hour: Int) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.PEAK_START_HOUR] = hour
        }
    }

    suspend fun savePeakEndHour(hour: Int) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.PEAK_END_HOUR] = hour
        }
    }

    suspend fun saveTaskStyle(style: TaskStyle) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.TASK_STYLE] = style.name
        }
    }

    suspend fun setOnboarded(onboarded: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.ONBOARDED] = onboarded
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun savePeakWindow(window: PeakWindow) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.PEAK_START_HOUR] = window.startHour
            preferences[PreferenceKeys.PEAK_END_HOUR] = window.endHour
        }
    }

    suspend fun saveWidgetStackIntervalMinutes(minutes: Int) {
        val safe = when (minutes) {
            15, 30, 60 -> minutes
            else -> 30
        }
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.WIDGET_STACK_INTERVAL] = safe
        }
    }
}
