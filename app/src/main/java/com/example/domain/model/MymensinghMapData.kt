package com.example.domain.model

enum class RoadZoneType(val labelBn: String) {
    CITY_BAZAAR("City & Bazaar"),
    BRIDGE_BRAHMAPUTRA("Shambhuganj Bridge • Brahmaputra"),
    UNIVERSITY_AVENUE("BAU Green Avenue"),
    HIGHWAY_FOUR_LANE("Dhaka-Mymensingh 4-Lane N3")
}

data class MymensinghLandmark(
    val id: String,
    val nameBn: String,
    val nameEn: String,
    val mapX: Float, // Normalized 0f..1f on Mymensingh district map
    val mapY: Float, // Normalized 0f..1f on Mymensingh district map
    val isBusStop: Boolean = true
)

data class RouteStop(
    val landmark: MymensinghLandmark,
    val segmentIndex: Int,
    val passengersWaiting: Int,
    val farePerPassenger: Int
)

data class RoadZoneSpan(
    val startSegment: Int,
    val endSegment: Int,
    val zoneType: RoadZoneType,
    val speedLimitKmh: Int,
    val signboardBn: String
)

data class MymensinghRoute(
    val id: String,
    val titleBn: String,
    val titleEn: String,
    val subtitleBn: String,
    val distanceKm: Float,
    val totalSegments: Int,
    val difficultyBn: String,
    val isEndless: Boolean = false,
    val zones: List<RoadZoneSpan>,
    val stops: List<RouteStop>,
    val pathPoints: List<Pair<Float, Float>>,
    val descriptionBn: String
)

object MymensinghMapData {
    // Normalized coordinates matching real Mymensingh City & South Highway layout
    val townHall = MymensinghLandmark("town_hall", "Town Hall More", "Town Hall More", 0.30f, 0.28f)
    val kachari = MymensinghLandmark("kachari", "Circuit House", "Circuit House", 0.36f, 0.19f, false)
    val ganginarPar = MymensinghLandmark("ganginar_par", "Ganginar Par", "Ganginar Par", 0.44f, 0.33f)
    val charpara = MymensinghLandmark("charpara", "Charpara More (MMCH)", "Charpara More", 0.48f, 0.48f)
    val patgudam = MymensinghLandmark("patgudam", "Patgudam Bridge More", "Patgudam Bridge More", 0.62f, 0.42f)
    val shambhuganj = MymensinghLandmark("shambhuganj", "Shambhuganj Bridge", "Shambhuganj Bridge", 0.79f, 0.33f)
    val kewatkhali = MymensinghLandmark("kewatkhali", "Kewatkhali", "Kewatkhali", 0.62f, 0.60f)
    val bauCampus = MymensinghLandmark("bau", "BAU Campus", "BAU Campus", 0.77f, 0.73f)
    val maskanda = MymensinghLandmark("maskanda", "Maskanda Terminal", "Maskanda Terminal", 0.43f, 0.66f)
    val bypassMore = MymensinghLandmark("bypass", "Bypass More", "Bypass More", 0.35f, 0.78f)
    val trishal = MymensinghLandmark("trishal", "Trishal (N3 Highway)", "Trishal N3", 0.39f, 0.93f)

    val allLandmarks = listOf(
        kachari, townHall, ganginarPar, charpara, patgudam,
        shambhuganj, kewatkhali, bauCampus, maskanda, bypassMore, trishal
    )

    // Winding Brahmaputra River polyline across Mymensingh (NW -> SE)
    val brahmaputraRiverPath = listOf(
        0.10f to 0.08f,
        0.26f to 0.12f,
        0.44f to 0.16f,
        0.58f to 0.25f,
        0.69f to 0.37f,
        0.76f to 0.50f,
        0.84f to 0.66f,
        0.94f to 0.86f
    )

