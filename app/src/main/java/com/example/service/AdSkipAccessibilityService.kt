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

        // Verified YouTube Skip Button Resource IDs
        private val SKIP_BUTTON_IDS = setOf(
            "com.google.android.youtube:id/skip_ad_button",
            "com.google.android.youtube:id/modern_skip_ad_button",
            "com.google.android.youtube:id/ad_skip_button",
            "com.google.android.youtube:id/skip_ad_button_container"
        )

        // Native search query strings for direct skip buttons
        private val SKIP_TEXT_QUERIES = listOf(
            "Skip Ad",
            "Skip ads",
            "Skip",
            "विज्ञापन छोड़ें",
            "विज्ञापन वगळा",
            "বিজ্ঞাপন এড়িয়ে যান",
            "விளம்பரத்தைத் தவிர்"
        )

        // Strict blacklist: NEVER click or interact with anything containing these terms
        private val FORBIDDEN_KEYWORDS = setOf(
            "report",
            "about this ad",
            "why this ad",
            "stop seeing",
            "ad info",
            "my ad center",
            "शिकायत",
            "विज्ञापन के बारे में",
            "विज्ञापन क्यों",
            "ad_info_button",
            "overflow_menu",
            "why_this_ad"
        )

        // Phrases that indicate an accidental report / info sheet is open
        private val REPORT_POPUP_INDICATORS = listOf(
            "About this ad",
            "Why this ad?",
            "Report this ad",
            "My Ad Center",
            "Stop seeing this ad",
            "विज्ञापन के बारे में",
            "विज्ञापन की शिकायत करें",
            "इस विज्ञापन की शिकायत करें"
        )

        // Common dismiss/close button IDs and texts
        private val CLOSE_BUTTON_IDS = setOf(
            "com.google.android.youtube:id/close_button",
            "com.google.android.youtube:id/touch_outside",
            "com.google.android.youtube:id/cancel_button",
            "com.google.android.youtube:id/dismiss"
        )

        private val CLOSE_BUTTON_TEXTS = listOf(
            "Close",
            "Dismiss",
            "Cancel",
            "Done",
            "बंद करें",
            "रद्द करें"
        )
    }

    private lateinit var prefsManager: PreferencesManager
    private var audioManager: AudioManager? = null
    private var originalVolume: Int = -1
    private var isCurrentlyMuted = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastSkipTimestamp = 0L
    private var lastDismissTimestamp = 0L

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
            inspectAndProcessWindow(rootNode)
        } catch (e: Exception) {
            Log.e(TAG, "Error inspecting node tree: ${e.message}")
        }
    }

    private fun inspectAndProcessWindow(rootNode: AccessibilityNodeInfo) {
        val now = System.currentTimeMillis()

        // 1. SHIELD WATCHDOG: Check if a Report or Info sheet is open.
        // If so, dismiss it immediately so user playback is uninterrupted!
        if (dismissReportSheetIfOpen(rootNode, now)) {
            return
        }

        // 2. SEARCH BY VERIFIED YOUTUBE VIEW IDs
        for (resId in SKIP_BUTTON_IDS) {
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(resId)
            for (node in nodes) {
                if (clickVerifiedSkipNode(node)) {
                    if (now - lastSkipTimestamp > 800L) {
                        lastSkipTimestamp = now
                        handleAdSkipped()
                    }
                    unmuteAudioIfNeeded()
                    return
                }
            }
        }

        // 3. SEARCH BY DIRECT SKIP TEXT
        for (query in SKIP_TEXT_QUERIES) {
            val nodes = rootNode.findAccessibilityNodeInfosByText(query)
            for (node in nodes) {
                if (clickVerifiedSkipNode(node)) {
                    if (now - lastSkipTimestamp > 800L) {
                        lastSkipTimestamp = now
                        handleAdSkipped()
                    }
                    unmuteAudioIfNeeded()
                    return
                }
            }
        }

        // 4. Mute Audio during unskippable ads if enabled (without touching any report/info UI)
        if (prefsManager.isMuteAdsEnabledSync()) {
            if (detectAdPlaying(rootNode)) {
                muteAudio()
            } else {
                unmuteAudioIfNeeded()
            }
        }
    }

    /**
     * Auto-dismisses any "Report this ad" / "About this ad" sheet if it ever opens.
     */
    private fun dismissReportSheetIfOpen(rootNode: AccessibilityNodeInfo, now: Long): Boolean {
        if (now - lastDismissTimestamp < 500L) return false

        var isReportSheetShowing = false
        for (indicator in REPORT_POPUP_INDICATORS) {
            val matches = rootNode.findAccessibilityNodeInfosByText(indicator)
            if (matches.isNotEmpty()) {
                isReportSheetShowing = true
                break
            }
        }

        if (!isReportSheetShowing) return false

        Log.w(TAG, "Report/Info dialog detected on YouTube! Auto-dismissing to prevent user interruption.")
        lastDismissTimestamp = now

        // Step A: Look for explicit close or dismiss buttons
        for (closeId in CLOSE_BUTTON_IDS) {
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(closeId)
            for (node in nodes) {
                if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    return true
                }
            }
        }

        // Step B: Look for close text
        for (closeText in CLOSE_BUTTON_TEXTS) {
            val nodes = rootNode.findAccessibilityNodeInfosByText(closeText)
            for (node in nodes) {
                if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    return true
                }
            }
        }

        // Step C: Fallback to System Global Back Action to close bottom sheet
        performGlobalAction(GLOBAL_ACTION_BACK)
        return true
    }

    /**
     * Strictly verifies that a node is genuinely a skip target and NOT a report or info button.
     */
    private fun isNodeForbidden(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val viewId = node.viewIdResourceName?.lowercase() ?: ""

        return FORBIDDEN_KEYWORDS.any { keyword ->
            text.contains(keyword) || desc.contains(keyword) || viewId.contains(keyword)
        }
    }

    private fun isGenuineSkipTarget(text: String, desc: String, viewId: String): Boolean {
        // Exclude countdown-only text like "you can skip in 5s" if there's no actual skip action
        if (text.contains("skip in") && !text.contains("skip ad") && !text.equals("skip", ignoreCase = true)) {
            // Unskippable countdown state
            return false
        }

        val hasSkipKeyword = text.contains("skip ad") ||
                text.equals("skip", ignoreCase = true) ||
                text.contains("विज्ञापन छोड़ें") ||
                desc.contains("skip ad") ||
                desc.equals("skip", ignoreCase = true) ||
                viewId.contains("skip_ad_button")

        return hasSkipKeyword
    }

    /**
     * Clicks only the targeted node, its direct children, or its direct parent container.
     * CRITICAL: Sibling traversal is completely REMOVED to avoid ever clicking adjacent "ⓘ" info buttons.
     */
    private fun clickVerifiedSkipNode(node: AccessibilityNodeInfo): Boolean {
        if (isNodeForbidden(node)) return false

        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val viewId = node.viewIdResourceName?.lowercase() ?: ""

        // 1. Direct click on node
        if (node.isClickable && node.isEnabled) {
            if (isGenuineSkipTarget(text, desc, viewId)) {
                return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }

        // 2. Click immediate child if button is wrapped
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (!isNodeForbidden(child) && child.isClickable && child.isEnabled) {
                val cText = child.text?.toString()?.lowercase() ?: ""
                val cDesc = child.contentDescription?.toString()?.lowercase() ?: ""
                val cId = child.viewIdResourceName?.lowercase() ?: ""
                if (isGenuineSkipTarget(cText, cDesc, cId)) {
                    if (child.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
                }
            }
        }

        // 3. Click immediate parent if container is the clickable wrapper
        val parent = node.parent
        if (parent != null && !isNodeForbidden(parent) && parent.isClickable && parent.isEnabled) {
            val pText = parent.text?.toString()?.lowercase() ?: ""
            val pDesc = parent.contentDescription?.toString()?.lowercase() ?: ""
            val pId = parent.viewIdResourceName?.lowercase() ?: ""
            if (isGenuineSkipTarget(pText, pDesc, pId) || pId.contains("skip_ad_button")) {
                return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
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
