package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.TileService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.example.data.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference

class AdSkipAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "JunglePulseAdSkip"
        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        @Volatile
        private var instanceRef: WeakReference<AdSkipAccessibilityService>? = null

        /**
         * Instantly turns OFF the Accessibility Service using Android's native disableSelf()
         * so Payment / UPI apps (PhonePe, GPay, Paytm, Banking apps) work immediately.
         */
        fun pauseForPayment(context: Context): Boolean {
            val service = instanceRef?.get() ?: return false
            return try {
                service.unmuteAudioIfNeeded()
                service.disableSelf()
                _isRunning.value = false
                instanceRef = null
                requestTileRefresh(context)
                Handler(Looper.getMainLooper()).post {
                    try {
                        Toast.makeText(
                            context.applicationContext,
                            "💳 Payment Mode ON: Accessibility OFF! Ab PhonePe/GPay chalayein",
                            Toast.LENGTH_LONG
                        ).show()
                    } catch (_: Exception) {}
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to disable self: ${e.message}")
                false
            }
        }

        private fun requestTileRefresh(context: Context) {
            try {
                TileService.requestListeningState(
                    context.applicationContext,
                    ComponentName(context.applicationContext, PaymentModeTileService::class.java)
                )
            } catch (_: Exception) {}
        }

        // Known Skip View IDs in official YouTube app
        internal val SKIP_VIEW_IDS = setOf(
            "com.google.android.youtube:id/skip_ad_button",
            "com.google.android.youtube:id/modern_skip_ad_button",
            "com.google.android.youtube:id/ad_skip_button",
            "com.google.android.youtube:id/skip_ad_button_text",
            "com.google.android.youtube:id/skip_ad_button_container"
        )

        // Exact search strings sent to findAccessibilityNodeInfosByText
        private val SKIP_TEXT_QUERIES = listOf(
            "Skip Ad",
            "Skip Ads",
            "Skip",
            "विज्ञापन छोड़ें",
            "विज्ञापन वगळा",
            "বিজ্ঞাপন এড়িয়ে যান",
            "விளம்பரத்தைத் தவிர்"
        )

        // Approved exact lowercase skip labels (prevents matching video titles/comments containing "skip")
        internal val EXACT_SKIP_LABELS = setOf(
            "skip",
            "skip ad",
            "skip ads",
            "skip ad ›",
            "विज्ञापन छोड़ें",
            "विज्ञापन वगळा",
            "বিজ্ঞাপন এড়িয়ে যান",
            "விளம்பரத்தைத் தவிர்"
        )

        // Explicit ignore list: NEVER interact with Report, Info, Menu, or Minimize UI nodes
        internal val IGNORED_NODE_KEYWORDS = setOf(
            "report",
            "info",
            "about this ad",
            "about ad",
            "why this ad",
            "my ad center",
            "stop seeing",
            "ad_info",
            "why_this_ad",
            "overflow",
            "menu",
            "more options",
            "minimize",
            "collapse",
            "player_collapse",
            "close",
            "share",
            "शिकायत",
            "जानकारी",
            "विज्ञापन के बारे में"
        )

        // YouTube 1-2 hour auto-pause / break prompts ("Video paused. Continue watching?")
        private val CONTINUE_PROMPT_QUERIES = listOf(
            "Continue watching?",
            "Video paused",
            "Time to take a break",
            "देखना जारी रखें",
            "वीडियो को रोक दिया गया"
        )

        internal val CONTINUE_BUTTON_LABELS = setOf(
            "yes",
            "continue",
            "ok",
            "dismiss",
            "हाँ",
            "जारी रखें",
            "ठीक है"
        )

        internal val CONTINUE_BUTTON_IDS = setOf(
            "android:id/button1",
            "com.google.android.youtube:id/modern_dialog_positive_button"
        )

        /**
         * Returns true if the given metadata belongs to a Report, Info, Menu, or Minimize element
         * that MUST be explicitly ignored.
         */
        internal fun isIgnoredMetadata(text: String?, contentDesc: String?, viewId: String?): Boolean {
            val t = text?.trim()?.lowercase() ?: ""
            val d = contentDesc?.trim()?.lowercase() ?: ""
            val id = viewId?.trim()?.lowercase() ?: ""

            return IGNORED_NODE_KEYWORDS.any { keyword ->
                t.contains(keyword) || d.contains(keyword) || id.contains(keyword)
            }
        }

        /**
         * Returns true ONLY if the metadata represents a genuine YouTube Skip Ad button/label
         * and does NOT match any Report/Info/Countdown/unrelated text.
         */
        internal fun isGenuineSkipMetadata(
            text: String?,
            contentDesc: String?,
            viewId: String?,
            matchedByViewId: Boolean
        ): Boolean {
            if (isIgnoredMetadata(text, contentDesc, viewId)) return false

            val t = text?.trim()?.lowercase() ?: ""
            val d = contentDesc?.trim()?.lowercase() ?: ""
            val id = viewId?.trim()?.lowercase() ?: ""

            // Ignore countdown-only text such as "You can skip in 5s" or "Skip in 4"
            if (t.contains("skip in") || d.contains("skip in")) return false

            if (matchedByViewId && id in SKIP_VIEW_IDS) {
                return t.isEmpty() || t in EXACT_SKIP_LABELS || d in EXACT_SKIP_LABELS
            }

            return t in EXACT_SKIP_LABELS || d in EXACT_SKIP_LABELS
        }

        /**
         * Returns true if a button belongs to YouTube's "Video paused. Continue watching?" dialog.
         */
        internal fun isValidContinueButtonMetadata(
            text: String?,
            contentDesc: String?,
            viewId: String?
        ): Boolean {
            if (isIgnoredMetadata(text, contentDesc, viewId)) return false
            val t = text?.trim()?.lowercase() ?: ""
            val d = contentDesc?.trim()?.lowercase() ?: ""
            val id = viewId?.trim()?.lowercase() ?: ""
            return t in CONTINUE_BUTTON_LABELS || d in CONTINUE_BUTTON_LABELS || id in CONTINUE_BUTTON_IDS
        }
    }

    private lateinit var prefsManager: PreferencesManager
    private var audioManager: AudioManager? = null
    private var originalVolume: Int = -1
    private var isCurrentlyMuted = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastSkipTimestamp = 0L
    private var lastResumeTimestamp = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instanceRef = WeakReference(this)
        _isRunning.value = true
        prefsManager = PreferencesManager.getInstance(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        requestTileRefresh(this)
        Log.d(TAG, "Jungle Pulse AdSkipService connected!")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkg = event.packageName?.toString() ?: ""
        if (pkg.isNotEmpty() && !pkg.contains("youtube")) return

        val rootNode = rootInActiveWindow ?: event.source ?: return

        try {
            // 1. Auto-Resume YouTube if "Video paused. Continue watching?" pops up after ~1-2 hours
            if (prefsManager.isAutoContinueEnabledSync()) {
                if (handleContinueWatchingPrompt(rootNode)) {
                    return
                }
            }

            // 2. Skip YouTube Ads if enabled
            if (prefsManager.isAdSkipEnabledSync()) {
                inspectAndSkipAds(rootNode)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in skip handler: ${e.message}")
        }
    }

    /**
     * Prevents YouTube from stopping playback after ~1-2 hours by detecting the
     * "Video paused. Continue watching?" or "Time to take a break" dialog and tapping Yes/Continue.
     */
    private fun handleContinueWatchingPrompt(rootNode: AccessibilityNodeInfo): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastResumeTimestamp < 3000L) return false

        var promptVisible = false
        for (promptText in CONTINUE_PROMPT_QUERIES) {
            val matches = rootNode.findAccessibilityNodeInfosByText(promptText)
            if (matches.any { it.isVisibleToUser }) {
                promptVisible = true
                break
            }
        }

        if (!promptVisible) return false

        // Search positive dialog IDs first
        for (btnId in CONTINUE_BUTTON_IDS) {
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(btnId)
            for (node in nodes) {
                if (!node.isVisibleToUser || shouldIgnoreNodeTree(node)) continue
                if (node.isClickable && node.isEnabled && isValidContinueButtonMetadata(
                        node.text?.toString(),
                        node.contentDescription?.toString(),
                        node.viewIdResourceName
                    )
                ) {
                    if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        lastResumeTimestamp = now
                        showToast("🔁 Video Resumed (Continue Watching)")
                        return true
                    }
                }
            }
        }

        // Search positive button text ("Yes", "Continue", "OK", "हाँ")
        for (label in CONTINUE_BUTTON_LABELS) {
            val nodes = rootNode.findAccessibilityNodeInfosByText(label)
            for (node in nodes) {
                if (!node.isVisibleToUser || shouldIgnoreNodeTree(node)) continue
                if (!isValidContinueButtonMetadata(
                        node.text?.toString(),
                        node.contentDescription?.toString(),
                        node.viewIdResourceName
                    )
                ) continue
                if (performSafeSkipClick(node)) {
                    lastResumeTimestamp = now
                    showToast("🔁 Video Resumed (Continue Watching)")
                    return true
                }
            }
        }

        return false
    }

    private fun inspectAndSkipAds(rootNode: AccessibilityNodeInfo) {
        val now = System.currentTimeMillis()

        // 1. Search by verified YouTube Skip View IDs
        for (viewId in SKIP_VIEW_IDS) {
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)
            for (node in nodes) {
                if (!node.isVisibleToUser) continue
                if (shouldIgnoreNodeTree(node)) continue
                if (!isGenuineSkipMetadata(
                        node.text?.toString(),
                        node.contentDescription?.toString(),
                        node.viewIdResourceName,
                        matchedByViewId = true
                    )
                ) continue

                if (performSafeSkipClick(node)) {
                    if (now - lastSkipTimestamp > 800L) {
                        lastSkipTimestamp = now
                        handleAdSkipped()
                    }
                    unmuteAudioIfNeeded()
                    return
                }
            }
        }

        // 2. Search by exact Skip Ad / Skip text
        for (query in SKIP_TEXT_QUERIES) {
            val nodes = rootNode.findAccessibilityNodeInfosByText(query)
            for (node in nodes) {
                if (!node.isVisibleToUser) continue
                if (shouldIgnoreNodeTree(node)) continue
                if (!isGenuineSkipMetadata(
                        node.text?.toString(),
                        node.contentDescription?.toString(),
                        node.viewIdResourceName,
                        matchedByViewId = false
                    )
                ) continue

                if (performSafeSkipClick(node)) {
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
     * Checks the node and its direct children to make sure it does not contain any
     * Report, Info, Menu, or Minimize nodes.
     */
    private fun shouldIgnoreNodeTree(node: AccessibilityNodeInfo): Boolean {
        if (isIgnoredMetadata(
                node.text?.toString(),
                node.contentDescription?.toString(),
                node.viewIdResourceName
            )
        ) {
            return true
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (isIgnoredMetadata(
                    child.text?.toString(),
                    child.contentDescription?.toString(),
                    child.viewIdResourceName
                )
            ) {
                return true
            }
        }
        return false
    }

    /**
     * Strictly clicks ONLY the validated skip/continue node, a validated child, or a compact
     * button wrapper parent (never a full-screen player or report/info container).
     */
    private fun performSafeSkipClick(node: AccessibilityNodeInfo): Boolean {
        if (shouldIgnoreNodeTree(node)) return false

        // 1. Direct click on the validated node itself
        if (node.isClickable && node.isEnabled) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }

        // 2. Direct child button if wrapped inside a skip container
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (!child.isVisibleToUser) continue
            if (shouldIgnoreNodeTree(child)) continue
            val isChildValidSkip = isGenuineSkipMetadata(
                child.text?.toString(),
                child.contentDescription?.toString(),
                child.viewIdResourceName,
                matchedByViewId = child.viewIdResourceName in SKIP_VIEW_IDS
            )
            if (isChildValidSkip && child.isClickable && child.isEnabled) {
                if (child.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            }
        }

        // 3. Compact parent button container (max 2 levels) ONLY if it's clearly a button wrapper
        //    and contains NO Report, Info, Menu, or Minimize children.
        var parent = node.parent
        var depth = 0
        while (parent != null && depth < 2) {
            if (shouldIgnoreNodeTree(parent)) {
                return false
            }
            val parentId = parent.viewIdResourceName?.lowercase() ?: ""
            val isCompactButtonWrapper = parent.childCount <= 3 || parentId in SKIP_VIEW_IDS
            if (isCompactButtonWrapper && parent.isClickable && parent.isEnabled && parent.isVisibleToUser) {
                return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            parent = parent.parent
            depth++
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

    private fun showToast(message: String) {
        mainHandler.post {
            try {
                Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {}
        }
    }

    override fun onInterrupt() {
        _isRunning.value = false
        instanceRef = null
        unmuteAudioIfNeeded()
        requestTileRefresh(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        _isRunning.value = false
        instanceRef = null
        unmuteAudioIfNeeded()
        requestTileRefresh(this)
    }
}
