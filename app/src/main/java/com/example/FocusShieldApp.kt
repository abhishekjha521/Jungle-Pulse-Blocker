package com.example

import android.app.Application
import com.example.data.PreferencesManager

class FocusShieldApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize PreferencesManager singleton eagerly
        PreferencesManager.getInstance(this)
    }
}
