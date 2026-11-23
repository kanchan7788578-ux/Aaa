package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.BusAudioEngine
import com.example.data.PlayerPreferences
import com.example.data.SettingsRepository
import com.example.domain.model.BusFleet
import com.example.domain.model.BusModel
import com.example.domain.model.CameraMode
import com.example.domain.model.MymensinghMapData
import com.example.domain.model.MymensinghRoute
import com.example.domain.model.RoadZoneType
import com.example.domain.model.RouteStop
import com.example.domain.model.SteeringMode
import com.example.domain.model.TrafficType
import com.example.domain.model.TrafficVehicle
import com.example.domain.model.WeatherTime
import com.example.engine.RoadBuilder
import com.example.engine.RoadSegment
import com.example.engine.SEGMENT_LENGTH
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

enum class AppDestination {
    MAP_HOME,
    GARAGE,
    SETTINGS,
    DRIVING
}

data class DrivingControlInput(
    val accelerate: Boolean = false,
    val brake: Boolean = false,
    val steerTarget: Float = 0f, // -1f (left) to +1f (right)
    val hornPressed: Boolean = false
)

data class DrivingUiState(
    val route: MymensinghRoute = MymensinghMapData.routes.first(),
    val bus: BusModel = BusFleet.buses.first(),
    val segments: List<RoadSegment> = emptyList(),
    val traffic: List<TrafficVehicle> = emptyList(),
    val playerZ: Float = 0f,
    val playerX: Float = -0.52f, // Start in Left lane (Bangladesh LHT)
    val speedKmh: Float = 0f,
    val steerSmooth: Float = 0f,
    val suspensionBounce: Float = 0f,
    val headlightsOn: Boolean = false,
    val wipersOn: Boolean = false,
    val leftSignalOn: Boolean = false,
    val rightSignalOn: Boolean = false,
    val doorOpen: Boolean = false,
    val isBraking: Boolean = false,
    val passengers: Int = 8,
    val fareEarnedTaka: Int = 0,
    val comfortPercent: Int = 100,
    val currentZoneTitleBn: String = "",
    val speedLimitKmh: Int = 60,
    val nextStop: RouteStop? = null,
    val distanceToNextStopMeters: Int = 0,
    val isInStopZone: Boolean = false,
    val servedStopIds: Set<String> = emptySet(),
    val boardingProgress: Float = 0f,
    val statusBannerBn: String? = null,
    val progressFraction: Float = 0f,
    val isPaused: Boolean = false,
    val isTripCompleted: Boolean = false,
    val earnedStars: Int = 3,
    val timeSec: Float = 0f
)

class BusGameViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)
    val audioEngine = BusAudioEngine()

    val preferences: StateFlow<PlayerPreferences> = repository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayerPreferences())

    private val _destination = MutableStateFlow(AppDestination.MAP_HOME)
    val destination: StateFlow<AppDestination> = _destination.asStateFlow()

    private val _drivingState = MutableStateFlow(DrivingUiState())
    val drivingState: StateFlow<DrivingUiState> = _drivingState.asStateFlow()

    private var controlInput = DrivingControlInput()
    private var lastCollisionTime = -10f
    private var wasBrakingSound = false
    private var rewardSavedForCurrentTrip = false

    init {
        viewModelScope.launch {
            preferences.collect { prefs ->
                audioEngine.isEnabled = prefs.soundEnabled
            }
        }
    }

    fun navigateTo(dest: AppDestination) {
        if (dest != AppDestination.DRIVING) {
            audioEngine.triggerHorn(false)
        }
        _destination.value = dest
    }

    fun selectBus(busId: String) {
        viewModelScope.launch { repository.setSelectedBus(busId) }
    }

    fun selectRoute(routeId: String) {
        viewModelScope.launch { repository.setSelectedRoute(routeId) }
    }

    fun setWeather(weather: WeatherTime) {
        viewModelScope.launch { repository.setWeatherTime(weather) }
    }

    fun toggleCameraMode() {
        val next = if (preferences.value.cameraMode == CameraMode.CHASE) CameraMode.COCKPIT else CameraMode.CHASE
        viewModelScope.launch { repository.setCameraMode(next) }
    }

    fun setCameraMode(mode: CameraMode) {
        viewModelScope.launch { repository.setCameraMode(mode) }
    }

    fun setSteeringMode(mode: SteeringMode) {
        viewModelScope.launch { repository.setSteeringMode(mode) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setSoundEnabled(enabled) }
    }

    fun startTrip(routeId: String = preferences.value.selectedRouteId, busId: String = preferences.value.selectedBusId) {
        val route = MymensinghMapData.routeById(routeId)
        val bus = BusFleet.byId(busId)
        val weather = preferences.value.weatherTime
        val track = RoadBuilder.buildTrack(route)
        val initialTraffic = spawnInitialTraffic(track)

        rewardSavedForCurrentTrip = false
        controlInput = DrivingControlInput()
        lastCollisionTime = -10f

        _drivingState.value = DrivingUiState(
            route = route,
            bus = bus,
            segments = track,
            traffic = initialTraffic,
            playerZ = 0f,
            playerX = -0.52f,
            speedKmh = 0f,
            headlightsOn = weather == WeatherTime.NIGHT || weather == WeatherTime.SUNSET,
            wipersOn = weather == WeatherTime.MONSOON_RAIN,
            passengers = 10,
            currentZoneTitleBn = route.zones.firstOrNull()?.signboardBn ?: route.titleBn,
            speedLimitKmh = route.zones.firstOrNull()?.speedLimitKmh ?: 60,
            nextStop = route.stops.firstOrNull(),
            statusBannerBn = "Trip Started: ${route.titleBn}"
        )
        _destination.value = AppDestination.DRIVING
    }

    fun updateControls(
        accelerate: Boolean? = null,
        brake: Boolean? = null,
        steerTarget: Float? = null,
        hornPressed: Boolean? = null
    ) {
        controlInput = controlInput.copy(
            accelerate = accelerate ?: controlInput.accelerate,
            brake = brake ?: controlInput.brake,
            steerTarget = steerTarget ?: controlInput.steerTarget,
            hornPressed = hornPressed ?: controlInput.hornPressed
        )
        if (hornPressed != null) {
            audioEngine.triggerHorn(hornPressed && !_drivingState.value.isPaused)
        }
    }

    fun toggleHeadlights() {
        _drivingState.update { it.copy(headlightsOn = !it.headlightsOn) }
    }

    fun toggleWipers() {
        _drivingState.update { it.copy(wipersOn = !it.wipersOn) }
    }

    fun toggleLeftSignal() {
        _drivingState.update { it.copy(leftSignalOn = !it.leftSignalOn, rightSignalOn = false) }
    }

    fun toggleRightSignal() {
        _drivingState.update { it.copy(rightSignalOn = !it.rightSignalOn, leftSignalOn = false) }
    }

    fun toggleDoor() {
        val state = _drivingState.value
        if (!state.doorOpen && state.speedKmh > 14f) {
            _drivingState.update { it.copy(statusBannerBn = "Stop the bus before opening doors!") }
            return
        }
        audioEngine.playDoorChime()
        _drivingState.update { it.copy(doorOpen = !it.doorOpen) }
    }

    fun togglePause() {
        _drivingState.update {
            val nextPause = !it.isPaused
            if (nextPause) audioEngine.triggerHorn(false)
            it.copy(isPaused = nextPause)
        }
    }

    fun stepSimulation(dtSec: Float) {
        val state = _drivingState.value
        if (state.isPaused || state.isTripCompleted || state.segments.isEmpty()) return

        val dt = dtSec.coerceIn(0.001f, 0.05f)
        val weather = preferences.value.weatherTime
        val totalLen = state.segments.size * SEGMENT_LENGTH
        val bus = state.bus

        // 1. Speed & Acceleration Physics
        var speed = state.speedKmh
        val canDrive = !state.doorOpen
        val gripFactor = if (weather == WeatherTime.MONSOON_RAIN) 0.85f else 1.0f

        if (controlInput.accelerate && canDrive) {
            val accelRate = 28f * bus.accelPower * (1f - (speed / (bus.maxSpeedKmh * 1.15f)).coerceIn(0f, 0.82f))
            speed += accelRate * dt
        } else {
            // Engine drag & rolling resistance
            speed -= (7.5f + speed * 0.08f) * dt
        }

        if (controlInput.brake) {
            val brakeRate = 58f * bus.brakingPower * gripFactor
            speed -= brakeRate * dt
            if (!wasBrakingSound && speed > 28f) {
                audioEngine.playBrakeHiss()
                wasBrakingSound = true
            }
        } else {
            wasBrakingSound = false
        }

        // Off-road shoulder slows the bus and adds rumble
        val offRoad = abs(state.playerX) > 0.96f
        if (offRoad && speed > 36f) {
            speed -= 34f * dt
        }

        speed = speed.coerceIn(0f, bus.maxSpeedKmh)

        // 2. Steering & Centrifugal Curve Physics
        val steerDiff = controlInput.steerTarget - state.steerSmooth
        val steerSmooth = state.steerSmooth + steerDiff * (10f * dt).coerceAtMost(1f)
        val segIndex = ((state.playerZ / SEGMENT_LENGTH).toInt()).coerceIn(0, state.segments.lastIndex)
        val currentSeg = state.segments[segIndex]

        val speedFactor = (speed / 75f).coerceIn(0.15f, 1.35f)
        var nextX = state.playerX + steerSmooth * bus.handling * 1.15f * speedFactor * dt
        // Gentle centrifugal drift on road curves
        nextX -= currentSeg.curve * (speed / 120f) * 0.16f * dt
        nextX = nextX.coerceIn(-1.32f, 1.32f)

        // 3. Forward Displacement (convert km/h into world units/sec)
        val worldSpeed = speed * 22f // tuned for realistic 60fps road feel
        var nextZ = state.playerZ + worldSpeed * dt
        if (state.route.isEndless && nextZ >= totalLen) {
            nextZ -= totalLen
        } else if (!state.route.isEndless) {
            nextZ = nextZ.coerceAtMost(totalLen - SEGMENT_LENGTH * 0.5f)
        }

        // 4. Suspension Bounce (road vibration + speed breakers + offroad)
        val newTime = state.timeSec + dt
        val rumbleAmp = when {
            currentSeg.hasSpeedBreaker && speed > 24f -> 9f
            offRoad && speed > 10f -> 6f
            else -> (speed / 100f) * 2.2f
        }
        val bounce = sin(newTime * 28f) * rumbleAmp

        // 5. Update Traffic & Check Collisions
        var comfort = state.comfortPercent
        var banner: String? = null
        val updatedTraffic = state.traffic.map { v ->
            var vz = v.zPos
            val vWorldSpeed = v.speedKmh * 22f
            vz += if (v.isOncoming) -vWorldSpeed * dt else vWorldSpeed * dt

            // Wrap traffic around player window so road always feels alive
            val relZ = vz - nextZ
            if (relZ < -500f) {
                vz = nextZ + 4800f + Random.nextFloat() * 3500f
            } else if (relZ > 11000f) {
                vz = nextZ - 200f
            }
            if (state.route.isEndless) {
                vz = ((vz % totalLen) + totalLen) % totalLen
            }

            // Horn yields slow left-lane traffic slightly toward the curb!
            var vx = v.laneX
            if (controlInput.hornPressed && !v.isOncoming && relZ in 150f..2200f && abs(vx - nextX) < 0.45f) {
                vx = (vx - 0.35f * dt).coerceAtLeast(-0.84f)
            }

            // Collision check with player bus
            val dz = abs(vz - nextZ)
            val dx = abs(vx - nextX)
            val hitWidth = 0.28f + v.type.widthUnits * 0.35f
            if (dz < 130f && dx < hitWidth && speed > 8f && (newTime - lastCollisionTime) > 1.1f) {
                lastCollisionTime = newTime
                speed = (speed * 0.38f).coerceAtLeast(4f)
                comfort = (comfort - 12).coerceAtLeast(15)
                banner = "Caution! Collided with ${v.type.labelBn}!"
                audioEngine.playBump()
            }

            v.copy(zPos = vz, laneX = vx, isBraking = relZ in 180f..700f && !v.isOncoming && speed > v.speedKmh + 15f)
        }

        // 6. Bus Stop & Boarding Logic
        val nextSegIdx = (nextZ / SEGMENT_LENGTH).toInt().coerceIn(0, state.segments.lastIndex)
        val activeStop = state.route.stops.firstOrNull { stop ->
            abs(nextSegIdx - stop.segmentIndex) <= 14 && stop.landmark.id !in state.servedStopIds
        }
        val upcomingStop = state.route.stops.firstOrNull { stop ->
            stop.landmark.id !in state.servedStopIds && stop.segmentIndex >= nextSegIdx - 14
        } ?: state.route.stops.lastOrNull()

        val distMeters = if (upcomingStop != null) {
            ((upcomingStop.segmentIndex - nextSegIdx).coerceAtLeast(0) * 5)
        } else 0

        var passengers = state.passengers
        var fareTaka = state.fareEarnedTaka
        var served = state.servedStopIds
        var boardingProg = state.boardingProgress
        var doorOpen = state.doorOpen
        var tripDone = false

        if (activeStop != null) {
            if (speed < 10f && doorOpen) {
                boardingProg += dt * 0.65f
                if (boardingProg >= 1f) {
                    val gained = activeStop.passengersWaiting.coerceAtMost(bus.capacity - passengers).coerceAtLeast(4)
                    val earned = gained * activeStop.farePerPassenger + 80
                    passengers = (passengers + gained).coerceAtMost(bus.capacity)
                    fareTaka += earned
                    served = served + activeStop.landmark.id
                    boardingProg = 0f
                    doorOpen = false
                    audioEngine.playDoorChime()
                    banner = "${activeStop.landmark.nameBn}: +৳$earned Fare Collected!"

                    // Final terminal reached?
                    if (!state.route.isEndless && activeStop.landmark.id == state.route.stops.lastOrNull()?.landmark?.id) {
                        tripDone = true
                    }
                } else {
                    banner = "Passengers boarding... (${(boardingProg * 100).toInt()}%)"
                }
            } else if (speed >= 10f) {
                banner = "${activeStop.landmark.nameBn} Bus Stop — Slow down & open Door"
            } else {
                banner = "Stopped at bay — Tap 'Door' to board passengers"
            }
        } else {
            boardingProg = 0f
            if (!state.route.isEndless && nextSegIdx >= state.segments.lastIndex - 20) {
                tripDone = true
            }
        }

        // Speed limit warning
        if (banner == null && speed > currentSeg.speedLimitKmh + 14) {
            banner = "Speed Limit ${currentSeg.speedLimitKmh} KM/H — Slow Down!"
        } else if (banner == null && currentSeg.zoneType == RoadZoneType.BRIDGE_BRAHMAPUTRA) {
            banner = "Crossing Shambhuganj Bridge • Brahmaputra River"
        }

        val progress = if (state.route.isEndless) {
            (nextZ / totalLen).coerceIn(0f, 1f)
        } else {
            (nextZ / (totalLen - SEGMENT_LENGTH)).coerceIn(0f, 1f)
        }

        val stars = when {
            comfort >= 85 -> 3
            comfort >= 60 -> 2
            else -> 1
        }

        if (tripDone && !rewardSavedForCurrentTrip) {
            rewardSavedForCurrentTrip = true
            val finalBonus = fareTaka + stars * 150
            fareTaka = finalBonus
            viewModelScope.launch { repository.recordTripReward(finalBonus) }
        }

        _drivingState.update {
            it.copy(
                traffic = updatedTraffic,
                playerZ = nextZ,
                playerX = nextX,
                speedKmh = speed,
                steerSmooth = steerSmooth,
                suspensionBounce = bounce,
                isBraking = controlInput.brake,
                doorOpen = doorOpen,
                passengers = passengers,
                fareEarnedTaka = fareTaka,
                comfortPercent = comfort,
                currentZoneTitleBn = currentSeg.zoneType.labelBn,
                speedLimitKmh = currentSeg.speedLimitKmh,
                nextStop = upcomingStop,
                distanceToNextStopMeters = distMeters,
                isInStopZone = activeStop != null,
                servedStopIds = served,
                boardingProgress = boardingProg,
                statusBannerBn = banner,
                progressFraction = progress,
                isTripCompleted = tripDone,
                earnedStars = stars,
                timeSec = newTime
            )
        }
    }

    private fun spawnInitialTraffic(segments: List<RoadSegment>): List<TrafficVehicle> {
        val list = ArrayList<TrafficVehicle>()
        val totalLen = segments.size * SEGMENT_LENGTH
        val leftTypes = listOf(
            TrafficType.CNG_AUTO,
            TrafficType.EASY_BIKE,
            TrafficType.RICKSHAW,
            TrafficType.TRUCK_BD,
            TrafficType.MICROBUS
        )
        val rightTypes = listOf(
            TrafficType.ONCOMING_BUS,
            TrafficType.TRUCK_BD,
            TrafficType.MICROBUS,
            TrafficType.SEDAN,
            TrafficType.CNG_AUTO
        )

        var id = 1
        // 14 left-lane (same direction) Bangladeshi vehicles
        for (i in 0 until 14) {
            val type = leftTypes[i % leftTypes.size]
            val lane = if (type == TrafficType.RICKSHAW || type == TrafficType.EASY_BIKE) {
                -0.78f + (i % 2) * 0.14f
            } else {
                -0.46f + ((i % 3) - 1) * 0.16f
            }
            list.add(
                TrafficVehicle(
                    id = id++,
                    type = type,
                    laneX = lane,
                    zPos = (800f + i * 750f).coerceAtMost(totalLen - 400f),
                    speedKmh = type.baseSpeedKmh + (i % 5) * 3f,
                    colorVariant = i,
                    isOncoming = false
                )
            )
        }

        // 10 right-lane (oncoming) vehicles
        for (i in 0 until 10) {
            val type = rightTypes[i % rightTypes.size]
            val lane = 0.42f + (i % 3) * 0.18f
            list.add(
                TrafficVehicle(
                    id = id++,
                    type = type,
                    laneX = lane,
                    zPos = (1400f + i * 980f).coerceAtMost(totalLen - 400f),
                    speedKmh = type.baseSpeedKmh,
                    colorVariant = i + 2,
                    isOncoming = true
                )
            )
        }
        return list
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }
}
