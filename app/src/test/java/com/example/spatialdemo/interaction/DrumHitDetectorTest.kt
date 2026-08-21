package com.example.spatialdemo.interaction

import com.example.spatialdemo.calibration.CalibrationProfile
import com.example.spatialdemo.domain.model.DrumId
import com.example.spatialdemo.domain.model.DrumSurface
import com.pico.spatial.core.math.Color4
import com.pico.spatial.core.math.Vector3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DrumHitDetectorTest {
    private val snare =
        DrumSurface(
            DrumId.SNARE,
            center = Vector3.ZERO,
            normal = Vector3.UP,
            radius = 0.25f,
            depth = 0.1f,
            color = Color4.WHITE,
        )

    @Test
    fun downwardCrossingInsideRadiusTriggersOneHit() {
        val detector = DrumHitDetector(listOf(snare))
        detector.process(frame(0L, 0.08f))
        val hits = detector.process(frame(20_000_000L, -0.01f))

        assertEquals(1, hits.size)
        assertEquals(DrumId.SNARE, hits.single().drumId)
        assertTrue(hits.single().intensity > 0f)
    }

    @Test
    fun slowCrossingDoesNotTrigger() {
        val detector = DrumHitDetector(listOf(snare))
        detector.process(frame(0L, 0.01f))
        val hits = detector.process(frame(100_000_000L, -0.005f))

        assertTrue(hits.isEmpty())
    }

    @Test
    fun crossingOutsideRadiusDoesNotTrigger() {
        val detector = DrumHitDetector(listOf(snare))
        detector.process(frame(0L, 0.08f, x = 0.4f))
        val hits = detector.process(frame(20_000_000L, -0.02f, x = 0.4f))

        assertTrue(hits.isEmpty())
    }

    @Test
    fun openHandDoesNotTrigger() {
        val detector = DrumHitDetector(listOf(snare))
        detector.process(frame(0L, 0.08f, grip = false))
        val hits = detector.process(frame(20_000_000L, -0.02f, grip = false))

        assertTrue(hits.isEmpty())
    }

    @Test
    fun updatedCalibrationChangesMinimumHitSpeed() {
        val detector = DrumHitDetector(listOf(snare))
        detector.update(CalibrationProfile(minimumHitSpeed = 2f), listOf(snare))
        detector.process(frame(0L, 0.08f))
        val hits = detector.process(frame(100_000_000L, -0.01f))

        assertTrue(hits.isEmpty())
        assertTrue((detector.consumeObservedApproachSpeed() ?: 0f) > 0f)
    }

    private fun frame(timestamp: Long, y: Float, x: Float = 0f, grip: Boolean = true) =
        HandFrame(
            timestamp,
            listOf(TrackedHand(HandSide.LEFT, Vector3(x, y, 0f), grip)),
        )
}
