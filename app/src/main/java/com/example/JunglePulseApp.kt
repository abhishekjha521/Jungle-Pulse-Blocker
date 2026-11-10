package com.example

import android.app.Application
import com.example.data.PreferencesManager

class JunglePulseApp : Application() {

    override fun onCreate() {
        super.onCreate()
        PreferencesManager.getInstance(this)
    }
}
