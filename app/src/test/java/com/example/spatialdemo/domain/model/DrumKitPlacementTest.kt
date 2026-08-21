package com.example.spatialdemo.domain.model

import com.example.spatialdemo.calibration.CalibrationProfile
import com.pico.spatial.core.math.Vector3
import org.junit.Assert.assertEquals
import org.junit.Test

class DrumKitPlacementTest {
    @Test
    fun kitOffsetMovesEveryHitSurfaceTogether() {
        val offset = Vector3(0.12f, -0.08f, 0.2f)

        val moved = DrumKit.surfaces(CalibrationProfile(kitOffset = offset))

        DrumKit.baseSurfaces.zip(moved).forEach { (base, actual) ->
            assertEquals(base.center.x + offset.x, actual.center.x, EPSILON)
            assertEquals(base.center.y + offset.y, actual.center.y, EPSILON)
            assertEquals(base.center.z + offset.z, actual.center.z, EPSILON)
        }
    }

    @Test
    fun drumSpecificOffsetRemainsRelativeToKitOffset() {
        val kitOffset = Vector3(0.1f, 0.05f, -0.1f)
        val snareOffset = Vector3(-0.03f, 0.02f, 0.04f)
        val profile =
            CalibrationProfile(
                kitOffset = kitOffset,
                surfaceOffsets = mapOf(DrumId.SNARE to snareOffset),
            )

        val snare = DrumKit.surfaces(profile).first { it.id == DrumId.SNARE }
        val base = DrumKit.baseSurfaces.first { it.id == DrumId.SNARE }

        assertEquals(base.center.x + kitOffset.x + snareOffset.x, snare.center.x, EPSILON)
        assertEquals(base.center.y + kitOffset.y + snareOffset.y, snare.center.y, EPSILON)
        assertEquals(base.center.z + kitOffset.z + snareOffset.z, snare.center.z, EPSILON)
    }

    private companion object {
        const val EPSILON = 0.0001f
    }
}
