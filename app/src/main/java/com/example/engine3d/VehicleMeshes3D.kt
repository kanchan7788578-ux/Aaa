package com.example.engine3d

import androidx.compose.ui.graphics.Color
import com.example.domain.model.BusModel
import com.example.domain.model.TrafficType
import com.example.engine.PropKind
import java.nio.FloatBuffer

data class GpuMesh(
    val buffer: FloatBuffer,
    val vertexCount: Int
)

object VehicleMeshes3D {

    fun buildBusMesh(bus: BusModel): GpuMesh {
        val mb = MeshBuilder(6000)
        val w = 2.65f
        val len = 11.4f
        val bodyH = 2.55f
        val floorY = 0.55f

        // Ground shadow slab (flat dark quad slightly above asphalt)
        mb.addQuad(
            -w * 0.56f, 0.04f, -len * 0.52f,
            w * 0.56f, 0.04f, -len * 0.52f,
            w * 0.56f, 0.04f, len * 0.52f,
            -w * 0.56f, 0.04f, len * 0.52f,
            0f, 1f, 0f, Color(0xFF06080A), 0f
        )

        // Lower chassis skirt
        mb.addBox(0f, floorY + 0.35f, 0f, w, 0.70f, len, Color(0xFF1E242B))

        // Main lower livery body
        mb.addBox(0f, floorY + 0.95f, 0f, w * 1.01f, 0.65f, len * 0.99f, bus.primaryColor)

        // Secondary livery stripe band
        mb.addBox(0f, floorY + 1.40f, 0f, w * 1.02f, 0.30f, len * 0.995f, bus.secondaryColor)
        mb.addBox(0f, floorY + 1.56f, 0f, w * 1.025f, 0.08f, len * 0.995f, bus.accentColor)

        // Upper glass cabin core
        mb.addBox(0f, floorY + 2.02f, -0.05f, w * 0.97f, 0.85f, len * 0.94f, Color(0xFF10202D))

        // Roof cap & AC pod
        mb.addBox(0f, floorY + bodyH + 0.02f, 0f, w * 0.95f, 0.22f, len * 0.95f, bus.primaryColor, topColor = bus.secondaryColor)
        mb.addBox(0f, floorY + bodyH + 0.24f, -0.4f, 1.45f, 0.24f, 3.8f, Color(0xFFE2E8F0))

        // Side Window Pillars for realistic coach look
        for (p in -2..2) {
            val pz = p * 2.15f
            mb.addBox(-w * 0.49f, floorY + 2.02f, pz, 0.06f, 0.86f, 0.16f, bus.primaryColor)
            mb.addBox(w * 0.49f, floorY + 2.02f, pz, 0.06f, 0.86f, 0.16f, bus.primaryColor)
        }

        // Side Mirrors
        mb.addBox(-w * 0.58f, floorY + 1.95f, -len * 0.47f, 0.14f, 0.42f, 0.18f, Color(0xFF263238))
        mb.addBox(w * 0.58f, floorY + 1.95f, -len * 0.47f, 0.14f, 0.42f, 0.18f, Color(0xFF263238))

        // Front & Rear Chrome/Dark Bumpers
        mb.addBox(0f, floorY + 0.18f, -len * 0.505f, w * 1.02f, 0.34f, 0.22f, Color(0xFF2A323B))
        mb.addBox(0f, floorY + 0.18f, len * 0.505f, w * 1.02f, 0.34f, 0.22f, Color(0xFF2A323B))

        // Rear Green License Plate & Glowing Tail/Brake Lights (+Z is back of bus facing camera)
        mb.addBox(0f, floorY + 0.56f, len * 0.505f, 0.65f, 0.22f, 0.06f, Color(0xFF1B5E20))
        mb.addBox(-w * 0.37f, floorY + 0.58f, len * 0.505f, 0.42f, 0.22f, 0.08f, Color(0xFFFF1744), surfaceType = 4f)
        mb.addBox(w * 0.37f, floorY + 0.58f, len * 0.505f, 0.42f, 0.22f, 0.08f, Color(0xFFFF1744), surfaceType = 4f)

        // Glowing Amber LED Rear Destination Bar
        mb.addBox(0f, floorY + 2.38f, len * 0.485f, 1.35f, 0.18f, 0.06f, Color(0xFFFFCA28), surfaceType = 4f)

        // Glowing Front Headlights (-Z is forward!)
        mb.addBox(-w * 0.36f, floorY + 0.52f, -len * 0.505f, 0.44f, 0.24f, 0.08f, Color(0xFFFFF59D), surfaceType = 4f)
        mb.addBox(w * 0.36f, floorY + 0.52f, -len * 0.505f, 0.44f, 0.24f, 0.08f, Color(0xFFFFF59D), surfaceType = 4f)

        // 4 Wheels (Front & Dual Rear)
        val wheelR = 0.54f
        val wheelW = 0.38f
        mb.addWheel(-w * 0.46f, wheelR, -len * 0.32f, wheelR, wheelW)
        mb.addWheel(w * 0.46f, wheelR, -len * 0.32f, wheelR, wheelW)
        mb.addWheel(-w * 0.46f, wheelR, len * 0.30f, wheelR, wheelW * 1.25f)
        mb.addWheel(w * 0.46f, wheelR, len * 0.30f, wheelR, wheelW * 1.25f)

        return GpuMesh(mb.toFloatBuffer(), mb.vertexCount)
    }

