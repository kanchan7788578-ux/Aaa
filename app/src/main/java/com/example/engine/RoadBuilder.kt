package com.example.engine

import com.example.domain.model.MymensinghRoute
import com.example.domain.model.RoadZoneType
import com.example.domain.model.RouteStop
import kotlin.math.PI
import kotlin.math.sin

const val SEGMENT_LENGTH = 200f
const val ROAD_HALF_WIDTH = 1150f

enum class PropKind {
    BANYAN_TREE,
    PALM_TREE,
    CITY_BUILDING,
    SHOP_STALL,
    ELECTRIC_POLE,
    HIGHWAY_SIGN,
    BRIDGE_TRUSS,
    BUS_SHELTER
}

data class SceneryProp(
    val kind: PropKind,
    val offsetFromCenter: Float, // multiplier of ROAD_HALF_WIDTH (e.g., -1.35f or +1.45f)
    val labelBn: String = "",
    val variant: Int = 0
)

data class RoadSegment(
    val index: Int,
    val zStart: Float,
    val zEnd: Float,
    val yElevation: Float,
    val curve: Float,
    val zoneType: RoadZoneType,
    val speedLimitKmh: Int,
    val hasSpeedBreaker: Boolean = false,
    val hasZebraCrossing: Boolean = false,
    val stop: RouteStop? = null,
    val leftProp: SceneryProp? = null,
    val rightProp: SceneryProp? = null
)

