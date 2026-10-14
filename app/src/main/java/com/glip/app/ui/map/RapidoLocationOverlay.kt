package com.glip.app.ui.map

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.view.animation.LinearInterpolator
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

/**
 * RapidoLocationOverlay:
 * Native 60 FPS Canvas overlay that renders a sleek Rapido / Uber style pulsing GPS beacon.
 * 1. Pulsing expanding translucent outer halo (animates continuously at 60 FPS)
 * 2. Subtle ground shadow
 * 3. Crisp white circular border
 * 4. High-contrast electric blue / cyan core dot
 * 5. Pinpoint white center reflection dot
 */
class RapidoLocationOverlay(
    private val mapView: MapView,
    var position: GeoPoint? = null
) : Overlay() {

    private var pulseFraction = 0f
    private val animator: ValueAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1600
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { anim ->
            pulseFraction = anim.animatedValue as Float
            mapView.postInvalidate()
        }
    }

    init {
        animator.start()
    }

    fun stop() {
        animator.cancel()
    }

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#33000000")
        style = Paint.Style.FILL
    }

    private val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#3B82F6") // Blue pulse
        style = Paint.Style.FILL
    }

    private val whiteBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }

    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#2563EB") // Electric Blue
        style = Paint.Style.FILL
    }

    private val innerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas, osmv: MapView, shadow: Boolean) {
        if (shadow) return
        val pos = position ?: return
        val projection = osmv.projection ?: return
        val screenPoint = Point()
        projection.toPixels(pos, screenPoint)

        val density = osmv.context.resources.displayMetrics.density
        val cx = screenPoint.x.toFloat()
        val cy = screenPoint.y.toFloat()

        // 1. Pulsing halo ring (radius expanding from 14dp to 34dp, alpha fading from 0.45 to 0.0)
        val minRadius = 14f * density
        val maxRadius = 34f * density
        val currentRadius = minRadius + (maxRadius - minRadius) * pulseFraction
        val currentAlpha = ((1f - pulseFraction) * 0.45f * 255).toInt().coerceIn(0, 255)
        pulsePaint.alpha = currentAlpha
        canvas.drawCircle(cx, cy, currentRadius, pulsePaint)

        // 2. Drop shadow under the puck
        canvas.drawCircle(cx, cy + (1.5f * density), 11.5f * density, shadowPaint)

        // 3. Crisp white outer ring
        canvas.drawCircle(cx, cy, 10f * density, whiteBorderPaint)

        // 4. Electric Blue core
        canvas.drawCircle(cx, cy, 7.5f * density, corePaint)

        // 5. Center white pinpoint
        canvas.drawCircle(cx, cy, 2.5f * density, innerDotPaint)
    }
}