    fun buildTrafficMesh(type: TrafficType, colorVariant: Int): GpuMesh {
        val mb = MeshBuilder(2500)
        when (type) {
            TrafficType.CNG_AUTO -> {
                val w = 1.35f
                val l = 2.65f
                mb.addBox(0f, 0.03f, 0f, w * 1.05f, 0.02f, l * 1.05f, Color(0xFF07090B))
                mb.addBox(0f, 0.58f, 0f, w, 0.58f, l, Color(0xFF0E7C4E)) // Green body
                mb.addBox(0f, 1.18f, 0.1f, w * 0.94f, 0.68f, l * 0.85f, Color(0xFF2D3830)) // Khaki canopy
                mb.addBox(-0.42f, 0.55f, l * 0.505f, 0.18f, 0.14f, 0.05f, Color(0xFFFF1744), surfaceType = 4f)
                mb.addBox(0.42f, 0.55f, l * 0.505f, 0.18f, 0.14f, 0.05f, Color(0xFFFF1744), surfaceType = 4f)
                mb.addWheel(0f, 0.28f, -l * 0.38f, 0.28f, 0.20f)
                mb.addWheel(-w * 0.48f, 0.28f, l * 0.35f, 0.28f, 0.20f)
                mb.addWheel(w * 0.48f, 0.28f, l * 0.35f, 0.28f, 0.20f)
            }

            TrafficType.EASY_BIKE -> {
                val bodyCol = if (colorVariant % 2 == 0) Color(0xFF0277BD) else Color(0xFFF9A825)
                val w = 1.45f
                val l = 2.9f
                mb.addBox(0f, 0.03f, 0f, w * 1.05f, 0.02f, l * 1.05f, Color(0xFF07090B))
                mb.addBox(0f, 0.58f, 0f, w, 0.55f, l, bodyCol)
                mb.addBox(0f, 1.58f, 0f, w * 0.96f, 0.14f, l * 0.96f, bodyCol)
                // Four thin roof pillars
                mb.addBox(-w * 0.45f, 1.15f, -l * 0.44f, 0.08f, 0.8f, 0.08f, Color(0xFFE0E0E0))
                mb.addBox(w * 0.45f, 1.15f, -l * 0.44f, 0.08f, 0.8f, 0.08f, Color(0xFFE0E0E0))
                mb.addBox(-w * 0.45f, 1.15f, l * 0.44f, 0.08f, 0.8f, 0.08f, Color(0xFFE0E0E0))
                mb.addBox(w * 0.45f, 1.15f, l * 0.44f, 0.08f, 0.8f, 0.08f, Color(0xFFE0E0E0))
                mb.addBox(-0.46f, 0.55f, l * 0.505f, 0.16f, 0.12f, 0.05f, Color(0xFFFF3D00), surfaceType = 4f)
                mb.addBox(0.46f, 0.55f, l * 0.505f, 0.16f, 0.12f, 0.05f, Color(0xFFFF3D00), surfaceType = 4f)
                mb.addWheel(0f, 0.28f, -l * 0.40f, 0.28f, 0.18f)
                mb.addWheel(-w * 0.48f, 0.28f, l * 0.36f, 0.28f, 0.18f)
                mb.addWheel(w * 0.48f, 0.28f, l * 0.36f, 0.28f, 0.18f)
            }

            TrafficType.RICKSHAW -> {
                val hoodCols = listOf(Color(0xFFE91E63), Color(0xFFFF5722), Color(0xFF009688))
                val hood = hoodCols[colorVariant % hoodCols.size]
                mb.addBox(0f, 0.03f, 0f, 1.15f, 0.02f, 2.2f, Color(0xFF07090B))
                mb.addBox(0f, 0.65f, 0.2f, 1.05f, 0.42f, 1.1f, Color(0xFFFFCA28))
                mb.addBox(0f, 1.25f, 0.35f, 1.02f, 0.82f, 0.85f, hood)
                mb.addWheel(-0.56f, 0.42f, 0.35f, 0.42f, 0.08f)
                mb.addWheel(0.56f, 0.42f, 0.35f, 0.42f, 0.08f)
                mb.addWheel(0f, 0.36f, -0.82f, 0.36f, 0.08f)
            }

            TrafficType.TRUCK_BD -> {
                val truckCol = if (colorVariant % 2 == 0) Color(0xFF1B5E20) else Color(0xFFBF360C)
                val w = 2.45f
                val l = 7.4f
                mb.addBox(0f, 0.03f, 0f, w * 1.08f, 0.02f, l * 1.05f, Color(0xFF07090B))
                // Front cab
                mb.addBox(0f, 1.55f, -l * 0.34f, w * 0.94f, 1.85f, 2.2f, truckCol)
                mb.addBox(0f, 1.90f, -l * 0.485f, w * 0.84f, 0.72f, 0.10f, Color(0xFF12202B))
                // Cargo box with Truck Art yellow-pink bands
                mb.addBox(0f, 2.05f, 0.95f, w, 2.35f, 4.9f, truckCol)
                mb.addBox(0f, 1.65f, 3.42f, w * 0.85f, 1.20f, 0.06f, Color(0xFFFFCA28))
                mb.addBox(0f, 1.65f, 3.44f, w * 0.45f, 0.65f, 0.06f, Color(0xFFE91E63))
                // Taillights & headlights
                mb.addBox(-0.85f, 0.85f, 3.44f, 0.35f, 0.20f, 0.06f, Color(0xFFFF1744), surfaceType = 4f)
                mb.addBox(0.85f, 0.85f, 3.44f, 0.35f, 0.20f, 0.06f, Color(0xFFFF1744), surfaceType = 4f)
                mb.addBox(-0.82f, 0.82f, -l * 0.495f, 0.34f, 0.22f, 0.06f, Color(0xFFFFF59D), surfaceType = 4f)
                mb.addBox(0.82f, 0.82f, -l * 0.495f, 0.34f, 0.22f, 0.06f, Color(0xFFFFF59D), surfaceType = 4f)
                mb.addWheel(-w * 0.46f, 0.50f, -l * 0.32f, 0.50f, 0.34f)
                mb.addWheel(w * 0.46f, 0.50f, -l * 0.32f, 0.50f, 0.34f)
                mb.addWheel(-w * 0.46f, 0.50f, l * 0.28f, 0.50f, 0.40f)
                mb.addWheel(w * 0.46f, 0.50f, l * 0.28f, 0.50f, 0.40f)
            }

            TrafficType.MICROBUS, TrafficType.SEDAN -> {
                val carCols = listOf(Color(0xFFCFD8DC), Color(0xFF263238), Color(0xFF8D6E63), Color(0xFF1E88E5))
                val c = carCols[colorVariant % carCols.size]
                val isVan = type == TrafficType.MICROBUS
                val w = if (isVan) 1.90f else 1.80f
                val l = if (isVan) 4.80f else 4.35f
                val h = if (isVan) 1.35f else 0.92f
                mb.addBox(0f, 0.03f, 0f, w * 1.06f, 0.02f, l * 1.04f, Color(0xFF07090B))
                mb.addBox(0f, 0.65f, 0f, w, 0.68f, l, c)
                mb.addBox(0f, 0.98f + h * 0.28f, if (isVan) 0f else 0.2f, w * 0.88f, h * 0.62f, l * (if (isVan) 0.82f else 0.54f), Color(0xFF14222D), topColor = c)
                mb.addBox(-w * 0.36f, 0.66f, l * 0.505f, 0.32f, 0.16f, 0.05f, Color(0xFFFF1744), surfaceType = 4f)
                mb.addBox(w * 0.36f, 0.66f, l * 0.505f, 0.32f, 0.16f, 0.05f, Color(0xFFFF1744), surfaceType = 4f)
                mb.addBox(-w * 0.36f, 0.62f, -l * 0.505f, 0.30f, 0.16f, 0.05f, Color(0xFFFFF59D), surfaceType = 4f)
                mb.addBox(w * 0.36f, 0.62f, -l * 0.505f, 0.30f, 0.16f, 0.05f, Color(0xFFFFF59D), surfaceType = 4f)
                mb.addWheel(-w * 0.45f, 0.33f, -l * 0.32f, 0.33f, 0.24f)
                mb.addWheel(w * 0.45f, 0.33f, -l * 0.32f, 0.33f, 0.24f)
                mb.addWheel(-w * 0.45f, 0.33f, l * 0.32f, 0.33f, 0.24f)
                mb.addWheel(w * 0.45f, 0.33f, l * 0.32f, 0.33f, 0.24f)
            }

            TrafficType.ONCOMING_BUS -> {
                val busCol = if (colorVariant % 2 == 0) Color(0xFFC62828) else Color(0xFF00695C)
                val w = 2.55f
                val l = 10.6f
                mb.addBox(0f, 0.03f, 0f, w * 1.08f, 0.02f, l * 1.04f, Color(0xFF07090B))
                mb.addBox(0f, 1.45f, 0f, w, 1.75f, l, busCol)
                mb.addBox(0f, 2.55f, 0f, w * 0.96f, 0.95f, l * 0.94f, Color(0xFF10202D), topColor = Color(0xFFECEFF1))
                // Notice: Oncoming buses face +Z toward the player!
                mb.addBox(-w * 0.36f, 1.05f, l * 0.505f, 0.42f, 0.24f, 0.08f, Color(0xFFFFF59D), surfaceType = 4f)
                mb.addBox(w * 0.36f, 1.05f, l * 0.505f, 0.42f, 0.24f, 0.08f, Color(0xFFFFF59D), surfaceType = 4f)
                mb.addBox(0f, 2.95f, l * 0.48f, 1.35f, 0.20f, 0.06f, Color(0xFFFFCA28), surfaceType = 4f)
                mb.addWheel(-w * 0.46f, 0.52f, -l * 0.31f, 0.52f, 0.36f)
                mb.addWheel(w * 0.46f, 0.52f, -l * 0.31f, 0.52f, 0.36f)
                mb.addWheel(-w * 0.46f, 0.52f, l * 0.31f, 0.52f, 0.36f)
                mb.addWheel(w * 0.46f, 0.52f, l * 0.31f, 0.52f, 0.36f)
            }
        }
        return GpuMesh(mb.toFloatBuffer(), mb.vertexCount)
    }

