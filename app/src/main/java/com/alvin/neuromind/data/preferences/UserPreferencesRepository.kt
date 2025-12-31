package com.alvin.neuromind.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeSetting { SYSTEM, LIGHT, DARK }

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(context: Context) {
    private val dataStore = context.dataStore
    private object PreferenceKeys {
        val THEME_SETTING = stringPreferencesKey("theme_setting")
    }
    val userTheme: Flow<ThemeSetting> = dataStore.data
        .map { preferences ->
            val themeName = preferences[PreferenceKeys.THEME_SETTING] ?: ThemeSetting.SYSTEM.name
            ThemeSetting.valueOf(themeName)
        }
    suspend fun saveThemeSetting(theme: ThemeSetting) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.THEME_SETTING] = theme.name
        }
    }
}