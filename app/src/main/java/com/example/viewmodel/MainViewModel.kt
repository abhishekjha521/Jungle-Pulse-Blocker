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
    val autoContinueEnabled = prefsManager.autoContinueEnabled
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

    fun pauseForPayment(context: Context) {
        val paused = AdSkipAccessibilityService.pauseForPayment(context)
        val isHindi = language.value == "hi"
        if (paused) {
            _isAccessibilityActive.value = false
            _feedbackMessage.value = if (isHindi)
                "💳 Payment Mode चालू! Accessibility OFF हो गई — अब PhonePe/GPay/Paytm चलाएं।"
            else
                "💳 Payment Safe Mode ON! Accessibility is OFF — PhonePe/GPay/Paytm will work now."
        } else {
            refreshAccessibilityStatus(context)
            _feedbackMessage.value = if (isHindi)
                "✅ Accessibility पहले से ही OFF है। Payment apps काम करेंगे।"
            else
                "✅ Accessibility is already OFF. Payment apps are ready to use."
        }
    }

    fun setAdSkipEnabled(enabled: Boolean) {
        prefsManager.setAdSkipEnabled(enabled)
    }

    fun setMuteAdsEnabled(enabled: Boolean) {
        prefsManager.setMuteAdsEnabled(enabled)
    }

    fun setAutoContinueEnabled(enabled: Boolean) {
        prefsManager.setAutoContinueEnabled(enabled)
    }

    fun toggleLanguage() {
        prefsManager.toggleLanguage()
    }

    fun simulateTestSkip() {
        prefsManager.recordAdSkipped()
        val isHindi = language.value == "hi"
        _feedbackMessage.value = if (isHindi) "⚡ टेस्ट सफल: 'Skip Ad' विज्ञापन स्किप हुआ!" else "⚡ Test Success: Ad Skipped!"
    }

    fun clearFeedbackMessage() {
        _feedbackMessage.value = null
    }
}
