package com.example.engine3d

import androidx.compose.ui.graphics.Color
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Builds packed 3D vertex buffers with stride 10 floats:
 * [x, y, z, nx, ny, nz, r, g, b, surfaceType]
 */
class MeshBuilder(initialCapacityFloats: Int = 4096) {
    private var data = FloatArray(initialCapacityFloats)
    var floatCount = 0
        private set

    val vertexCount: Int
        get() = floatCount / 10

    private fun ensureCapacity(extraFloats: Int) {
        if (floatCount + extraFloats > data.size) {
            val newSize = (data.size * 2).coerceAtLeast(floatCount + extraFloats)
            data = data.copyOf(newSize)
        }
    }

    fun addVertex(
        x: Float, y: Float, z: Float,
        nx: Float, ny: Float, nz: Float,
        color: Color,
        surfaceType: Float = 0f
    ) {
        ensureCapacity(10)
        data[floatCount++] = x
        data[floatCount++] = y
        data[floatCount++] = z
        data[floatCount++] = nx
        data[floatCount++] = ny
        data[floatCount++] = nz
        data[floatCount++] = color.red
        data[floatCount++] = color.green
        data[floatCount++] = color.blue
        data[floatCount++] = surfaceType
    }

    fun addTriangle(
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        nx: Float, ny: Float, nz: Float,
        color: Color,
        surfaceType: Float = 0f
    ) {
        addVertex(x1, y1, z1, nx, ny, nz, color, surfaceType)
        addVertex(x2, y2, z2, nx, ny, nz, color, surfaceType)
        addVertex(x3, y3, z3, nx, ny, nz, color, surfaceType)
    }

    fun addQuad(
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        x4: Float, y4: Float, z4: Float,
        nx: Float, ny: Float, nz: Float,
        color: Color,
        surfaceType: Float = 0f
    ) {
        addTriangle(x1, y1, z1, x2, y2, z2, x3, y3, z3, nx, ny, nz, color, surfaceType)
        addTriangle(x1, y1, z1, x3, y3, z3, x4, y4, z4, nx, ny, nz, color, surfaceType)
    }

    /**
     * Adds an axis-aligned 3D box centered at (cx, cy, cz) with dimensions (sx, sy, sz).
     */
    fun addBox(
        cx: Float, cy: Float, cz: Float,
        sx: Float, sy: Float, sz: Float,
        color: Color,
        surfaceType: Float = 0f,
        topColor: Color = color,
        sideShade: Float = 0.88f
    ) {
        val hx = sx * 0.5f
        val hy = sy * 0.5f
        val hz = sz * 0.5f
        val sideCol = Color(
            (color.red * sideShade).coerceIn(0f, 1f),
            (color.green * sideShade).coerceIn(0f, 1f),
            (color.blue * sideShade).coerceIn(0f, 1f)
        )
        // Front (+Z)
        addQuad(
            cx - hx, cy - hy, cz + hz,
            cx + hx, cy - hy, cz + hz,
            cx + hx, cy + hy, cz + hz,
            cx - hx, cy + hy, cz + hz,
            0f, 0f, 1f, color, surfaceType
        )
        // Back (-Z)
        addQuad(
            cx + hx, cy - hy, cz - hz,
            cx - hx, cy - hy, cz - hz,
            cx - hx, cy + hy, cz - hz,
            cx + hx, cy + hy, cz - hz,
            0f, 0f, -1f, color, surfaceType
        )
        // Left (-X)
        addQuad(
            cx - hx, cy - hy, cz - hz,
            cx - hx, cy - hy, cz + hz,
            cx - hx, cy + hy, cz + hz,
            cx - hx, cy + hy, cz - hz,
            -1f, 0f, 0f, sideCol, surfaceType
        )
        // Right (+X)
        addQuad(
            cx + hx, cy - hy, cz + hz,
            cx + hx, cy - hy, cz - hz,
            cx + hx, cy + hy, cz - hz,
            cx + hx, cy + hy, cz + hz,
            1f, 0f, 0f, sideCol, surfaceType
        )
        // Top (+Y)
        addQuad(
            cx - hx, cy + hy, cz + hz,
            cx + hx, cy + hy, cz + hz,
            cx + hx, cy + hy, cz - hz,
            cx - hx, cy + hy, cz - hz,
            0f, 1f, 0f, topColor, surfaceType
        )
        // Bottom (-Y)
        addQuad(
            cx - hx, cy - hy, cz - hz,
            cx + hx, cy - hy, cz - hz,
            cx + hx, cy - hy, cz + hz,
            cx - hx, cy - hy, cz + hz,
            0f, -1f, 0f, sideCol, 0f
        )
    }

