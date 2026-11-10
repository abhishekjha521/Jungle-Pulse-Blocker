package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.data.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdSkipAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "JunglePulseAdSkip"
        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        // Target video packages
        private val TARGET_PACKAGES = setOf(
            "com.google.android.youtube",
            "com.google.android.apps.youtube.music",
            "com.google.android.youtube.tv",
            "com.android.chrome"
        )

        // Known skip button IDs in YouTube
        private val SKIP_BUTTON_IDS = setOf(
            "com.google.android.youtube:id/skip_ad_button",
            "com.google.android.youtube:id/modern_skip_ad_button",
            "com.google.android.youtube:id/ad_skip_button",
            "com.google.android.youtube:id/subhead_ad_button",
            "com.google.android.youtube:id/countdown_cancel_button",
            "com.google.android.youtube:id/preskip_countdown",
            "com.google.android.youtube:id/ad_progress_text",
            "com.google.android.youtube:id/skip_ad_button_container",
            "com.google.android.youtube:id/ad_compact_banner_close_button",
            "com.google.android.youtube:id/close_button",
            "com.google.android.youtube:id/dismiss_button"
        )

        // Multi-language skip phrases with focus on "skip in"
        private val SKIP_TEXT_KEYWORDS = listOf(
            "skip in",          // Target requested specifically by user
            "skip in:",
            "skip in 5",
            "skip in 4",
            "skip in 3",
            "skip in 2",
            "skip in 1",
            "skip in 0",
            "skip ad in",
            "skip ads in",
            "you can skip",
            "skip ad",
            "skip ads",
            "skip",
            "विज्ञापन छोड़ें", // Hindi
            "में छोड़ें",      // Hindi countdown
            "বিজ্ঞাপন এড়িয়ে যান", // Bengali
            "विज्ञापन वगळा",    // Marathi
            "விளம்பரத்தைத் தவிர்", // Tamil
            "광고 건너뛰기",     // Korean
            "ignorer l'annonce", // French
            "saltar anuncio"     // Spanish
        )

        private val CONTINUE_WATCHING_TEXTS = listOf(
            "yes",
            "continue",
            "keep watching",
            "हाँ",
            "जारी रखें"
        )
    }

    private lateinit var prefsManager: PreferencesManager
    private var audioManager: AudioManager? = null
    private var originalVolume: Int = -1
    private var isCurrentlyMuted = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastSkipTimestamp = 0L

    // Active watcher for "skip in" countdowns
    private var isWatchingCountdown = false
    private val countdownRunnable = object : Runnable {
        override fun run() {
            if (!isWatchingCountdown) return
            val root = rootInActiveWindow
            if (root != null) {
                try {
                    val tapped = findAndTapSkipInNode(root)
                    if (tapped) {
                        Log.d(TAG, "Tapped skip node during countdown watch")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in countdown watch: ${e.message}")
                }
            }
            mainHandler.postDelayed(this, 150L)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        _isRunning.value = true
        prefsManager = PreferencesManager.getInstance(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        Log.d(TAG, "Jungle Pulse AdSkipAccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return

        if (!TARGET_PACKAGES.contains(packageName)) return
        if (!prefsManager.isAdSkipEnabledSync()) return

        val rootNode = rootInActiveWindow ?: return

        try {
            inspectAndSkipAds(rootNode, packageName)
        } catch (e: Exception) {
            Log.e(TAG, "Error inspecting node tree: ${e.message}")
        }
    }

    private fun inspectAndSkipAds(rootNode: AccessibilityNodeInfo, packageName: String) {
        val now = System.currentTimeMillis()

        // 1. Check for Skip Button by Resource IDs
        for (resId in SKIP_BUTTON_IDS) {
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(resId)
            for (node in nodes) {
                val clicked = clickNodeOrParentOrChildren(node)
                if (clicked) {
                    if (now - lastSkipTimestamp > 1000L) {
                        lastSkipTimestamp = now
                        Log.d(TAG, "Tapped skip ad button by ID: $resId")
                        handleAdSkipped(packageName)
                    }
                    unmuteAudioIfNeeded()
                    return
                }
            }
        }

        // 2. Search for any text containing "Skip in" or "Skip Ad"
        val foundSkip = findAndTapSkipInNode(rootNode)
        if (foundSkip) {
            if (now - lastSkipTimestamp > 1000L) {
                lastSkipTimestamp = now
                handleAdSkipped(packageName)
            }
            unmuteAudioIfNeeded()
            return
        }

        // 3. Check for "Continue Watching" dialog if enabled
        if (prefsManager.isContinueWatchingEnabledSync()) {
            checkForContinueWatchingDialog(rootNode)
        }

        // 4. Mute Ad Audio if an unskippable ad is actively displaying
        if (prefsManager.isMuteAdsEnabledSync()) {
            val isAdPlaying = detectAdPlaying(rootNode)
            if (isAdPlaying) {
                muteAudio()
                startCountdownWatch()
            } else {
                stopCountdownWatch()
                unmuteAudioIfNeeded()
            }
        }
    }

    private fun findAndTapSkipInNode(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""

        val isSkipInMatch = text.contains("skip in") || desc.contains("skip in")
        val matchesGeneral = SKIP_TEXT_KEYWORDS.any { keyword ->
            text.contains(keyword) || desc.contains(keyword)
        }

        if (isSkipInMatch || matchesGeneral) {
            Log.d(TAG, "Found target skip text: '$text' / '$desc'")
            val clicked = clickNodeOrParentOrChildren(node)
            if (clicked) return true
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val handled = findAndTapSkipInNode(child)
            if (handled) return true
        }

        return false
    }

    /**
     * Clicks the node directly, or if not clickable, searches up the hierarchy
     * (up to 4 levels) and tests the parent or sibling views for clickability.
     */
    private fun clickNodeOrParentOrChildren(node: AccessibilityNodeInfo): Boolean {
        // Direct click
        if (node.isClickable && node.isEnabled) {
            val clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (clicked) return true
        }

        // Traverse up to clickable parent/container
        var target: AccessibilityNodeInfo? = node.parent
        var depth = 0
        while (target != null && depth < 4) {
            if (target.isClickable && target.isEnabled) {
                val clicked = target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (clicked) return true
            }

            // Check siblings inside container
            for (j in 0 until target.childCount) {
                val sibling = target.getChild(j) ?: continue
                if (sibling.isClickable && sibling.isEnabled) {
                    val clicked = sibling.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    if (clicked) return true
                }
            }
            target = target.parent
            depth++
        }
        return false
    }

    private fun startCountdownWatch() {
        if (!isWatchingCountdown) {
            isWatchingCountdown = true
            mainHandler.post(countdownRunnable)
        }
    }

    private fun stopCountdownWatch() {
        if (isWatchingCountdown) {
            isWatchingCountdown = false
            mainHandler.removeCallbacks(countdownRunnable)
        }
    }

    private fun detectAdPlaying(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""

        if (text.startsWith("ad ·") || text.startsWith("ad 1 of") || text.startsWith("ad 2 of") ||
            text.contains("skip in") || text.contains("video will play after ad") ||
            desc.contains("advertisement") || desc.contains("skip in")
        ) {
            return true
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val hasAd = detectAdPlaying(child)
            if (hasAd) return true
        }
        return false
    }

    private fun checkForContinueWatchingDialog(node: AccessibilityNodeInfo) {
        val text = node.text?.toString()?.lowercase() ?: ""
        if (text.contains("still watching?") || text.contains("video paused")) {
            for (kw in CONTINUE_WATCHING_TEXTS) {
                val yesNodes = node.findAccessibilityNodeInfosByText(kw)
                for (btn in yesNodes) {
                    if (btn.isClickable) {
                        btn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        Log.d(TAG, "Clicked continue watching dialog button")
                        return
                    }
                }
            }
        }
    }

    private fun muteAudio() {
        val am = audioManager ?: return
        if (!isCurrentlyMuted) {
            try {
                originalVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
                isCurrentlyMuted = true
                mainHandler.post {
                    prefsManager.recordAdMuted("YouTube")
                }
                Log.d(TAG, "Muted audio for unskippable ad")
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
                Log.d(TAG, "Unmuted audio after ad completed")
            } catch (e: Exception) {
                Log.e(TAG, "Error unmuting audio: ${e.message}")
            }
        }
    }

    private fun handleAdSkipped(packageName: String) {
        mainHandler.post {
            val appLabel = if (packageName.contains("music")) "YouTube Music" else "YouTube"
            prefsManager.recordAdSkipped(appLabel, 15L)
        }
    }

    override fun onInterrupt() {
        _isRunning.value = false
        stopCountdownWatch()
        unmuteAudioIfNeeded()
        Log.d(TAG, "Jungle Pulse AdSkipAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        _isRunning.value = false
        stopCountdownWatch()
        unmuteAudioIfNeeded()
    }
}
