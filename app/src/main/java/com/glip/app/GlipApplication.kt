package com.glip.app

import android.app.Application
import org.osmdroid.config.Configuration
import java.io.File

class GlipApplication : Application() {
    override fun onCreate() {
        super.onCreate()

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

        // Initialize Live Advertisement Management System
        com.glip.app.ads.AdManager.initialize(this)
    }
}