    val routes = listOf(
        MymensinghRoute(
            id = "route_shambhuganj_bridge",
            titleBn = "Town Hall <-> Shambhuganj Express",
            titleEn = "City & Shambhuganj Bridge",
            subtitleBn = "Maskanda -> Charpara -> Town Hall -> Shambhuganj Bridge",
            distanceKm = 5.4f,
            totalSegments = 1200,
            difficultyBn = "Medium Traffic",
            zones = listOf(
                RoadZoneSpan(0, 320, RoadZoneType.CITY_BAZAAR, 55, "MASKANDA TERMINAL"),
                RoadZoneSpan(320, 620, RoadZoneType.CITY_BAZAAR, 45, "CHARPARA • HOSPITAL ZONE"),
                RoadZoneSpan(620, 860, RoadZoneType.CITY_BAZAAR, 50, "PATGUDAM BRIDGE MORE"),
                RoadZoneSpan(860, 1200, RoadZoneType.BRIDGE_BRAHMAPUTRA, 70, "SHAMBHUGANJ BRIDGE")
            ),
            stops = listOf(
                RouteStop(maskanda, 40, 12, 20),
                RouteStop(charpara, 360, 14, 25),
                RouteStop(townHall, 640, 10, 25),
                RouteStop(shambhuganj, 1150, 0, 35)
            ),
            pathPoints = listOf(
                0.43f to 0.66f,
                0.48f to 0.48f,
                0.30f to 0.28f,
                0.44f to 0.33f,
                0.62f to 0.42f,
                0.79f to 0.33f
            ),
            descriptionBn = "Depart Maskanda Terminal, navigate busy Charpara and Town Hall intersections, and cross the iconic steel Shambhuganj Bridge over the Brahmaputra River."
        ),
        MymensinghRoute(
            id = "route_bau_campus",
            titleBn = "BAU Green University Shuttle",
            titleEn = "Town Hall to BAU Campus",
            subtitleBn = "Town Hall -> Ganginar Par -> Kewatkhali -> BAU",
            distanceKm = 4.6f,
            totalSegments = 1050,
            difficultyBn = "Scenic City Route",
            zones = listOf(
                RoadZoneSpan(0, 320, RoadZoneType.CITY_BAZAAR, 45, "TOWN HALL & GANGINAR PAR"),
                RoadZoneSpan(320, 650, RoadZoneType.CITY_BAZAAR, 55, "KEWATKHALI OVERPASS"),
                RoadZoneSpan(650, 1050, RoadZoneType.UNIVERSITY_AVENUE, 60, "BAU UNIVERSITY CAMPUS")
            ),
            stops = listOf(
                RouteStop(townHall, 35, 14, 15),
                RouteStop(ganginarPar, 290, 12, 20),
                RouteStop(kewatkhali, 620, 10, 20),
                RouteStop(bauCampus, 1000, 0, 30)
            ),
            pathPoints = listOf(
                0.30f to 0.28f,
                0.44f to 0.33f,
                0.48f to 0.48f,
                0.62f to 0.60f,
                0.77f to 0.73f
            ),
            descriptionBn = "Cruise along the lush tree-lined Brahmaputra riverbank and carry students and commuters to Bangladesh Agricultural University."
        ),
        MymensinghRoute(
            id = "route_trishal_highway",
            titleBn = "Mymensingh <-> Trishal (N3 Highway)",
            titleEn = "Mymensingh-Trishal 4-Lane",
            subtitleBn = "Maskanda -> Bypass More -> Trishal Highway",
            distanceKm = 7.2f,
            totalSegments = 1450,
            difficultyBn = "High-Speed Highway",
            zones = listOf(
                RoadZoneSpan(0, 340, RoadZoneType.CITY_BAZAAR, 55, "MASKANDA -> BYPASS"),
                RoadZoneSpan(340, 1450, RoadZoneType.HIGHWAY_FOUR_LANE, 95, "DHAKA-MYMENSINGH N3")
            ),
            stops = listOf(
                RouteStop(maskanda, 40, 16, 25),
                RouteStop(bypassMore, 350, 14, 30),
                RouteStop(trishal, 1400, 0, 50)
            ),
            pathPoints = listOf(
                0.43f to 0.66f,
                0.35f to 0.78f,
                0.39f to 0.93f
            ),
            descriptionBn = "Open up the throttle on the wide 4-lane Dhaka-Mymensingh N3 highway from Maskanda & Bypass More straight to Trishal."
        ),
        MymensinghRoute(
            id = "route_free_drive",
            titleBn = "Free Drive Mymensingh Circuit",
            titleEn = "Free Drive Endless Loop",
            subtitleBn = "City Center, Brahmaputra Bridge & N3 Loop",
            distanceKm = 99.9f,
            totalSegments = 1600,
            difficultyBn = "Unlimited",
            isEndless = true,
            zones = listOf(
                RoadZoneSpan(0, 400, RoadZoneType.CITY_BAZAAR, 60, "MYMENSINGH CITY"),
                RoadZoneSpan(400, 750, RoadZoneType.BRIDGE_BRAHMAPUTRA, 75, "SHAMBHUGANJ BRIDGE"),
                RoadZoneSpan(750, 1150, RoadZoneType.UNIVERSITY_AVENUE, 65, "BAU GREEN AVENUE"),
                RoadZoneSpan(1150, 1600, RoadZoneType.HIGHWAY_FOUR_LANE, 95, "BYPASS HIGHWAY")
            ),
            stops = listOf(
                RouteStop(charpara, 180, 10, 20),
                RouteStop(shambhuganj, 580, 10, 25),
                RouteStop(bauCampus, 960, 10, 25),
                RouteStop(bypassMore, 1380, 10, 30)
            ),
            pathPoints = listOf(
                0.43f to 0.66f,
                0.48f to 0.48f,
                0.62f to 0.42f,
                0.79f to 0.33f,
                0.77f to 0.73f,
                0.62f to 0.60f,
                0.35f to 0.78f,
                0.43f to 0.66f
            ),
            descriptionBn = "Explore Mymensingh city streets, Shambhuganj Bridge, BAU avenue, and the highway freely with no time limits."
        )
    )

    fun routeById(id: String): MymensinghRoute = routes.find { it.id == id } ?: routes.first()
}
