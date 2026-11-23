package com.example.engine3d

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.domain.model.BusFleet
import com.example.domain.model.CameraMode
import com.example.domain.model.TrafficType
import com.example.domain.model.WeatherTime
import com.example.engine.PropKind
import com.example.engine.RoadSegment
import com.example.engine.SEGMENT_LENGTH
import com.example.viewmodel.DrivingUiState
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class Bus3DRenderer : GLSurfaceView.Renderer {

    @Volatile
    var uiState: DrivingUiState = DrivingUiState()

    @Volatile
    var weatherTime: WeatherTime = WeatherTime.MORNING

    @Volatile
    var cameraMode: CameraMode = CameraMode.CHASE

    private var worldProgram = 0
    private var skyProgram = 0

    // World shader uniform & attribute handles
    private var uMvpLoc = 0
    private var uModelLoc = 0
    private var uCameraPosLoc = 0
    private var uSunDirLoc = 0
    private var uAmbientLoc = 0
    private var uSunColorLoc = 0
    private var uFogColorLoc = 0
    private var uFogDensityLoc = 0
    private var uTimeLoc = 0
    private var uIsNightLoc = 0
    private var uIsRainLoc = 0
    private var uHeadlightsOnLoc = 0
    private var uHeadlightPosLoc = 0
    private var uHeadlightDirLoc = 0

    private var aPosLoc = 0
    private var aNormalLoc = 0
    private var aColorLoc = 0
    private var aSurfaceTypeLoc = 0

    // Sky handles
    private var aSkyPosLoc = 0
    private var aSkyColorLoc = 0
    private var skyBuffer: FloatBuffer? = null
    private val skyFloats = FloatArray(4 * 5)

    // Prebuilt static meshes
    private val busMeshes = HashMap<String, GpuMesh>()
    private val trafficMeshes = HashMap<Pair<TrafficType, Int>, GpuMesh>()
    private val propMeshes = HashMap<Pair<PropKind, Int>, GpuMesh>()

    // Track 3D geometry cache
    private var cachedRouteId: String? = null
    private var cachedSegmentCount = -1
    private var track3D: Track3DWorld? = null

    // Reusable matrices (zero per-frame allocations)
    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.05f, 0.08f, 0.12f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)

        worldProgram = WorldShaders.compileProgram(WorldShaders.VERTEX_SHADER, WorldShaders.FRAGMENT_SHADER)
        skyProgram = WorldShaders.compileProgram(WorldShaders.SKY_VERTEX_SHADER, WorldShaders.SKY_FRAGMENT_SHADER)

        uMvpLoc = GLES20.glGetUniformLocation(worldProgram, "uMvpMatrix")
        uModelLoc = GLES20.glGetUniformLocation(worldProgram, "uModelMatrix")
        uCameraPosLoc = GLES20.glGetUniformLocation(worldProgram, "uCameraPos")
        uSunDirLoc = GLES20.glGetUniformLocation(worldProgram, "uSunDir")
        uAmbientLoc = GLES20.glGetUniformLocation(worldProgram, "uAmbientColor")
        uSunColorLoc = GLES20.glGetUniformLocation(worldProgram, "uSunColor")
        uFogColorLoc = GLES20.glGetUniformLocation(worldProgram, "uFogColor")
        uFogDensityLoc = GLES20.glGetUniformLocation(worldProgram, "uFogDensity")
        uTimeLoc = GLES20.glGetUniformLocation(worldProgram, "uTime")
        uIsNightLoc = GLES20.glGetUniformLocation(worldProgram, "uIsNightOrSunset")
        uIsRainLoc = GLES20.glGetUniformLocation(worldProgram, "uIsRain")
        uHeadlightsOnLoc = GLES20.glGetUniformLocation(worldProgram, "uHeadlightsOn")
        uHeadlightPosLoc = GLES20.glGetUniformLocation(worldProgram, "uHeadlightPos")
        uHeadlightDirLoc = GLES20.glGetUniformLocation(worldProgram, "uHeadlightDir")

        aPosLoc = GLES20.glGetAttribLocation(worldProgram, "aPosition")
        aNormalLoc = GLES20.glGetAttribLocation(worldProgram, "aNormal")
        aColorLoc = GLES20.glGetAttribLocation(worldProgram, "aColor")
        aSurfaceTypeLoc = GLES20.glGetAttribLocation(worldProgram, "aSurfaceType")

        aSkyPosLoc = GLES20.glGetAttribLocation(skyProgram, "aPos")
        aSkyColorLoc = GLES20.glGetAttribLocation(skyProgram, "aColor")

        skyBuffer = ByteBuffer.allocateDirect(skyFloats.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()

        // Prebuild all 4 Bangladeshi Coach Meshes
        busMeshes.clear()
        BusFleet.buses.forEach { bus ->
            busMeshes[bus.id] = VehicleMeshes3D.buildBusMesh(bus)
        }

        // Prebuild Traffic Vehicle Meshes (variants 0..3)
        trafficMeshes.clear()
        TrafficType.entries.forEach { type ->
            for (v in 0..3) {
                trafficMeshes[type to v] = VehicleMeshes3D.buildTrafficMesh(type, v)
            }
        }

        // Prebuild Roadside Scenery Prop Meshes
        propMeshes.clear()
        PropKind.entries.forEach { kind ->
            for (v in 0..3) {
                propMeshes[kind to v] = VehicleMeshes3D.buildPropMesh(kind, v)
            }
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.coerceAtLeast(1).toFloat()
        val fov = if (aspect > 1.2f) 56f else 66f
        Matrix.perspectiveM(projMatrix, 0, fov, aspect, 0.4f, 520f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val state = uiState
        val weather = weatherTime
        val camMode = cameraMode

        ensureTrackBuilt(state.route.id, state.segments)
        val world = track3D

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // 1. Draw Full-Screen Atmospheric Sky Gradient
        drawSky(weather)

        if (world == null || state.segments.isEmpty()) return

        // 2. Compute Player Bus 3D World Pose
        val busTrackPt = world.sampleAtPlayerZ(state.playerZ, state.route.isEndless)
        val yawRad = busTrackPt.yawDeg * (PI / 180.0)
        val rightX = cos(yawRad).toFloat()
        val rightZ = sin(yawRad).toFloat()
        val fwdX = sin(yawRad).toFloat()
        val fwdZ = -cos(yawRad).toFloat()

        val lateralOffset = state.playerX * ROAD_HALF_W_3D
        val busX = busTrackPt.x + rightX * lateralOffset
        val busY = busTrackPt.y + state.suspensionBounce * 0.015f
        val busZ = busTrackPt.z + rightZ * lateralOffset
        val busYawDeg = -busTrackPt.yawDeg - state.steerSmooth * 5.5f

        // 3. Position 3D Camera (BUSSID Chase Cam or Driver Cockpit Cam)
        val camX: Float
        val camY: Float
        val camZ: Float
        val lookX: Float
        val lookY: Float
        val lookZ: Float

        if (camMode == CameraMode.COCKPIT) {
            // Inside right-hand-drive cabin looking out through the front windshield
            camX = busX + rightX * 0.55f + fwdX * 4.1f
            camY = busY + 2.45f
            camZ = busZ + rightZ * 0.55f + fwdZ * 4.1f
            lookX = busX + fwdX * 36f + rightX * (state.steerSmooth * 2.2f)
            lookY = busY + 1.9f
            lookZ = busZ + fwdZ * 36f + rightZ * (state.steerSmooth * 2.2f)
        } else {
            val chaseDist = 13.8f
            val chaseHeight = 4.6f
            camX = busX - fwdX * chaseDist - rightX * (state.steerSmooth * 0.9f)
            camY = busY + chaseHeight
            camZ = busZ - fwdZ * chaseDist - rightZ * (state.steerSmooth * 0.9f)
            lookX = busX + fwdX * 16f
            lookY = busY + 1.85f
            lookZ = busZ + fwdZ * 16f
        }

        Matrix.setLookAtM(viewMatrix, 0, camX, camY, camZ, lookX, lookY, lookZ, 0f, 1f, 0f)
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        // 4. Activate 3D World Shader & Lighting Uniforms
        GLES20.glUseProgram(worldProgram)
        configureLightingUniforms(weather, state, camX, camY, camZ, busX, busY, busZ, fwdX, fwdZ)

        // 5. Render Visible 3D Road Chunks
        val segCount = state.segments.size
        val totalLen = segCount * SEGMENT_LENGTH
        val wrappedZ = if (state.route.isEndless) ((state.playerZ % totalLen) + totalLen) % totalLen else state.playerZ.coerceIn(0f, totalLen)
        val playerSegIdx = (wrappedZ / SEGMENT_LENGTH).toInt().coerceIn(0, segCount - 1)
        val minSeg = (playerSegIdx - 4).coerceAtLeast(0)
        val maxSeg = (playerSegIdx + 86).coerceAtMost(segCount - 1)

        Matrix.setIdentityM(modelMatrix, 0)
        for (chunk in world.chunks) {
            if (chunk.endSeg >= minSeg && chunk.startSeg <= maxSeg) {
                drawMesh(chunk.mesh, modelMatrix)
            }
        }

        // 6. Render Visible 3D Roadside Props (Trees, Buildings, Bridge Trusses, Poles, Bus Shelters)
        for (idx in minSeg..maxSeg) {
            val seg = state.segments[idx]
            val pt = world.points[idx]
            val rRad = pt.yawDeg * (PI / 180.0)
            val rx = cos(rRad).toFloat()
            val rz = sin(rRad).toFloat()

            seg.leftProp?.let { prop ->
                val mesh = propMeshes[prop.kind to (prop.variant % 4)]
                if (mesh != null) {
                    val px = pt.x + rx * (prop.offsetFromCenter * ROAD_HALF_W_3D * 1.15f)
                    val pz = pt.z + rz * (prop.offsetFromCenter * ROAD_HALF_W_3D * 1.15f)
                    Matrix.setIdentityM(modelMatrix, 0)
                    Matrix.translateM(modelMatrix, 0, px, pt.y, pz)
                    Matrix.rotateM(modelMatrix, 0, -pt.yawDeg, 0f, 1f, 0f)
                    drawMesh(mesh, modelMatrix)
                }
            }
            seg.rightProp?.let { prop ->
                val mesh = propMeshes[prop.kind to (prop.variant % 4)]
                if (mesh != null) {
                    val px = pt.x + rx * (prop.offsetFromCenter * ROAD_HALF_W_3D * 1.15f)
                    val pz = pt.z + rz * (prop.offsetFromCenter * ROAD_HALF_W_3D * 1.15f)
                    Matrix.setIdentityM(modelMatrix, 0)
                    Matrix.translateM(modelMatrix, 0, px, pt.y, pz)
                    Matrix.rotateM(modelMatrix, 0, -pt.yawDeg + 180f, 0f, 1f, 0f)
                    drawMesh(mesh, modelMatrix)
                }
            }
        }

        // 7. Render 3D Traffic Vehicles on the Track
        for (veh in state.traffic) {
            val vz = if (state.route.isEndless) ((veh.zPos % totalLen) + totalLen) % totalLen else veh.zPos
            val vSeg = (vz / SEGMENT_LENGTH).toInt()
            if (vSeg < minSeg || vSeg > maxSeg) continue

            val tPt = world.sampleAtPlayerZ(vz, state.route.isEndless)
            val trRad = tPt.yawDeg * (PI / 180.0)
            val tx = tPt.x + cos(trRad).toFloat() * (veh.laneX * ROAD_HALF_W_3D)
            val tz = tPt.z + sin(trRad).toFloat() * (veh.laneX * ROAD_HALF_W_3D)

            val mesh = trafficMeshes[veh.type to (veh.colorVariant % 4)] ?: continue
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, tx, tPt.y, tz)
            val orientYaw = if (veh.isOncoming && veh.type != TrafficType.ONCOMING_BUS) {
                -tPt.yawDeg + 180f
            } else {
                -tPt.yawDeg
            }
            Matrix.rotateM(modelMatrix, 0, orientYaw, 0f, 1f, 0f)
            drawMesh(mesh, modelMatrix)
        }

        // 8. Render Player's 3D Coach Bus (in Chase Cam)
        if (camMode == CameraMode.CHASE) {
            val playerMesh = busMeshes[state.bus.id] ?: busMeshes.values.firstOrNull()
            if (playerMesh != null) {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, busX, busY, busZ)
                Matrix.rotateM(modelMatrix, 0, busYawDeg, 0f, 1f, 0f)
                // Body roll tilt when cornering!
                Matrix.rotateM(modelMatrix, 0, state.steerSmooth * 2.4f, 0f, 0f, 1f)
                drawMesh(playerMesh, modelMatrix)
            }
        }
    }

    private fun ensureTrackBuilt(routeId: String, segments: List<RoadSegment>) {
        if (segments.isEmpty()) return
        if (cachedRouteId == routeId && cachedSegmentCount == segments.size && track3D != null) return
        cachedRouteId = routeId
        cachedSegmentCount = segments.size
        track3D = TrackMeshBuilder.buildWorld3D(segments)
    }

    private fun configureLightingUniforms(
        weather: WeatherTime,
        state: DrivingUiState,
        camX: Float, camY: Float, camZ: Float,
        busX: Float, busY: Float, busZ: Float,
        fwdX: Float, fwdZ: Float
    ) {
        GLES20.glUniform3f(uCameraPosLoc, camX, camY, camZ)
        GLES20.glUniform1f(uTimeLoc, state.timeSec)

        when (weather) {
            WeatherTime.MORNING -> {
                GLES20.glUniform3f(uSunDirLoc, 0.45f, 0.78f, -0.35f)
                GLES20.glUniform3f(uAmbientLoc, 0.46f, 0.48f, 0.50f)
                GLES20.glUniform3f(uSunColorLoc, 0.72f, 0.68f, 0.60f)
                GLES20.glUniform3f(uFogColorLoc, 0.68f, 0.80f, 0.88f)
                GLES20.glUniform1f(uFogDensityLoc, 0.0026f)
                GLES20.glUniform1f(uIsNightLoc, 0f)
                GLES20.glUniform1f(uIsRainLoc, 0f)
            }
            WeatherTime.SUNSET -> {
                GLES20.glUniform3f(uSunDirLoc, 0.25f, 0.35f, -0.90f)
                GLES20.glUniform3f(uAmbientLoc, 0.38f, 0.28f, 0.34f)
                GLES20.glUniform3f(uSunColorLoc, 0.92f, 0.56f, 0.30f)
                GLES20.glUniform3f(uFogColorLoc, 0.82f, 0.48f, 0.34f)
                GLES20.glUniform1f(uFogDensityLoc, 0.0034f)
                GLES20.glUniform1f(uIsNightLoc, 1f)
                GLES20.glUniform1f(uIsRainLoc, 0f)
            }
            WeatherTime.MONSOON_RAIN -> {
                GLES20.glUniform3f(uSunDirLoc, 0.20f, 0.85f, -0.30f)
                GLES20.glUniform3f(uAmbientLoc, 0.34f, 0.38f, 0.42f)
                GLES20.glUniform3f(uSunColorLoc, 0.42f, 0.46f, 0.52f)
                GLES20.glUniform3f(uFogColorLoc, 0.36f, 0.43f, 0.48f)
                GLES20.glUniform1f(uFogDensityLoc, 0.0048f)
                GLES20.glUniform1f(uIsNightLoc, 0f)
                GLES20.glUniform1f(uIsRainLoc, 1f)
            }
            WeatherTime.NIGHT -> {
                GLES20.glUniform3f(uSunDirLoc, 0.30f, 0.80f, -0.30f)
                GLES20.glUniform3f(uAmbientLoc, 0.14f, 0.17f, 0.24f)
                GLES20.glUniform3f(uSunColorLoc, 0.22f, 0.26f, 0.38f)
                GLES20.glUniform3f(uFogColorLoc, 0.05f, 0.08f, 0.14f)
                GLES20.glUniform1f(uFogDensityLoc, 0.0052f)
                GLES20.glUniform1f(uIsNightLoc, 1f)
                GLES20.glUniform1f(uIsRainLoc, 0f)
            }
        }

        val lightsActive = state.headlightsOn || weather == WeatherTime.NIGHT
        GLES20.glUniform1f(uHeadlightsOnLoc, if (lightsActive) 1f else 0f)
        GLES20.glUniform3f(uHeadlightPosLoc, busX + fwdX * 5.6f, busY + 1.15f, busZ + fwdZ * 5.6f)
        GLES20.glUniform3f(uHeadlightDirLoc, fwdX, -0.14f, fwdZ)
    }

    private fun drawMesh(mesh: GpuMesh, modelMat: FloatArray) {
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMat, 0)
        GLES20.glUniformMatrix4fv(uMvpLoc, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uModelLoc, 1, false, modelMat, 0)

        val buf = mesh.buffer
        val strideBytes = 10 * 4

        buf.position(0)
        GLES20.glEnableVertexAttribArray(aPosLoc)
        GLES20.glVertexAttribPointer(aPosLoc, 3, GLES20.GL_FLOAT, false, strideBytes, buf)

        buf.position(3)
        GLES20.glEnableVertexAttribArray(aNormalLoc)
        GLES20.glVertexAttribPointer(aNormalLoc, 3, GLES20.GL_FLOAT, false, strideBytes, buf)

        buf.position(6)
        GLES20.glEnableVertexAttribArray(aColorLoc)
        GLES20.glVertexAttribPointer(aColorLoc, 3, GLES20.GL_FLOAT, false, strideBytes, buf)

        buf.position(9)
        GLES20.glEnableVertexAttribArray(aSurfaceTypeLoc)
        GLES20.glVertexAttribPointer(aSurfaceTypeLoc, 1, GLES20.GL_FLOAT, false, strideBytes, buf)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mesh.vertexCount)
    }

    private fun drawSky(weather: WeatherTime) {
        val topR: Float; val topG: Float; val topB: Float
        val botR: Float; val botG: Float; val botB: Float
        when (weather) {
            WeatherTime.MORNING -> {
                topR = 0.08f; topG = 0.42f; topB = 0.78f
                botR = 0.70f; botG = 0.84f; botB = 0.92f
            }
            WeatherTime.SUNSET -> {
                topR = 0.14f; topG = 0.10f; topB = 0.34f
                botR = 0.92f; botG = 0.48f; botB = 0.24f
            }
            WeatherTime.MONSOON_RAIN -> {
                topR = 0.12f; topG = 0.18f; topB = 0.24f
                botR = 0.38f; botG = 0.45f; botB = 0.50f
            }
            WeatherTime.NIGHT -> {
                topR = 0.01f; topG = 0.03f; topB = 0.08f
                botR = 0.06f; botG = 0.10f; botB = 0.18f
            }
        }

        // Fullscreen quad (triangle strip: BL, BR, TL, TR)
        val arr = skyFloats
        // Bottom-left
        arr[0] = -1f; arr[1] = -1f; arr[2] = botR; arr[3] = botG; arr[4] = botB
        // Bottom-right
        arr[5] = 1f; arr[6] = -1f; arr[7] = botR; arr[8] = botG; arr[9] = botB
        // Top-left
        arr[10] = -1f; arr[11] = 1f; arr[12] = topR; arr[13] = topG; arr[14] = topB
        // Top-right
        arr[15] = 1f; arr[16] = 1f; arr[17] = topR; arr[18] = topG; arr[19] = topB

        val sb = skyBuffer ?: return
        sb.position(0)
        sb.put(arr)
        sb.position(0)

        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glUseProgram(skyProgram)

        val stride = 5 * 4
        sb.position(0)
        GLES20.glEnableVertexAttribArray(aSkyPosLoc)
        GLES20.glVertexAttribPointer(aSkyPosLoc, 2, GLES20.GL_FLOAT, false, stride, sb)

        sb.position(2)
        GLES20.glEnableVertexAttribArray(aSkyColorLoc)
        GLES20.glVertexAttribPointer(aSkyColorLoc, 3, GLES20.GL_FLOAT, false, stride, sb)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    }
}
