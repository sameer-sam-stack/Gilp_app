package com.glip.app.ui.map

import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.XYTileSource

/**
 * Modern Map Styles for Glip Go.
 * Provides clean, minimalist, high-contrast street maps matching Rapido, Uber, and Ola.
 * 100% Royalty-Free and Open-Source under ODbL and CC BY 3.0 / CC BY 4.0 licenses.
 */
object MapStyles {
    /**
     * CartoDB Positron (Light Minimalist):
     * Soft pastel roads, uncluttered typography, subtle buildings.
     * High-performance worldwide Fastly CDN edge caching.
     */
    val RAPIDO_CLEAN: ITileSource = XYTileSource(
        "CartoPositron",
        1,
        20,
        256,
        ".png",
        arrayOf(
            "https://a.basemaps.cartocdn.com/light_all/",
            "https://b.basemaps.cartocdn.com/light_all/",
            "https://c.basemaps.cartocdn.com/light_all/",
            "https://d.basemaps.cartocdn.com/light_all/"
        ),
        "© OpenStreetMap contributors, © CARTO"
    )
}
