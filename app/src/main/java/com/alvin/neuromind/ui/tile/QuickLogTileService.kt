package com.alvin.neuromind.ui.tile

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.alvin.neuromind.R
import com.alvin.neuromind.QuickLogEntryActivity

class QuickLogTileService : TileService() {

    private val mainHandler = Handler(Looper.getMainLooper())

    private fun launchQuickLog() {
        val intent = Intent(this, QuickLogEntryActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                77,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            label = getString(R.string.quick_log_tile_label)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subtitle = getString(R.string.quick_log_tile_subtitle)
            }
            state = Tile.STATE_INACTIVE
            icon = Icon.createWithResource(this@QuickLogTileService, R.drawable.ic_launcher_monochrome)
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subtitle = "Opening voice log..."
            }
            updateTile()
        }

        if (isLocked) {
            unlockAndRun { launchQuickLog() }
        } else {
            launchQuickLog()
        }

        mainHandler.postDelayed({
            qsTile?.apply {
                state = Tile.STATE_INACTIVE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    subtitle = getString(R.string.quick_log_tile_subtitle)
                }
                updateTile()
            }
        }, 1200L)
    }
}




