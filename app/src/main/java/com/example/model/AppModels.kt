package com.example.model

enum class AppCategory(val displayNameEn: String, val displayNameHi: String) {
    VIDEO("Video & Streaming", "वीडियो और स्ट्रीमिंग"),
    SOCIAL("Social & Chat", "सोशल और चैटिंग"),
    GAMING("Games & Play", "गेम्स और खेल"),
    PRODUCTIVITY("Work & Study", "काम और पढ़ाई"),
    OTHER("Other Apps", "अन्य ऐप्स")
}

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val usageTimeMillis: Long,
    val launchCount: Int = 0,
    val category: AppCategory = AppCategory.OTHER,
    val dailyLimitMinutes: Int = 0, // 0 = no limit
    val iconDrawableRes: Int? = null
) {
    val usageMinutes: Int
        get() = (usageTimeMillis / (1000 * 60)).toInt()

    val formattedTime: String
        get() {
            val totalMinutes = usageMinutes
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
        }

    val isLimitExceeded: Boolean
        get() = dailyLimitMinutes in 1..usageMinutes

    val progressRatio: Float
        get() = if (dailyLimitMinutes > 0) {
            (usageMinutes.toFloat() / dailyLimitMinutes.toFloat()).coerceIn(0f, 1f)
        } else 0f
}

data class AdEvent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val appName: String,
    val actionText: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class FocusSessionState(
    val isActive: Boolean = false,
    val totalSeconds: Int = 25 * 60,
    val remainingSeconds: Int = 25 * 60,
    val title: String = "Deep Work",
    val blockedAppsCount: Int = 4
)
