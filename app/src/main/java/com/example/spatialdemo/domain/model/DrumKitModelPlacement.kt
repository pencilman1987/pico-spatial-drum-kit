package com.example.spatialdemo.domain.model

import com.pico.spatial.core.math.Vector3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Measured placement data for the production GLB in Stage space. */
object DrumKitModelPlacement {
    const val scale = 1.4f
    const val yawDegrees = 201.79572f

    // Measured from the black three-legged throne in drum_kit_refined.glb.
    // The previous value accidentally referenced the floor-tom head.
    val sourceSeatCenter = Vector3(0.19720f, 0.13590f, -0.45630f)

    // Midpoint of the two rack-tom playing surfaces. It defines the direction a seated
    // player naturally faces, and is rotated onto Stage -Z.
    val sourcePlayingCenter = Vector3(-0.01913f, 0.31858f, 0.08468f)

    // Keeps the source floor on Stage Y=0 and the real throne center at Stage X/Z=0.
    val modelPosition = Vector3(0.01915f, 0.50780f, -0.69566f)

    /** Converts a measured source-model point through the production transform. */
    fun sourcePointInStage(sourcePoint: Vector3): Vector3 = rotateSource(sourcePoint) * scale + modelPosition

    /** Converts a measured source-model direction without applying translation or scale. */
    fun sourceDirectionInStage(sourceDirection: Vector3): Vector3 = rotateSource(sourceDirection).normalize()

    private fun rotateSource(source: Vector3): Vector3 {
        val radians = yawDegrees * PI.toFloat() / 180f
        val cosine = cos(radians)
        val sine = sin(radians)
        return Vector3(
            cosine * source.x + sine * source.z,
            source.y,
            -sine * source.x + cosine * source.z,
        )
    }
}
