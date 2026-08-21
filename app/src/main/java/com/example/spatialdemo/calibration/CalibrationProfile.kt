package com.example.spatialdemo.calibration

import com.example.spatialdemo.domain.model.DrumId
import com.pico.spatial.core.math.Vector3

data class CalibrationProfile(
    val minimumHitSpeed: Float = 0.35f,
    val fullVelocitySpeed: Float = 2.8f,
    val pinchGripThreshold: Float = 0.06f,
    val curlGripThreshold: Float = 0.115f,
    val virtualStickLength: Float = 0.32f,
    val rearmDistance: Float = 0.055f,
    val cooldownMillis: Long = 80L,
    val kitOffset: Vector3 = Vector3.ZERO,
    val surfaceOffsets: Map<DrumId, Vector3> = emptyMap(),
    val calibratedOnDevice: Boolean = false,
    val measuredSoftwareLatencyMs: Float? = null,
) {
    fun sanitized() =
        copy(
            minimumHitSpeed = minimumHitSpeed.coerceIn(0.1f, 2.5f),
            fullVelocitySpeed = fullVelocitySpeed.coerceIn(minimumHitSpeed + 0.1f, 6f),
            pinchGripThreshold = pinchGripThreshold.coerceIn(0.015f, 0.12f),
            curlGripThreshold = curlGripThreshold.coerceIn(0.05f, 0.2f),
            virtualStickLength = virtualStickLength.coerceIn(0.18f, 0.5f),
            rearmDistance = rearmDistance.coerceIn(0.02f, 0.12f),
            cooldownMillis = cooldownMillis.coerceIn(30L, 180L),
            kitOffset = kitOffset.coerceComponents(-0.5f, 0.5f),
            surfaceOffsets = surfaceOffsets.mapValues { (_, offset) -> offset.coerceComponents(-0.35f, 0.35f) },
            measuredSoftwareLatencyMs = measuredSoftwareLatencyMs?.coerceIn(0f, 250f),
        )
}

private fun Vector3.coerceComponents(minimum: Float, maximum: Float) =
    Vector3(x.coerceIn(minimum, maximum), y.coerceIn(minimum, maximum), z.coerceIn(minimum, maximum))
