package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.PreferencesManager
import com.example.model.AppCategory
import com.example.model.AppUsageInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun testAppNameString() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Jungle Pulse Blocker", appName)
    }

    @Test
    fun testPreferencesManagerAdSkipAndLimits() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferencesManager.getInstance(context)

        prefs.setAdSkipEnabled(true)
        assertTrue(prefs.isAdSkipEnabledSync())

        prefs.setAppLimit("com.google.android.youtube", 45)
        assertEquals(45, prefs.getAppLimit("com.google.android.youtube"))

        val initialSkipped = prefs.adsSkippedCount.value
        prefs.recordAdSkipped("YouTube", 15L)
        assertEquals(initialSkipped + 1, prefs.adsSkippedCount.value)
    }

    @Test
    fun testAppUsageInfoLimitCalculation() {
        val normalUsage = AppUsageInfo(
            packageName = "com.google.android.youtube",
            appName = "YouTube",
            usageTimeMillis = 30 * 60 * 1000L, // 30 mins
            dailyLimitMinutes = 45,
            category = AppCategory.VIDEO
        )
        assertFalse(normalUsage.isLimitExceeded)
        assertEquals("30m", normalUsage.formattedTime)

        val exceededUsage = AppUsageInfo(
            packageName = "com.google.android.youtube",
            appName = "YouTube",
            usageTimeMillis = 50 * 60 * 1000L, // 50 mins
            dailyLimitMinutes = 45,
            category = AppCategory.VIDEO
        )
        assertTrue(exceededUsage.isLimitExceeded)
    }
}
