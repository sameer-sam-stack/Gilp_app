package com.glip.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val MapBase = Color(0xFFE9EEF4)
private val MapPark = Color(0xFFD5EBD0)
private val MapWater = Color(0xFFBBDEFB)
private val RouteBlue = Color(0xFF2979FF)
private val RouteIdle = Color(0xFF90A4AE)
private val PinRed = Color(0xFFE53935)
private val StartGreen = Color(0xFF43A047)
private val RideYellow = Color(0xFFFFC107)
private val RideGreen = Color(0xFF1B5E20)

/**
 * Map-style loader: pickup se drop tak route dheere dheere banta hai,
 * ride ka dot us par chalta hai, aur drop pin dhadakta hai.
 *
 * Use: LoadingScreen me AutoRickshawLoader ki jagah
 *      RouteLoader(Modifier.size(width = 300.dp, height = 170.dp))
 */
@Composable
fun RouteLoader(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "route")

    // 0 -> 1 route banta hai, phir thoda ruk kar dobara shuru
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 3200
                0f at 0 using FastOutSlowInEasing
                1f at 2600
            }
        ),
        label = "progress"
    )
    val ripple by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "ripple"
    )
    val bounce by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(450, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bounce"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val card = Path().apply {
            addRoundRect(RoundRect(Rect(0f, 0f, w, h), CornerRadius(16.dp.toPx())))
        }

        clipPath(card) {
            // Map ka background
            drawRect(MapBase)
            drawRoundRect(
                MapWater, Offset(w * 0.04f, h * 0.06f), Size(w * 0.16f, h * 0.106f),
                CornerRadius(9.dp.toPx())
            )
            drawRoundRect(
                MapPark, Offset(w * 0.28f, h * 0.58f), Size(w * 0.18f, h * 0.24f),
                CornerRadius(8.dp.toPx())
            )
            drawRoundRect(
                MapPark, Offset(w * 0.80f, h * 0.07f), Size(w * 0.17f, h * 0.11f),
                CornerRadius(8.dp.toPx())
            )

            // Sadkein
            listOf(0.20f, 0.55f, 0.85f).forEach {
                drawLine(Color.White, Offset(0f, h * it), Offset(w, h * it), 6.dp.toPx())
            }
            listOf(0.25f, 0.50f, 0.78f).forEach {
                drawLine(Color.White, Offset(w * it, 0f), Offset(w * it, h), 6.dp.toPx())
            }

            // Route
            val route = Path().apply {
                moveTo(w * 0.13f, h * 0.74f)
                cubicTo(w * 0.34f, h * 0.74f, w * 0.28f, h * 0.30f, w * 0.50f, h * 0.30f)
                cubicTo(w * 0.72f, h * 0.30f, w * 0.74f, h * 0.62f, w * 0.87f, h * 0.62f)
            }
            val measure = PathMeasure().apply { setPath(route, false) }
            val len = measure.length
            val start = Offset(w * 0.13f, h * 0.74f)
            val end = Offset(w * 0.87f, h * 0.62f)

            // Abhi tak na chali hui route (dashed)
            drawPath(
                route, RouteIdle,
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(8.dp.toPx(), 7.dp.toPx())
                    )
                )
            )

            // Chali hui route (solid blue)
            val done = Path()
            measure.getSegment(0f, len * progress, done, true)
            drawPath(done, RouteBlue, style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))

            // Pickup marker
            drawCircle(Color.White, 7.dp.toPx(), start)
            drawCircle(StartGreen, 5.dp.toPx(), start)

            // Ride ka dot (aas-paas pulse ke saath)
            val pos = measure.getPosition(len * progress)
            drawCircle(
                RouteBlue.copy(alpha = 0.25f * (1f - ripple)),
                (9f + 12f * ripple).dp.toPx(),
                pos
            )
            drawCircle(RideYellow, 8.dp.toPx(), pos)
            drawCircle(RideGreen, 8.dp.toPx(), pos, style = Stroke(2.5.dp.toPx()))
            drawCircle(Color.White, 2.5.dp.toPx(), pos)

            // Drop pin: neeche ripple, upar uchhalta pin
            val rr = (4f + 14f * ripple).dp.toPx()
            drawOval(
                PinRed.copy(alpha = 0.3f * (1f - ripple)),
                Offset(end.x - rr, end.y - rr * 0.4f),
                Size(rr * 2f, rr * 0.8f)
            )
            val headY = end.y - 14.dp.toPx() - bounce * 3.dp.toPx()
            val tip = Path().apply {
                moveTo(end.x - 6.dp.toPx(), headY + 5.dp.toPx())
                lineTo(end.x, end.y)
                lineTo(end.x + 6.dp.toPx(), headY + 5.dp.toPx())
                close()
            }
            drawPath(tip, PinRed)
            drawCircle(PinRed, 8.dp.toPx(), Offset(end.x, headY))
            drawCircle(Color.White, 3.dp.toPx(), Offset(end.x, headY))
        }
    }
}

/**
 * Fullscreen App Loading / Splash Screen showing RouteLoader
 * Prevents initial blank screen while map, GPS and assets are loading.
 */
@Composable
fun AppLoadingSplashScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            RouteLoader(
                modifier = Modifier.size(width = 300.dp, height = 170.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Glip",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 24.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Connecting shared routes near you...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8),
                fontSize = 13.sp
            )
        }
    }
}
