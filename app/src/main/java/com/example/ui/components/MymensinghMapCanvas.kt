package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.example.domain.model.MymensinghMapData
import com.example.domain.model.MymensinghRoute

@Composable
fun MymensinghInteractiveMap(
    selectedRoute: MymensinghRoute,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mapPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0D171F))
            .border(1.dp, Color(0xFF233746), RoundedCornerShape(20.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Subtle dark topographic grid
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF13262F), Color(0xFF091218)),
                    center = Offset(w * 0.5f, h * 0.45f),
                    radius = w * 0.8f
                )
            )
            for (g in 1..6) {
                drawLine(Color(0xFF182C38), Offset(0f, h * g / 7f), Offset(w, h * g / 7f), strokeWidth = 1f)
                drawLine(Color(0xFF182C38), Offset(w * g / 7f, 0f), Offset(w * g / 7f, h), strokeWidth = 1f)
            }

            // Brahmaputra River flowing NW -> SE
            val riverPath = Path().apply {
                MymensinghMapData.brahmaputraRiverPath.forEachIndexed { idx, (nx, ny) ->
                    val px = nx * w
                    val py = ny * h
                    if (idx == 0) moveTo(px, py) else lineTo(px, py)
                }
            }
            drawPath(
                riverPath,
                color = Color(0xFF1565A0).copy(alpha = 0.42f),
                style = Stroke(width = w * 0.085f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawPath(
                riverPath,
                color = Color(0xFF29B6F6).copy(alpha = 0.35f),
                style = Stroke(width = w * 0.032f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Draw all faint Mymensingh road connections
            MymensinghMapData.routes.forEach { route ->
                val basePath = Path().apply {
                    route.pathPoints.forEachIndexed { idx, (nx, ny) ->
                        if (idx == 0) moveTo(nx * w, ny * h) else lineTo(nx * w, ny * h)
                    }
                }
                drawPath(
                    basePath,
                    color = Color(0xFF2A3E4C),
                    style = Stroke(width = 7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // Highlight selected route with glowing emerald & animated dashed path
            val activePath = Path().apply {
                selectedRoute.pathPoints.forEachIndexed { idx, (nx, ny) ->
                    if (idx == 0) moveTo(nx * w, ny * h) else lineTo(nx * w, ny * h)
                }
            }
            drawPath(
                activePath,
                color = Color(0xFF0E7C4E).copy(alpha = 0.45f),
                style = Stroke(width = 16f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawPath(
                activePath,
                color = Color(0xFF4CD995),
                style = Stroke(
                    width = 6f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f), phase = -pulse * 60f)
                )
            )

            // Draw Landmark Nodes + Bengali Labels
            val activeStopIds = selectedRoute.stops.map { it.landmark.id }.toSet()
            MymensinghMapData.allLandmarks.forEach { lm ->
                val lx = lm.mapX * w
                val ly = lm.mapY * h
                val isSelectedStop = lm.id in activeStopIds

                if (isSelectedStop) {
                    drawCircle(
                        color = Color(0xFFFFCA3A).copy(alpha = (1f - pulse) * 0.45f),
                        radius = 12f + pulse * 14f,
                        center = Offset(lx, ly)
                    )
                    drawCircle(Color(0xFFFFCA3A), radius = 8.5f, center = Offset(lx, ly))
                    drawCircle(Color(0xFF0B141A), radius = 3.5f, center = Offset(lx, ly))
                } else {
                    drawCircle(Color(0xFF607D8B), radius = 4.5f, center = Offset(lx, ly))
                }

                drawContext.canvas.nativeCanvas.apply {
                    val paint = Paint().apply {
                        color = if (isSelectedStop) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#8FA3B5")
                        textSize = if (isSelectedStop) 25f else 20f
                        typeface = if (isSelectedStop) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                        textAlign = if (lm.mapX > 0.65f) Paint.Align.RIGHT else Paint.Align.LEFT
                        isAntiAlias = true
                        setShadowLayer(4f, 0f, 1f, android.graphics.Color.BLACK)
                    }
                    val xOff = if (lm.mapX > 0.65f) -14f else 14f
                    drawText(lm.nameBn, lx + xOff, ly + 6f, paint)
                }
            }

            // River Label
            drawContext.canvas.nativeCanvas.apply {
                val riverPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#7DD3FC")
                    textSize = 22f
                    typeface = Typeface.DEFAULT_BOLD
                    isAntiAlias = true
                }
                drawText("Brahmaputra River", w * 0.56f, h * 0.16f, riverPaint)
            }
        }

        // Map Watermark Badge
        Text(
            text = "MYMENSINGH DISTRICT MAP • GPS",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF4CD995),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xAA0B141A))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun MymensinghLiveMiniMap(
    route: MymensinghRoute,
    progressFraction: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xCC0C141B))
            .border(1.dp, Color(0xFF2D4454), RoundedCornerShape(14.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val w = size.width
            val h = size.height

            // River hint
            val river = Path().apply {
                MymensinghMapData.brahmaputraRiverPath.forEachIndexed { idx, (nx, ny) ->
                    if (idx == 0) moveTo(nx * w, ny * h) else lineTo(nx * w, ny * h)
                }
            }
            drawPath(river, Color(0xFF1E88E5).copy(alpha = 0.4f), style = Stroke(width = 7f, cap = StrokeCap.Round))

            // Route polyline
            val pts = route.pathPoints
            if (pts.size >= 2) {
                val path = Path().apply {
                    pts.forEachIndexed { idx, (nx, ny) ->
                        if (idx == 0) moveTo(nx * w, ny * h) else lineTo(nx * w, ny * h)
                    }
                }
                drawPath(path, Color(0xFF4CD995).copy(alpha = 0.7f), style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round))

                // Interpolate live GPS bus position along polyline
                val totalSegs = pts.size - 1
                val scaled = progressFraction.coerceIn(0f, 1f) * totalSegs
                val segIdx = scaled.toInt().coerceIn(0, totalSegs - 1)
                val localT = scaled - segIdx
                val pA = pts[segIdx]
                val pB = pts[segIdx + 1]
                val bx = (pA.first + (pB.first - pA.first) * localT) * w
                val by = (pA.second + (pB.second - pA.second) * localT) * h

                drawCircle(Color(0xFFFFCA3A).copy(alpha = 0.35f), radius = 10f, center = Offset(bx, by))
                drawCircle(Color(0xFFFFCA3A), radius = 5f, center = Offset(bx, by))
            }
        }
    }
}
