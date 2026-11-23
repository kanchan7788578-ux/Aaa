package com.example.domain.model

import androidx.compose.ui.graphics.Color

data class BusModel(
    val id: String,
    val nameBn: String,
    val nameEn: String,
    val operatorTag: String,
    val routeBoardBn: String,
    val licensePlate: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color,
    val maxSpeedKmh: Float,
    val accelPower: Float,
    val brakingPower: Float,
    val handling: Float,
    val capacity: Int,
    val descriptionBn: String
)

object BusFleet {
    val buses = listOf(
        BusModel(
            id = "ena_express",
            nameBn = "Ena Transport (AC)",
            nameEn = "Hino / Hyundai Express Coach",
            operatorTag = "ENA EXPRESS",
            routeBoardBn = "DHAKA <-> MYMENSINGH",
            licensePlate = "DHAKA METRO-B 11-4582",
            primaryColor = Color(0xFFD32F2F),
            secondaryColor = Color(0xFFF5F7FA),
            accentColor = Color(0xFFFFC107),
            maxSpeedKmh = 122f,
            accelPower = 1.15f,
            brakingPower = 1.20f,
            handling = 1.10f,
            capacity = 42,
            descriptionBn = "High-speed AC coach on the Mymensingh-Dhaka corridor with crisp air braking and highway stability."
        ),
        BusModel(
            id = "shoukhin_super",
            nameBn = "Shoukhin Paribahan",
            nameEn = "Super Express Coach",
            operatorTag = "SHOUKHIN SUPER",
            routeBoardBn = "MYMENSINGH <-> MOHAKHALI",
            licensePlate = "MYMENSINGH-B 04-1290",
            primaryColor = Color(0xFF0E7C4E),
            secondaryColor = Color(0xFFFFFDF7),
            accentColor = Color(0xFFFFB300),
            maxSpeedKmh = 112f,
            accelPower = 1.05f,
            brakingPower = 1.10f,
            handling = 1.22f,
            capacity = 48,
            descriptionBn = "Balanced suspension and responsive steering built for tight city intersections and bridges."
        ),
        BusModel(
            id = "alam_asia",
            nameBn = "Alam Asia Coach",
            nameEn = "Luxury Highway Express",
            operatorTag = "ALAM ASIA",
            routeBoardBn = "MYMENSINGH <-> SHAMBHUGANJ",
            licensePlate = "DHAKA METRO-B 15-7731",
            primaryColor = Color(0xFF1565C0),
            secondaryColor = Color(0xFFECEFF1),
            accentColor = Color(0xFFFF6D00),
            maxSpeedKmh = 118f,
            accelPower = 1.12f,
            brakingPower = 1.08f,
            handling = 1.14f,
            capacity = 45,
            descriptionBn = "Powerful diesel torque ideal for climbing Shambhuganj Bridge and highway overtaking."
        ),
        BusModel(
            id = "brtc_city",
            nameBn = "BRTC City Express",
            nameEn = "Bangladesh State Shuttle",
            operatorTag = "BRTC BANGLADESH",
            routeBoardBn = "TOWN HALL <-> BAU CAMPUS",
            licensePlate = "DHAKA METRO-B 12-0084",
            primaryColor = Color(0xFFB71C1C),
            secondaryColor = Color(0xFFFFCA28),
            accentColor = Color(0xFF1B5E20),
            maxSpeedKmh = 104f,
            accelPower = 0.96f,
            brakingPower = 1.25f,
            handling = 1.18f,
            capacity = 56,
            descriptionBn = "Maximum passenger capacity with heavy-duty pneumatic brakes for busy city stops."
        )
    )

    fun byId(id: String): BusModel = buses.find { it.id == id } ?: buses.first()
}

enum class WeatherTime(val labelBn: String, val labelEn: String) {
    MORNING("Morning", "Morning"),
    SUNSET("Sunset", "Sunset"),
    MONSOON_RAIN("Monsoon Rain", "Monsoon"),
    NIGHT("Night", "Night")
}

enum class CameraMode(val labelBn: String, val labelEn: String) {
    CHASE("Chase Cam", "Chase Cam"),
    COCKPIT("Cockpit View", "Cockpit")
}

enum class SteeringMode(val labelBn: String, val labelEn: String) {
    WHEEL("Steering Wheel", "Wheel"),
    BUTTONS("Steer Buttons", "Buttons")
}

enum class TrafficType(
    val widthUnits: Float,
    val baseSpeedKmh: Float,
    val labelBn: String
) {
    CNG_AUTO(0.46f, 44f, "CNG Auto"),
    EASY_BIKE(0.50f, 34f, "Easy-Bike"),
    RICKSHAW(0.36f, 18f, "Rickshaw"),
    TRUCK_BD(0.84f, 52f, "Cargo Truck"),
    MICROBUS(0.66f, 68f, "Microbus"),
    SEDAN(0.62f, 74f, "Sedan"),
    ONCOMING_BUS(0.88f, 64f, "Bus")
}

data class TrafficVehicle(
    val id: Int,
    val type: TrafficType,
    val laneX: Float, // -1.0 (far left) to +1.0 (far right)
    val zPos: Float,
    val speedKmh: Float,
    val colorVariant: Int,
    val isOncoming: Boolean,
    val isBraking: Boolean = false
)
