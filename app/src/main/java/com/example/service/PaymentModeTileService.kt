package com.example.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

/**
 * Quick Settings Tile ("Payment Safe / Ad Skip") so the user can pull down the notification shade
 * from inside PhonePe / Google Pay / Paytm and disable Accessibility in 1 tap via disableSelf().
 * Tapping again opens Accessibility Settings directly to re-enable YouTube Ad Skip.
 */
class PaymentModeTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val isServiceActive = isAccessibilityEnabled()

        if (isServiceActive) {
            val paused = AdSkipAccessibilityService.pauseForPayment(this)
            if (!paused) {
                openAccessibilitySettings()
            }
            updateTileState(forcePaused = paused)
        } else {
            Toast.makeText(
                applicationContext,
                "⚡ 'Jungle Pulse YouTube Skip' ON karein",
                Toast.LENGTH_SHORT
            ).show()
            openAccessibilitySettings()
        }
    }

    private fun updateTileState(forcePaused: Boolean = false) {
        val tile = qsTile ?: return
        val active = if (forcePaused) false else isAccessibilityEnabled()

        if (active) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = "Ad Skip: ON"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Tap = Payment OFF"
            }
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "Payment Safe"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Skip OFF • Tap ON"
            }
        }
        tile.updateTile()
    }

    private fun isAccessibilityEnabled(): Boolean {
        if (AdSkipAccessibilityService.isRunning.value) return true
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(packageName)
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    101,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        } catch (_: Exception) {}
    }
}
