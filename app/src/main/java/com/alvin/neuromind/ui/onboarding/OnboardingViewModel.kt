package com.alvin.neuromind.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alvin.neuromind.data.preferences.PeakWindow
import com.alvin.neuromind.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(private val userPrefs: UserPreferencesRepository) : ViewModel() {
    private val _selectedWindow = MutableStateFlow(PeakWindow.LATE_MORNING)
    val selectedWindow: StateFlow<PeakWindow> = _selectedWindow.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    fun selectWindow(window: PeakWindow) {
        _selectedWindow.value = window
    }

    fun toggleNotifications() {
        _notificationsEnabled.value = !_notificationsEnabled.value
    }

    fun finish(onDone: () -> Unit) = viewModelScope.launch {
        userPrefs.savePeakWindow(_selectedWindow.value)
        userPrefs.setNotificationsEnabled(_notificationsEnabled.value)
        userPrefs.setOnboarded(true)
        onDone()
    }
}

class OnboardingViewModelFactory(
    private val userPrefs: UserPreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OnboardingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OnboardingViewModel(userPrefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
