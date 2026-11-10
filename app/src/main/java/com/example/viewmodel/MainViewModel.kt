package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PreferencesManager
import com.example.data.UsageStatsHelper
import com.example.model.AppUsageInfo
import com.example.model.FocusSessionState
import com.example.service.AdBlockVpnService
import com.example.service.AdSkipAccessibilityService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsManager = PreferencesManager.getInstance(application)
    private val usageStatsHelper = UsageStatsHelper(application)

    // Reactive Settings from PreferencesManager
    val adSkipEnabled = prefsManager.adSkipEnabled
    val muteAdsEnabled = prefsManager.muteAdsEnabled
    val continueWatchingEnabled = prefsManager.continueWatchingEnabled
    val vpnAdBlockEnabled = prefsManager.vpnAdBlockEnabled
    val adsSkippedCount = prefsManager.adsSkippedCount
    val adsBlockedDnsCount = prefsManager.adsBlockedDnsCount
    val timeSavedSeconds = prefsManager.timeSavedSeconds
    val language = prefsManager.language
    val recentEvents = prefsManager.recentEvents

    // Live Service State
    val isAccessibilityActive: StateFlow<Boolean> = AdSkipAccessibilityService.isRunning
    val isVpnActive: StateFlow<Boolean> = AdBlockVpnService.isRunning

    // Usage Stats State
    private val _usageStats = MutableStateFlow<List<AppUsageInfo>>(emptyList())
    val usageStats: StateFlow<List<AppUsageInfo>> = _usageStats.asStateFlow()

    private val _hasUsagePermission = MutableStateFlow(false)
    val hasUsagePermission: StateFlow<Boolean> = _hasUsagePermission.asStateFlow()

    // Focus Timer State
    private val _focusSession = MutableStateFlow(FocusSessionState())
    val focusSession: StateFlow<FocusSessionState> = _focusSession.asStateFlow()
    private var focusJob: Job? = null

    // SnackBar / Toast message channel
    private val _userFeedbackMessage = MutableStateFlow<String?>(null)
    val userFeedbackMessage: StateFlow<String?> = _userFeedbackMessage.asStateFlow()

    init {
        refreshUsageStats()
    }

    fun refreshUsageStats() {
        val hasPerm = usageStatsHelper.hasUsageStatsPermission()
        _hasUsagePermission.value = hasPerm
        val list = usageStatsHelper.getTodayUsageStats()
        _usageStats.value = list
    }

    fun setAdSkipEnabled(enabled: Boolean) {
        prefsManager.setAdSkipEnabled(enabled)
    }

    fun setMuteAdsEnabled(enabled: Boolean) {
        prefsManager.setMuteAdsEnabled(enabled)
    }

    fun setContinueWatchingEnabled(enabled: Boolean) {
        prefsManager.setContinueWatchingEnabled(enabled)
    }

    fun toggleLanguage() {
        val current = language.value
        val newLang = if (current == "en") "hi" else "en"
        prefsManager.setLanguage(newLang)
    }

    fun setAppLimit(packageName: String, limitMinutes: Int) {
        prefsManager.setAppLimit(packageName, limitMinutes)
        refreshUsageStats()
        val msg = if (language.value == "hi") {
            "नया स्क्रीन टाइम लिमिट सेट हुआ: $limitMinutes मिनट"
        } else {
            "New daily limit set: $limitMinutes min"
        }
        _userFeedbackMessage.value = msg
    }

    fun simulateAdSkipped() {
        prefsManager.recordAdSkipped("YouTube", 15L)
        val msg = if (language.value == "hi") {
            "⚡ विज्ञापन सफलतापूर्वक स्किप हुआ! (+15s बचाए)"
        } else {
            "⚡ Ad skipped instantly! (+15s saved)"
        }
        _userFeedbackMessage.value = msg
    }

    fun clearFeedbackMessage() {
        _userFeedbackMessage.value = null
    }

    fun startFocusSession(minutes: Int, title: String) {
        focusJob?.cancel()
        val totalSec = minutes * 60
        _focusSession.value = FocusSessionState(
            isActive = true,
            totalSeconds = totalSec,
            remainingSeconds = totalSec,
            title = title,
            blockedAppsCount = 4
        )

        focusJob = viewModelScope.launch {
            while (isActive && _focusSession.value.remainingSeconds > 0) {
                delay(1000L)
                val remaining = _focusSession.value.remainingSeconds - 1
                _focusSession.value = _focusSession.value.copy(remainingSeconds = remaining)
            }
            if (_focusSession.value.remainingSeconds <= 0) {
                _focusSession.value = _focusSession.value.copy(isActive = false)
                _userFeedbackMessage.value = if (language.value == "hi") {
                    "🎉 बधाई! फ़ोकस सेशन सफलतापूर्वक पूरा हुआ!"
                } else {
                    "🎉 Focus Session Completed! Great job staying focused."
                }
            }
        }
    }

    fun stopFocusSession() {
        focusJob?.cancel()
        _focusSession.value = _focusSession.value.copy(isActive = false)
    }
}
