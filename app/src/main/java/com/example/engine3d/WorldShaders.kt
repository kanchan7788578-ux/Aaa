package com.example.engine3d

import android.opengl.GLES20

object WorldShaders {

    const val VERTEX_SHADER = """
        uniform mat4 uMvpMatrix;
        uniform mat4 uModelMatrix;
        attribute vec3 aPosition;
        attribute vec3 aNormal;
        attribute vec3 aColor;
        attribute float aSurfaceType;

        varying vec3 vWorldPos;
        varying vec3 vNormal;
        varying vec3 vColor;
        varying float vSurfaceType;

        void main() {
            vec4 worldPos = uModelMatrix * vec4(aPosition, 1.0);
            vWorldPos = worldPos.xyz;
            vNormal = normalize((uModelMatrix * vec4(aNormal, 0.0)).xyz);
            vColor = aColor;
            vSurfaceType = aSurfaceType;
            gl_Position = uMvpMatrix * vec4(aPosition, 1.0);
        }
    """

    // Surface Types:
    // 0.0 = Standard shaded 3D geometry (bus body, trees, signs, bridge steel)
    // 1.0 = Asphalt road (procedural aggregate grain + wet rain sheen + headlight pool)
    // 2.0 = Brahmaputra River water (animated wave shimmer)
    // 3.0 = Building facade (procedural window grid, lit at night/sunset)
    // 4.0 = Emissive (headlights, brake lights, streetlamp bulbs, LED board)
    const val FRAGMENT_SHADER = """
        precision mediump float;

        uniform vec3 uCameraPos;
        uniform vec3 uSunDir;
        uniform vec3 uAmbientColor;
        uniform vec3 uSunColor;
        uniform vec3 uFogColor;
        uniform float uFogDensity;
        uniform float uTime;
        uniform float uIsNightOrSunset;
        uniform float uIsRain;
        uniform float uHeadlightsOn;
        uniform vec3 uHeadlightPos;
        uniform vec3 uHeadlightDir;

        varying vec3 vWorldPos;
        varying vec3 vNormal;
        varying vec3 vColor;
        varying float vSurfaceType;

        float hash21(vec2 p) {
            p = fract(p * vec2(123.34, 456.21));
            p += dot(p, p + 45.32);
            return fract(p.x * p.y);
        }

        void main() {
            vec3 baseColor = vColor;
            vec3 normal = normalize(vNormal);

            // Emissive lights ignore fog dimming mostly and glow brightly
            if (vSurfaceType > 3.5) {
                float dist = length(vWorldPos - uCameraPos);
                float fog = clamp(exp(-dist * uFogDensity * 0.45), 0.25, 1.0);
                vec3 col = mix(uFogColor, baseColor, fog);
                gl_FragColor = vec4(col, 1.0);
                return;
            }

            // Procedural Asphalt Detail (SurfaceType ~ 1.0)
            if (vSurfaceType > 0.5 && vSurfaceType < 1.5) {
                float grain = hash21(floor(vWorldPos.xz * 7.5)) * 0.09 - 0.045;
                baseColor += vec3(grain);
                if (uIsRain > 0.5) {
                    // Wet glossy road reflection
                    vec3 viewDir = normalize(uCameraPos - vWorldPos);
                    vec3 halfVec = normalize(uSunDir + viewDir);
                    float spec = pow(max(dot(normal, halfVec), 0.0), 28.0);
                    baseColor = baseColor * 0.78 + vec3(0.20, 0.26, 0.34) * spec;
                }
            }
            // Procedural River Water Ripples (SurfaceType ~ 2.0)
            else if (vSurfaceType > 1.5 && vSurfaceType < 2.5) {
                float wave1 = sin(vWorldPos.x * 0.9 + uTime * 2.4) * cos(vWorldPos.z * 0.6 - uTime * 1.8);
                float wave2 = sin(vWorldPos.x * 2.1 - uTime * 3.1);
                baseColor += vec3(0.04, 0.09, 0.14) * (wave1 + wave2 * 0.5);
            }
            // Procedural Building Windows (SurfaceType ~ 3.0)
            else if (vSurfaceType > 2.5 && vSurfaceType < 3.5) {
                float wx = fract((vWorldPos.x + vWorldPos.z) * 0.45);
                float wy = fract(vWorldPos.y * 0.38);
                if (wx > 0.25 && wx < 0.75 && wy > 0.28 && wy < 0.76 && vWorldPos.y > 1.2) {
                    float seed = hash21(floor(vec2(vWorldPos.x + vWorldPos.z, vWorldPos.y)));
                    if (uIsNightOrSunset > 0.5 && seed > 0.28) {
                        baseColor = vec3(1.0, 0.84, 0.45);
                    } else {
                        baseColor = vec3(0.10, 0.16, 0.22);
                    }
                }
            }

            // Directional Sun/Moon Diffuse Lighting
            float diff = max(dot(normal, normalize(uSunDir)), 0.0);
            vec3 litColor = baseColor * (uAmbientColor + uSunColor * diff);

            // Dynamic Bus Headlight Cone on Road & Nearby Geometry
            if (uHeadlightsOn > 0.5) {
                vec3 toFrag = vWorldPos - uHeadlightPos;
                float distToBus = length(toFrag);
                if (distToBus > 1.5 && distToBus < 58.0) {
                    vec3 dirToFrag = toFrag / distToBus;
                    float align = dot(dirToFrag, normalize(uHeadlightDir));
                    if (align > 0.78) {
                        float cone = smoothstep(0.78, 0.95, align);
                        float atten = 1.0 - smoothstep(12.0, 58.0, distToBus);
                        litColor += baseColor * vec3(1.05, 0.94, 0.72) * cone * atten * 0.95;
                    }
                }
            }

            // Atmospheric Horizon Fog
            float camDist = length(vWorldPos - uCameraPos);
            float fogFactor = clamp(exp(-camDist * uFogDensity), 0.0, 1.0);
            vec3 finalColor = mix(uFogColor, litColor, fogFactor);

            gl_FragColor = vec4(finalColor, 1.0);
        }
    """

    const val SKY_VERTEX_SHADER = """
        attribute vec2 aPos;
        attribute vec3 aColor;
        varying vec3 vColor;
        void main() {
            vColor = aColor;
            gl_Position = vec4(aPos, 0.999, 1.0);
        }
    """

    const val SKY_FRAGMENT_SHADER = """
        precision mediump float;
        varying vec3 vColor;
        void main() {
            gl_FragColor = vec4(vColor, 1.0);
        }
    """

    fun compileProgram(vertSrc: String, fragSrc: String): Int {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertSrc)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragSrc)
        val prog = GLES20.glCreateProgram()
        GLES20.glAttachShader(prog, vs)
        GLES20.glAttachShader(prog, fs)
        GLES20.glLinkProgram(prog)
        GLES20.glDeleteShader(vs)
        GLES20.glDeleteShader(fs)
        return prog
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, src)
        GLES20.glCompileShader(shader)
        return shader
    }
}
