package com.alvin.neuromind

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.alvin.neuromind.data.NeuromindApplication

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // This is the modern, recommended way to enable edge-to-edge display
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
            )
        )
        super.onCreate(savedInstanceState)

        setContent {
            val application = application as NeuromindApplication
            NeuromindApp(
                repository = application.repository,
                scheduler = application.scheduler,
                userPreferencesRepository = application.userPreferencesRepository
            )
        }
    }
}