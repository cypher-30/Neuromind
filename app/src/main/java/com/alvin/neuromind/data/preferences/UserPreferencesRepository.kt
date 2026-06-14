package com.alvin.neuromind.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

enum class ThemeSetting { SYSTEM, LIGHT, DARK }
enum class TaskStyle { ANALYTICAL, CREATIVE, BALANCED }

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
}
