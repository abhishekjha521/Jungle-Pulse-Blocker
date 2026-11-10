package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AdEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("focus_shield_prefs", Context.MODE_PRIVATE)

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
        private const val KEY_CONTINUE_WATCHING = "continue_watching_enabled"
        private const val KEY_VPN_ADBLOCK = "vpn_adblock_enabled"
        private const val KEY_ADS_SKIPPED = "ads_skipped_count"
        private const val KEY_ADS_MUTED = "ads_muted_count"
        private const val KEY_ADS_BLOCKED_DNS = "ads_blocked_dns_count"
        private const val KEY_TIME_SAVED_SEC = "time_saved_seconds"
        private const val KEY_LANGUAGE = "app_language"
        private const val KEY_LIMIT_PREFIX = "app_limit_"
    }

    private val _adSkipEnabled = MutableStateFlow(prefs.getBoolean(KEY_AD_SKIP, true))
    val adSkipEnabled: StateFlow<Boolean> = _adSkipEnabled.asStateFlow()

    private val _muteAdsEnabled = MutableStateFlow(prefs.getBoolean(KEY_MUTE_ADS, true))
    val muteAdsEnabled: StateFlow<Boolean> = _muteAdsEnabled.asStateFlow()

    private val _continueWatchingEnabled = MutableStateFlow(prefs.getBoolean(KEY_CONTINUE_WATCHING, true))
    val continueWatchingEnabled: StateFlow<Boolean> = _continueWatchingEnabled.asStateFlow()

    private val _vpnAdBlockEnabled = MutableStateFlow(prefs.getBoolean(KEY_VPN_ADBLOCK, false))
    val vpnAdBlockEnabled: StateFlow<Boolean> = _vpnAdBlockEnabled.asStateFlow()

    private val _adsSkippedCount = MutableStateFlow(prefs.getInt(KEY_ADS_SKIPPED, 18))
    val adsSkippedCount: StateFlow<Int> = _adsSkippedCount.asStateFlow()

    private val _adsBlockedDnsCount = MutableStateFlow(prefs.getInt(KEY_ADS_BLOCKED_DNS, 84))
    val adsBlockedDnsCount: StateFlow<Int> = _adsBlockedDnsCount.asStateFlow()

    private val _timeSavedSeconds = MutableStateFlow(prefs.getLong(KEY_TIME_SAVED_SEC, 360L))
    val timeSavedSeconds: StateFlow<Long> = _timeSavedSeconds.asStateFlow()

    private val _language = MutableStateFlow(prefs.getString(KEY_LANGUAGE, "en") ?: "en")
    val language: StateFlow<String> = _language.asStateFlow()

    private val _recentEvents = MutableStateFlow<List<AdEvent>>(
        listOf(
            AdEvent(appName = "YouTube", actionText = "Auto-Skipped Video Ad (Saved 15s)"),
            AdEvent(appName = "YouTube", actionText = "Muted Ad Audio during non-skippable ad"),
            AdEvent(appName = "Chrome Browser", actionText = "Blocked Ad Tracking Domain"),
            AdEvent(appName = "YouTube", actionText = "Auto-Skipped Mid-Roll Ad")
        )
    )
    val recentEvents: StateFlow<List<AdEvent>> = _recentEvents.asStateFlow()

    fun setAdSkipEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AD_SKIP, enabled).apply()
        _adSkipEnabled.value = enabled
    }

    fun setMuteAdsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MUTE_ADS, enabled).apply()
        _muteAdsEnabled.value = enabled
    }

    fun setContinueWatchingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CONTINUE_WATCHING, enabled).apply()
        _continueWatchingEnabled.value = enabled
    }

    fun setVpnAdBlockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VPN_ADBLOCK, enabled).apply()
        _vpnAdBlockEnabled.value = enabled
    }

    fun setLanguage(lang: String) {
        prefs.edit().putString(KEY_LANGUAGE, lang).apply()
        _language.value = lang
    }

    fun recordAdSkipped(appName: String = "YouTube", savedSec: Long = 15L) {
        val newCount = prefs.getInt(KEY_ADS_SKIPPED, 18) + 1
        val newTime = prefs.getLong(KEY_TIME_SAVED_SEC, 360L) + savedSec
        prefs.edit()
            .putInt(KEY_ADS_SKIPPED, newCount)
            .putLong(KEY_TIME_SAVED_SEC, newTime)
            .apply()
        _adsSkippedCount.value = newCount
        _timeSavedSeconds.value = newTime

        val event = AdEvent(
            appName = appName,
            actionText = "Auto-Skipped Video Ad (Saved ${savedSec}s)"
        )
        _recentEvents.value = (listOf(event) + _recentEvents.value).take(20)
    }

    fun recordAdMuted(appName: String = "YouTube") {
        val newCount = prefs.getInt(KEY_ADS_MUTED, 0) + 1
        prefs.edit().putInt(KEY_ADS_MUTED, newCount).apply()
        val event = AdEvent(
            appName = appName,
            actionText = "Muted ad audio stream"
        )
        _recentEvents.value = (listOf(event) + _recentEvents.value).take(20)
    }

    fun recordDnsAdBlocked(domain: String = "adservice.google.com") {
        val newCount = prefs.getInt(KEY_ADS_BLOCKED_DNS, 84) + 1
        prefs.edit().putInt(KEY_ADS_BLOCKED_DNS, newCount).apply()
        _adsBlockedDnsCount.value = newCount

        val event = AdEvent(
            appName = "DNS Filter",
            actionText = "Blocked ad query: $domain"
        )
        _recentEvents.value = (listOf(event) + _recentEvents.value).take(20)
    }

    fun setAppLimit(packageName: String, limitMinutes: Int) {
        prefs.edit().putInt(KEY_LIMIT_PREFIX + packageName, limitMinutes).apply()
    }

    fun getAppLimit(packageName: String): Int {
        // Preset helpful defaults if not set yet:
        val saved = prefs.getInt(KEY_LIMIT_PREFIX + packageName, -1)
        if (saved != -1) return saved
        return when (packageName) {
            "com.google.android.youtube" -> 45
            "com.instagram.android" -> 30
            "com.facebook.katana" -> 30
            else -> 0
        }
    }

    // Direct synchronous reads for Background Services:
    fun isAdSkipEnabledSync(): Boolean = prefs.getBoolean(KEY_AD_SKIP, true)
    fun isMuteAdsEnabledSync(): Boolean = prefs.getBoolean(KEY_MUTE_ADS, true)
    fun isContinueWatchingEnabledSync(): Boolean = prefs.getBoolean(KEY_CONTINUE_WATCHING, true)
}
