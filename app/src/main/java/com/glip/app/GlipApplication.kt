package com.glip.app

import android.app.Application

class GlipApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize any app-wide crash prevention or logging safeguards here
    }
}
