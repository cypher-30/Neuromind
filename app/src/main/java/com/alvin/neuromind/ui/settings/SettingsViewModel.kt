package com.alvin.neuromind.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alvin.neuromind.data.preferences.ThemeSetting
import com.alvin.neuromind.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.CreationExtras

class SettingsViewModel(private val repository: com.alvin.neuromind.data.preferences.UserPreferencesRepository) : ViewModel() {

    val themeSetting: StateFlow<ThemeSetting> = repository.userTheme.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = ThemeSetting.SYSTEM
    )

    fun changeTheme(theme: ThemeSetting) {
        viewModelScope.launch {
            repository.saveThemeSetting(theme)
        }
    }
}

class SettingsViewModelFactory(
    private val repository: com.alvin.neuromind.data.preferences.UserPreferencesRepository
) : ViewModelProvider.Factory {
    // --- FIX: Updated the create function signature ---
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}