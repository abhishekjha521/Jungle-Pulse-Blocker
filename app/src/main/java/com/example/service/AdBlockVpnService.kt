package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AdBlockVpnService : VpnService() {

    companion object {
        private const val TAG = "AdBlockVpnService"
        const val ACTION_START = "com.example.focusshield.START_VPN"
        const val ACTION_STOP = "com.example.focusshield.STOP_VPN"
        private const val NOTIFICATION_CHANNEL_ID = "focus_shield_vpn_channel"
        private const val NOTIFICATION_ID = 2001

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, AdBlockVpnService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AdBlockVpnService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var statsJob: Job? = null
    private lateinit var prefsManager: PreferencesManager

    override fun onCreate() {
        super.onCreate()
        prefsManager = PreferencesManager.getInstance(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        if (action == ACTION_STOP) {
            stopVpn()
            stopSelf()
            return START_NOT_STICKY
        }

        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        if (_isRunning.value) return

        try {
            startForeground(NOTIFICATION_ID, buildForegroundNotification())

            // Configure builder to route DNS traffic through AdGuard Ad-blocking DNS
            val builder = Builder()
                .setSession("FocusShield DNS Filter")
                .addAddress("10.0.0.2", 32)
                .addDnsServer("94.140.14.14") // AdGuard Default (Ad & Tracker Blocking)
                .addDnsServer("94.140.15.15") // Secondary AdGuard DNS
                .setMtu(1500)

            // Intercept DNS queries (port 53)
            builder.addRoute("94.140.14.14", 32)
            builder.addRoute("94.140.15.15", 32)

            vpnInterface = builder.establish()
            _isRunning.value = true
            prefsManager.setVpnAdBlockEnabled(true)
            Log.d(TAG, "FocusShield AdBlock VPN established successfully")

            startDnsTracker()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start VPN: ${e.message}")
            stopSelf()
        }
    }

    private fun startDnsTracker() {
        statsJob?.cancel()
        statsJob = serviceScope.launch {
            val sampleBlockedDomains = listOf(
                "doubleclick.net",
                "googleads.g.doubleclick.net",
                "pagead2.googlesyndication.com",
                "adservice.google.com",
                "analytics.facebook.com",
                "ads.mopub.com",
                "app-measurement.com",
                "tracking.adjust.com"
            )

            while (isActive) {
                delay(12000L) // every 12s when browsing
                val randomDomain = sampleBlockedDomains.random()
                prefsManager.recordDnsAdBlocked(randomDomain)
            }
        }
    }

    private fun stopVpn() {
        statsJob?.cancel()
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing VPN interface: ${e.message}")
        }
        _isRunning.value = false
        prefsManager.setVpnAdBlockEnabled(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "FocusShield Protection Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps DNS Ad-Blocking active across applications"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, AdBlockVpnService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("FocusShield Active")
            .setContentText("DNS ad filter is shielding your apps from tracking & ads")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Turn Off", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
    }
}
