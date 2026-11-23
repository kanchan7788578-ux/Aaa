package com.example.engine

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import com.example.domain.model.BusModel
import com.example.domain.model.CameraMode
import com.example.domain.model.RoadZoneType
import com.example.domain.model.TrafficVehicle
import com.example.domain.model.WeatherTime
import com.example.engine.VehicleRenderer.drawCockpitOverlay
import com.example.engine.VehicleRenderer.drawPlayerBusChase
import com.example.engine.VehicleRenderer.drawTrafficVehicle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class ProjectedPoint(
    val screenX: Float,
    val screenY: Float,
    val screenW: Float,
    val scale: Float,
    val worldZ: Float
)

object RoadWorldRenderer {

    private const val DRAW_DISTANCE = 100
    private const val CAMERA_DEPTH = 0.96f

    fun DrawScope.renderWorld(
        segments: List<RoadSegment>,
        playerZ: Float,
        playerX: Float, // -1.2 .. +1.2 (in units of ROAD_HALF_WIDTH)
        playerBus: BusModel,
        traffic: List<TrafficVehicle>,
        weather: WeatherTime,
        cameraMode: CameraMode,
        speedKmh: Float,
        steerInput: Float,
        isBraking: Boolean,
        headlightsOn: Boolean,
        wipersOn: Boolean,
        leftSignalOn: Boolean,
        rightSignalOn: Boolean,
        doorOpen: Boolean,
        suspensionBounce: Float,
        timeSec: Float,
        isEndless: Boolean
    ) {
        if (segments.isEmpty()) return
        val w = size.width
        val h = size.height
        val isLandscape = w > h
        val horizonFrac = if (isLandscape) 0.36f else 0.35f
        val roadSpread = if (isLandscape) w * 0.56f else w * 0.72f
        val totalLen = segments.size * SEGMENT_LENGTH
        val wrappedPlayerZ = if (isEndless) ((playerZ % totalLen) + totalLen) % totalLen else playerZ.coerceIn(0f, totalLen - SEGMENT_LENGTH)
        val baseIndex = (wrappedPlayerZ / SEGMENT_LENGTH).toInt().coerceIn(0, segments.lastIndex)
        val baseSeg = segments[baseIndex]
        val basePercent = (wrappedPlayerZ % SEGMENT_LENGTH) / SEGMENT_LENGTH
        val nextSeg = segments[(baseIndex + 1).coerceAtMost(segments.lastIndex)]
        val baseY = baseSeg.yElevation + (nextSeg.yElevation - baseSeg.yElevation) * basePercent
        val cameraHeight = if (cameraMode == CameraMode.COCKPIT) 880f else 1120f

        // 1. Sky & Distant Mymensingh Horizon
        drawSkyAndHorizon(w, h, horizonFrac, weather, baseSeg.curve, timeSec)

        // 2. Project segments forward
        val projected = ArrayList<ProjectedPoint>(DRAW_DISTANCE)
        val segRefs = ArrayList<RoadSegment>(DRAW_DISTANCE)
        var xOffset = 0f
        var dx = -(baseSeg.curve * basePercent)

        for (n in 0 until DRAW_DISTANCE) {
            val rawIndex = baseIndex + n
            val segIndex = if (isEndless) {
                rawIndex % segments.size
            } else {
                if (rawIndex >= segments.size) break else rawIndex
            }
            val seg = segments[segIndex]
            val loopOffset = if (isEndless && rawIndex >= segments.size) totalLen else 0f
            val camZ = (seg.zStart + loopOffset) - wrappedPlayerZ
            if (camZ <= 12f && n > 0) {
                xOffset += dx
                dx += seg.curve
                continue
            }
            val zDist = camZ.coerceAtLeast(32f)
            val scale = CAMERA_DEPTH / (zDist / 1000f)
            val worldX = -(playerX * ROAD_HALF_WIDTH) + xOffset
            val worldY = seg.yElevation - (baseY + cameraHeight)

            val screenX = (w * 0.5f) + (scale * worldX * roadSpread / 1000f)
            val screenY = (h * horizonFrac) - (scale * worldY * h * 0.54f / 1000f)
            val screenW = scale * ROAD_HALF_WIDTH * roadSpread / 1000f

            projected.add(ProjectedPoint(screenX, screenY, screenW, scale, seg.zStart + loopOffset))
            segRefs.add(seg)

            xOffset += dx
            dx += seg.curve
        }

        // 3. Draw Road Polygons Back-to-Front with Horizon Clipping
        val clipY = FloatArray(projected.size) { h }
        var maxY = h

        // First find visible horizon clip line from front to back
        val visibleSegments = ArrayList<Int>(projected.size)
        for (i in 0 until projected.size - 1) {
            val p1 = projected[i]
            val p2 = projected[i + 1]
            clipY[i] = maxY
            if (p2.screenY >= maxY || p1.screenY <= p2.screenY) {
                continue
            }
            visibleSegments.add(i)
            maxY = p2.screenY
        }

        // Fill ground below the furthest visible horizon band
        val horizonY = if (projected.isNotEmpty()) maxY.coerceAtMost(h * 0.72f) else h * horizonFrac
        val groundColor = groundColorFor(baseSeg.zoneType, weather)
        drawRect(groundColor, Offset(0f, horizonY), Size(w, h - horizonY))

        // Render road strips back-to-front
        for (idxPos in visibleSegments.indices.reversed()) {
            val i = visibleSegments[idxPos]
            val p1 = projected[i]
            val p2 = projected[i + 1]
            val seg = segRefs[i]
            drawRoadSegmentQuad(w, p1, p2, seg, weather)
        }

        // 4. Draw Roadside Scenery & Traffic Back-to-Front
        for (i in (projected.size - 2) downTo 0) {
            val p = projected[i]
            val seg = segRefs[i]
            val maxAllowedY = clipY[i]
            if (p.screenY > maxAllowedY + 40f || p.screenW < 4f) continue

            seg.leftProp?.let { prop ->
                drawSceneryProp(prop, p.screenX + prop.offsetFromCenter * p.screenW, p.screenY, p.screenW, weather)
            }
            seg.rightProp?.let { prop ->
                drawSceneryProp(prop, p.screenX + prop.offsetFromCenter * p.screenW, p.screenY, p.screenW, weather)
            }

            // Draw any traffic vehicles in this segment's Z slice
            val zMin = seg.zStart
            val zMax = seg.zEnd
            for (veh in traffic) {
                val vz = if (isEndless) ((veh.zPos % totalLen) + totalLen) % totalLen else veh.zPos
                if (vz in zMin..zMax) {
                    val vScreenX = p.screenX + veh.laneX * p.screenW
                    drawTrafficVehicle(veh, vScreenX, p.screenY, p.screenW * 0.74f, weather)
                }
            }
        }

        // 5. Monsoon Rain Streaks & Night Atmosphere
        if (weather == WeatherTime.MONSOON_RAIN) {
            drawRainDrops(w, h, timeSec, speedKmh)
        }

        // 6. Player Bus (Chase Cam) OR Driver Cockpit (1st Person)
        val blinkPhase = ((timeSec * 2.5f).toInt() % 2) == 0
        if (cameraMode == CameraMode.CHASE) {
            val busW = if (isLandscape) (h * 0.44f) else (w * 0.48f)
            val busH = busW * 0.88f
            drawPlayerBusChase(
                bus = playerBus,
                centerX = w * 0.5f + steerInput * 12f,
                bottomY = if (isLandscape) h * 0.90f else h * 0.86f,
                busWidth = busW,
                busHeight = busH,
                steerInput = steerInput,
                isBraking = isBraking,
                headlightsOn = headlightsOn,
                leftSignalOn = leftSignalOn,
                rightSignalOn = rightSignalOn,
                blinkPhase = blinkPhase,
                suspensionBounce = suspensionBounce,
                doorOpen = doorOpen,
                weather = weather,
                timeSec = timeSec
            )
        } else {
            // In cockpit view, project subtle headlight cone from bottom edge
            if (headlightsOn || weather == WeatherTime.NIGHT) {
                val coneAlpha = if (weather == WeatherTime.NIGHT) 0.25f else 0.12f
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xFFFFE082).copy(alpha = coneAlpha)),
                        startY = h * horizonFrac,
                        endY = h * 0.78f
                    ),
                    topLeft = Offset(w * 0.12f, h * horizonFrac),
                    size = Size(w * 0.76f, h * (0.78f - horizonFrac))
                )
            }
            drawCockpitOverlay(
                bus = playerBus,
                speedKmh = speedKmh,
                steerInput = steerInput,
                wipersOn = wipersOn,
                weather = weather,
                timeSec = timeSec
            )
        }
    }

    private fun DrawScope.drawSkyAndHorizon(
        w: Float,
        h: Float,
        horizonFrac: Float,
        weather: WeatherTime,
        curve: Float,
        timeSec: Float
    ) {
        val horizonY = h * (horizonFrac + 0.02f)
        val skyColors = when (weather) {
            WeatherTime.MORNING -> listOf(Color(0xFF1565C0), Color(0xFF4FC3F7), Color(0xFFFFECB3))
            WeatherTime.SUNSET -> listOf(Color(0xFF1A1446), Color(0xFFC2185B), Color(0xFFFF8F00))
            WeatherTime.MONSOON_RAIN -> listOf(Color(0xFF1C2733), Color(0xFF37474F), Color(0xFF607D8B))
            WeatherTime.NIGHT -> listOf(Color(0xFF030712), Color(0xFF0B1528), Color(0xFF16243D))
        }
        drawRect(
            brush = Brush.verticalGradient(skyColors, startY = 0f, endY = horizonY),
            topLeft = Offset.Zero,
            size = Size(w, horizonY + 20f)
        )

        // Sun / Moon Disc
        when (weather) {
            WeatherTime.MORNING -> {
                drawCircle(Color(0xFFFFF59D).copy(alpha = 0.28f), radius = 64f, center = Offset(w * 0.76f, horizonY * 0.54f))
                drawCircle(Color(0xFFFFF9C4), radius = 26f, center = Offset(w * 0.76f, horizonY * 0.54f))
            }
            WeatherTime.SUNSET -> {
                drawCircle(Color(0xFFFF6F00).copy(alpha = 0.35f), radius = 85f, center = Offset(w * 0.52f, horizonY * 0.78f))
                drawCircle(Color(0xFFFFB300), radius = 38f, center = Offset(w * 0.52f, horizonY * 0.78f))
            }
            WeatherTime.NIGHT -> {
                drawCircle(Color(0xFFE0F2FE), radius = 18f, center = Offset(w * 0.80f, horizonY * 0.34f))
                // Twinkling stars
                for (s in 0 until 18) {
                    val sx = ((s * 173) % 100) / 100f * w
                    val sy = ((s * 89) % 70) / 100f * horizonY
                    val a = 0.4f + 0.4f * sin(timeSec * 2f + s).toFloat()
                    drawCircle(Color.White.copy(alpha = a), radius = 1.8f, center = Offset(sx, sy))
                }
            }
            WeatherTime.MONSOON_RAIN -> {
                // Soft storm clouds
                for (c in 0..4) {
                    val cx = w * (0.15f + c * 0.2f)
                    drawOval(Color(0xFF263238).copy(alpha = 0.45f), Offset(cx - 80f, horizonY * 0.18f), Size(180f, 54f))
                }
            }
        }

        // Distant Mymensingh Tree-line & Mosque/Tower Silhouette
        val silColor = when (weather) {
            WeatherTime.MORNING -> Color(0xFF1B4332)
            WeatherTime.SUNSET -> Color(0xFF2B1B2E)
            WeatherTime.MONSOON_RAIN -> Color(0xFF1A2926)
            WeatherTime.NIGHT -> Color(0xFF090E16)
        }
        val silPath = Path().apply {
            moveTo(0f, horizonY + 10f)
            val steps = 20
            for (k in 0..steps) {
                val px = w * k / steps.toFloat()
                val bump = sin(k * 1.15f + curve * 0.1f) * 14f + cos(k * 2.3f) * 8f
                lineTo(px, horizonY - 12f - bump)
            }
            lineTo(w, horizonY + 10f)
            close()
        }
        drawPath(silPath, silColor)
    }

    private fun DrawScope.drawRoadSegmentQuad(
        screenW: Float,
        p1: ProjectedPoint,
        p2: ProjectedPoint,
        seg: RoadSegment,
        weather: WeatherTime
    ) {
        val alt = (seg.index / 3) % 2 == 0
        val isBridge = seg.zoneType == RoadZoneType.BRIDGE_BRAHMAPUTRA

        // 1. Surrounding Terrain (Grass / Paddy Field / Brahmaputra River Water)
        val terrainColor = if (isBridge) {
            when (weather) {
                WeatherTime.MORNING -> if (alt) Color(0xFF1565A0) else Color(0xFF1976B5)
                WeatherTime.SUNSET -> if (alt) Color(0xFF4A3B59) else Color(0xFF5E476B)
                WeatherTime.MONSOON_RAIN -> if (alt) Color(0xFF2A4552) else Color(0xFF2F4D5B)
                WeatherTime.NIGHT -> if (alt) Color(0xFF0A1929) else Color(0xFF0D2034)
            }
        } else {
            val base = groundColorFor(seg.zoneType, weather)
            if (alt) base else base.copy(
                red = (base.red * 0.92f).coerceIn(0f, 1f),
                green = (base.green * 0.92f).coerceIn(0f, 1f),
                blue = (base.blue * 0.92f).coerceIn(0f, 1f)
            )
        }
        drawRect(terrainColor, Offset(0f, p2.screenY), Size(screenW, (p1.screenY - p2.screenY) + 1f))

        // 2. Roadside Dirt/Brick Shoulder or Bridge Deck Curb
        val shoulderScale = 1.18f
        val shoulderColor = if (isBridge) {
            Color(0xFF546E7A)
        } else if (alt) {
            Color(0xFF5D5042)
        } else {
            Color(0xFF4F4337)
        }
        drawQuad(
            p1.screenX, p1.screenY, p1.screenW * shoulderScale,
            p2.screenX, p2.screenY, p2.screenW * shoulderScale,
            shoulderColor
        )

        // 3. Realistic Asphalt Surface (darker glossy wet asphalt in Monsoon!)
        val asphaltColor = when (weather) {
            WeatherTime.MONSOON_RAIN -> if (alt) Color(0xFF1C222B) else Color(0xFF212833)
            WeatherTime.NIGHT -> if (alt) Color(0xFF151920) else Color(0xFF191E26)
            else -> if (alt) Color(0xFF2A2F36) else Color(0xFF2F353D)
        }
        drawQuad(
            p1.screenX, p1.screenY, p1.screenW,
            p2.screenX, p2.screenY, p2.screenW,
            asphaltColor
        )

        // 4. Subtle Tire Wear Tracks in Left & Right Lanes
        val trackColor = asphaltColor.copy(
            red = (asphaltColor.red * 0.84f),
            green = (asphaltColor.green * 0.84f),
            blue = (asphaltColor.blue * 0.84f)
        )
        drawLaneStrip(p1, p2, -0.50f, 0.22f, trackColor)
        drawLaneStrip(p1, p2, 0.50f, 0.22f, trackColor)

        // 5. Bus Stop Bay Highlight on Left Curb
        if (seg.stop != null) {
            drawLaneStrip(p1, p2, -0.84f, 0.24f, Color(0xFF0E7C4E).copy(alpha = 0.65f))
        }

        // 6. Outer White Edge Lines
        drawLaneStrip(p1, p2, -0.95f, 0.03f, Color(0xFFE2E8F0))
        drawLaneStrip(p1, p2, 0.95f, 0.03f, Color(0xFFE2E8F0))

        // 7. Center Divider (Raised Yellow-Black Median on 4-lane Highway, Double Yellow Line otherwise)
        if (seg.zoneType == RoadZoneType.HIGHWAY_FOUR_LANE) {
            val medianColor = if (alt) Color(0xFFFFC107) else Color(0xFF212529)
            drawLaneStrip(p1, p2, 0.0f, 0.065f, medianColor)
        } else {
            drawLaneStrip(p1, p2, -0.02f, 0.018f, Color(0xFFFFCA28))
            drawLaneStrip(p1, p2, 0.02f, 0.018f, Color(0xFFFFCA28))
        }

        // Dashed White Sub-Lane Divider on Left & Right halves
        if ((seg.index / 4) % 2 == 0) {
            drawLaneStrip(p1, p2, -0.50f, 0.022f, Color(0xFFCBD5E1).copy(alpha = 0.85f))
            drawLaneStrip(p1, p2, 0.50f, 0.022f, Color(0xFFCBD5E1).copy(alpha = 0.85f))
        }

        // 8. Speed Breaker Warning Strips
        if (seg.hasSpeedBreaker) {
            drawLaneStrip(p1, p2, 0f, 1.85f, if (alt) Color(0xFFFFC107) else Color(0xFF111111))
        }

        // 9. Zebra Crossing
        if (seg.hasZebraCrossing) {
            for (z in -3..3) {
                drawLaneStrip(p1, p2, z * 0.24f, 0.11f, Color.White.copy(alpha = 0.90f))
            }
        }
    }

    private fun DrawScope.drawQuad(
        x1: Float, y1: Float, w1: Float,
        x2: Float, y2: Float, w2: Float,
        color: Color
    ) {
        val path = Path().apply {
            moveTo(x1 - w1, y1 + 0.8f)
            lineTo(x2 - w2, y2)
            lineTo(x2 + w2, y2)
            lineTo(x1 + w1, y1 + 0.8f)
            close()
        }
        drawPath(path, color)
    }

    private fun DrawScope.drawLaneStrip(
        p1: ProjectedPoint,
        p2: ProjectedPoint,
        centerRatio: Float,
        widthRatio: Float,
        color: Color
    ) {
        val x1 = p1.screenX + p1.screenW * centerRatio
        val x2 = p2.screenX + p2.screenW * centerRatio
        val hw1 = p1.screenW * widthRatio * 0.5f
        val hw2 = p2.screenW * widthRatio * 0.5f
        drawQuad(x1, p1.screenY, hw1, x2, p2.screenY, hw2, color)
    }

    private fun DrawScope.drawSceneryProp(
        prop: SceneryProp,
        x: Float,
        y: Float,
        roadHalfScreenW: Float,
        weather: WeatherTime
    ) {
        val unit = roadHalfScreenW * 0.28f
        if (unit < 3f) return

        when (prop.kind) {
            PropKind.BANYAN_TREE -> {
                val trunkW = unit * 0.28f
                val trunkH = unit * 1.4f
                drawRect(Color(0xFF4E342E), Offset(x - trunkW / 2f, y - trunkH), Size(trunkW, trunkH))
                val leafDark = if (weather == WeatherTime.NIGHT) Color(0xFF0A2E1A) else Color(0xFF1B5E20)
                val leafMid = if (weather == WeatherTime.NIGHT) Color(0xFF103F24) else Color(0xFF2E7D32)
                drawCircle(leafDark, radius = unit * 1.05f, center = Offset(x, y - trunkH * 1.15f))
                drawCircle(leafMid, radius = unit * 0.78f, center = Offset(x - unit * 0.45f, y - trunkH * 0.95f))
                drawCircle(leafMid, radius = unit * 0.74f, center = Offset(x + unit * 0.45f, y - trunkH * 0.92f))
            }

            PropKind.PALM_TREE -> {
                val h = unit * 3.1f
                drawLine(Color(0xFF5D4037), Offset(x, y), Offset(x + unit * 0.16f, y - h), strokeWidth = (unit * 0.16f).coerceAtLeast(2f))
                val top = Offset(x + unit * 0.16f, y - h)
                val frondColor = if (weather == WeatherTime.NIGHT) Color(0xFF0B301C) else Color(0xFF2E7D32)
                for (a in listOf(-140, -100, -45, 0, 45, 100, 140)) {
                    val rad = a * PI / 180.0
                    drawLine(
                        frondColor,
                        top,
                        Offset(top.x + (cos(rad) * unit * 1.15f).toFloat(), top.y + (sin(rad) * unit * 0.65f).toFloat()),
                        strokeWidth = (unit * 0.12f).coerceAtLeast(1.5f)
                    )
                }
            }

            PropKind.CITY_BUILDING -> {
                val bw = unit * 2.2f
                val bh = unit * (2.6f + (prop.variant % 3) * 0.75f)
                val bColors = listOf(Color(0xFF78909C), Color(0xFFA1887F), Color(0xFF90A4AE), Color(0xFFBCAAA4))
                val wallColor = bColors[prop.variant % bColors.size].let {
                    if (weather == WeatherTime.NIGHT) it.copy(red = it.red * 0.28f, green = it.green * 0.28f, blue = it.blue * 0.35f) else it
                }
                drawRect(wallColor, Offset(x - bw / 2f, y - bh), Size(bw, bh))
                // Windows
                val winColor = if (weather == WeatherTime.NIGHT || weather == WeatherTime.SUNSET) {
                    Color(0xFFFFD54F).copy(alpha = 0.85f)
                } else {
                    Color(0xFF263238).copy(alpha = 0.75f)
                }
                val cols = 3
                val rows = 4
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        if ((r + c + prop.variant) % 5 == 0 && weather == WeatherTime.NIGHT) continue
                        val wx = x - bw * 0.34f + c * bw * 0.26f
                        val wy = y - bh * 0.86f + r * bh * 0.18f
                        drawRect(winColor, Offset(wx, wy), Size(bw * 0.16f, bh * 0.10f))
                    }
                }
            }

            PropKind.SHOP_STALL -> {
                val sw = unit * 1.75f
                val sh = unit * 1.35f
                drawRect(Color(0xFF455A64), Offset(x - sw / 2f, y - sh), Size(sw, sh))
                // Slanted tin roof
                drawRect(Color(0xFF1565C0), Offset(x - sw * 0.56f, y - sh * 1.14f), Size(sw * 1.12f, sh * 0.20f))
                // Warm tea-stall glow
                drawRect(Color(0xFFFFB74D).copy(alpha = 0.85f), Offset(x - sw * 0.36f, y - sh * 0.65f), Size(sw * 0.72f, sh * 0.45f))
            }

            PropKind.ELECTRIC_POLE -> {
                val ph = unit * 3.0f
                drawLine(Color(0xFF455A64), Offset(x, y), Offset(x, y - ph), strokeWidth = (unit * 0.11f).coerceAtLeast(1.5f))
                val armDir = if (prop.offsetFromCenter < 0) 1f else -1f
                val lampPos = Offset(x + armDir * unit * 0.55f, y - ph * 0.94f)
                drawLine(Color(0xFF607D8B), Offset(x, y - ph * 0.94f), lampPos, strokeWidth = (unit * 0.08f).coerceAtLeast(1.5f))
                if (weather == WeatherTime.NIGHT || weather == WeatherTime.SUNSET || weather == WeatherTime.MONSOON_RAIN) {
                    drawCircle(Color(0xFFFFE082).copy(alpha = 0.30f), radius = unit * 0.55f, center = lampPos)
                    drawCircle(Color(0xFFFFF59D), radius = unit * 0.13f, center = lampPos)
                }
            }

            PropKind.HIGHWAY_SIGN -> {
                val poleH = unit * 2.5f
                val signW = unit * 2.8f
                val signH = unit * 1.05f
                drawLine(Color(0xFF90A4AE), Offset(x, y), Offset(x, y - poleH), strokeWidth = (unit * 0.10f).coerceAtLeast(2f))
                drawRoundRect(
                    Color(0xFF00695C),
                    Offset(x - signW / 2f, y - poleH - signH * 0.5f),
                    Size(signW, signH),
                    CornerRadius(4f, 4f)
                )
                drawRoundRect(
                    Color.White,
                    Offset(x - signW / 2f + 2f, y - poleH - signH * 0.5f + 2f),
                    Size(signW - 4f, signH - 4f),
                    CornerRadius(3f, 3f),
                    style = Stroke(width = 2f)
                )
                if (signW > 38f && prop.labelBn.isNotEmpty()) {
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = Paint().apply {
                            color = android.graphics.Color.WHITE
                            textAlign = Paint.Align.CENTER
                            textSize = (signH * 0.36f).coerceIn(10f, 22f)
                            typeface = Typeface.DEFAULT_BOLD
                            isAntiAlias = true
                        }
                        drawText(prop.labelBn.take(18), x, y - poleH + signH * 0.12f, paint)
                    }
                }
            }

            PropKind.BRIDGE_TRUSS -> {
                val trussH = unit * 3.8f
                val trussW = unit * 0.22f
                val steel = if (weather == WeatherTime.NIGHT) Color(0xFF263238) else Color(0xFF546E7A)
                drawRect(steel, Offset(x - trussW / 2f, y - trussH), Size(trussW, trussH))
                // Cross-beam overhead every pair
                if (prop.offsetFromCenter < 0) {
                    val rightX = x + roadHalfScreenW * 2.16f
                    drawLine(steel, Offset(x, y - trussH), Offset(rightX, y - trussH), strokeWidth = (unit * 0.24f).coerceAtLeast(3f))
                    // Diagonal steel bracing
                    drawLine(steel.copy(alpha = 0.7f), Offset(x, y - trussH * 0.5f), Offset(x + unit * 0.5f, y - trussH), strokeWidth = (unit * 0.10f).coerceAtLeast(1.5f))
                }
            }

            PropKind.BUS_SHELTER -> {
                val sw = unit * 2.6f
                val sh = unit * 1.8f
                drawRect(Color(0xFF0E7C4E), Offset(x - sw / 2f, y - sh), Size(sw, sh * 0.28f))
                drawRect(Color(0xFF37474F), Offset(x - sw * 0.44f, y - sh * 0.72f), Size(unit * 0.16f, sh * 0.72f))
                drawRect(Color(0xFF37474F), Offset(x + sw * 0.28f, y - sh * 0.72f), Size(unit * 0.16f, sh * 0.72f))
                // Waiting passengers
                drawCircle(Color(0xFFFFCC80), radius = unit * 0.15f, center = Offset(x - unit * 0.3f, y - unit * 0.9f))
                drawRect(Color(0xFFE91E63), Offset(x - unit * 0.44f, y - unit * 0.75f), Size(unit * 0.28f, unit * 0.75f))
                drawCircle(Color(0xFFFFCC80), radius = unit * 0.15f, center = Offset(x + unit * 0.25f, y - unit * 0.95f))
                drawRect(Color(0xFF1E88E5), Offset(x + unit * 0.11f, y - unit * 0.80f), Size(unit * 0.28f, unit * 0.80f))
            }
        }
    }

    private fun DrawScope.drawRainDrops(w: Float, h: Float, timeSec: Float, speedKmh: Float) {
        val dropCount = 75
        val speedFactor = 1f + speedKmh / 60f
        for (i in 0 until dropCount) {
            val rx = ((i * 97 + timeSec * 120f) % w)
            val ry = ((i * 53 + timeSec * 900f * speedFactor) % h)
            drawLine(
                color = Color(0xFFB3E5FC).copy(alpha = 0.36f),
                start = Offset(rx, ry),
                end = Offset(rx - 8f, ry + 24f),
                strokeWidth = 2f
            )
        }
    }

    private fun groundColorFor(zone: RoadZoneType, weather: WeatherTime): Color {
        return when (weather) {
            WeatherTime.NIGHT -> Color(0xFF0A1612)
            WeatherTime.MONSOON_RAIN -> if (zone == RoadZoneType.CITY_BAZAAR) Color(0xFF213029) else Color(0xFF173D25)
            WeatherTime.SUNSET -> if (zone == RoadZoneType.CITY_BAZAAR) Color(0xFF2D3326) else Color(0xFF284724)
            WeatherTime.MORNING -> when (zone) {
                RoadZoneType.CITY_BAZAAR -> Color(0xFF334733)
                RoadZoneType.UNIVERSITY_AVENUE -> Color(0xFF1F6B36)
                RoadZoneType.HIGHWAY_FOUR_LANE -> Color(0xFF2C6E31)
                RoadZoneType.BRIDGE_BRAHMAPUTRA -> Color(0xFF1565A0)
            }
        }
    }
}
