package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.BusFleet
import com.example.domain.model.CameraMode
import com.example.domain.model.MymensinghMapData
import com.example.domain.model.SteeringMode
import com.example.domain.model.WeatherTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mymensingh_bus_prefs")

data class PlayerPreferences(
    val selectedBusId: String = BusFleet.buses.first().id,
    val selectedRouteId: String = MymensinghMapData.routes.first().id,
    val weatherTime: WeatherTime = WeatherTime.MORNING,
    val cameraMode: CameraMode = CameraMode.CHASE,
    val steeringMode: SteeringMode = SteeringMode.WHEEL,
    val soundEnabled: Boolean = true,
    val totalTakaEarned: Int = 0,
    val tripsCompleted: Int = 0
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val SELECTED_BUS = stringPreferencesKey("selected_bus_id")
        val SELECTED_ROUTE = stringPreferencesKey("selected_route_id")
        val WEATHER_TIME = stringPreferencesKey("weather_time")
        val CAMERA_MODE = stringPreferencesKey("camera_mode")
        val STEERING_MODE = stringPreferencesKey("steering_mode")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val TOTAL_TAKA = intPreferencesKey("total_taka")
        val TRIPS_COMPLETED = intPreferencesKey("trips_completed")
    }

    val preferencesFlow: Flow<PlayerPreferences> = context.dataStore.data.map { prefs ->
        PlayerPreferences(
            selectedBusId = prefs[Keys.SELECTED_BUS] ?: BusFleet.buses.first().id,
            selectedRouteId = prefs[Keys.SELECTED_ROUTE] ?: MymensinghMapData.routes.first().id,
            weatherTime = prefs[Keys.WEATHER_TIME]?.let { runCatching { WeatherTime.valueOf(it) }.getOrNull() }
                ?: WeatherTime.MORNING,
            cameraMode = prefs[Keys.CAMERA_MODE]?.let { runCatching { CameraMode.valueOf(it) }.getOrNull() }
                ?: CameraMode.CHASE,
            steeringMode = prefs[Keys.STEERING_MODE]?.let { runCatching { SteeringMode.valueOf(it) }.getOrNull() }
                ?: SteeringMode.WHEEL,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            totalTakaEarned = prefs[Keys.TOTAL_TAKA] ?: 0,
            tripsCompleted = prefs[Keys.TRIPS_COMPLETED] ?: 0
        )
    }

    suspend fun setSelectedBus(busId: String) {
        context.dataStore.edit { it[Keys.SELECTED_BUS] = busId }
    }

    suspend fun setSelectedRoute(routeId: String) {
        context.dataStore.edit { it[Keys.SELECTED_ROUTE] = routeId }
    }

    suspend fun setWeatherTime(weather: WeatherTime) {
        context.dataStore.edit { it[Keys.WEATHER_TIME] = weather.name }
    }

    suspend fun setCameraMode(mode: CameraMode) {
        context.dataStore.edit { it[Keys.CAMERA_MODE] = mode.name }
    }

    suspend fun setSteeringMode(mode: SteeringMode) {
        context.dataStore.edit { it[Keys.STEERING_MODE] = mode.name }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun recordTripReward(takaEarned: Int) {
        context.dataStore.edit { prefs ->
            val currentTaka = prefs[Keys.TOTAL_TAKA] ?: 0
            val currentTrips = prefs[Keys.TRIPS_COMPLETED] ?: 0
            prefs[Keys.TOTAL_TAKA] = currentTaka + takaEarned
            prefs[Keys.TRIPS_COMPLETED] = currentTrips + 1
        }
    }
}
