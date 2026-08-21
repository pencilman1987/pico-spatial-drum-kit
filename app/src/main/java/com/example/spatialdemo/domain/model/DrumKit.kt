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
    private const val PLAYABLE_RADIUS_INSET = 0.94f

    val baseSurfaces =
        listOf(
            measuredSurface(
                DrumId.CRASH,
                sourceCenter = Vector3(0.38362f, 0.44619f, 0.12016f),
                sourceNormal = Vector3(-0.20901f, 0.88232f, -0.42170f),
                sourceRadius = 0.14627f,
                depth = 0.025f,
                color = Color4.fromSRGBHex("E7B84BFF"),
                isCymbal = true,
            ),
            measuredSurface(
                DrumId.RIDE,
                sourceCenter = Vector3(-0.53527f, 0.26424f, -0.05126f),
                sourceNormal = Vector3(0.01883f, 0.99541f, -0.09385f),
                sourceRadius = 0.21908f,
                depth = 0.025f,
                color = Color4.fromSRGBHex("D7A83FFF"),
                isCymbal = true,
            ),
            measuredSurface(
                DrumId.HI_HAT,
                sourceCenter = Vector3(0.51506f, 0.21747f, -0.08427f),
                sourceNormal = Vector3(-0.11893f, 0.98839f, -0.09453f),
                sourceRadius = 0.12225f,
                depth = 0.035f,
                color = Color4.fromSRGBHex("C9942FFF"),
                isCymbal = true,
            ),
            measuredSurface(
                DrumId.TOM_HIGH,
                sourceCenter = Vector3(0.10147f, 0.31324f, 0.10433f),
                sourceNormal = Vector3(0.09852f, 0.73700f, -0.66868f),
                sourceRadius = 0.09058f,
                depth = 0.16f,
                color = Color4.fromSRGBHex("D8DEE7FF"),
            ),
            measuredSurface(
                DrumId.TOM_MID,
                sourceCenter = Vector3(-0.13972f, 0.32392f, 0.06502f),
                sourceNormal = Vector3(0.25739f, 0.56072f, -0.78698f),
                sourceRadius = 0.09800f,
                depth = 0.18f,
                color = Color4.fromSRGBHex("BFC8D5FF"),
            ),
            measuredSurface(
                DrumId.SNARE,
                sourceCenter = Vector3(0.22663f, 0.11986f, -0.12977f),
                sourceNormal = Vector3(0.03178f, 0.99947f, 0.00743f),
                sourceRadius = 0.12852f,
                depth = 0.17f,
                color = Color4.fromSRGBHex("EDF0F4FF"),
            ),
            measuredSurface(
                DrumId.TOM_FLOOR,
                sourceCenter = Vector3(-0.29982f, 0.08210f, -0.20105f),
                sourceNormal = Vector3(0.13106f, 0.99137f, 0.00104f),
                sourceRadius = 0.13353f,
                depth = 0.30f,
                color = Color4.fromSRGBHex("9DA8B7FF"),
            ),
            measuredSurface(
                DrumId.KICK,
                sourceCenter = Vector3(-0.02253f, -0.10502f, -0.02912f),
                sourceNormal = Vector3(0.10564f, -0.01505f, -0.99429f),
                sourceRadius = 0.20638f,
                depth = 0.28f,
                color = Color4.fromSRGBHex("27303EFF"),
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

    private fun measuredSurface(
        id: DrumId,
        sourceCenter: Vector3,
        sourceNormal: Vector3,
        sourceRadius: Float,
        depth: Float,
        color: Color4,
        isCymbal: Boolean = false,
    ) =
        DrumSurface(
            id = id,
            center = DrumKitModelPlacement.sourcePointInStage(sourceCenter),
            normal = DrumKitModelPlacement.sourceDirectionInStage(sourceNormal),
            radius = sourceRadius * DrumKitModelPlacement.scale * PLAYABLE_RADIUS_INSET,
            depth = depth,
            color = color,
            isCymbal = isCymbal,
        )
}
