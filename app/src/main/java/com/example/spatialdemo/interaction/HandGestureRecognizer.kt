package com.example.spatialdemo.interaction

import com.example.spatialdemo.calibration.CalibrationProfile

object HandGestureRecognizer {
    fun isGripActive(
        pinchDistanceMeters: Float,
        curlDistancesMeters: List<Float>,
        profile: CalibrationProfile,
    ): Boolean {
        val calibration = profile.sanitized()
        val pinching = pinchDistanceMeters < calibration.pinchGripThreshold
        val curledFingerCount = curlDistancesMeters.count { it < calibration.curlGripThreshold }
        return pinching || curledFingerCount >= REQUIRED_CURLED_FINGERS
    }

    private const val REQUIRED_CURLED_FINGERS = 2
}
