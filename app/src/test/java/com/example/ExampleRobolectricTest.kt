package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.PreferencesManager
import com.example.service.AdSkipAccessibilityService
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
    fun testPreferencesManagerAdSkip() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferencesManager.getInstance(context)

        prefs.setAdSkipEnabled(true)
        assertTrue(prefs.isAdSkipEnabledSync())

        val initialSkipped = prefs.adsSkippedCount.value
        prefs.recordAdSkipped()
        assertEquals(initialSkipped + 1, prefs.adsSkippedCount.value)
    }

    @Test
    fun testExplicitlyIgnoresReportAndInfoNodes() {
        assertTrue(AdSkipAccessibilityService.isIgnoredMetadata("Report ad", null, null))
        assertTrue(AdSkipAccessibilityService.isIgnoredMetadata(null, "Ad info", null))
        assertTrue(AdSkipAccessibilityService.isIgnoredMetadata("Why this ad?", null, null))
        assertTrue(AdSkipAccessibilityService.isIgnoredMetadata(null, "About this ad", null))
        assertTrue(AdSkipAccessibilityService.isIgnoredMetadata(null, null, "com.google.android.youtube:id/ad_info_button"))
        assertTrue(AdSkipAccessibilityService.isIgnoredMetadata("More options", "Overflow menu", null))
        assertTrue(AdSkipAccessibilityService.isIgnoredMetadata(null, "Minimize player", "com.google.android.youtube:id/player_collapse_button"))

        assertFalse(AdSkipAccessibilityService.isGenuineSkipMetadata("Report", null, null, matchedByViewId = false))
        assertFalse(AdSkipAccessibilityService.isGenuineSkipMetadata("Info", null, null, matchedByViewId = false))
        assertFalse(AdSkipAccessibilityService.isGenuineSkipMetadata("Skip", "Ad info", "com.google.android.youtube:id/ad_info_button", matchedByViewId = true))
    }

    @Test
    fun testOnlyGenuineSkipNodesAreAccepted() {
        assertTrue(AdSkipAccessibilityService.isGenuineSkipMetadata("Skip", null, null, matchedByViewId = false))
        assertTrue(AdSkipAccessibilityService.isGenuineSkipMetadata("Skip Ad", null, null, matchedByViewId = false))
        assertTrue(AdSkipAccessibilityService.isGenuineSkipMetadata("Skip Ads", null, null, matchedByViewId = false))
        assertTrue(AdSkipAccessibilityService.isGenuineSkipMetadata("विज्ञापन छोड़ें", null, null, matchedByViewId = false))
        assertTrue(
            AdSkipAccessibilityService.isGenuineSkipMetadata(
                null,
                null,
                "com.google.android.youtube:id/skip_ad_button",
                matchedByViewId = true
            )
        )

        // Countdown-only and unrelated video titles containing "skip" must be rejected
        assertFalse(AdSkipAccessibilityService.isGenuineSkipMetadata("Skip in 5", null, null, matchedByViewId = false))
        assertFalse(AdSkipAccessibilityService.isGenuineSkipMetadata("You can skip in 3s", null, "com.google.android.youtube:id/skip_ad_button", matchedByViewId = true))
        assertFalse(AdSkipAccessibilityService.isGenuineSkipMetadata("How to skip rope workout", null, null, matchedByViewId = false))
    }

    @Test
    fun testContinueWatchingButtonValidation() {
        assertTrue(AdSkipAccessibilityService.isValidContinueButtonMetadata("Yes", null, null))
        assertTrue(AdSkipAccessibilityService.isValidContinueButtonMetadata("Continue", null, null))
        assertTrue(AdSkipAccessibilityService.isValidContinueButtonMetadata("हाँ", null, null))
        assertTrue(AdSkipAccessibilityService.isValidContinueButtonMetadata(null, null, "android:id/button1"))

        assertFalse(AdSkipAccessibilityService.isValidContinueButtonMetadata("Cancel", null, null))
        assertFalse(AdSkipAccessibilityService.isValidContinueButtonMetadata("Report", null, null))
        assertFalse(AdSkipAccessibilityService.isValidContinueButtonMetadata("Ad info", null, null))
    }
}
