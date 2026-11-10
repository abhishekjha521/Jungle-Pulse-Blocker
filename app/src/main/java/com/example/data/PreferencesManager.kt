package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("jungle_pulse_prefs", Context.MODE_PRIVATE)

    companion object {
        @Volatile
        private var INSTANCE: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context).also { INSTANCE = it }
            }
        }

        private const val KEY_AD_SKIP = "ad_skip_enabled"
        private const val KEY_MUTE_ADS = "mute_ads_enabled"
        private const val KEY_ADS_SKIPPED = "ads_skipped_count"
        private const val KEY_LANGUAGE = "app_language"
    }

    private val _adSkipEnabled = MutableStateFlow(prefs.getBoolean(KEY_AD_SKIP, true))
    val adSkipEnabled: StateFlow<Boolean> = _adSkipEnabled.asStateFlow()

    private val _muteAdsEnabled = MutableStateFlow(prefs.getBoolean(KEY_MUTE_ADS, true))
    val muteAdsEnabled: StateFlow<Boolean> = _muteAdsEnabled.asStateFlow()

    private val _adsSkippedCount = MutableStateFlow(prefs.getInt(KEY_ADS_SKIPPED, 0))
    val adsSkippedCount: StateFlow<Int> = _adsSkippedCount.asStateFlow()

    private val _language = MutableStateFlow(prefs.getString(KEY_LANGUAGE, "hi") ?: "hi")
    val language: StateFlow<String> = _language.asStateFlow()

    fun setAdSkipEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AD_SKIP, enabled).apply()
        _adSkipEnabled.value = enabled
    }

    fun setMuteAdsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MUTE_ADS, enabled).apply()
        _muteAdsEnabled.value = enabled
    }

    fun toggleLanguage() {
        val next = if (_language.value == "hi") "en" else "hi"
        prefs.edit().putString(KEY_LANGUAGE, next).apply()
        _language.value = next
    }

    fun recordAdSkipped() {
        val newCount = prefs.getInt(KEY_ADS_SKIPPED, 0) + 1
        prefs.edit().putInt(KEY_ADS_SKIPPED, newCount).apply()
        _adsSkippedCount.value = newCount
    }

    fun isAdSkipEnabledSync(): Boolean = prefs.getBoolean(KEY_AD_SKIP, true)
    fun isMuteAdsEnabledSync(): Boolean = prefs.getBoolean(KEY_MUTE_ADS, true)
}