    fun buildPropMesh(kind: PropKind, variant: Int): GpuMesh {
        val mb = MeshBuilder(1200)
        when (kind) {
            PropKind.BANYAN_TREE -> {
                mb.addBox(0f, 2.0f, 0f, 0.72f, 4.0f, 0.72f, Color(0xFF4E342E))
                mb.addCone(0f, 3.3f, 0f, 3.2f, 3.4f, Color(0xFF1B5E20), sides = 8)
                mb.addCone(0f, 5.0f, 0f, 2.5f, 2.8f, Color(0xFF2E7D32), sides = 8)
                mb.addCone(0f, 6.4f, 0f, 1.6f, 2.2f, Color(0xFF388E3C), sides = 7)
            }

            PropKind.PALM_TREE -> {
                mb.addBox(0f, 4.0f, 0f, 0.40f, 8.0f, 0.40f, Color(0xFF5D4037))
                mb.addCone(0f, 7.6f, 0f, 2.4f, 1.8f, Color(0xFF2E7D32), sides = 6)
                mb.addCone(0f, 8.2f, 0f, 1.8f, 1.6f, Color(0xFF388E3C), sides = 6)
            }

            PropKind.CITY_BUILDING -> {
                val h = 10f + (variant % 4) * 3.5f
                val w = 7.5f
                val cols = listOf(Color(0xFF78909C), Color(0xFFA1887F), Color(0xFF8D99AE), Color(0xFFBCAAA4))
                val c = cols[variant % cols.size]
                mb.addBox(0f, h * 0.5f, 0f, w, h, 6.5f, c, surfaceType = 3f, topColor = Color(0xFF455A64))
                mb.addBox(0f, h + 0.6f, 0f, 1.6f, 1.2f, 1.6f, Color(0xFF263238))
            }

            PropKind.SHOP_STALL -> {
                mb.addBox(0f, 1.6f, 0f, 4.6f, 3.2f, 3.6f, Color(0xFF455A64))
                mb.addBox(0f, 3.35f, 0f, 5.2f, 0.30f, 4.2f, Color(0xFF1565C0))
                mb.addBox(0f, 1.4f, 1.82f, 3.2f, 1.6f, 0.08f, Color(0xFFFFB74D), surfaceType = 4f)
            }

            PropKind.ELECTRIC_POLE -> {
                mb.addBox(0f, 4.2f, 0f, 0.24f, 8.4f, 0.24f, Color(0xFF546E7A))
                mb.addBox(0.8f, 8.1f, 0f, 1.8f, 0.18f, 0.18f, Color(0xFF78909C))
                mb.addBox(1.5f, 7.92f, 0f, 0.42f, 0.24f, 0.42f, Color(0xFFFFF59D), surfaceType = 4f)
            }

            PropKind.HIGHWAY_SIGN -> {
                mb.addBox(-1.6f, 2.8f, 0f, 0.22f, 5.6f, 0.22f, Color(0xFF90A4AE))
                mb.addBox(1.6f, 2.8f, 0f, 0.22f, 5.6f, 0.22f, Color(0xFF90A4AE))
                mb.addBox(0f, 4.8f, 0f, 4.6f, 1.8f, 0.16f, Color(0xFF00695C))
                mb.addBox(0f, 4.8f, 0.09f, 4.3f, 1.5f, 0.04f, Color(0xFFECEFF1))
            }

            PropKind.BRIDGE_TRUSS -> {
                val steel = Color(0xFF546E7A)
                mb.addBox(0f, 4.2f, 0f, 0.55f, 8.4f, 0.55f, steel)
                mb.addBox(0f, 2.4f, 0f, 0.35f, 0.45f, 6.0f, steel)
                mb.addBox(0f, 8.2f, 0f, 0.45f, 0.55f, 6.0f, steel)
            }

            PropKind.BUS_SHELTER -> {
                mb.addBox(0f, 2.9f, 0f, 3.6f, 0.35f, 7.5f, Color(0xFF0E7C4E))
                mb.addBox(-1.4f, 1.4f, -3.2f, 0.28f, 2.8f, 0.28f, Color(0xFF37474F))
                mb.addBox(-1.4f, 1.4f, 3.2f, 0.28f, 2.8f, 0.28f, Color(0xFF37474F))
                mb.addBox(0f, 0.6f, 0f, 1.2f, 0.45f, 4.8f, Color(0xFFFFCA28))
            }
        }
        return GpuMesh(mb.toFloatBuffer(), mb.vertexCount)
    }
}
