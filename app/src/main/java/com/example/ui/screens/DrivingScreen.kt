package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.opengl.GLSurfaceView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DoorSliding
import androidx.compose.material.icons.rounded.Highlight
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.PlayerPreferences
import com.example.domain.model.CameraMode
import com.example.domain.model.SteeringMode
import com.example.domain.model.WeatherTime
import com.example.engine.VehicleRenderer.drawCockpitOverlay
import com.example.engine3d.Bus3DRenderer
import com.example.ui.components.MymensinghLiveMiniMap
import com.example.ui.theme.BengalEmerald
import com.example.ui.theme.BengalEmeraldLight
import com.example.ui.theme.ExpressCrimson
import com.example.ui.theme.HeadlightAmber
import com.example.viewmodel.DrivingUiState

@Composable
fun DrivingScreen(
    state: DrivingUiState,
    preferences: PlayerPreferences,
    onStepFrame: (Float) -> Unit,
    onAccelerateChange: (Boolean) -> Unit,
    onBrakeChange: (Boolean) -> Unit,
    onSteerChange: (Float) -> Unit,
    onHornChange: (Boolean) -> Unit,
    onToggleDoor: () -> Unit,
    onToggleHeadlights: () -> Unit,
    onToggleWipers: () -> Unit,
    onToggleLeftSignal: () -> Unit,
    onToggleRightSignal: () -> Unit,
    onToggleCamera: () -> Unit,
    onCycleWeather: () -> Unit,
    onToggleSound: () -> Unit,
    onTogglePause: () -> Unit,
    onRestartTrip: () -> Unit,
    onExitToMap: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    var isLandscapeMode by remember { mutableStateOf(true) }

    BackHandler {
        if (state.isTripCompleted) {
            onExitToMap()
        } else {
            onTogglePause()
        }
    }

    DisposableEffect(view, isLandscapeMode) {
        val activity = context as? Activity
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        if (activity != null) {
            activity.requestedOrientation = if (isLandscapeMode) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
            }
        }
        onDispose {
            onHornChange(false)
            onAccelerateChange(false)
            onBrakeChange(false)
        }
    }

    DisposableEffect(Unit) {
        val activity = context as? Activity
        val window = activity?.window
        onDispose {
            if (window != null) {
                WindowCompat.getInsetsController(window, view).show(WindowInsetsCompat.Type.systemBars())
            }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        }
    }

    // 60fps Simulation Loop synced to Display VSync
    LaunchedEffect(state.isPaused, state.isTripCompleted) {
        var lastNanos = 0L
        while (!state.isPaused && !state.isTripCompleted) {
            withFrameNanos { nanos ->
                if (lastNanos != 0L) {
                    val dt = ((nanos - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                    onStepFrame(dt)
                }
                lastNanos = nanos
            }
        }
    }

    val renderer = remember { Bus3DRenderer() }
    renderer.uiState = state
    renderer.weatherTime = preferences.weatherTime
    renderer.cameraMode = preferences.cameraMode

    val lifecycleOwner = LocalLifecycleOwner.current
    var glViewRef by remember { mutableStateOf<GLSurfaceView?>(null) }

    DisposableEffect(lifecycleOwner, glViewRef) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> glViewRef?.onResume()
                Lifecycle.Event.ON_PAUSE -> glViewRef?.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // 1. Real-Time OpenGL ES 2.0 3D World (3D Buses, Mymensingh Roads, Shambhuganj Bridge & Traffic)
        AndroidView(
            factory = { ctx ->
                GLSurfaceView(ctx).apply {
                    setEGLContextClientVersion(2)
                    setRenderer(renderer)
                    renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    glViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 1b. Transparent Cockpit Interior / Rain Drops Overlay over 3D View
        if (preferences.cameraMode == CameraMode.COCKPIT || preferences.weatherTime == WeatherTime.MONSOON_RAIN) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (preferences.weatherTime == WeatherTime.MONSOON_RAIN) {
                    val speedFactor = 1f + state.speedKmh / 60f
                    for (i in 0 until 70) {
                        val rx = ((i * 97 + state.timeSec * 120f) % size.width)
                        val ry = ((i * 53 + state.timeSec * 900f * speedFactor) % size.height)
                        drawLine(
                            color = Color(0xFFB3E5FC).copy(alpha = 0.34f),
                            start = Offset(rx, ry),
                            end = Offset(rx - 8f, ry + 24f),
                            strokeWidth = 2f
                        )
                    }
                }
                if (preferences.cameraMode == CameraMode.COCKPIT) {
                    drawCockpitOverlay(
                        bus = state.bus,
                        speedKmh = state.speedKmh,
                        steerInput = state.steerSmooth,
                        wipersOn = state.wipersOn,
                        weather = preferences.weatherTime,
                        timeSec = state.timeSec
                    )
                }
            }
        }

        // 2. Top Driving Telemetry & Live Mymensingh GPS Mini-Map
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left: Live Mymensingh Mini-Map + Zone Name
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    MymensinghLiveMiniMap(
                        route = state.route,
                        progressFraction = state.progressFraction,
                        modifier = Modifier.size(width = 76.dp, height = 68.dp)
                    )
                    Surface(
                        color = Color(0xA60C141C),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF2B3D4E))
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text(
                                text = state.currentZoneTitleBn,
                                style = MaterialTheme.typography.labelSmall,
                                color = BengalEmeraldLight
                            )
                            state.nextStop?.let { stop ->
                                Text(
                                    text = "${stop.landmark.nameBn} (${state.distanceToNextStopMeters}m)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                text = "Pax: ${state.passengers}/${state.bus.capacity} • ৳${state.fareEarnedTaka}",
                                style = MaterialTheme.typography.labelSmall,
                                color = HeadlightAmber
                            )
                        }
                    }
                }

                // Right: Utility Icon Strip (Camera, Rotate Full-Screen, Weather, Sound, Pause)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HudCircleIconButton(
                        icon = Icons.Rounded.Videocam,
                        contentDescription = "Toggle Camera",
                        onClick = onToggleCamera,
                        testTag = "hud_camera_button"
                    )
                    HudCircleIconButton(
                        icon = Icons.Rounded.ScreenRotation,
                        contentDescription = "Toggle Landscape Fullscreen",
                        onClick = { isLandscapeMode = !isLandscapeMode },
                        testTag = "hud_rotate_button"
                    )
                    HudCircleIconButton(
                        icon = Icons.Rounded.WbSunny,
                        contentDescription = "Cycle Weather",
                        onClick = onCycleWeather,
                        testTag = "hud_weather_button"
                    )
                    HudCircleIconButton(
                        icon = if (preferences.soundEnabled) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                        contentDescription = "Toggle Sound",
                        onClick = onToggleSound,
                        testTag = "hud_sound_button"
                    )
                    HudCircleIconButton(
                        icon = Icons.Rounded.Pause,
                        contentDescription = "Pause",
                        onClick = onTogglePause,
                        testTag = "hud_pause_button"
                    )
                }
            }

            // Speedometer & Comfort Pill Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xCC0D151D),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF2D4152))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${state.speedKmh.toInt()}",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = MaterialTheme.typography.labelSmall.fontFamily
                            ),
                            color = if (state.speedKmh > state.speedLimitKmh + 10) ExpressCrimson else Color.White
                        )
                        Text(
                            text = "KM/H",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF9FB0C0),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Speed Limit Circle
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(3.dp, ExpressCrimson, CircleShape)
                        ) {
                            Text(
                                text = "${state.speedLimitKmh}",
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Surface(
                    color = Color(0xCC0D151D),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Safety: ${state.comfortPercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.comfortPercent >= 75) BengalEmeraldLight else HeadlightAmber,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            }

            // Live Dynamic Banner (Bus stop prompt / Boarding progress / Bridge alert)
            state.statusBannerBn?.let { bannerText ->
                Surface(
                    color = if (state.isInStopZone) Color(0xDD0B4F32) else Color(0xCC16202B),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (state.isInStopZone) BengalEmeraldLight else HeadlightAmber.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = bannerText,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                        if (state.boardingProgress > 0f) {
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { state.boardingProgress },
                                color = HeadlightAmber,
                                trackColor = Color(0xFF0A2E1D),
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }
            }
        }

        // 3. Bottom Ergonomic Multi-Touch Driving Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Utility Dashboard Buttons Row (Signals, Door, Horn, Lights, Wiper)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmallToggleControl(
                    label = "◀",
                    active = state.leftSignalOn,
                    activeColor = HeadlightAmber,
                    onClick = onToggleLeftSignal,
                    testTag = "btn_signal_left"
                )
                Spacer(modifier = Modifier.width(6.dp))
                SmallToggleControl(
                    label = "Lights",
                    icon = Icons.Rounded.Highlight,
                    active = state.headlightsOn,
                    activeColor = HeadlightAmber,
                    onClick = onToggleHeadlights,
                    testTag = "btn_headlights"
                )
                Spacer(modifier = Modifier.width(6.dp))
                SmallToggleControl(
                    label = if (state.doorOpen) "Close" else "Door",
                    icon = Icons.Rounded.DoorSliding,
                    active = state.doorOpen || state.isInStopZone,
                    activeColor = BengalEmeraldLight,
                    onClick = onToggleDoor,
                    testTag = "btn_door"
                )
                Spacer(modifier = Modifier.width(6.dp))
                SmallToggleControl(
                    label = "Wiper",
                    icon = Icons.Rounded.CleaningServices,
                    active = state.wipersOn,
                    activeColor = Color(0xFF4FC3F7),
                    onClick = onToggleWipers,
                    testTag = "btn_wiper"
                )
                Spacer(modifier = Modifier.width(6.dp))
                SmallToggleControl(
                    label = "▶",
                    active = state.rightSignalOn,
                    activeColor = HeadlightAmber,
                    onClick = onToggleRightSignal,
                    testTag = "btn_signal_right"
                )
            }

            // Main Left (Steering) + Center (Horn) + Right (Brake/Gas Pedals)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Steering Control (Wheel or Left/Right Buttons)
                if (preferences.steeringMode == SteeringMode.WHEEL) {
                    SteeringWheelControl(
                        steerValue = state.steerSmooth,
                        onSteerChange = onSteerChange
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SteerPadButton(
                            icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDesc = "Steer Left",
                            onHold = { held -> onSteerChange(if (held) -1f else 0f) },
                            testTag = "btn_steer_left"
                        )
                        SteerPadButton(
                            icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDesc = "Steer Right",
                            onHold = { held -> onSteerChange(if (held) 1f else 0f) },
                            testTag = "btn_steer_right"
                        )
                    }
                }

                // Center Tactile Air-Horn Button
                var hornHeld by remember { mutableStateOf(false) }
                Surface(
                    shape = CircleShape,
                    color = if (hornHeld) HeadlightAmber else Color(0xCC2A1F0A),
                    border = BorderStroke(2.dp, HeadlightAmber),
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                hornHeld = true
                                onHornChange(true)
                                do {
                                    val event = awaitPointerEvent()
                                } while (event.changes.any { it.pressed })
                                hornHeld = false
                                onHornChange(false)
                            }
                        }
                        .testTag("btn_horn")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Campaign,
                            contentDescription = "Bus Horn",
                            tint = if (hornHeld) Color.Black else HeadlightAmber,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "HORN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hornHeld) Color.Black else HeadlightAmber
                        )
                    }
                }

                // Right: Brake & Gas Pedals
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    PedalButton(
                        labelBn = "BRAKE",
                        labelEn = "STOP",
                        baseColor = ExpressCrimson,
                        heightDp = 82,
                        onHoldChange = onBrakeChange,
                        testTag = "btn_brake_pedal"
                    )
                    PedalButton(
                        labelBn = "GAS",
                        labelEn = "DRIVE",
                        baseColor = BengalEmerald,
                        heightDp = 104,
                        onHoldChange = onAccelerateChange,
                        testTag = "btn_gas_pedal"
                    )
                }
            }
        }

        // 4. Pause Overlay
        if (state.isPaused && !state.isTripCompleted) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xAA05090D)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(0.85f)
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Game Paused", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = "${state.route.titleBn} • ${state.bus.nameBn}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onTogglePause,
                            colors = ButtonDefaults.buttonColors(containerColor = BengalEmerald),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Resume Drive")
                        }
                        OutlinedButton(
                            onClick = onRestartTrip,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Restart Route")
                        }
                        OutlinedButton(
                            onClick = onExitToMap,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Back to Mymensingh Map")
                        }
                    }
                }
            }
        }

        // 5. Trip Complete Summary Overlay
        if (state.isTripCompleted) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xBB050A0E)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, BengalEmeraldLight),
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(0.75f)
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Trip Completed!",
                            style = MaterialTheme.typography.headlineMedium,
                            color = BengalEmeraldLight
                        )
                        Text(
                            text = state.route.titleBn,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (s in 1..3) {
                                Icon(
                                    imageVector = Icons.Rounded.Star,
                                    contentDescription = null,
                                    tint = if (s <= state.earnedStars) HeadlightAmber else Color(0xFF394652),
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }
                        Surface(
                            color = Color(0xFF101C24),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SummaryStatRow("Total Fare + Bonus", "৳${state.fareEarnedTaka}", HeadlightAmber)
                                SummaryStatRow("Passengers Carried", "${state.passengers} Pax", Color.White)
                                SummaryStatRow("Driving Safety Score", "${state.comfortPercent}%", BengalEmeraldLight)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onExitToMap,
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text("Map")
                            }
                            Button(
                                onClick = onRestartTrip,
                                colors = ButtonDefaults.buttonColors(containerColor = BengalEmerald),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Icon(Icons.Rounded.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Drive Again")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStatRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFA7B4C2))
        Text(value, style = MaterialTheme.typography.titleMedium, color = valueColor)
    }
}

@Composable
private fun HudCircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = CircleShape,
        color = Color(0xCC0D151D),
        border = BorderStroke(1.dp, Color(0xFF2D4152)),
        modifier = Modifier
            .size(42.dp)
            .testTag(testTag)
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SmallToggleControl(
    label: String,
    active: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    testTag: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (active) activeColor.copy(alpha = 0.25f) else Color(0xBB10171F),
        border = BorderStroke(1.dp, if (active) activeColor else Color(0xFF334252)),
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = label, tint = if (active) activeColor else Color.White, modifier = Modifier.size(16.dp))
            }
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (active) activeColor else Color.White
            )
        }
    }
}

