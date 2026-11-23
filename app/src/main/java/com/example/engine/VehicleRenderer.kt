package com.example.engine

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import com.example.domain.model.BusModel
import com.example.domain.model.TrafficType
import com.example.domain.model.TrafficVehicle
import com.example.domain.model.WeatherTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object VehicleRenderer {

    fun DrawScope.drawPlayerBusChase(
        bus: BusModel,
        centerX: Float,
        bottomY: Float,
        busWidth: Float,
        busHeight: Float,
        steerInput: Float,
        isBraking: Boolean,
        headlightsOn: Boolean,
        leftSignalOn: Boolean,
        rightSignalOn: Boolean,
        blinkPhase: Boolean,
        suspensionBounce: Float,
        doorOpen: Boolean,
        weather: WeatherTime,
        timeSec: Float
    ) {
        val bounceY = suspensionBounce
        val rollDeg = steerInput * 3.2f
        val topY = bottomY - busHeight + bounceY
        val leftX = centerX - busWidth / 2f

        // 1. Project realistic headlight cones onto the asphalt road ahead
        if (headlightsOn || weather == WeatherTime.NIGHT || weather == WeatherTime.SUNSET) {
            val alpha = if (weather == WeatherTime.NIGHT) 0.42f else if (headlightsOn) 0.24f else 0.12f
            val beamColor = Color(0xFFFFE599)
            val leftBeam = Path().apply {
                moveTo(leftX + busWidth * 0.14f, bottomY - busHeight * 0.18f)
                lineTo(leftX - busWidth * 0.55f, bottomY - busHeight * 1.45f)
                lineTo(leftX + busWidth * 0.42f, bottomY - busHeight * 1.45f)
                close()
            }
            val rightBeam = Path().apply {
                moveTo(leftX + busWidth * 0.86f, bottomY - busHeight * 0.18f)
                lineTo(leftX + busWidth * 0.58f, bottomY - busHeight * 1.45f)
                lineTo(leftX + busWidth * 1.55f, bottomY - busHeight * 1.45f)
                close()
            }
            drawPath(
                leftBeam,
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, beamColor.copy(alpha = alpha)),
                    startY = bottomY - busHeight * 1.45f,
                    endY = bottomY
                )
            )
            drawPath(
                rightBeam,
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, beamColor.copy(alpha = alpha)),
                    startY = bottomY - busHeight * 1.45f,
                    endY = bottomY
                )
            )
        }

        // 2. Soft dark ground drop shadow under chassis
        drawOval(
            color = Color.Black.copy(alpha = 0.58f),
            topLeft = Offset(centerX - busWidth * 0.56f, bottomY - busHeight * 0.08f),
            size = Size(busWidth * 1.12f, busHeight * 0.18f)
        )

        rotate(degrees = rollDeg, pivot = Offset(centerX, bottomY)) {
            // 3. Dual Rear Tires & Black Mudflaps
            val tireW = busWidth * 0.17f
            val tireH = busHeight * 0.22f
            val leftTireX = leftX + busWidth * 0.06f
            val rightTireX = leftX + busWidth * 0.77f
            val tireY = bottomY - tireH * 0.75f + bounceY * 0.4f

            drawRoundRect(Color(0xFF14171A), Offset(leftTireX, tireY), Size(tireW, tireH), CornerRadius(6f, 6f))
            drawRoundRect(Color(0xFF14171A), Offset(rightTireX, tireY), Size(tireW, tireH), CornerRadius(6f, 6f))
            // Red/yellow Bangladeshi mudflap trim
            drawRect(bus.accentColor, Offset(leftTireX + 2f, tireY + tireH * 0.65f), Size(tireW - 4f, tireH * 0.12f))
            drawRect(bus.accentColor, Offset(rightTireX + 2f, tireY + tireH * 0.65f), Size(tireW - 4f, tireH * 0.12f))

            // 4. Exhaust puff on right side when accelerating
            val puffPhase = (timeSec * 3.5f) % 1f
            drawCircle(
                color = Color(0xFFB0BEC5).copy(alpha = (1f - puffPhase) * 0.22f),
                radius = busWidth * (0.04f + puffPhase * 0.05f),
                center = Offset(leftX + busWidth * 0.16f - puffPhase * 18f, bottomY - busHeight * 0.08f - puffPhase * 22f)
            )

            // 5. Side Mirrors extending out
            val mirrorY = topY + busHeight * 0.19f
            drawRoundRect(Color(0xFF263238), Offset(leftX - busWidth * 0.08f, mirrorY), Size(busWidth * 0.08f, busHeight * 0.14f), CornerRadius(4f, 4f))
            drawRoundRect(Color(0xFF263238), Offset(leftX + busWidth, mirrorY), Size(busWidth * 0.08f, busHeight * 0.14f), CornerRadius(4f, 4f))

            // 6. Roof AC Pod
            drawRoundRect(
                color = Color(0xFFE0E6ED),
                topLeft = Offset(centerX - busWidth * 0.28f, topY - busHeight * 0.06f),
                size = Size(busWidth * 0.56f, busHeight * 0.09f),
                cornerRadius = CornerRadius(10f, 10f)
            )

            // 7. Main Bus Coach Body (tapered roof slightly narrower than base for 3D perspective)
            val bodyPath = Path().apply {
                moveTo(leftX + busWidth * 0.05f, topY + busHeight * 0.03f)
                lineTo(leftX + busWidth * 0.95f, topY + busHeight * 0.03f)
                lineTo(leftX + busWidth, bottomY - busHeight * 0.12f)
                lineTo(leftX, bottomY - busHeight * 0.12f)
                close()
            }
            drawPath(
                bodyPath,
                brush = Brush.verticalGradient(
                    colors = listOf(bus.primaryColor, bus.primaryColor.copy(alpha = 0.86f), Color(0xFF1A1F26)),
                    startY = topY,
                    endY = bottomY
                )
            )

            // Secondary Livery Band (white/gold middle stripe)
            drawRect(
                color = bus.secondaryColor,
                topLeft = Offset(leftX + busWidth * 0.02f, topY + busHeight * 0.45f),
                size = Size(busWidth * 0.96f, busHeight * 0.18f)
            )
            // Accent pin-stripe
            drawRect(
                color = bus.accentColor,
                topLeft = Offset(leftX + busWidth * 0.02f, topY + busHeight * 0.63f),
                size = Size(busWidth * 0.96f, busHeight * 0.035f)
            )

            // Door open glow on left curb side
            if (doorOpen) {
                drawRect(
                    color = Color(0xFF4CD995).copy(alpha = 0.65f),
                    topLeft = Offset(leftX - busWidth * 0.02f, topY + busHeight * 0.22f),
                    size = Size(busWidth * 0.05f, busHeight * 0.56f)
                )
            }

            // 8. Rear Tinted Windshield + Sky Reflection
            val glassRect = Rect(
                left = leftX + busWidth * 0.09f,
                top = topY + busHeight * 0.11f,
                right = leftX + busWidth * 0.91f,
                bottom = topY + busHeight * 0.41f
            )
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF101923), Color(0xFF243B55), Color(0xFF141E28)),
                    start = Offset(glassRect.left, glassRect.top),
                    end = Offset(glassRect.right, glassRect.bottom)
                ),
                topLeft = glassRect.topLeft,
                size = glassRect.size,
                cornerRadius = CornerRadius(10f, 10f)
            )
            // Diagonal glass highlight
            drawLine(
                color = Color.White.copy(alpha = 0.16f),
                start = Offset(glassRect.left + glassRect.width * 0.15f, glassRect.top + 4f),
                end = Offset(glassRect.left + glassRect.width * 0.35f, glassRect.bottom - 4f),
                strokeWidth = 6f
            )

            // LED Route Header Bar inside top rear glass
            drawRoundRect(
                color = Color(0xFF0A0D10),
                topLeft = Offset(centerX - busWidth * 0.34f, topY + busHeight * 0.05f),
                size = Size(busWidth * 0.68f, busHeight * 0.06f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawContext.canvas.nativeCanvas.apply {
                val paint = Paint().apply {
                    color = android.graphics.Color.parseColor("#FFB300")
                    textAlign = Paint.Align.CENTER
                    textSize = (busHeight * 0.048f).coerceAtLeast(12f)
                    typeface = Typeface.DEFAULT_BOLD
                    isAntiAlias = true
                }
                drawText(bus.operatorTag, centerX, topY + busHeight * 0.095f, paint)
            }

            // 9. Engine Louvers & Rear License Plate
            drawRoundRect(
                color = Color(0xFF1C232B),
                topLeft = Offset(centerX - busWidth * 0.25f, topY + busHeight * 0.69f),
                size = Size(busWidth * 0.50f, busHeight * 0.13f),
                cornerRadius = CornerRadius(5f, 5f)
            )
            // Green Bangladeshi Commercial License Plate
            drawRoundRect(
                color = Color(0xFF1B5E20),
                topLeft = Offset(centerX - busWidth * 0.15f, topY + busHeight * 0.72f),
                size = Size(busWidth * 0.30f, busHeight * 0.07f),
                cornerRadius = CornerRadius(3f, 3f)
            )

            // 10. Lower Bumper
            drawRoundRect(
                color = Color(0xFF26303B),
                topLeft = Offset(leftX, bottomY - busHeight * 0.14f),
                size = Size(busWidth, busHeight * 0.07f),
                cornerRadius = CornerRadius(6f, 6f)
            )

            // 11. Tail Lights & Dynamic Brake Glow
            val tailY = topY + busHeight * 0.69f
            val tailW = busWidth * 0.14f
            val tailH = busHeight * 0.085f
            val leftTailX = leftX + busWidth * 0.04f
            val rightTailX = leftX + busWidth * 0.82f

            val tailGlowColor = if (isBraking) Color(0xFFFF1744) else Color(0xFFB71C1C)
            if (isBraking) {
                drawCircle(
                    color = Color(0xFFFF1744).copy(alpha = 0.45f),
                    radius = tailW * 1.15f,
                    center = Offset(leftTailX + tailW / 2f, tailY + tailH / 2f)
                )
                drawCircle(
                    color = Color(0xFFFF1744).copy(alpha = 0.45f),
                    radius = tailW * 1.15f,
                    center = Offset(rightTailX + tailW / 2f, tailY + tailH / 2f)
                )
            }
            drawRoundRect(tailGlowColor, Offset(leftTailX, tailY), Size(tailW, tailH), CornerRadius(4f, 4f))
            drawRoundRect(tailGlowColor, Offset(rightTailX, tailY), Size(tailW, tailH), CornerRadius(4f, 4f))

            // Amber Turn Signals
            val leftAmber = if (leftSignalOn && blinkPhase) Color(0xFFFFC400) else Color(0xFF6E4B00)
            val rightAmber = if (rightSignalOn && blinkPhase) Color(0xFFFFC400) else Color(0xFF6E4B00)
            drawRoundRect(leftAmber, Offset(leftTailX, tailY - tailH * 0.65f), Size(tailW * 0.7f, tailH * 0.52f), CornerRadius(3f, 3f))
            drawRoundRect(rightAmber, Offset(rightTailX + tailW * 0.3f, tailY - tailH * 0.65f), Size(tailW * 0.7f, tailH * 0.52f), CornerRadius(3f, 3f))
        }
    }

    fun DrawScope.drawTrafficVehicle(
        vehicle: TrafficVehicle,
        screenX: Float,
        bottomY: Float,
        scaleWidth: Float,
        weather: WeatherTime
    ) {
        val w = scaleWidth * vehicle.type.widthUnits
        if (w < 6f) return

        val h = when (vehicle.type) {
            TrafficType.TRUCK_BD -> w * 1.18f
            TrafficType.ONCOMING_BUS -> w * 1.24f
            TrafficType.CNG_AUTO -> w * 0.95f
            TrafficType.EASY_BIKE -> w * 1.02f
            TrafficType.RICKSHAW -> w * 1.12f
            TrafficType.MICROBUS -> w * 0.86f
            TrafficType.SEDAN -> w * 0.68f
        }
        val leftX = screenX - w / 2f
        val topY = bottomY - h

        // Ground shadow
        drawOval(
            color = Color.Black.copy(alpha = 0.46f),
            topLeft = Offset(leftX - w * 0.05f, bottomY - h * 0.07f),
            size = Size(w * 1.10f, h * 0.14f)
        )

        when (vehicle.type) {
            TrafficType.CNG_AUTO -> {
                // Iconic Green Bangladeshi CNG Auto-rickshaw with yellow-green canopy
                drawRoundRect(Color(0xFF14171A), Offset(leftX + w * 0.06f, bottomY - h * 0.22f), Size(w * 0.16f, h * 0.22f), CornerRadius(3f, 3f))
                drawRoundRect(Color(0xFF14171A), Offset(leftX + w * 0.78f, bottomY - h * 0.22f), Size(w * 0.16f, h * 0.22f), CornerRadius(3f, 3f))
                // Lower green body
                drawRoundRect(Color(0xFF0B6E3D), Offset(leftX + w * 0.04f, topY + h * 0.46f), Size(w * 0.92f, h * 0.44f), CornerRadius(8f, 8f))
                // Yellow-khaki canvas roof
                val roofPath = Path().apply {
                    moveTo(leftX + w * 0.14f, topY)
                    lineTo(leftX + w * 0.86f, topY)
                    lineTo(leftX + w * 0.95f, topY + h * 0.48f)
                    lineTo(leftX + w * 0.05f, topY + h * 0.48f)
                    close()
                }
                drawPath(roofPath, Color(0xFF2E3830))
                // Rear tiny window & taillights
                drawRoundRect(Color(0xFF81D4FA).copy(alpha = 0.45f), Offset(leftX + w * 0.24f, topY + h * 0.16f), Size(w * 0.52f, h * 0.20f), CornerRadius(4f, 4f))
                drawCircle(Color(0xFFFF1744), radius = w * 0.06f, center = Offset(leftX + w * 0.18f, topY + h * 0.72f))
                drawCircle(Color(0xFFFF1744), radius = w * 0.06f, center = Offset(leftX + w * 0.82f, topY + h * 0.72f))
            }

            TrafficType.EASY_BIKE -> {
                // Mymensingh signature Battery Easy-Bike (blue or yellow open frame)
                val bodyColor = if (vehicle.colorVariant % 2 == 0) Color(0xFF0277BD) else Color(0xFFF9A825)
                drawRoundRect(Color(0xFF14171A), Offset(leftX + w * 0.08f, bottomY - h * 0.20f), Size(w * 0.14f, h * 0.20f), CornerRadius(3f, 3f))
                drawRoundRect(Color(0xFF14171A), Offset(leftX + w * 0.78f, bottomY - h * 0.20f), Size(w * 0.14f, h * 0.20f), CornerRadius(3f, 3f))
                // Open cabin interior shadow
                drawRect(Color(0xFF1A2229), Offset(leftX + w * 0.12f, topY + h * 0.12f), Size(w * 0.76f, h * 0.42f))
                // Roof & corner steel poles
                drawRoundRect(bodyColor, Offset(leftX + w * 0.06f, topY), Size(w * 0.88f, h * 0.14f), CornerRadius(6f, 6f))
                drawRect(bodyColor, Offset(leftX + w * 0.06f, topY + h * 0.46f), Size(w * 0.88f, h * 0.42f))
                drawLine(Color(0xFFE0E0E0), Offset(leftX + w * 0.10f, topY + h * 0.12f), Offset(leftX + w * 0.10f, topY + h * 0.48f), strokeWidth = (w * 0.04f).coerceAtLeast(1.5f))
                drawLine(Color(0xFFE0E0E0), Offset(leftX + w * 0.90f, topY + h * 0.12f), Offset(leftX + w * 0.90f, topY + h * 0.48f), strokeWidth = (w * 0.04f).coerceAtLeast(1.5f))
                drawCircle(Color(0xFFFF3D00), radius = w * 0.055f, center = Offset(leftX + w * 0.18f, topY + h * 0.68f))
                drawCircle(Color(0xFFFF3D00), radius = w * 0.055f, center = Offset(leftX + w * 0.82f, topY + h * 0.68f))
            }

            TrafficType.RICKSHAW -> {
                // Colorful painted Bangladeshi cycle rickshaw
                val hoodColors = listOf(Color(0xFFE91E63), Color(0xFFFF5722), Color(0xFF009688))
                val hoodColor = hoodColors[vehicle.colorVariant % hoodColors.size]
                // Thin spoked rear wheels
                drawRect(Color(0xFF111111), Offset(leftX + w * 0.04f, bottomY - h * 0.44f), Size(w * 0.09f, h * 0.44f))
                drawRect(Color(0xFF111111), Offset(leftX + w * 0.87f, bottomY - h * 0.44f), Size(w * 0.09f, h * 0.44f))
                // Painted Hood
                drawRoundRect(hoodColor, Offset(leftX + w * 0.14f, topY), Size(w * 0.72f, h * 0.58f), CornerRadius(14f, 14f))
                drawRoundRect(Color(0xFFFFEB3B), Offset(leftX + w * 0.22f, topY + h * 0.56f), Size(w * 0.56f, h * 0.24f), CornerRadius(4f, 4f))
            }

            TrafficType.TRUCK_BD -> {
                // Bangladeshi Cargo Truck with colorful Truck Art rear panel!
                drawRoundRect(Color(0xFF14171A), Offset(leftX + w * 0.05f, bottomY - h * 0.18f), Size(w * 0.20f, h * 0.18f), CornerRadius(4f, 4f))
                drawRoundRect(Color(0xFF14171A), Offset(leftX + w * 0.75f, bottomY - h * 0.18f), Size(w * 0.20f, h * 0.18f), CornerRadius(4f, 4f))
                val baseColor = if (vehicle.colorVariant % 2 == 0) Color(0xFF1B5E20) else Color(0xFFBF360C)
                drawRoundRect(baseColor, Offset(leftX, topY), Size(w, h * 0.85f), CornerRadius(6f, 6f))
                // Decorative Truck Art Border & Motif
                drawRect(Color(0xFFFFCA28), Offset(leftX + w * 0.07f, topY + h * 0.08f), Size(w * 0.86f, h * 0.54f), style = Stroke(width = (w * 0.04f).coerceAtLeast(2f)))
                drawCircle(Color(0xFFE91E63), radius = w * 0.14f, center = Offset(screenX, topY + h * 0.34f))
                drawCircle(Color(0xFF00E5FF), radius = w * 0.08f, center = Offset(screenX, topY + h * 0.34f))
                // Yellow-Black Bumper
                drawRect(Color(0xFFFFC107), Offset(leftX, bottomY - h * 0.20f), Size(w, h * 0.08f))
                drawRect(Color(0xFFFF1744), Offset(leftX + w * 0.06f, topY + h * 0.72f), Size(w * 0.14f, h * 0.06f))
                drawRect(Color(0xFFFF1744), Offset(leftX + w * 0.80f, topY + h * 0.72f), Size(w * 0.14f, h * 0.06f))
            }

            TrafficType.MICROBUS, TrafficType.SEDAN -> {
                val carColors = listOf(Color(0xFFCFD8DC), Color(0xFF263238), Color(0xFF8D6E63), Color(0xFF1E88E5))
                val paintColor = carColors[vehicle.colorVariant % carColors.size]
                drawRoundRect(Color(0xFF14171A), Offset(leftX + w * 0.08f, bottomY - h * 0.20f), Size(w * 0.18f, h * 0.20f), CornerRadius(4f, 4f))
                drawRoundRect(Color(0xFF14171A), Offset(leftX + w * 0.74f, bottomY - h * 0.20f), Size(w * 0.18f, h * 0.20f), CornerRadius(4f, 4f))
                // Cabin glass
                drawRoundRect(paintColor, Offset(leftX + w * 0.10f, topY), Size(w * 0.80f, h * 0.45f), CornerRadius(10f, 10f))
                drawRoundRect(Color(0xFF102027), Offset(leftX + w * 0.16f, topY + h * 0.08f), Size(w * 0.68f, h * 0.28f), CornerRadius(6f, 6f))
                // Lower trunk/body
                drawRoundRect(paintColor, Offset(leftX, topY + h * 0.38f), Size(w, h * 0.50f), CornerRadius(8f, 8f))
                val lightColor = if (vehicle.isOncoming) {
                    if (weather == WeatherTime.NIGHT) Color(0xFFFFF59D) else Color(0xFFFFF9C4)
                } else {
                    if (vehicle.isBraking) Color(0xFFFF1744) else Color(0xFFD50000)
                }
                drawRoundRect(lightColor, Offset(leftX + w * 0.06f, topY + h * 0.50f), Size(w * 0.18f, h * 0.12f), CornerRadius(3f, 3f))
                drawRoundRect(lightColor, Offset(leftX + w * 0.76f, topY + h * 0.50f), Size(w * 0.18f, h * 0.12f), CornerRadius(3f, 3f))
                if (vehicle.isOncoming && (weather == WeatherTime.NIGHT || weather == WeatherTime.SUNSET)) {
                    drawCircle(Color(0xFFFFF59D).copy(alpha = 0.35f), radius = w * 0.26f, center = Offset(leftX + w * 0.15f, topY + h * 0.56f))
                    drawCircle(Color(0xFFFFF59D).copy(alpha = 0.35f), radius = w * 0.26f, center = Offset(leftX + w * 0.85f, topY + h * 0.56f))
                }
            }

            TrafficType.ONCOMING_BUS -> {
                val busColor = if (vehicle.colorVariant % 2 == 0) Color(0xFFC62828) else Color(0xFF00695C)
                drawRoundRect(Color(0xFF14171A), Offset(leftX + w * 0.06f, bottomY - h * 0.16f), Size(w * 0.16f, h * 0.16f), CornerRadius(4f, 4f))
                drawRoundRect(Color(0xFF14171A), Offset(leftX + w * 0.78f, bottomY - h * 0.16f), Size(w * 0.16f, h * 0.16f), CornerRadius(4f, 4f))
                drawRoundRect(busColor, Offset(leftX, topY), Size(w, h * 0.90f), CornerRadius(10f, 10f))
                // Front windshield
                drawRoundRect(Color(0xFF16222E), Offset(leftX + w * 0.08f, topY + h * 0.12f), Size(w * 0.84f, h * 0.42f), CornerRadius(8f, 8f))
                // Glowing Front Headlights
                val glowAlpha = if (weather == WeatherTime.NIGHT) 0.55f else 0.28f
                drawCircle(Color(0xFFFFF176).copy(alpha = glowAlpha), radius = w * 0.24f, center = Offset(leftX + w * 0.16f, topY + h * 0.74f))
                drawCircle(Color(0xFFFFF176).copy(alpha = glowAlpha), radius = w * 0.24f, center = Offset(leftX + w * 0.84f, topY + h * 0.74f))
                drawRoundRect(Color(0xFFFFF9C4), Offset(leftX + w * 0.08f, topY + h * 0.70f), Size(w * 0.18f, h * 0.08f), CornerRadius(4f, 4f))
                drawRoundRect(Color(0xFFFFF9C4), Offset(leftX + w * 0.74f, topY + h * 0.70f), Size(w * 0.18f, h * 0.08f), CornerRadius(4f, 4f))
            }
        }
    }

    fun DrawScope.drawCockpitOverlay(
        bus: BusModel,
        speedKmh: Float,
        steerInput: Float,
        wipersOn: Boolean,
        weather: WeatherTime,
        timeSec: Float
    ) {
        val w = size.width
        val h = size.height

        // A-Pillars & Upper Visor Frame
        val pillarColor = Color(0xFF12161B)
        drawRect(pillarColor, Offset(0f, 0f), Size(w, h * 0.08f))
        val leftPillar = Path().apply {
            moveTo(0f, 0f)
            lineTo(w * 0.065f, 0f)
            lineTo(w * 0.03f, h)
            lineTo(0f, h)
            close()
        }
        val rightPillar = Path().apply {
            moveTo(w, 0f)
            lineTo(w * 0.935f, 0f)
            lineTo(w * 0.97f, h)
            lineTo(w, h)
            close()
        }
        drawPath(leftPillar, pillarColor)
        drawPath(rightPillar, pillarColor)

        // Rear-view Mirror & Swaying Emerald Hanging Ornament
        val mirrorW = w * 0.20f
        val mirrorH = h * 0.065f
        val mirrorX = (w - mirrorW) / 2f
        val mirrorY = h * 0.085f
        drawLine(Color(0xFF263238), Offset(w / 2f, 0f), Offset(w / 2f, mirrorY), strokeWidth = 6f)
        drawRoundRect(Color(0xFF1F2833), Offset(mirrorX - 4f, mirrorY - 4f), Size(mirrorW + 8f, mirrorH + 8f), CornerRadius(10f, 10f))
        drawRoundRect(Color(0xFF334756), Offset(mirrorX, mirrorY), Size(mirrorW, mirrorH), CornerRadius(8f, 8f))

        // Swaying charm
        val sway = sin(timeSec * 3.5f) * 6f - steerInput * 16f
        drawLine(Color(0xFFFFCA28), Offset(w / 2f, mirrorY + mirrorH), Offset(w / 2f + sway, mirrorY + mirrorH + 26f), strokeWidth = 2.5f)
        drawCircle(Color(0xFF0E7C4E), radius = 7f, center = Offset(w / 2f + sway, mirrorY + mirrorH + 31f))

        // Animated Twin Windshield Wipers (active in rain or when toggled)
        val dashTopY = h * 0.74f
        if (wipersOn || weather == WeatherTime.MONSOON_RAIN) {
            val sweep = ((sin(timeSec * 4.5f) + 1f) * 0.5f) // 0..1
            val angleDeg = -65f + sweep * 110f
            val pivot1 = Offset(w * 0.28f, dashTopY + 12f)
            val pivot2 = Offset(w * 0.68f, dashTopY + 12f)
            rotate(angleDeg, pivot1) {
                drawLine(Color(0xFF0F1317), pivot1, Offset(pivot1.x, pivot1.y - h * 0.36f), strokeWidth = 7f, cap = StrokeCap.Round)
            }
            rotate(angleDeg, pivot2) {
                drawLine(Color(0xFF0F1317), pivot2, Offset(pivot2.x, pivot2.y - h * 0.36f), strokeWidth = 7f, cap = StrokeCap.Round)
            }
        }

        // Curved Bus Dashboard
        val dashPath = Path().apply {
            moveTo(0f, dashTopY + h * 0.05f)
            quadraticTo(w * 0.5f, dashTopY - h * 0.04f, w, dashTopY + h * 0.05f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            dashPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF1D242C), Color(0xFF0F1419)),
                startY = dashTopY,
                endY = h
            )
        )
        // Accent trim strip across dash
        drawLine(
            color = bus.accentColor.copy(alpha = 0.65f),
            start = Offset(w * 0.08f, dashTopY + h * 0.045f),
            end = Offset(w * 0.92f, dashTopY + h * 0.045f),
            strokeWidth = 3f
        )

        // Analog Speedometer Dial in center-left of dash
        val dialCenter = Offset(w * 0.50f, dashTopY + h * 0.08f)
        val dialR = (w.coerceAtMost(h)) * 0.085f
        drawCircle(Color(0xFF0B0E12), radius = dialR, center = dialCenter)
        drawCircle(Color(0xFF2D3947), radius = dialR, center = dialCenter, style = Stroke(3f))
        val speedRatio = (speedKmh / 140f).coerceIn(0f, 1f)
        drawArc(
            color = Color(0xFF4CD995),
            startAngle = 140f,
            sweepAngle = 260f * speedRatio,
            useCenter = false,
            topLeft = Offset(dialCenter.x - dialR * 0.78f, dialCenter.y - dialR * 0.78f),
            size = Size(dialR * 1.56f, dialR * 1.56f),
            style = Stroke(width = 5f, cap = StrokeCap.Round)
        )
        val needleAngleRad = (140f + 260f * speedRatio) * (PI / 180.0)
        drawLine(
            color = Color(0xFFFF5252),
            start = dialCenter,
            end = Offset(
                dialCenter.x + (cos(needleAngleRad) * dialR * 0.72f).toFloat(),
                dialCenter.y + (sin(needleAngleRad) * dialR * 0.72f).toFloat()
            ),
            strokeWidth = 3.5f,
            cap = StrokeCap.Round
        )
    }

    /**
     * Detailed vector showcase of a Bangladeshi coach bus for the Garage screen.
     */
    fun DrawScope.drawGarageBusShowcase(bus: BusModel, timeSec: Float) {
        val w = size.width
        val h = size.height
        val floatY = sin(timeSec * 1.8f) * 3f

        // Ground shadow
        drawOval(
            color = Color.Black.copy(alpha = 0.55f),
            topLeft = Offset(w * 0.12f, h * 0.78f),
            size = Size(w * 0.76f, h * 0.13f)
        )

        val bodyLeft = w * 0.14f
        val bodyRight = w * 0.86f
        val bodyW = bodyRight - bodyLeft
        val bodyTop = h * 0.22f + floatY
        val bodyBottom = h * 0.80f + floatY
        val bodyH = bodyBottom - bodyTop

        // Roof AC Pod
        drawRoundRect(
            color = Color(0xFFCFD8DC),
            topLeft = Offset(bodyLeft + bodyW * 0.18f, bodyTop - bodyH * 0.08f),
            size = Size(bodyW * 0.52f, bodyH * 0.10f),
            cornerRadius = CornerRadius(12f, 12f)
        )

        // Main Coach Shell
        val shell = Path().apply {
            moveTo(bodyLeft + bodyW * 0.06f, bodyTop)
            lineTo(bodyRight - bodyW * 0.03f, bodyTop)
            quadraticTo(bodyRight + bodyW * 0.02f, bodyTop + bodyH * 0.45f, bodyRight, bodyBottom - bodyH * 0.08f)
            lineTo(bodyLeft, bodyBottom - bodyH * 0.08f)
            quadraticTo(bodyLeft - bodyW * 0.02f, bodyTop + bodyH * 0.45f, bodyLeft + bodyW * 0.06f, bodyTop)
            close()
        }
        drawPath(
            shell,
            brush = Brush.verticalGradient(
                colors = listOf(bus.primaryColor, bus.primaryColor.copy(alpha = 0.9f), Color(0xFF1C232B)),
                startY = bodyTop,
                endY = bodyBottom
            )
        )

        // Secondary & Accent Swooshes
        drawRoundRect(
            color = bus.secondaryColor,
            topLeft = Offset(bodyLeft + bodyW * 0.01f, bodyTop + bodyH * 0.48f),
            size = Size(bodyW * 0.98f, bodyH * 0.22f),
            cornerRadius = CornerRadius(6f, 6f)
        )
        drawRect(
            color = bus.accentColor,
            topLeft = Offset(bodyLeft + bodyW * 0.01f, bodyTop + bodyH * 0.70f),
            size = Size(bodyW * 0.98f, bodyH * 0.05f)
        )

        // Side Passenger Windows + Front Panorama Windshield
        val winTop = bodyTop + bodyH * 0.12f
        val winH = bodyH * 0.30f
        for (idx in 0 until 5) {
            val wx = bodyLeft + bodyW * (0.06f + idx * 0.145f)
            drawRoundRect(
                color = Color(0xFF10202B),
                topLeft = Offset(wx, winTop),
                size = Size(bodyW * 0.125f, winH),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawLine(
                color = Color.White.copy(alpha = 0.14f),
                start = Offset(wx + 4f, winTop + 4f),
                end = Offset(wx + bodyW * 0.05f, winTop + winH - 4f),
                strokeWidth = 3f
            )
        }
        // Front Windshield on right
        drawRoundRect(
            color = Color(0xFF142634),
            topLeft = Offset(bodyRight - bodyW * 0.19f, winTop),
            size = Size(bodyW * 0.18f, winH * 1.15f),
            cornerRadius = CornerRadius(10f, 10f)
        )

        // Headlight glow on front right
        drawCircle(
            color = Color(0xFFFFD54F).copy(alpha = 0.40f),
            radius = bodyH * 0.15f,
            center = Offset(bodyRight - bodyW * 0.03f, bodyBottom - bodyH * 0.20f)
        )
        drawRoundRect(
            color = Color(0xFFFFF59D),
            topLeft = Offset(bodyRight - bodyW * 0.07f, bodyBottom - bodyH * 0.24f),
            size = Size(bodyW * 0.07f, bodyH * 0.09f),
            cornerRadius = CornerRadius(4f, 4f)
        )

        // Two Alloy Wheels
        val wheelR = bodyH * 0.17f
        val wheelY = bodyBottom - bodyH * 0.06f
        listOf(bodyLeft + bodyW * 0.23f, bodyRight - bodyW * 0.23f).forEach { wx ->
            drawCircle(Color(0xFF101316), radius = wheelR, center = Offset(wx, wheelY))
            drawCircle(Color(0xFFB0BEC5), radius = wheelR * 0.54f, center = Offset(wx, wheelY))
            drawCircle(Color(0xFF37474F), radius = wheelR * 0.22f, center = Offset(wx, wheelY))
        }
    }
}
