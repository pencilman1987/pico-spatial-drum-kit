package com.example.spatialdemo.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.spatialdemo.audio.DrumAudioEngine
import com.example.spatialdemo.audio.DrumAudioStatus
import com.example.spatialdemo.calibration.CalibrationProfile
import com.example.spatialdemo.data.repository.SharedPreferencesCalibrationRepository
import com.example.spatialdemo.domain.model.DrumId
import com.example.spatialdemo.domain.model.DrumKit
import com.example.spatialdemo.domain.usecase.CalibrationUseCase
import com.example.spatialdemo.interaction.DrumHitDetector
import com.example.spatialdemo.interaction.HandSide
import com.example.spatialdemo.interaction.TrackedHand
import com.example.spatialdemo.tracking.HandTrackingController
import com.example.spatialdemo.ui.drum.DrumEvent
import com.example.spatialdemo.ui.drum.DrumUiState
import com.example.spatialdemo.ui.drum.components.DrumHud
import com.pico.spatial.core.ecs.LookAtComponent
import com.pico.spatial.core.ecs.LookAtForwardDirection
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.ui.foundation.content.SpatialView
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class PoseSample(
    val pinchDistance: Float,
    val curlDistance: Float,
)

@Composable
fun HomeStage(
    uiState: DrumUiState,
    onEvent: (DrumEvent) -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val gestureModeEnabled by rememberUpdatedState(uiState.gestureModeEnabled)
    val calibrationUseCase =
        remember { CalibrationUseCase(SharedPreferencesCalibrationRepository(context)) }
    var calibration by remember { mutableStateOf(calibrationUseCase.load()) }
    val tracker = remember { HandTrackingController() }
    val detector = remember { DrumHitDetector(DrumKit.surfaces(calibration), calibration) }
    val audio = remember { DrumAudioEngine(context) }
    val sceneRef = remember { AtomicReference<DrumScene?>(null) }
    var trackingStatus by remember { mutableStateOf("正在启动…") }
    var performanceStatus by remember { mutableStateOf("鼓组模型加载中…") }
    var modelReady by remember { mutableStateOf(false) }
    var audioStatus by remember { mutableStateOf(audio.status()) }
    var liveHand by remember { mutableStateOf<TrackedHand?>(null) }
    var visibleHandSides by remember { mutableStateOf(emptySet<HandSide>()) }
    var gripPose by remember { mutableStateOf<PoseSample?>(null) }
    var openPose by remember { mutableStateOf<PoseSample?>(null) }
    var speedSamples by remember { mutableStateOf(emptyList<Float>()) }
    var latencySamples by remember { mutableStateOf(emptyList<Float>()) }

    DisposableEffect(Unit) {
        tracker.updateCalibration(calibration)
        trackingStatus = tracker.start()
        onDispose {
            tracker.stop()
            detector.reset()
            sceneRef.getAndSet(null)?.destroy()
            audio.release()
        }
    }

    LaunchedEffect(calibration) {
        val surfaces = DrumKit.surfaces(calibration)
        tracker.updateCalibration(calibration)
        detector.update(calibration, surfaces)
        audio.updateSurfaces(surfaces)
        sceneRef.get()?.apply {
            applyKitOffset(calibration.kitOffset)
            applySurfaces(surfaces)
        }
    }

    LaunchedEffect(uiState.showCalibration) {
        sceneRef.get()?.setCalibrationOverlayVisible(uiState.showCalibration)
    }

    LaunchedEffect(uiState.gestureModeEnabled) {
        detector.reset()
        if (!uiState.gestureModeEnabled) {
            performanceStatus = "手势敲鼓已暂停"
        } else if (modelReady) {
            performanceStatus = "捏合或握拳模拟握槌，向下穿过鼓面"
        }
    }

    LaunchedEffect(Unit) {
        val scene = waitForScene(sceneRef)
        modelReady = scene.loadProductionModel()
        performanceStatus =
            if (modelReady) {
                if (gestureModeEnabled) {
                    "捏合或握拳模拟握槌，向下穿过鼓面"
                } else {
                    "手势敲鼓已暂停"
                }
            } else {
                "正式模型加载失败，已启用安全鼓面"
            }
    }

    LaunchedEffect(Unit) {
        var lastTimestamp = Long.MIN_VALUE
        while (true) {
            val frame = tracker.latest()
            if (frame != null && frame.timestampNanos != lastTimestamp) {
                lastTimestamp = frame.timestampNanos
                val sceneFrame = sceneRef.get()?.trackingFrameToScene(frame) ?: frame
                liveHand = sceneFrame.hands.firstOrNull()
                visibleHandSides = sceneFrame.hands.mapTo(mutableSetOf()) { it.side }
                val hits =
                    if (gestureModeEnabled && sceneRef.get() != null) {
                        detector.process(sceneFrame)
                    } else {
                        emptyList()
                    }
                hits.forEach { hit ->
                    val telemetry = audio.play(hit.drumId, hit.intensity, frame.timestampNanos)
                    if (telemetry.submitted) {
                        latencySamples = (latencySamples + telemetry.trackingToSubmissionMs).takeLast(30)
                    }
                    sceneRef.get()?.registerHit(hit.drumId, hit.intensity, frame.timestampNanos)
                    performanceStatus =
                        if (telemetry.submitted) {
                            "${hit.handSide.name.lowercase().replaceFirstChar(Char::uppercase)} · " +
                                "${hit.drumId.displayName} · ${(hit.intensity * 100).toInt()}%"
                        } else {
                            "${hit.drumId.displayName} 已命中 · 音频仍在加载"
                        }
                }
                if (gestureModeEnabled) {
                    detector.consumeObservedApproachSpeed()?.let { speed ->
                        if (speed.isFinite() && speed > 0f) {
                            speedSamples = (speedSamples + speed).takeLast(20)
                        }
                    }
                }
            }
            val latestAudioStatus = audio.status()
            if (latestAudioStatus != audioStatus) audioStatus = latestAudioStatus
            sceneRef.get()?.update(System.nanoTime())
            delay(8)
        }
    }

    val softwareLatencyMs = latencySamples.takeIf { it.isNotEmpty() }?.average()?.toFloat()
    val preview: (DrumId) -> Unit = { drumId ->
        val now = System.nanoTime()
        val telemetry = audio.preview(drumId)
        audioStatus = audio.status()
        if (telemetry.submitted) {
            performanceStatus = "试听 · ${drumId.displayName}"
            sceneRef.get()?.registerHit(drumId, 0.72f, now)
        } else {
            performanceStatus = "音频仍在加载，请稍候"
        }
    }

    SpatialView(
        initial = { content, attachments ->
            sceneRef.set(DrumScene.create(content))
            sceneRef.get()?.apply {
                applyKitOffset(calibration.kitOffset)
                applySurfaces(DrumKit.surfaces(calibration))
            }
            attachments.entity(id = "status")?.apply {
                components[TransformComponent::class.java]?.position = Vector3(0f, 1.52f, -1.55f)
                content.addEntity(this)
                val viewerFacing =
                    LookAtComponent().apply {
                        alignLocalUpToWorldUp = true
                        lookAtForwardDirection = LookAtForwardDirection.POSITIVE_Z
                    }
                components.set(viewerFacing)
                viewerFacing.setViewerAsTarget()
            }
        },
        attachments = {
            AttachmentPanel(id = "status") {
                DrumHud(
                    uiState = uiState,
                    calibration = calibration,
                    trackingStatus = trackingStatus,
                    handPresenceStatus = handPresenceLabel(visibleHandSides),
                    performanceStatus = performanceStatus,
                    audioStatus = audioStatus,
                    modelReady = modelReady,
                    softwareLatencyMs = softwareLatencyMs,
                    liveHand = liveHand,
                    speedSamples = speedSamples,
                    gripPoseRecorded = gripPose != null,
                    openPoseRecorded = openPose != null,
                    onEvent = onEvent,
                    onProfileChange = { calibration = it.sanitized() },
                    onRecordGrip = {
                        liveHand?.toPoseSample()?.let { sample ->
                            gripPose = sample
                            calibration = applyPoseSamples(calibration, gripPose, openPose)
                        }
                    },
                    onRecordOpen = {
                        liveHand?.toPoseSample()?.let { sample ->
                            openPose = sample
                            calibration = applyPoseSamples(calibration, gripPose, openPose)
                        }
                    },
                    onApplySpeedSamples = { calibration = applySpeedSamples(calibration, speedSamples) },
                    onClearSpeedSamples = { speedSamples = emptyList() },
                    onSave = {
                        val saved =
                            calibration.copy(
                                calibratedOnDevice = true,
                                measuredSoftwareLatencyMs = softwareLatencyMs,
                            ).sanitized()
                        calibration = calibrationUseCase.save(saved)
                        performanceStatus = "校准参数已保存到本机"
                    },
                    onReset = {
                        calibration = calibrationUseCase.reset()
                        speedSamples = emptyList()
                        latencySamples = emptyList()
                        gripPose = null
                        openPose = null
                        performanceStatus = "已恢复安全默认参数"
                    },
                    onPreview = preview,
                    onPreviewAll = {
                        coroutineScope.launch {
                            DrumId.entries.forEach { drumId ->
                                preview(drumId)
                                delay(280)
                            }
                        }
                    },
                )
            }
        },
    )
}