@Composable
private fun SteeringWheelControl(
    steerValue: Float,
    onSteerChange: (Float) -> Unit
) {
    Box(
        modifier = Modifier
            .size(116.dp)
            .clip(CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val centerX = size.width / 2f
                    fun computeSteer(x: Float): Float {
                        return ((x - centerX) / (size.width * 0.45f)).coerceIn(-1f, 1f)
                    }
                    onSteerChange(computeSteer(down.position.x))
                    while (true) {
                        val event = awaitPointerEvent()
                        val active = event.changes.firstOrNull { it.pressed }
                        if (active != null) {
                            onSteerChange(computeSteer(active.position.x))
                        } else {
                            break
                        }
                    }
                    onSteerChange(0f)
                }
            }
            .testTag("steering_wheel_control")
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
            val r = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(Color(0x990C1218), radius = r, center = center)
            rotate(degrees = steerValue * 115f, pivot = center) {
                // Outer leather rim
                drawCircle(
                    color = Color(0xFF1F2730),
                    radius = r - 6f,
                    center = center,
                    style = Stroke(width = 14f)
                )
                drawCircle(
                    color = BengalEmeraldLight.copy(alpha = 0.7f),
                    radius = r - 6f,
                    center = center,
                    style = Stroke(width = 2.5f)
                )
                // Top center marker band
                drawArc(
                    color = HeadlightAmber,
                    startAngle = -102f,
                    sweepAngle = 24f,
                    useCenter = false,
                    topLeft = Offset(center.x - r + 6f, center.y - r + 6f),
                    size = Size((r - 6f) * 2f, (r - 6f) * 2f),
                    style = Stroke(width = 14f)
                )
                // Three spokes
                drawLine(Color(0xFF394654), Offset(center.x - r + 10f, center.y), Offset(center.x + r - 10f, center.y), strokeWidth = 10f, cap = StrokeCap.Round)
                drawLine(Color(0xFF394654), center, Offset(center.x, center.y + r - 10f), strokeWidth = 10f, cap = StrokeCap.Round)
                // Center horn hub
                drawCircle(Color(0xFF141A21), radius = r * 0.28f, center = center)
                drawCircle(HeadlightAmber, radius = r * 0.28f, center = center, style = Stroke(2f))
            }
        }
    }
}

