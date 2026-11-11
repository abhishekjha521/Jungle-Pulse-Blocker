package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.example.data.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdSkipAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "JunglePulseAdSkip"
        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        // Known Skip View IDs in official YouTube app
        private val SKIP_VIEW_IDS = listOf(
            "com.google.android.youtube:id/skip_ad_button",
            "com.google.android.youtube:id/modern_skip_ad_button",
            "com.google.android.youtube:id/ad_skip_button",
            "com.google.android.youtube:id/skip_ad_button_text",
            "com.google.android.youtube:id/skip_ad_button_container"
        )

        // Text strings indicating a skip button
        private val SKIP_TEXTS = listOf(
            "Skip Ad",
            "Skip ad",
            "Skip ads",
            "Skip Ads",
            "Skip",
            "skip",
            "विज्ञापन छोड़ें",
            "विज्ञापन वगळा",
            "বিজ্ঞাপন এড়িয়ে যান",
            "விளம்பரத்தைத் தவிர்"
        )
    }

    private lateinit var prefsManager: PreferencesManager
    private var audioManager: AudioManager? = null
    private var originalVolume: Int = -1
    private var isCurrentlyMuted = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastSkipTimestamp = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        _isRunning.value = true
        prefsManager = PreferencesManager.getInstance(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        Log.d(TAG, "Jungle Pulse AdSkipService connected!")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (!prefsManager.isAdSkipEnabledSync()) return

        val pkg = event.packageName?.toString() ?: ""
        if (pkg.isNotEmpty() && !pkg.contains("youtube")) return

        val rootNode = rootInActiveWindow ?: event.source ?: return

        try {
            inspectAndSkipAds(rootNode)
        } catch (e: Exception) {
            Log.e(TAG, "Error in skip handler: ${e.message}")
        }
    }

    private fun inspectAndSkipAds(rootNode: AccessibilityNodeInfo) {
        val now = System.currentTimeMillis()

        // 1. Search by YouTube Skip View IDs
        for (viewId in SKIP_VIEW_IDS) {
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)
            for (node in nodes) {
                if (performSkipClick(node)) {
                    if (now - lastSkipTimestamp > 800L) {
                        lastSkipTimestamp = now
                        handleAdSkipped()
                    }
                    unmuteAudioIfNeeded()
                    return
                }
            }
        }

        // 2. Search by Skip Text
        for (query in SKIP_TEXTS) {
            val nodes = rootNode.findAccessibilityNodeInfosByText(query)
            for (node in nodes) {
                val nodeText = node.text?.toString()?.lowercase() ?: ""
                val nodeDesc = node.contentDescription?.toString()?.lowercase() ?: ""

                // Ignore countdown-only text like "You can skip in 5s"
                if (nodeText.contains("skip in") && !nodeText.contains("skip ad") && !nodeText.equals("skip", ignoreCase = true)) {
                    continue
                }

                // Ignore any report or about-ad elements
                if (nodeText.contains("report") || nodeText.contains("about") ||
                    nodeDesc.contains("report") || nodeDesc.contains("about")
                ) {
                    continue
                }

                if (performSkipClick(node)) {
                    if (now - lastSkipTimestamp > 800L) {
                        lastSkipTimestamp = now
                        handleAdSkipped()
                    }
                    unmuteAudioIfNeeded()
                    return
                }
            }
        }

        // 3. Auto-Mute during unskippable ads if enabled
        if (prefsManager.isMuteAdsEnabledSync()) {
            if (detectAdPlaying(rootNode)) {
                muteAudio()
            } else {
                unmuteAudioIfNeeded()
            }
        }
    }

    /**
     * Attempts to click the skip button.
     * Clicks the node directly if clickable, or its immediate parent if the container holds the click listener.
     * Strictly avoids siblings and NEVER calls BACK actions (which would minimize the video).
     */
    private fun performSkipClick(node: AccessibilityNodeInfo): Boolean {
        // Direct click
        if (node.isClickable && node.isEnabled) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }

        // Parent container click (up to 2 levels)
        var parent = node.parent
        var depth = 0
        while (parent != null && depth < 2) {
            if (parent.isClickable && parent.isEnabled) {
                return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            parent = parent.parent
            depth++
        }

        // Child click (if wrapped inside a layout)
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (child.isClickable && child.isEnabled) {
                if (child.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            }
        }

        return false
    }

    private fun detectAdPlaying(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""

        if (text.startsWith("ad ·") || text.startsWith("ad 1 of") || text.startsWith("ad 2 of") ||
            text.contains("video will play after ad") || text.contains("skip in") ||
            desc.contains("advertisement")
        ) {
            return true
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (detectAdPlaying(child)) return true
        }
        return false
    }

    private fun muteAudio() {
        val am = audioManager ?: return
        if (!isCurrentlyMuted) {
            try {
                originalVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
                isCurrentlyMuted = true
            } catch (e: Exception) {
                Log.e(TAG, "Error muting audio: ${e.message}")
            }
        }
    }

    private fun unmuteAudioIfNeeded() {
        val am = audioManager ?: return
        if (isCurrentlyMuted) {
            try {
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
                if (originalVolume >= 0) {
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
                }
                isCurrentlyMuted = false
            } catch (e: Exception) {
                Log.e(TAG, "Error unmuting audio: ${e.message}")
            }
        }
    }

    private fun handleAdSkipped() {
        mainHandler.post {
            prefsManager.recordAdSkipped()
            try {
                Toast.makeText(applicationContext, "⚡ YouTube Ad Skipped!", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {}
        }
    }

    override fun onInterrupt() {
        _isRunning.value = false
        unmuteAudioIfNeeded()
    }

    override fun onDestroy() {
        super.onDestroy()
        _isRunning.value = false
        unmuteAudioIfNeeded()
    }
}
