package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.PlayerPreferences
import com.example.domain.model.CameraMode
import com.example.domain.model.SteeringMode
import com.example.domain.model.WeatherTime
import com.example.ui.theme.BengalEmerald
import com.example.ui.theme.BengalEmeraldLight
import com.example.ui.theme.HeadlightAmber

@Composable
fun SettingsScreen(
    preferences: PlayerPreferences,
    onSetSteeringMode: (SteeringMode) -> Unit,
    onSetCameraMode: (CameraMode) -> Unit,
    onSetWeather: (WeatherTime) -> Unit,
    onSetSoundEnabled: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Driving Settings & Guide",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Customize steering controls, camera view, weather, and audio",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            SettingsSectionCard(
                icon = Icons.Rounded.SportsEsports,
                title = "Steering Control Mode"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SteeringMode.entries.forEach { mode ->
                        FilterChip(
                            selected = preferences.steeringMode == mode,
                            onClick = { onSetSteeringMode(mode) },
                            label = { Text(mode.labelBn) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BengalEmerald,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("setting_steer_${mode.name.lowercase()}")
                        )
                    }
                }
            }
        }

        item {
            SettingsSectionCard(
                icon = Icons.Rounded.Videocam,
                title = "Default Camera View"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CameraMode.entries.forEach { mode ->
                        FilterChip(
                            selected = preferences.cameraMode == mode,
                            onClick = { onSetCameraMode(mode) },
                            label = { Text(mode.labelBn) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BengalEmerald,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("setting_camera_${mode.name.lowercase()}")
                        )
                    }
                }
            }
        }

        item {
            SettingsSectionCard(
                icon = Icons.Rounded.DirectionsBus,
                title = "Weather & Time of Day"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WeatherTime.entries.take(2).forEach { w ->
                        FilterChip(
                            selected = preferences.weatherTime == w,
                            onClick = { onSetWeather(w) },
                            label = { Text(w.labelBn) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BengalEmerald,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WeatherTime.entries.drop(2).forEach { w ->
                        FilterChip(
                            selected = preferences.weatherTime == w,
                            onClick = { onSetWeather(w) },
                            label = { Text(w.labelBn) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BengalEmerald,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Rounded.VolumeUp, contentDescription = null, tint = HeadlightAmber)
                        Column {
                            Text("Bus Horn & Sound Effects", style = MaterialTheme.typography.titleMedium)
                            Text("Dual-tone air horn, brake hiss & door chime", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = preferences.soundEnabled,
                        onCheckedChange = onSetSoundEnabled,
                        colors = SwitchDefaults.colors(checkedTrackColor = BengalEmerald),
                        modifier = Modifier.testTag("sound_toggle_switch")
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF13241D))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Mymensingh Road Rules & Tips",
                        style = MaterialTheme.typography.titleMedium,
                        color = BengalEmeraldLight
                    )
                    Text("• Bangladesh uses Left-Hand Traffic: drive in the left lanes while oncoming vehicles approach on the right.", style = MaterialTheme.typography.bodyMedium)
                    Text("• Stop smoothly inside green Bus Stop bays at Charpara, Town Hall & Shambhuganj, then tap 'Door' to board passengers.", style = MaterialTheme.typography.bodyMedium)
                    Text("• Hold 'Horn' behind slower CNGs or Rickshaws so they yield toward the shoulder.", style = MaterialTheme.typography.bodyMedium)
                    Text("• Toggle 'Lights' at Night/Sunset and 'Wipers' during Monsoon Rain for clear visibility.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(icon, contentDescription = null, tint = BengalEmeraldLight)
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}
