package com.example

import com.example.domain.model.BusFleet
import com.example.domain.model.MymensinghMapData
import com.example.domain.model.RoadZoneType
import com.example.engine.RoadBuilder
import com.example.engine3d.TrackMeshBuilder
import com.example.engine3d.VehicleMeshes3D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun allMymensinghRoutesBuildValidTracks() {
        MymensinghMapData.routes.forEach { route ->
            val segments = RoadBuilder.buildTrack(route)
            assertEquals(route.totalSegments, segments.size)
            assertTrue(route.stops.isNotEmpty())
        }
    }

    @Test
    fun shambhuganjBridgeHasPositiveArchElevation() {
        val bridgeRoute = MymensinghMapData.routeById("route_shambhuganj_bridge")
        val bridgeSegs = RoadBuilder.buildTrack(bridgeRoute)
        assertTrue(bridgeSegs.any { it.zoneType == RoadZoneType.BRIDGE_BRAHMAPUTRA && it.yElevation > 500f })
    }

    @Test
    fun busFleetContainsBangladeshiCoaches() {
        assertEquals(4, BusFleet.buses.size)
        assertTrue(BusFleet.buses.any { it.id == "ena_express" })
        assertTrue(BusFleet.buses.any { it.id == "shoukhin_super" })
    }

    @Test
    fun world3DMeshBuildsValidGeometry() {
        val route = MymensinghMapData.routeById("route_shambhuganj_bridge")
        val segments = RoadBuilder.buildTrack(route)
        val world3D = TrackMeshBuilder.buildWorld3D(segments)
        assertEquals(segments.size + 1, world3D.points.size)
        assertTrue(world3D.chunks.isNotEmpty())
        assertTrue(world3D.chunks.all { it.mesh.vertexCount > 0 })

        BusFleet.buses.forEach { bus ->
            val busMesh = VehicleMeshes3D.buildBusMesh(bus)
            assertTrue(busMesh.vertexCount > 100)
        }
    }
}
