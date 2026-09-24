package com.alvin.neuromind

import android.Manifest
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingRouteRequest = AppRouteIntents.parse(intent)
    }
}
