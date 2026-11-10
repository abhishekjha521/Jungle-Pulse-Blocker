package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import com.example.data.PreferencesManager
import com.example.service.AdSkipAccessibilityService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsManager = PreferencesManager.getInstance(application)

    val adSkipEnabled = prefsManager.adSkipEnabled
    val muteAdsEnabled = prefsManager.muteAdsEnabled
    val adsSkippedCount = prefsManager.adsSkippedCount
    val language = prefsManager.language

    private val _isAccessibilityActive = MutableStateFlow(false)
    val isAccessibilityActive: StateFlow<Boolean> = _isAccessibilityActive.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    init {
        refreshAccessibilityStatus(application)
    }

    fun refreshAccessibilityStatus(context: Context) {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        val isEnabledInSystem = enabledServices.contains(context.packageName)
        _isAccessibilityActive.value = isEnabledInSystem || AdSkipAccessibilityService.isRunning.value
    }

    fun setAdSkipEnabled(enabled: Boolean) {
        prefsManager.setAdSkipEnabled(enabled)
    }

    fun setMuteAdsEnabled(enabled: Boolean) {
        prefsManager.setMuteAdsEnabled(enabled)
    }

    fun toggleLanguage() {
        prefsManager.toggleLanguage()
    }

    fun simulateTestSkip() {
        prefsManager.recordAdSkipped()
        val isHindi = language.value == "hi"
        _feedbackMessage.value = if (isHindi) "⚡ टेस्ट सफल: 'Skip in' विज्ञापन स्किप हुआ!" else "⚡ Test Success: Ad Skipped!"
    }

    fun clearFeedbackMessage() {
        _feedbackMessage.value = null
    }
}
