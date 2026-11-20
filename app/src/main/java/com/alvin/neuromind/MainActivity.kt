package com.alvin.neuromind

import android.Manifest
import android.app.UiModeManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.alvin.neuromind.data.NeuromindApplication
import com.alvin.neuromind.data.preferences.ThemeSetting
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import com.alvin.neuromind.navigation.AppRouteIntents
import com.alvin.neuromind.navigation.AppRouteRequest
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {

    private var pendingRouteRequest: AppRouteRequest? by mutableStateOf(null)

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op: silently accepted or denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                Color.WHITE,
                Color.BLACK,
            ),
            navigationBarStyle = SystemBarStyle.auto(
                Color.WHITE,
                Color.BLACK,
            )
        )
        super.onCreate(savedInstanceState)
        // A recreated activity (rotation, process restore) or a relaunch from
        // Recents still carries the original deep-link extras; only act on a
        // genuinely new launch so e.g. a reminder tap isn't replayed.
        val launchedFromHistory = intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
        if (savedInstanceState == null && !launchedFromHistory) {
            pendingRouteRequest = AppRouteIntents.parse(intent)
        }

        syncSplashNightMode()

        // Request POST_NOTIFICATIONS on Android 13+ so workers actually deliver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val application = application as NeuromindApplication
            NeuromindApp(
                repository = application.repository,
                scheduler = application.scheduler,
                userPreferencesRepository = application.userPreferencesRepository,
                showComposeSplash = savedInstanceState == null,
                pendingRouteRequest = pendingRouteRequest,
                onPendingRouteHandled = {
                    pendingRouteRequest = null
                    intent.removeExtra(AppRouteIntents.EXTRA_ROUTE)
                }
            )
        }
    }

    /**
     * The system draws the splash/starting window before any app code runs,
     * using the *system* night mode, so a Light app on a Dark phone (or the
     * reverse) flashed the wrong ground on cold launch. On Android 12+ the
     * app's own choice can be registered with UiModeManager; the system
     * remembers it for this app's future splash screens.
     */
    private fun syncSplashNightMode() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val uiModeManager = getSystemService(UiModeManager::class.java) ?: return
        val prefs = (application as NeuromindApplication).userPreferencesRepository
        lifecycleScope.launch {
            prefs.userTheme.distinctUntilChanged().collect { theme ->
                val mode = when (theme) {
                    ThemeSetting.LIGHT -> UiModeManager.MODE_NIGHT_NO
                    ThemeSetting.DARK -> UiModeManager.MODE_NIGHT_YES
                    ThemeSetting.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
                }
                // Only on a real change: applying it updates the app's
                // configuration, which recreates this activity.
                val store = getSharedPreferences("splash_night_mode", MODE_PRIVATE)
                if (store.getInt("applied", -1) != mode) {
                    runCatching { uiModeManager.setApplicationNightMode(mode) }
                        .onSuccess { store.edit().putInt("applied", mode).apply() }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingRouteRequest = AppRouteIntents.parse(intent)
    }
}
