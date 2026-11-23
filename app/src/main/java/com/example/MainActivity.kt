package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.model.WeatherTime
import com.example.ui.screens.DrivingScreen
import com.example.ui.screens.GarageScreen
import com.example.ui.screens.MymensinghHomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.BengalEmerald
import com.example.ui.theme.BengalEmeraldLight
import com.example.ui.theme.MymensinghBusTheme
import com.example.viewmodel.AppDestination
import com.example.viewmodel.BusGameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MymensinghBusTheme(darkTheme = true) {
                MymensinghBusApp()
            }
        }
    }
}

@Composable
fun MymensinghBusApp(viewModel: BusGameViewModel = viewModel()) {
    val destination by viewModel.destination.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val drivingState by viewModel.drivingState.collectAsStateWithLifecycle()

    if (destination == AppDestination.DRIVING) {
        DrivingScreen(
            state = drivingState,
            preferences = preferences,
            onStepFrame = viewModel::stepSimulation,
            onAccelerateChange = { viewModel.updateControls(accelerate = it) },
            onBrakeChange = { viewModel.updateControls(brake = it) },
            onSteerChange = { viewModel.updateControls(steerTarget = it) },
            onHornChange = { viewModel.updateControls(hornPressed = it) },
            onToggleDoor = viewModel::toggleDoor,
            onToggleHeadlights = viewModel::toggleHeadlights,
            onToggleWipers = viewModel::toggleWipers,
            onToggleLeftSignal = viewModel::toggleLeftSignal,
            onToggleRightSignal = viewModel::toggleRightSignal,
            onToggleCamera = viewModel::toggleCameraMode,
            onCycleWeather = {
                val all = WeatherTime.entries
                val next = all[(preferences.weatherTime.ordinal + 1) % all.size]
                viewModel.setWeather(next)
            },
            onToggleSound = { viewModel.setSoundEnabled(!preferences.soundEnabled) },
            onTogglePause = viewModel::togglePause,
            onRestartTrip = { viewModel.startTrip() },
            onExitToMap = { viewModel.navigateTo(AppDestination.MAP_HOME) }
        )
        return
    }

    if (destination == AppDestination.GARAGE || destination == AppDestination.SETTINGS) {
        BackHandler { viewModel.navigateTo(AppDestination.MAP_HOME) }
    }

    val navItems = listOf(
        Triple(AppDestination.MAP_HOME, "Map & Routes", Pair(Icons.Rounded.Map, Icons.Outlined.Map)),
        Triple(AppDestination.GARAGE, "Bus Garage", Pair(Icons.Rounded.DirectionsBus, Icons.Outlined.DirectionsBus)),
        Triple(AppDestination.SETTINGS, "Settings", Pair(Icons.Rounded.Settings, Icons.Outlined.Settings))
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (!useRail) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        navItems.forEach { (dest, label, icons) ->
                            val selected = destination == dest
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.navigateTo(dest) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) icons.first else icons.second,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = BengalEmeraldLight,
                                    selectedTextColor = BengalEmeraldLight,
                                    indicatorColor = BengalEmerald.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier.testTag("nav_${dest.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                        navItems.forEach { (dest, label, icons) ->
                            val selected = destination == dest
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.navigateTo(dest) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) icons.first else icons.second,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = BengalEmeraldLight,
                                    selectedTextColor = BengalEmeraldLight,
                                    indicatorColor = BengalEmerald.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier.testTag("rail_${dest.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (destination) {
                        AppDestination.MAP_HOME -> MymensinghHomeScreen(
                            preferences = preferences,
                            onSelectRoute = viewModel::selectRoute,
                            onSelectWeather = viewModel::setWeather,
                            onStartDrive = { viewModel.startTrip() },
                            onOpenGarage = { viewModel.navigateTo(AppDestination.GARAGE) }
                        )
                        AppDestination.GARAGE -> GarageScreen(
                            selectedBusId = preferences.selectedBusId,
                            onSelectBus = viewModel::selectBus,
                            onStartDrive = { viewModel.startTrip() }
                        )
                        AppDestination.SETTINGS -> SettingsScreen(
                            preferences = preferences,
                            onSetSteeringMode = viewModel::setSteeringMode,
                            onSetCameraMode = viewModel::setCameraMode,
                            onSetWeather = viewModel::setWeather,
                            onSetSoundEnabled = viewModel::setSoundEnabled
                        )
                        AppDestination.DRIVING -> Unit
                    }
                }
            }
        }
    }
}
