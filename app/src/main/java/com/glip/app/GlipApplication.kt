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
    }
}
