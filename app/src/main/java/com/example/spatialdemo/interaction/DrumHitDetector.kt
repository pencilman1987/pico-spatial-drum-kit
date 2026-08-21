package com.example.spatialdemo.interaction

import com.example.spatialdemo.calibration.CalibrationProfile
import com.example.spatialdemo.domain.model.DrumId
import com.example.spatialdemo.domain.model.DrumSurface
import com.pico.spatial.core.math.Vector3
import kotlin.math.max

enum class HandSide { LEFT, RIGHT }

data class TrackedHand(
    val side: HandSide,
    val stickTip: Vector3,
    val gripActive: Boolean,
    val stickBase: Vector3 = stickTip,
    val pinchDistanceMeters: Float = Float.NaN,
    val averageCurlDistanceMeters: Float = Float.NaN,
    val curledFingerCount: Int = 0,
)

data class HandFrame(
    val timestampNanos: Long,
    val hands: List<TrackedHand>,
)

data class DrumHit(
    val drumId: DrumId,
    val handSide: HandSide,
    val intensity: Float,
    val speedMetersPerSecond: Float,
)

class DrumHitDetector(
    private var surfaces: List<DrumSurface>,
    initialCalibration: CalibrationProfile = CalibrationProfile(),
) {
    private data class HandHistory(val position: Vector3, val timestampNanos: Long)

    private val histories = mutableMapOf<HandSide, HandHistory>()
    private val armed = mutableMapOf<Pair<HandSide, DrumId>, Boolean>()
    private val lastHitAt = mutableMapOf<Pair<HandSide, DrumId>, Long>()
    private var calibration = initialCalibration.sanitized()
    private var observedApproachSpeed: Float? = null

    fun process(frame: HandFrame): List<DrumHit> {
        val hits = mutableListOf<DrumHit>()
        val visibleSides = frame.hands.mapTo(mutableSetOf()) { it.side }
        histories.keys.retainAll(visibleSides)

        frame.hands.forEach { hand ->
            val previous = histories[hand.side]
            histories[hand.side] = HandHistory(hand.stickTip, frame.timestampNanos)
            if (!hand.gripActive || previous == null) return@forEach

            val elapsedSeconds =
                (frame.timestampNanos - previous.timestampNanos).coerceAtLeast(1L) / 1_000_000_000f
            val delta = hand.stickTip - previous.position

            val candidate =
                surfaces.mapNotNull { surface ->
                    crossingCandidate(
                        hand = hand,
                        surface = surface,
                        previous = previous.position,
                        current = hand.stickTip,
                        delta = delta,
                        elapsedSeconds = elapsedSeconds,
                        timestampNanos = frame.timestampNanos,
                    )
                }.minByOrNull { it.first }

            candidate?.second?.let(hits::add)
        }
        return hits
    }

    fun reset() {
        histories.clear()
        armed.clear()
        lastHitAt.clear()
        observedApproachSpeed = null
    }

    fun update(profile: CalibrationProfile, updatedSurfaces: List<DrumSurface>) {
        calibration = profile.sanitized()
        surfaces = updatedSurfaces
        reset()
    }

    fun consumeObservedApproachSpeed(): Float? {
        val speed = observedApproachSpeed
        observedApproachSpeed = null
        return speed
    }

    private fun crossingCandidate(
        hand: TrackedHand,
        surface: DrumSurface,
        previous: Vector3,
        current: Vector3,
        delta: Vector3,
        elapsedSeconds: Float,
        timestampNanos: Long,
    ): Pair<Float, DrumHit>? {
        val key = hand.side to surface.id
        val previousDistance = Vector3.dot(previous - surface.center, surface.normal)
        val currentDistance = Vector3.dot(current - surface.center, surface.normal)

        if (currentDistance > calibration.rearmDistance) armed[key] = true
        if (armed[key] == false) return null
        if (previousDistance <= 0f || currentDistance > 0f) return null

        val denominator = previousDistance - currentDistance
        if (denominator <= 0.00001f) return null
        val crossingFraction = previousDistance / denominator
        if (crossingFraction !in 0f..1f) return null

        val contact = previous + delta * crossingFraction
        val radialOffset = contact - surface.center - surface.normal * Vector3.dot(contact - surface.center, surface.normal)
        if (radialOffset.length() > surface.radius) return null

        val approachSpeed = -Vector3.dot(delta / elapsedSeconds, surface.normal)
        observedApproachSpeed = approachSpeed
        if (approachSpeed < calibration.minimumHitSpeed) return null
        val lastHit = lastHitAt[key]
        val cooldownNanos = calibration.cooldownMillis * 1_000_000L
        if (lastHit != null && timestampNanos - lastHit < cooldownNanos) return null

        armed[key] = false
        lastHitAt[key] = timestampNanos
        val intensity =
            ((approachSpeed - calibration.minimumHitSpeed) /
                    max(0.01f, calibration.fullVelocitySpeed - calibration.minimumHitSpeed))
                .coerceIn(0.12f, 1f)
        return crossingFraction to DrumHit(surface.id, hand.side, intensity, approachSpeed)
    }
}