@Composable
private fun SteerPadButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDesc: String,
    onHold: (Boolean) -> Unit,
    testTag: String
) {
    var pressed by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (pressed) BengalEmerald else Color(0xCC111922),
        border = BorderStroke(1.5.dp, if (pressed) BengalEmeraldLight else Color(0xFF354656)),
        modifier = Modifier
            .size(66.dp)
            .clip(RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    onHold(true)
                    do {
                        val e = awaitPointerEvent()
                    } while (e.changes.any { it.pressed })
                    pressed = false
                    onHold(false)
                }
            }
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDesc, tint = Color.White, modifier = Modifier.size(34.dp))
        }
    }
}

@Composable
private fun PedalButton(
    labelBn: String,
    labelEn: String,
    baseColor: Color,
    heightDp: Int,
    onHoldChange: (Boolean) -> Unit,
    testTag: String
) {
    var pressed by remember { mutableStateOf(false) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(62.dp)
            .height(heightDp.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    colors = if (pressed) {
                        listOf(baseColor, baseColor.copy(alpha = 0.85f))
                    } else {
                        listOf(Color(0xCC1A222C), baseColor.copy(alpha = 0.55f))
                    }
                )
            )
            .border(1.5.dp, baseColor, RoundedCornerShape(14.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    onHoldChange(true)
                    do {
                        val e = awaitPointerEvent()
                    } while (e.changes.any { it.pressed })
                    pressed = false
                    onHoldChange(false)
                }
            }
            .testTag(testTag)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(labelBn, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(labelEn, color = Color.White.copy(alpha = 0.75f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}
