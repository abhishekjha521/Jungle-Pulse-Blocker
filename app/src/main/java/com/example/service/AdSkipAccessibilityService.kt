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

        // Known skip button IDs in YouTube
        private val SKIP_BUTTON_IDS = setOf(
            "com.google.android.youtube:id/skip_ad_button",
            "com.google.android.youtube:id/modern_skip_ad_button",
            "com.google.android.youtube:id/ad_skip_button",
            "com.google.android.youtube:id/subhead_ad_button",
            "com.google.android.youtube:id/countdown_cancel_button",
            "com.google.android.youtube:id/preskip_countdown",
            "com.google.android.youtube:id/ad_progress_text",
            "com.google.android.youtube:id/skip_ad_button_container"
        )

        // Native search keywords
        private val NATIVE_SEARCH_STRINGS = listOf(
            "Skip",
            "skip",
            "Skip in",
            "skip in",
            "Skip Ad",
            "Skip ad",
            "Skip ads",
            "विज्ञापन छोड़ें",
            "में छोड़ें"
        )

        private val SKIP_KEYWORDS = listOf(
            "skip in",
            "skip ad",
            "skip ads",
            "skip",
            "विज्ञापन छोड़ें",
            "में छोड़ें",
            "বিজ্ঞাপন এড়িয়ে যান",
            "विज्ञापन वगळा",
            "விளம்பரத்தைத் தविர்"
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
        Log.d(TAG, "Jungle Pulse AdSkipService connected successfully!")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (!prefsManager.isAdSkipEnabledSync()) return

        val rootNode = rootInActiveWindow ?: event.source ?: return

        try {
            inspectAndSkipYouTubeAds(rootNode)
        } catch (e: Exception) {
            Log.e(TAG, "Error inspecting node tree: ${e.message}")
        }
    }

    private fun inspectAndSkipYouTubeAds(rootNode: AccessibilityNodeInfo) {
        val now = System.currentTimeMillis()

        // 1. Android Native Fast Text Scan (0ms look up)
        for (query in NATIVE_SEARCH_STRINGS) {
            val nodes = rootNode.findAccessibilityNodeInfosByText(query)
            for (node in nodes) {
                if (clickNodeOrParentOrChildren(node)) {
                    if (now - lastSkipTimestamp > 800L) {
                        lastSkipTimestamp = now
                        handleAdSkipped()
                    }
                    unmuteAudioIfNeeded()
                    return
                }
            }
        }

        // 2. Search by YouTube View IDs
        for (resId in SKIP_BUTTON_IDS) {
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(resId)
            for (node in nodes) {
                if (clickNodeOrParentOrChildren(node)) {
                    if (now - lastSkipTimestamp > 800L) {
                        lastSkipTimestamp = now
                        handleAdSkipped()
                    }
                    unmuteAudioIfNeeded()
                    return
                }
            }
        }

        // 3. Recursive tree search
        if (findAndTapSkipInNode(rootNode)) {
            if (now - lastSkipTimestamp > 800L) {
                lastSkipTimestamp = now
                handleAdSkipped()
            }
            unmuteAudioIfNeeded()
            return
        }

        // 4. Mute Audio for unskippable ads if enabled
        if (prefsManager.isMuteAdsEnabledSync()) {
            if (detectAdPlaying(rootNode)) {
                muteAudio()
            } else {
                unmuteAudioIfNeeded()
            }
        }
    }

    private fun findAndTapSkipInNode(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""

        val matches = SKIP_KEYWORDS.any { keyword ->
            text.contains(keyword) || desc.contains(keyword)
        }

        if (matches) {
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

    private fun clickNodeOrParentOrChildren(node: AccessibilityNodeInfo): Boolean {
        // Direct click
        if (node.isClickable && node.isEnabled) {
            if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
        }

        // Check child buttons
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (child.isClickable && child.isEnabled) {
                if (child.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            }
        }

        // Check parent containers
        var target: AccessibilityNodeInfo? = node.parent
        var depth = 0
        while (target != null && depth < 4) {
            if (target.isClickable && target.isEnabled) {
                if (target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            }
            for (j in 0 until target.childCount) {
                val sibling = target.getChild(j) ?: continue
                if (sibling.isClickable && sibling.isEnabled) {
                    if (sibling.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
                }
            }
            target = target.parent
            depth++
        }

        return false
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