private suspend fun waitForScene(sceneRef: AtomicReference<DrumScene?>): DrumScene {
    while (sceneRef.get() == null) delay(16)
    return requireNotNull(sceneRef.get())
}

private fun handPresenceLabel(sides: Set<HandSide>): String =
    when (sides) {
        setOf(HandSide.LEFT, HandSide.RIGHT) -> "左右手已识别"
        setOf(HandSide.LEFT) -> "左手已识别"
        setOf(HandSide.RIGHT) -> "右手已识别"
        else -> "等待双手"
    }

private fun TrackedHand.toPoseSample(): PoseSample? =
    if (pinchDistanceMeters.isFinite() && averageCurlDistanceMeters.isFinite()) {
        PoseSample(pinchDistanceMeters, averageCurlDistanceMeters)
    } else {
        null
    }

private fun applyPoseSamples(
    profile: CalibrationProfile,
    grip: PoseSample?,
    open: PoseSample?,
): CalibrationProfile {
    if (grip == null || open == null) return profile
    return profile.copy(
        pinchGripThreshold = (grip.pinchDistance + open.pinchDistance) / 2f,
        curlGripThreshold = (grip.curlDistance + open.curlDistance) / 2f,
    ).sanitized()
}

private fun applySpeedSamples(profile: CalibrationProfile, samples: List<Float>): CalibrationProfile {
    if (samples.size < 3) return profile
    val sorted = samples.sorted()
    val low = sorted[((sorted.lastIndex) * 0.2f).toInt()]
    val high = sorted[((sorted.lastIndex) * 0.9f).toInt()]
    return profile.copy(
        minimumHitSpeed = low * 0.72f,
        fullVelocitySpeed = high.coerceAtLeast(low + 0.25f),
    ).sanitized()
}
