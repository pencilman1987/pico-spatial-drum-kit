package com.example.spatialdemo.tracking

import com.example.spatialdemo.calibration.CalibrationProfile
import com.example.spatialdemo.interaction.HandFrame
import com.example.spatialdemo.interaction.HandSide
import com.example.spatialdemo.interaction.TrackedHand
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.tracking.DataProvider
import com.pico.spatial.tracking.hand.HandJoint
import com.pico.spatial.tracking.hand.HandPose
import com.pico.spatial.tracking.hand.HandTrackingData
import com.pico.spatial.tracking.hand.HandTrackingProvider
import java.util.concurrent.atomic.AtomicReference

class HandTrackingController {
    private val provider = HandTrackingProvider()
    private val latestFrame = AtomicReference<HandFrame?>(null)
    @Volatile private var calibration = CalibrationProfile()
    private val listener = DataProvider.DataListener<HandTrackingData> { data ->
        val hands = buildList {
            data.left?.toTrackedHand(HandSide.LEFT)?.let(::add)
            data.right?.toTrackedHand(HandSide.RIGHT)?.let(::add)
        }
        latestFrame.set(HandFrame(System.nanoTime(), hands))
    }

    fun start(): String {
        provider.addListener(listener)
        val result = provider.start()
        return "${result.name} · ${provider.supportState.name}"
    }

    fun stop() {
        provider.removeListener(listener)
        provider.stop()
        latestFrame.set(null)
    }

    fun latest(): HandFrame? = latestFrame.get()

    fun updateCalibration(profile: CalibrationProfile) {
        calibration = profile.sanitized()
    }

    private fun HandPose.toTrackedHand(side: HandSide): TrackedHand {
        val palm = joint(HandJoint.Index.PALM).position
        val wrist = joint(HandJoint.Index.WRIST).position
        val indexMetacarpal = joint(HandJoint.Index.INDEX_METACARPAL).position
        val indexTip = joint(HandJoint.Index.INDEX_TIP).position
        val thumbTip = joint(HandJoint.Index.THUMB_TIP).position
        val middleTip = joint(HandJoint.Index.MIDDLE_TIP).position
        val ringTip = joint(HandJoint.Index.RING_TIP).position
        val littleTip = joint(HandJoint.Index.LITTLE_TIP).position

        val pinchDistance = Vector3.distance(indexTip, thumbTip)
        val curlDistances = listOf(middleTip, ringTip, littleTip).map { Vector3.distance(it, palm) }
        val pinch = pinchDistance < calibration.pinchGripThreshold
        val curledCount =
            curlDistances.count { it < calibration.curlGripThreshold }
        val gripActive = pinch || curledCount >= 2

        val handAxis = indexMetacarpal - wrist
        val direction = if (handAxis.length() > 0.001f) handAxis.normalize() else (indexTip - palm).normalize()
        val virtualStickTip = palm + direction * calibration.virtualStickLength
        return TrackedHand(
            side = side,
            stickTip = virtualStickTip,
            gripActive = gripActive,
            stickBase = palm,
            pinchDistanceMeters = pinchDistance,
            averageCurlDistanceMeters = curlDistances.average().toFloat(),
            curledFingerCount = curledCount,
        )
    }
}
