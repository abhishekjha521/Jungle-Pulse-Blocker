package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.PreferencesManager
import org.junit.Assert.assertEquals
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
}