object RoadBuilder {
    fun buildTrack(route: MymensinghRoute): List<RoadSegment> {
        val count = route.totalSegments
        val segments = ArrayList<RoadSegment>(count)
        val stopByStart = route.stops.associateBy { it.segmentIndex }

        for (i in 0 until count) {
            val zoneSpan = route.zones.firstOrNull { i in it.startSegment until it.endSegment }
                ?: route.zones.last()
            val zoneType = zoneSpan.zoneType

            // Smooth elevation profile: rise dramatically over Shambhuganj Brahmaputra Bridge
            val elevation = when (zoneType) {
                RoadZoneType.BRIDGE_BRAHMAPUTRA -> {
                    val spanLen = (zoneSpan.endSegment - zoneSpan.startSegment).coerceAtLeast(1)
                    val t = (i - zoneSpan.startSegment).toFloat() / spanLen.toFloat()
                    // Smooth arch bridge deck peaking at +950 units
                    (sin(t * PI) * 950.0).toFloat()
                }
                RoadZoneType.HIGHWAY_FOUR_LANE -> {
                    (sin(i * 0.015) * 140.0).toFloat()
                }
                RoadZoneType.UNIVERSITY_AVENUE -> {
                    (sin(i * 0.02) * 80.0).toFloat()
                }
                RoadZoneType.CITY_BAZAAR -> 0f
            }

            // Road curvature: realistic bends at Mymensingh intersections & curves
            val curve = when {
                i < 25 -> 0f
                zoneType == RoadZoneType.BRIDGE_BRAHMAPUTRA -> {
                    // Bridge approaches curve slightly, bridge span straight
                    val spanLen = (zoneSpan.endSegment - zoneSpan.startSegment).coerceAtLeast(1)
                    val t = (i - zoneSpan.startSegment).toFloat() / spanLen.toFloat()
                    if (t < 0.14f) 1.4f else if (t > 0.86f) -1.2f else 0f
                }
                zoneType == RoadZoneType.CITY_BAZAAR -> {
                    when {
                        i % 190 in 50..95 -> 1.85f
                        i % 190 in 125..165 -> -1.65f
                        else -> 0f
                    }
                }
                zoneType == RoadZoneType.UNIVERSITY_AVENUE -> {
                    (sin(i * 0.025) * 2.1).toFloat()
                }
                else -> {
                    when {
                        i % 260 in 70..135 -> -1.4f
                        i % 260 in 175..230 -> 1.5f
                        else -> 0f
                    }
                }
            }

            // Bus stop bay spans 22 segments centered around stop.segmentIndex
            val activeStop = route.stops.firstOrNull { i in (it.segmentIndex - 6)..(it.segmentIndex + 16) }

            val hasZebra = activeStop != null && i == activeStop.segmentIndex - 8
            val hasSpeedBreaker = zoneType == RoadZoneType.CITY_BAZAAR &&
                (i % 140 == 70) && activeStop == null

            // Roadside scenery props
            val leftProp: SceneryProp?
            val rightProp: SceneryProp?

            if (zoneType == RoadZoneType.BRIDGE_BRAHMAPUTRA) {
                leftProp = if (i % 6 == 0) SceneryProp(PropKind.BRIDGE_TRUSS, -1.08f, variant = i) else null
                rightProp = if (i % 6 == 0) SceneryProp(PropKind.BRIDGE_TRUSS, 1.08f, variant = i) else null
            } else if (activeStop != null && i == activeStop.segmentIndex) {
                leftProp = SceneryProp(PropKind.BUS_SHELTER, -1.42f, labelBn = activeStop.landmark.nameBn)
                rightProp = SceneryProp(PropKind.ELECTRIC_POLE, 1.24f)
            } else if (i % 45 == 12) {
                leftProp = SceneryProp(PropKind.HIGHWAY_SIGN, -1.32f, labelBn = zoneSpan.signboardBn)
                rightProp = if (zoneType == RoadZoneType.CITY_BAZAAR) {
                    SceneryProp(PropKind.CITY_BUILDING, 1.65f, variant = i)
                } else {
                    SceneryProp(PropKind.PALM_TREE, 1.45f, variant = i)
                }
            } else {
                leftProp = when {
                    i % 8 == 0 -> SceneryProp(PropKind.ELECTRIC_POLE, -1.16f)
                    zoneType == RoadZoneType.CITY_BAZAAR && i % 4 == 2 ->
                        SceneryProp(if (i % 12 == 2) PropKind.SHOP_STALL else PropKind.CITY_BUILDING, -1.62f, variant = i)
                    zoneType == RoadZoneType.UNIVERSITY_AVENUE && i % 3 == 1 ->
                        SceneryProp(if (i % 6 == 1) PropKind.BANYAN_TREE else PropKind.PALM_TREE, -1.45f, variant = i)
                    zoneType == RoadZoneType.HIGHWAY_FOUR_LANE && i % 5 == 2 ->
                        SceneryProp(if (i % 10 == 2) PropKind.PALM_TREE else PropKind.BANYAN_TREE, -1.55f, variant = i)
                    else -> null
                }
                rightProp = when {
                    i % 8 == 4 -> SceneryProp(PropKind.ELECTRIC_POLE, 1.16f)
                    zoneType == RoadZoneType.CITY_BAZAAR && i % 4 == 0 ->
                        SceneryProp(if (i % 12 == 0) PropKind.SHOP_STALL else PropKind.CITY_BUILDING, 1.64f, variant = i + 3)
                    zoneType == RoadZoneType.UNIVERSITY_AVENUE && i % 3 == 2 ->
                        SceneryProp(if (i % 6 == 2) PropKind.BANYAN_TREE else PropKind.PALM_TREE, 1.48f, variant = i + 5)
                    zoneType == RoadZoneType.HIGHWAY_FOUR_LANE && i % 5 == 3 ->
                        SceneryProp(if (i % 10 == 3) PropKind.BANYAN_TREE else PropKind.PALM_TREE, 1.56f, variant = i + 7)
                    else -> null
                }
            }

            segments.add(
                RoadSegment(
                    index = i,
                    zStart = i * SEGMENT_LENGTH,
                    zEnd = (i + 1) * SEGMENT_LENGTH,
                    yElevation = elevation,
                    curve = curve,
                    zoneType = zoneType,
                    speedLimitKmh = zoneSpan.speedLimitKmh,
                    hasSpeedBreaker = hasSpeedBreaker,
                    hasZebraCrossing = hasZebra,
                    stop = stopByStart[i] ?: activeStop,
                    leftProp = leftProp,
                    rightProp = rightProp
                )
            )
        }
        return segments
    }
}
