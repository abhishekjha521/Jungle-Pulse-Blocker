package com.example.data

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Process
import com.example.model.AppCategory
import com.example.model.AppUsageInfo
import java.util.Calendar

class UsageStatsHelper(private val context: Context) {

    private val preferencesManager = PreferencesManager.getInstance(context)

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getTodayUsageStats(): List<AppUsageInfo> {
        if (!hasUsageStatsPermission()) {
            return getPreviewUsageStats()
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return getPreviewUsageStats()

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val usageStatsList: List<UsageStats> = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: emptyList()

        if (usageStatsList.isEmpty()) {
            return getPreviewUsageStats()
        }

        val pm = context.packageManager
        val aggregatedMap = mutableMapOf<String, Long>()

        for (stat in usageStatsList) {
            if (stat.totalTimeInForeground > 30_000) { // filter out < 30s
                val current = aggregatedMap[stat.packageName] ?: 0L
                aggregatedMap[stat.packageName] = current + stat.totalTimeInForeground
            }
        }

        val resultList = mutableListOf<AppUsageInfo>()

        for ((pkg, timeMillis) in aggregatedMap) {
            // Filter system launcher/system packages to keep list clean
            if (pkg == "com.android.systemui" || pkg == context.packageName) continue

            val appName = try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                pkg.substringAfterLast('.')
            }

            val category = resolveCategory(pkg)
            val limit = preferencesManager.getAppLimit(pkg)

            resultList.add(
                AppUsageInfo(
                    packageName = pkg,
                    appName = appName,
                    usageTimeMillis = timeMillis,
                    category = category,
                    dailyLimitMinutes = limit
                )
            )
        }

        // Sort by longest usage first
        return resultList.sortedByDescending { it.usageTimeMillis }
    }

    private fun resolveCategory(packageName: String): AppCategory {
        return when {
            packageName.contains("youtube") || packageName.contains("netflix") ||
                    packageName.contains("hotstar") || packageName.contains("primevideo") ||
                    packageName.contains("spotify") || packageName.contains("twitch") -> AppCategory.VIDEO

            packageName.contains("instagram") || packageName.contains("facebook") ||
                    packageName.contains("twitter") || packageName.contains("whatsapp") ||
                    packageName.contains("tiktok") || packageName.contains("snapchat") ||
                    packageName.contains("telegram") || packageName.contains("threads") -> AppCategory.SOCIAL

            packageName.contains("game") || packageName.contains("pubg") ||
                    packageName.contains("roblox") || packageName.contains("supercell") ||
                    packageName.contains("candycrush") -> AppCategory.GAMING

            packageName.contains("slack") || packageName.contains("notion") ||
                    packageName.contains("docs") || packageName.contains("sheets") ||
                    packageName.contains("gmail") || packageName.contains("outlook") ||
                    packageName.contains("calculator") -> AppCategory.PRODUCTIVITY

            else -> AppCategory.OTHER
        }
    }

    // Realistic preview data shown when permission is not yet enabled
    private fun getPreviewUsageStats(): List<AppUsageInfo> {
        return listOf(
            AppUsageInfo(
                packageName = "com.google.android.youtube",
                appName = "YouTube",
                usageTimeMillis = 68 * 60 * 1000L, // 1h 8m
                category = AppCategory.VIDEO,
                dailyLimitMinutes = preferencesManager.getAppLimit("com.google.android.youtube")
            ),
            AppUsageInfo(
                packageName = "com.instagram.android",
                appName = "Instagram",
                usageTimeMillis = 42 * 60 * 1000L, // 42m
                category = AppCategory.SOCIAL,
                dailyLimitMinutes = preferencesManager.getAppLimit("com.instagram.android")
            ),
            AppUsageInfo(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                usageTimeMillis = 35 * 60 * 1000L, // 35m
                category = AppCategory.SOCIAL,
                dailyLimitMinutes = preferencesManager.getAppLimit("com.whatsapp")
            ),
            AppUsageInfo(
                packageName = "com.android.chrome",
                appName = "Chrome",
                usageTimeMillis = 24 * 60 * 1000L, // 24m
                category = AppCategory.PRODUCTIVITY,
                dailyLimitMinutes = 0
            ),
            AppUsageInfo(
                packageName = "com.google.android.apps.youtube.music",
                appName = "YouTube Music",
                usageTimeMillis = 18 * 60 * 1000L, // 18m
                category = AppCategory.VIDEO,
                dailyLimitMinutes = 0
            )
        )
    }
}
