package com.example.engine3d

import androidx.compose.ui.graphics.Color
import com.example.domain.model.RoadZoneType
import com.example.engine.RoadSegment
import com.example.engine.SEGMENT_LENGTH
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

const val SEG_LEN_3D = 6.0f
const val ROAD_HALF_W_3D = 5.6f

data class TrackPoint3D(
    val x: Float,
    val y: Float,
    val z: Float,
    val yawDeg: Float
)

data class SegmentChunk3D(
    val startSeg: Int,
    val endSeg: Int,
    val mesh: GpuMesh
)

data class Track3DWorld(
    val points: List<TrackPoint3D>,
    val chunks: List<SegmentChunk3D>
) {
    fun sampleAtPlayerZ(playerZ: Float, isEndless: Boolean): TrackPoint3D {
        if (points.size < 2) return TrackPoint3D(0f, 0f, 0f, 0f)
        val segCount = points.size - 1
        val totalWorldZ = segCount * SEGMENT_LENGTH
        val wrapped = if (isEndless) ((playerZ % totalWorldZ) + totalWorldZ) % totalWorldZ else playerZ.coerceIn(0f, totalWorldZ - 0.1f)
        val exactSeg = wrapped / SEGMENT_LENGTH
        val idx = exactSeg.toInt().coerceIn(0, segCount - 1)
        val t = (exactSeg - idx).coerceIn(0f, 1f)
        val p0 = points[idx]
        val p1 = points[idx + 1]
        return TrackPoint3D(
            x = p0.x + (p1.x - p0.x) * t,
            y = p0.y + (p1.y - p0.y) * t,
            z = p0.z + (p1.z - p0.z) * t,
            yawDeg = p0.yawDeg + (p1.yawDeg - p0.yawDeg) * t
        )
    }
}

object TrackMeshBuilder {
    private const val CHUNK_SIZE = 16

    fun buildWorld3D(segments: List<RoadSegment>): Track3DWorld {
        val points = ArrayList<TrackPoint3D>(segments.size + 1)
        var curX = 0f
        var curZ = 0f
        var headingRad = 0f

        for (i in segments.indices) {
            val seg = segments[i]
            val curY = seg.yElevation * 0.012f
            points.add(TrackPoint3D(curX, curY, curZ, (headingRad * 180.0 / PI).toFloat()))

            // Smooth bounded S-curves along -Z forward axis
            headingRad = (headingRad + seg.curve * 0.015f).coerceIn(-0.62f, 0.62f)
            curX += sin(headingRad) * SEG_LEN_3D
            curZ -= cos(headingRad) * SEG_LEN_3D
        }
        val lastSeg = segments.lastOrNull()
        val endY = (lastSeg?.yElevation ?: 0f) * 0.012f
        points.add(TrackPoint3D(curX, endY, curZ, (headingRad * 180.0 / PI).toFloat()))

        // Build chunks of 16 segments each
        val chunks = ArrayList<SegmentChunk3D>()
        var start = 0
        while (start < segments.size) {
            val end = (start + CHUNK_SIZE).coerceAtMost(segments.size)
            val mesh = buildChunkMesh(segments, points, start, end)
            chunks.add(SegmentChunk3D(start, end, mesh))
            start = end
        }

        return Track3DWorld(points, chunks)
    }

