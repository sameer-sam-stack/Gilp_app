package com.glip.app

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import org.osmdroid.config.Configuration
import java.io.File

class GlipApplication : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        try {
            MultiDex.install(this)
        } catch (_: Exception) {}
    }

    override fun onCreate() {
        super.onCreate()

        try {
            // Initialize osmdroid storage & user agent so real street tiles load properly
            val basePath = File(cacheDir, "osmdroid")
            val tileCache = File(basePath, "tiles")
            basePath.mkdirs()
            tileCache.mkdirs()

            Configuration.getInstance().userAgentValue = packageName
            Configuration.getInstance().osmdroidBasePath = basePath
            Configuration.getInstance().osmdroidTileCache = tileCache
            Configuration.getInstance().cacheMapTileCount = 18
            Configuration.getInstance().cacheMapTileOvershoot = 18
            Configuration.getInstance().tileDownloadThreads = 4
            Configuration.getInstance().tileFileSystemThreads = 4
        } catch (_: Exception) {}

        // Initialize Live Advertisement Management System
        try {
            com.glip.app.ads.AdManager.initialize(this)
        } catch (_: Exception) {}
    }
}
