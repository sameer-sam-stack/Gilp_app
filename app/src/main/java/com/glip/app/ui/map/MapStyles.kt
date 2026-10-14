package com.glip.app.ui.map

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView

/**
 * Modern Map Styles for Glip Go.
 * Uses official OpenStreetMap (OSM) Mapnik tiles with a modern pastel color filter.
 * 100% Free, NO API KEY REQUIRED, Zero Copyright / Licensing issues.
 */
object MapStyles {
    /**
     * Official OpenStreetMap tile source (100% Free, no API key required, no watermarks).
     */
    val RAPIDO_CLEAN: ITileSource = TileSourceFactory.MAPNIK

    /**
     * Applies a modern Rapido / Uber style pastel tone filter to standard OSM tiles:
     * - Calms harsh raw greens and bright saturation down to soft, elegant pastel roads.
     * - Maintains crisp street name readability.
     * - Completely local GPU filter, zero network cost and no API keys required.
     */
    fun applyRapidoTone(mapView: MapView) {
        try {
            val matrix = ColorMatrix().apply {
                // Subtle desaturation (0.78f) turns harsh mapnik colors into soft modern pastel tones
                setSaturation(0.78f)
            }
            mapView.overlayManager.tilesOverlay.setColorFilter(ColorMatrixColorFilter(matrix))
        } catch (_: Exception) {}
    }

    /**
     * Applies a diffused, soft-focus pastel tone filter for background blurred maps.
     * Softens harsh roads and text details so the background acts as a true frosted glass layer.
     */
    fun applyBlurredTone(mapView: MapView) {
        try {
            val matrix = ColorMatrix(floatArrayOf(
                0.60f, 0.20f, 0.20f, 0f, 20f,
                0.20f, 0.60f, 0.20f, 0f, 20f,
                0.20f, 0.20f, 0.60f, 0f, 25f,
                0f,    0f,    0f,    1f, 0f
            ))
            mapView.overlayManager.tilesOverlay.setColorFilter(ColorMatrixColorFilter(matrix))
        } catch (_: Exception) {}
    }
}
