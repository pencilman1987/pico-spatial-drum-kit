package com.example.spatialdemo.interaction

import com.example.spatialdemo.calibration.CalibrationProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HandGestureRecognizerTest {
    private val profile = CalibrationProfile(pinchGripThreshold = 0.06f, curlGripThreshold = 0.115f)

    @Test
    fun pinchActsAsGripWithoutController() {
        assertTrue(
            HandGestureRecognizer.isGripActive(
                pinchDistanceMeters = 0.035f,
                curlDistancesMeters = listOf(0.16f, 0.17f, 0.18f),
                profile = profile,
            ),
        )
    }

    @Test
    fun twoCurledFingersActAsGripWithoutPinch() {
        assertTrue(
            HandGestureRecognizer.isGripActive(
                pinchDistanceMeters = 0.09f,
                curlDistancesMeters = listOf(0.08f, 0.09f, 0.17f),
                profile = profile,
            ),
        )
    }

    @Test
    fun openHandDoesNotActAsGrip() {
        assertFalse(
            HandGestureRecognizer.isGripActive(
                pinchDistanceMeters = 0.09f,
                curlDistancesMeters = listOf(0.16f, 0.17f, 0.18f),
                profile = profile,
            ),
        )
    }
}