    /**
     * Adds a 3D wheel cylinder oriented along the X axis (rolling along Z).
     */
    fun addWheel(
        cx: Float, cy: Float, cz: Float,
        radius: Float, width: Float,
        tireColor: Color = Color(0xFF14171C),
        rimColor: Color = Color(0xFFB0BEC5),
        segments: Int = 12
    ) {
        val hw = width * 0.5f
        for (i in 0 until segments) {
            val a1 = (2.0 * PI * i / segments)
            val a2 = (2.0 * PI * (i + 1) / segments)
            val y1 = (sin(a1) * radius).toFloat()
            val z1 = (cos(a1) * radius).toFloat()
            val y2 = (sin(a2) * radius).toFloat()
            val z2 = (cos(a2) * radius).toFloat()

            val ny = ((y1 + y2) * 0.5f) / radius
            val nz = ((z1 + z2) * 0.5f) / radius

            // Tire tread
            addQuad(
                cx - hw, cy + y1, cz + z1,
                cx + hw, cy + y1, cz + z1,
                cx + hw, cy + y2, cz + z2,
                cx - hw, cy + y2, cz + z2,
                0f, ny, nz, tireColor, 0f
            )
            // Outer hubcap face
            val rimR = 0.55f
            addTriangle(
                cx + hw + 0.01f, cy, cz,
                cx + hw + 0.01f, cy + y1 * rimR, cz + z1 * rimR,
                cx + hw + 0.01f, cy + y2 * rimR, cz + z2 * rimR,
                1f, 0f, 0f, rimColor, 0f
            )
            addTriangle(
                cx - hw - 0.01f, cy, cz,
                cx - hw - 0.01f, cy + y2 * rimR, cz + z2 * rimR,
                cx - hw - 0.01f, cy + y1 * rimR, cz + z1 * rimR,
                -1f, 0f, 0f, rimColor, 0f
            )
        }
    }

    /**
     * Adds a 3D cone/pyramid (for trees or bridge accents).
     */
    fun addCone(
        cx: Float, bottomY: Float, cz: Float,
        radius: Float, height: Float,
        color: Color,
        sides: Int = 7
    ) {
        val topY = bottomY + height
        for (i in 0 until sides) {
            val a1 = 2.0 * PI * i / sides
            val a2 = 2.0 * PI * (i + 1) / sides
            val x1 = (cos(a1) * radius).toFloat()
            val z1 = (sin(a1) * radius).toFloat()
            val x2 = (cos(a2) * radius).toFloat()
            val z2 = (sin(a2) * radius).toFloat()
            val nx = ((x1 + x2) * 0.5f) / radius
            val nz = ((z1 + z2) * 0.5f) / radius
            addTriangle(
                cx + x1, bottomY, cz + z1,
                cx + x2, bottomY, cz + z2,
                cx, topY, cz,
                nx, 0.45f, nz, color, 0f
            )
        }
    }

    fun toFloatBuffer(): FloatBuffer {
        val bb = ByteBuffer.allocateDirect(floatCount * 4).order(ByteOrder.nativeOrder())
        val fb = bb.asFloatBuffer()
        fb.put(data, 0, floatCount)
        fb.position(0)
        return fb
    }
}
