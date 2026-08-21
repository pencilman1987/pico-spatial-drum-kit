package com.example.spatialdemo.domain.model

import com.example.spatialdemo.calibration.CalibrationProfile
import com.pico.spatial.core.math.Color4
import com.pico.spatial.core.math.Vector3

enum class DrumId(val displayName: String) {
    CRASH("Crash"),
    RIDE("Ride"),
    HI_HAT("Hi-Hat"),
    TOM_HIGH("Tom 1"),
    TOM_MID("Tom 2"),
    SNARE("Snare"),
    TOM_FLOOR("Floor Tom"),
    KICK("Kick"),
}

data class DrumSurface(
    val id: DrumId,
    val center: Vector3,
    val normal: Vector3,
    val radius: Float,
    val depth: Float,
    val color: Color4,
    val isCymbal: Boolean = false,
)

object DrumKit {
    val baseSurfaces =
        listOf(
            DrumSurface(
                DrumId.CRASH,
                Vector3(-0.62f, 1.38f, -0.92f),
                Vector3.UP,
                0.22f,
                0.025f,
                Color4.fromSRGBHex("E7B84BFF"),
                isCymbal = true,
            ),
            DrumSurface(
                DrumId.RIDE,
                Vector3(0.62f, 1.36f, -0.96f),
                Vector3.UP,
                0.24f,
                0.025f,
                Color4.fromSRGBHex("D7A83FFF"),
                isCymbal = true,
            ),
            DrumSurface(
                DrumId.HI_HAT,
                Vector3(-0.64f, 1.04f, -0.58f),
                Vector3.UP,
                0.18f,
                0.035f,
                Color4.fromSRGBHex("C9942FFF"),
                isCymbal = true,
            ),
            DrumSurface(
                DrumId.TOM_HIGH,
                Vector3(-0.25f, 1.16f, -0.86f),
                Vector3.UP,
                0.18f,
                0.16f,
                Color4.fromSRGBHex("D8DEE7FF"),
            ),
            DrumSurface(
                DrumId.TOM_MID,
                Vector3(0.24f, 1.14f, -0.88f),
                Vector3.UP,
                0.19f,
                0.18f,
                Color4.fromSRGBHex("BFC8D5FF"),
            ),
            DrumSurface(
                DrumId.SNARE,
                Vector3(-0.30f, 0.86f, -0.54f),
                Vector3.UP,
                0.20f,
                0.17f,
                Color4.fromSRGBHex("EDF0F4FF"),
            ),
            DrumSurface(
                DrumId.TOM_FLOOR,
                Vector3(0.52f, 0.83f, -0.58f),
                Vector3.UP,
                0.22f,
                0.30f,
                Color4.fromSRGBHex("9DA8B7FF"),
            ),
            DrumSurface(
                DrumId.KICK,
                Vector3(0.08f, 0.48f, -0.93f),
                Vector3.BACK,
                0.28f,
                0.28f,
                Color4.fromSRGBHex("27303EFF"),
            ),
        )

    val surfaces: List<DrumSurface>
        get() = baseSurfaces

    fun surfaces(profile: CalibrationProfile): List<DrumSurface> =
        baseSurfaces.map { surface ->
            surface.copy(
                center = surface.center + profile.kitOffset + (profile.surfaceOffsets[surface.id] ?: Vector3.ZERO),
            )
        }
}