    private fun buildChunkMesh(
        segments: List<RoadSegment>,
        points: List<TrackPoint3D>,
        startSeg: Int,
        endSeg: Int
    ): GpuMesh {
        val mb = MeshBuilder(6000)

        for (i in startSeg until endSeg) {
            val seg = segments[i]
            val p0 = points[i]
            val p1 = points[i + 1]
            val isBridge = seg.zoneType == RoadZoneType.BRIDGE_BRAHMAPUTRA
            val alt = (i % 2) == 0

            val r0 = p0.yawDeg * (PI / 180.0)
            val r1 = p1.yawDeg * (PI / 180.0)
            val rx0 = cos(r0).toFloat()
            val rz0 = sin(r0).toFloat()
            val rx1 = cos(r1).toFloat()
            val rz1 = sin(r1).toFloat()

            fun addRoadStrip(
                offsetCenter: Float,
                halfWidth: Float,
                yOffset: Float,
                color: Color,
                surfaceType: Float = 0f
            ) {
                val c0x = p0.x + rx0 * offsetCenter
                val c0z = p0.z + rz0 * offsetCenter
                val c1x = p1.x + rx1 * offsetCenter
                val c1z = p1.z + rz1 * offsetCenter

                mb.addQuad(
                    c0x - rx0 * halfWidth, p0.y + yOffset, c0z - rz0 * halfWidth,
                    c0x + rx0 * halfWidth, p0.y + yOffset, c0z + rz0 * halfWidth,
                    c1x + rx1 * halfWidth, p1.y + yOffset, c1z + rz1 * halfWidth,
                    c1x - rx1 * halfWidth, p1.y + yOffset, c1z - rz1 * halfWidth,
                    0f, 1f, 0f, color, surfaceType
                )
            }

            // 1. Wide Surrounding Ground OR Shimmering Brahmaputra River Water
            if (isBridge) {
                val riverCol = if (alt) Color(0xFF1565A0) else Color(0xFF1976B5)
                val waterY = -2.2f
                mb.addQuad(
                    p0.x - rx0 * 95f, waterY, p0.z - rz0 * 95f,
                    p0.x + rx0 * 95f, waterY, p0.z + rz0 * 95f,
                    p1.x + rx1 * 95f, waterY, p1.z + rz1 * 95f,
                    p1.x - rx1 * 95f, waterY, p1.z - rz1 * 95f,
                    0f, 1f, 0f, riverCol, surfaceType = 2f
                )
                // Bridge Concrete Deck Underside & Steel Guardrails
                addRoadStrip(0f, ROAD_HALF_W_3D + 0.85f, -0.04f, Color(0xFF546E7A))
                addRoadStrip(-ROAD_HALF_W_3D - 0.65f, 0.22f, 0.55f, Color(0xFF455A64))
                addRoadStrip(ROAD_HALF_W_3D + 0.65f, 0.22f, 0.55f, Color(0xFF455A64))
                // Overhead Bridge Cross Girder every 6 segments
                if (i % 6 == 0) {
                    val beamY = p0.y + 8.2f
                    mb.addQuad(
                        p0.x - rx0 * (ROAD_HALF_W_3D + 0.6f), beamY, p0.z - rz0 * (ROAD_HALF_W_3D + 0.6f) - 0.3f,
                        p0.x + rx0 * (ROAD_HALF_W_3D + 0.6f), beamY, p0.z + rz0 * (ROAD_HALF_W_3D + 0.6f) - 0.3f,
                        p0.x + rx0 * (ROAD_HALF_W_3D + 0.6f), beamY + 0.55f, p0.z + rz0 * (ROAD_HALF_W_3D + 0.6f) + 0.3f,
                        p0.x - rx0 * (ROAD_HALF_W_3D + 0.6f), beamY + 0.55f, p0.z - rz0 * (ROAD_HALF_W_3D + 0.6f) + 0.3f,
                        0f, -1f, 0f, Color(0xFF455A64)
                    )
                }
            } else {
                val grassCol = when (seg.zoneType) {
                    RoadZoneType.CITY_BAZAAR -> if (alt) Color(0xFF2E3D2F) else Color(0xFF2B392C)
                    RoadZoneType.UNIVERSITY_AVENUE -> if (alt) Color(0xFF1F6634) else Color(0xFF1C5E30)
                    else -> if (alt) Color(0xFF28632D) else Color(0xFF245B29)
                }
                addRoadStrip(0f, 85f, -0.08f, grassCol)
                // Dirt/Gravel Shoulders
                val shoulderCol = if (alt) Color(0xFF5D4F40) else Color(0xFF544739)
                addRoadStrip(0f, ROAD_HALF_W_3D + 1.35f, -0.03f, shoulderCol)
            }

            // 2. Realistic Procedural Asphalt Surface (surfaceType = 1.0f)
            val asphaltCol = if (alt) Color(0xFF272C33) else Color(0xFF2A3038)
            addRoadStrip(0f, ROAD_HALF_W_3D, 0.0f, asphaltCol, surfaceType = 1f)

            // 3. Outer White Edge Lines
            addRoadStrip(-ROAD_HALF_W_3D + 0.25f, 0.11f, 0.015f, Color(0xFFE2E8F0))
            addRoadStrip(ROAD_HALF_W_3D - 0.25f, 0.11f, 0.015f, Color(0xFFE2E8F0))

            // 4. Center Divider (Raised 3D Median on 4-lane Highway, Double Yellow Lines otherwise)
            if (seg.zoneType == RoadZoneType.HIGHWAY_FOUR_LANE) {
                val medCol = if (alt) Color(0xFFFFCA28) else Color(0xFF1E242A)
                addRoadStrip(0f, 0.26f, 0.22f, medCol)
            } else {
                addRoadStrip(-0.14f, 0.07f, 0.015f, Color(0xFFFFCA28))
                addRoadStrip(0.14f, 0.07f, 0.015f, Color(0xFFFFCA28))
            }

            // 5. Dashed Lane Dividers
            if ((i % 3) != 0) {
                addRoadStrip(-ROAD_HALF_W_3D * 0.5f, 0.08f, 0.015f, Color(0xFFCBD5E1))
                addRoadStrip(ROAD_HALF_W_3D * 0.5f, 0.08f, 0.015f, Color(0xFFCBD5E1))
            }

            // 6. Green Bus Stop Bay Highlight
            if (seg.stop != null) {
                addRoadStrip(-ROAD_HALF_W_3D * 0.72f, 1.35f, 0.012f, Color(0xFF0E7C4E))
            }

            // 7. Speed Breaker Hump
            if (seg.hasSpeedBreaker) {
                addRoadStrip(0f, ROAD_HALF_W_3D * 0.96f, 0.09f, if (alt) Color(0xFFFFC107) else Color(0xFF14181D))
            }

            // 8. Zebra Crossing
            if (seg.hasZebraCrossing) {
                for (z in -3..3) {
                    addRoadStrip(z * 1.4f, 0.38f, 0.018f, Color.White)
                }
            }
        }

        return GpuMesh(mb.toFloatBuffer(), mb.vertexCount)
    }
}
