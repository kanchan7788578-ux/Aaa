package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.PlayerPreferences
import com.example.domain.model.BusFleet
import com.example.domain.model.MymensinghMapData
import com.example.domain.model.MymensinghRoute
import com.example.domain.model.WeatherTime
import com.example.ui.components.MymensinghInteractiveMap
import com.example.ui.theme.BengalEmerald
import com.example.ui.theme.BengalEmeraldLight
import com.example.ui.theme.HeadlightAmber

@Composable
fun MymensinghHomeScreen(
    preferences: PlayerPreferences,
    onSelectRoute: (String) -> Unit,
    onSelectWeather: (WeatherTime) -> Unit,
    onStartDrive: () -> Unit,
    onOpenGarage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedRoute = MymensinghMapData.routeById(preferences.selectedRouteId)
    val selectedBus = BusFleet.byId(preferences.selectedBusId)
    var showFullMapDialog by remember { mutableStateOf(false) }

    if (showFullMapDialog) {
        Dialog(
            onDismissRequest = { showFullMapDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF091218))
                    .padding(12.dp)
            ) {
                MymensinghInteractiveMap(
                    selectedRoute = selectedRoute,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = Color(0xCC0C141C),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    IconButton(onClick = { showFullMapDialog = false }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close Full Map",
                            tint = Color.White
                        )
                    }
                }
                Button(
                    onClick = {
                        showFullMapDialog = false
                        onStartDrive()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BengalEmerald),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                        .fillMaxWidth(0.75f)
                        .height(52.dp)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Start Drive on This Route", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 600.dp

        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1.05f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HeroBannerCard(
                        selectedBusName = selectedBus.nameBn,
                        totalTaka = preferences.totalTakaEarned,
                        tripsCompleted = preferences.tripsCompleted,
                        onOpenGarage = onOpenGarage
                    )
                    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        MymensinghInteractiveMap(
                            selectedRoute = selectedRoute,
                            modifier = Modifier.fillMaxSize()
                        )
                        ExpandMapBadge(
                            onClick = { showFullMapDialog = true },
                            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(0.95f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WeatherSelectorRow(
                        selectedWeather = preferences.weatherTime,
                        onSelectWeather = onSelectWeather
                    )
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(MymensinghMapData.routes, key = { it.id }) { route ->
                            RouteOptionCard(
                                route = route,
                                isSelected = route.id == selectedRoute.id,
                                onClick = { onSelectRoute(route.id) }
                            )
                        }
                    }
                    StartDriveButton(onStartDrive = onStartDrive)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    HeroBannerCard(
                        selectedBusName = selectedBus.nameBn,
                        totalTaka = preferences.totalTakaEarned,
                        tripsCompleted = preferences.tripsCompleted,
                        onOpenGarage = onOpenGarage
                    )
                }

                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        MymensinghInteractiveMap(
                            selectedRoute = selectedRoute,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                        )
                        ExpandMapBadge(
                            onClick = { showFullMapDialog = true },
                            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
                        )
                    }
                }

                item {
                    WeatherSelectorRow(
                        selectedWeather = preferences.weatherTime,
                        onSelectWeather = onSelectWeather
                    )
                }

                item {
                    Text(
                        text = "Select Mymensingh Route",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                items(MymensinghMapData.routes, key = { it.id }) { route ->
                    RouteOptionCard(
                        route = route,
                        isSelected = route.id == selectedRoute.id,
                        onClick = { onSelectRoute(route.id) }
                    )
                }

                item {
                    StartDriveButton(onStartDrive = onStartDrive)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ExpandMapBadge(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = Color(0xCC0D161E),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, BengalEmeraldLight.copy(alpha = 0.6f)),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag("expand_full_map_button")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Fullscreen,
                contentDescription = "Full Map",
                tint = BengalEmeraldLight,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Full Map",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    }
}

@Composable
private fun HeroBannerCard(
    selectedBusName: String,
    totalTaka: Int,
    tripsCompleted: Int,
    onOpenGarage: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(145.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_mymensingh_hero_1795435681726),
                contentDescription = "Mymensingh Shambhuganj Bridge Express Bus",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x55060B10),
                                Color(0xCC0A1118),
                                Color(0xF50B1117)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = BengalEmerald.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "BANGLADESH • MYMENSINGH",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    Surface(
                        color = Color(0xBB151E26),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, HeadlightAmber.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "৳$totalTaka  •  $tripsCompleted Trips",
                            style = MaterialTheme.typography.labelSmall,
                            color = HeadlightAmber,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mymensingh Bus Simulator",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Coach: $selectedBusName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BengalEmeraldLight
                        )
                    }
                    Surface(
                        color = Color(0xCC1E2833),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onOpenGarage)
                            .testTag("quick_garage_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DirectionsBus,
                                contentDescription = "Change Bus",
                                tint = HeadlightAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Change Bus",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherSelectorRow(
    selectedWeather: WeatherTime,
    onSelectWeather: (WeatherTime) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        WeatherTime.entries.forEach { weather ->
            val icon = when (weather) {
                WeatherTime.MORNING -> Icons.Rounded.WbSunny
                WeatherTime.SUNSET -> Icons.Rounded.WbTwilight
                WeatherTime.MONSOON_RAIN -> Icons.Rounded.WaterDrop
                WeatherTime.NIGHT -> Icons.Rounded.NightsStay
            }
            val selected = weather == selectedWeather
            FilterChip(
                selected = selected,
                onClick = { onSelectWeather(weather) },
                label = { Text(weather.labelBn) },
                leadingIcon = {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BengalEmerald,
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White
                ),
                modifier = Modifier.testTag("weather_chip_${weather.name.lowercase()}")
            )
        }
    }
}

@Composable
private fun RouteOptionCard(
    route: MymensinghRoute,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF132922) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) BengalEmeraldLight else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("route_card_${route.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Route,
                        contentDescription = null,
                        tint = if (isSelected) BengalEmeraldLight else HeadlightAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = route.titleBn,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    color = if (isSelected) BengalEmerald else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (route.isEndless) "Free" else "${route.distanceKm} km",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Text(
                text = route.subtitleBn,
                style = MaterialTheme.typography.bodyMedium,
                color = BengalEmeraldLight.copy(alpha = 0.9f)
            )
            Text(
                text = route.descriptionBn,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StartDriveButton(onStartDrive: () -> Unit) {
    Button(
        onClick = onStartDrive,
        colors = ButtonDefaults.buttonColors(
            containerColor = BengalEmerald,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("start_drive_button")
    ) {
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Start Drive (Landscape Mode)",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
    }
}
