package com.example.spatialdemo.ui.drum.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.spatialdemo.audio.DrumAudioStatus
import com.example.spatialdemo.calibration.CalibrationProfile
import com.example.spatialdemo.domain.model.DrumId
import com.example.spatialdemo.interaction.TrackedHand
import com.example.spatialdemo.ui.drum.CalibrationPage
import com.example.spatialdemo.ui.drum.DrumEvent
import com.example.spatialdemo.ui.drum.DrumUiState
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Slider
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.foundation.material.backgroundMaterial
import com.pico.spatial.ui.platform.Material
import java.util.Locale

@Composable
fun DrumHud(
    uiState: DrumUiState,
    calibration: CalibrationProfile,
    trackingStatus: String,
    handPresenceStatus: String,
    performanceStatus: String,
    audioStatus: DrumAudioStatus,
    modelReady: Boolean,
    softwareLatencyMs: Float?,
    liveHand: TrackedHand?,
    speedSamples: List<Float>,
    gripPoseRecorded: Boolean,
    openPoseRecorded: Boolean,
    onEvent: (DrumEvent) -> Unit,
    onProfileChange: (CalibrationProfile) -> Unit,
    onRecordGrip: () -> Unit,
    onRecordOpen: () -> Unit,
    onApplySpeedSamples: () -> Unit,
    onClearSpeedSamples: () -> Unit,
    onSave: () -> Unit,
    onReset: () -> Unit,
    onPreview: (DrumId) -> Unit,
    onPreviewAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            Modifier
                .size(
                    width = if (uiState.showCalibration) 760.dp else 720.dp,
                    height = if (uiState.showCalibration) 690.dp else 210.dp,
                ).clip(RoundedCornerShape(24.dp))
                .backgroundMaterial(enable = true, style = Material.Regular)
                .padding(
                    horizontal = if (uiState.showCalibration) 32.dp else 24.dp,
                    vertical = if (uiState.showCalibration) 24.dp else 18.dp,
                ).then(modifier),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "SPATIAL DRUMS",
            color = PicoTheme.colorScheme.labelPrimary,
            style = PicoTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = performanceStatus,
            color = PicoTheme.colorScheme.labelSecondary,
            style = PicoTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text =
                "${if (uiState.gestureModeEnabled) "手势敲鼓已开启" else "手势敲鼓已暂停"} · $handPresenceStatus · " +
                    "${if (modelReady) "鼓组已就绪" else "鼓组加载中"} · " +
                    audioStatus.label + " · " + latencyLabel(softwareLatencyMs) + " · $trackingStatus",
            color = PicoTheme.colorScheme.labelTertiary,
            style = PicoTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(if (uiState.showCalibration) 12.dp else 10.dp))
        if (uiState.showCalibration) {
            CalibrationContent(
                uiState = uiState,
                profile = calibration,
                liveHand = liveHand,
                speedSamples = speedSamples,
                gripPoseRecorded = gripPoseRecorded,
                openPoseRecorded = openPoseRecorded,
                audioReady = audioStatus.isReady,
                onEvent = onEvent,
                onProfileChange = onProfileChange,
                onRecordGrip = onRecordGrip,
                onRecordOpen = onRecordOpen,
                onApplySpeedSamples = onApplySpeedSamples,
                onClearSpeedSamples = onClearSpeedSamples,
                onSave = onSave,
                onReset = onReset,
                onPreview = onPreview,
                onPreviewAll = onPreviewAll,
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { onEvent(DrumEvent.ToggleGestureMode) }) {
                    Text(if (uiState.gestureModeEnabled) "暂停手势" else "开启手势")
                }
                Button(onClick = { onEvent(DrumEvent.OpenPlacement) }) { Text("调整鼓组位置") }
                Button(onClick = { onEvent(DrumEvent.OpenCalibration) }) { Text("校准与设置") }
                Button(
                    enabled = audioStatus.isReady,
                    onClick = { onPreview(DrumId.SNARE) },
                ) { Text(if (audioStatus.isReady) "试听军鼓" else "音频加载中") }
            }
        }
    }
}

@Composable
private fun CalibrationContent(
    uiState: DrumUiState,
    profile: CalibrationProfile,
    liveHand: TrackedHand?,
    speedSamples: List<Float>,
    gripPoseRecorded: Boolean,
    openPoseRecorded: Boolean,
    audioReady: Boolean,
    onEvent: (DrumEvent) -> Unit,
    onProfileChange: (CalibrationProfile) -> Unit,
    onRecordGrip: () -> Unit,
    onRecordOpen: () -> Unit,
    onApplySpeedSamples: () -> Unit,
    onClearSpeedSamples: () -> Unit,
    onSave: () -> Unit,
    onReset: () -> Unit,
    onPreview: (DrumId) -> Unit,
    onPreviewAll: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = { onEvent(DrumEvent.SelectCalibrationPage(CalibrationPage.GESTURE)) }) {
            Text("手势与声音")
        }
        Button(onClick = { onEvent(DrumEvent.SelectCalibrationPage(CalibrationPage.SURFACES)) }) {
            Text("鼓面位置")
        }
        Button(onClick = { onEvent(DrumEvent.SelectCalibrationPage(CalibrationPage.PLACEMENT)) }) {
            Text("鼓组位置")
        }
    }
    Spacer(Modifier.height(10.dp))
    when (uiState.calibrationPage) {
        CalibrationPage.GESTURE ->
            GestureCalibration(
                profile = profile,
                gestureModeEnabled = uiState.gestureModeEnabled,
                liveHand = liveHand,
                speedSamples = speedSamples,
                gripPoseRecorded = gripPoseRecorded,
                openPoseRecorded = openPoseRecorded,
                audioReady = audioReady,
                onProfileChange = onProfileChange,
                onToggleGestureMode = { onEvent(DrumEvent.ToggleGestureMode) },
                onRecordGrip = onRecordGrip,
                onRecordOpen = onRecordOpen,
                onApplySpeedSamples = onApplySpeedSamples,
                onClearSpeedSamples = onClearSpeedSamples,
                onPreview = onPreview,
                onPreviewAll = onPreviewAll,
            )

        CalibrationPage.SURFACES ->
            SurfaceCalibration(
                profile = profile,
                selectedDrum = uiState.selectedDrum,
                onEvent = onEvent,
                onProfileChange = onProfileChange,
            )

        CalibrationPage.PLACEMENT ->
            KitPlacement(
                profile = profile,
                onProfileChange = onProfileChange,
            )
    }
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = onSave) { Text("保存本机校准") }
        Button(onClick = onReset) { Text("全部复位") }
        Button(onClick = { onEvent(DrumEvent.CloseCalibration) }) { Text("完成") }
    }
}

@Composable
private fun GestureCalibration(
    profile: CalibrationProfile,
    gestureModeEnabled: Boolean,
    liveHand: TrackedHand?,
    speedSamples: List<Float>,
    gripPoseRecorded: Boolean,
    openPoseRecorded: Boolean,
    audioReady: Boolean,
    onProfileChange: (CalibrationProfile) -> Unit,
    onToggleGestureMode: () -> Unit,
    onRecordGrip: () -> Unit,
    onRecordOpen: () -> Unit,
    onApplySpeedSamples: () -> Unit,
    onClearSpeedSamples: () -> Unit,
    onPreview: (DrumId) -> Unit,
    onPreviewAll: () -> Unit,
) {
    val pinch = liveHand?.pinchDistanceMeters?.takeIf(Float::isFinite)
    val curl = liveHand?.averageCurlDistanceMeters?.takeIf(Float::isFinite)
    Text(
        text =
            "无需手柄 · ${if (gestureModeEnabled) "手势敲鼓已开启" else "手势敲鼓已暂停"} · " +
                "捏合 ${metersToMillimeters(pinch)} · 弯指 ${metersToMillimeters(curl)} · " +
                "握槌 ${if (liveHand?.gripActive == true) "是" else "否"}",
        color = PicoTheme.colorScheme.labelSecondary,
        style = PicoTheme.typography.bodyMedium,
    )
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = onToggleGestureMode) {
            Text(if (gestureModeEnabled) "暂停手势敲鼓" else "开启手势敲鼓")
        }
        Button(onClick = onRecordGrip) { Text(if (gripPoseRecorded) "已记录握姿" else "记录握姿") }
        Button(onClick = onRecordOpen) { Text(if (openPoseRecorded) "已记录张手" else "记录张手") }
    }
    MetricSlider(
        label = "握槌捏合阈值 ${millimeters(profile.pinchGripThreshold)}",
        value = profile.pinchGripThreshold,
        valueRange = 0.02f..0.10f,
        onValueChange = { onProfileChange(profile.copy(pinchGripThreshold = it)) },
    )
    MetricSlider(
        label = "最低敲击速度 ${format(profile.minimumHitSpeed)} m/s",
        value = profile.minimumHitSpeed,
        valueRange = 0.10f..1.50f,
        onValueChange = { onProfileChange(profile.copy(minimumHitSpeed = it)) },
    )
    MetricSlider(
        label = "满力度速度 ${format(profile.fullVelocitySpeed)} m/s",
        value = profile.fullVelocitySpeed,
        valueRange = 0.50f..5.00f,
        onValueChange = { onProfileChange(profile.copy(fullVelocitySpeed = it)) },
    )
    MetricSlider(
        label = "手势敲击延伸长度 ${millimeters(profile.virtualStickLength)}",
        value = profile.virtualStickLength,
        valueRange = 0.20f..0.45f,
        onValueChange = { onProfileChange(profile.copy(virtualStickLength = it)) },
    )
    Text(
        text = "速度样本 ${speedSamples.size}/20" + speedSamples.lastOrNull()?.let { " · 最近 ${format(it)} m/s" }.orEmpty(),
        color = PicoTheme.colorScheme.labelTertiary,
        style = PicoTheme.typography.bodySmall,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = onApplySpeedSamples) { Text("采用速度样本") }
        Button(onClick = onClearSpeedSamples) { Text("清空样本") }
    }
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(enabled = audioReady, onClick = { onPreview(DrumId.SNARE) }) { Text("试听军鼓") }
        Button(enabled = audioReady, onClick = { onPreview(DrumId.KICK) }) { Text("试听底鼓") }
        Button(enabled = audioReady, onClick = onPreviewAll) { Text("依次试听全部") }
    }
}

@Composable
private fun SurfaceCalibration(
    profile: CalibrationProfile,
    selectedDrum: DrumId,
    onEvent: (DrumEvent) -> Unit,
    onProfileChange: (CalibrationProfile) -> Unit,
) {
    val ids = DrumId.entries
    val index = ids.indexOf(selectedDrum)
    val offset = profile.surfaceOffsets[selectedDrum] ?: Vector3.ZERO
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(onClick = { onEvent(DrumEvent.SelectDrum(ids[(index - 1 + ids.size) % ids.size])) }) {
            Text("上一个")
        }
        Text(
            text = selectedDrum.displayName,
            color = PicoTheme.colorScheme.labelPrimary,
            style = PicoTheme.typography.titleMedium,
        )
        Button(onClick = { onEvent(DrumEvent.SelectDrum(ids[(index + 1) % ids.size])) }) {
            Text("下一个")
        }
    }
    Spacer(Modifier.height(8.dp))
    Text(
        text = "画面中的半透明鼓面是实际命中区域；按厘米微调中心位置。",
        color = PicoTheme.colorScheme.labelTertiary,
        style = PicoTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
    )
    SurfaceOffsetSlider(
        label = "左右 X",
        value = offset.x,
        onValueChange = { value ->
            onProfileChange(profile.withSurfaceOffset(selectedDrum, Vector3(value, offset.y, offset.z)))
        },
    )
    SurfaceOffsetSlider(
        label = "上下 Y",
        value = offset.y,
        onValueChange = { value ->
            onProfileChange(profile.withSurfaceOffset(selectedDrum, Vector3(offset.x, value, offset.z)))
        },
    )
    SurfaceOffsetSlider(
        label = "前后 Z",
        value = offset.z,
        onValueChange = { value ->
            onProfileChange(profile.withSurfaceOffset(selectedDrum, Vector3(offset.x, offset.y, value)))
        },
    )
    Spacer(Modifier.height(6.dp))
    Button(onClick = { onProfileChange(profile.withSurfaceOffset(selectedDrum, Vector3.ZERO)) }) {
        Text("复位当前鼓面")
    }
}

@Composable
private fun KitPlacement(
    profile: CalibrationProfile,
    onProfileChange: (CalibrationProfile) -> Unit,
) {
    val offset = profile.kitOffset
    Text(
        text = "默认将你置于鼓凳中心；此处可调整整套鼓组和所有命中区域。",
        color = PicoTheme.colorScheme.labelSecondary,
        style = PicoTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = "+X 向右 · +Y 向上 · +Z 靠近你",
        color = PicoTheme.colorScheme.labelTertiary,
        style = PicoTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
    )
    KitOffsetSlider(
        label = "左右 X",
        value = offset.x,
        onValueChange = { value ->
            onProfileChange(profile.copy(kitOffset = Vector3(value, offset.y, offset.z)))
        },
    )
    KitOffsetSlider(
        label = "上下 Y",
        value = offset.y,
        onValueChange = { value ->
            onProfileChange(profile.copy(kitOffset = Vector3(offset.x, value, offset.z)))
        },
    )
    KitOffsetSlider(
        label = "远近 Z",
        value = offset.z,
        onValueChange = { value ->
            onProfileChange(profile.copy(kitOffset = Vector3(offset.x, offset.y, value)))
        },
    )
    Spacer(Modifier.height(6.dp))
    Button(onClick = { onProfileChange(profile.copy(kitOffset = Vector3.ZERO)) }) {
        Text("复位到鼓凳演奏位")
    }
}

@Composable
private fun MetricSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        color = PicoTheme.colorScheme.labelSecondary,
        style = PicoTheme.typography.bodyMedium,
    )
    Slider(
        modifier = Modifier.width(620.dp).then(modifier),
        value = value.coerceIn(valueRange.start, valueRange.endInclusive),
        valueRange = valueRange,
        onValueChange = onValueChange,
    )
}

@Composable
private fun SurfaceOffsetSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    MetricSlider(
        label = "$label ${signedCentimeters(value)}",
        value = value,
        valueRange = -0.35f..0.35f,
        onValueChange = onValueChange,
        modifier = modifier,
    )
}

@Composable
private fun KitOffsetSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
) {
    MetricSlider(
        label = "$label ${signedCentimeters(value)}",
        value = value,
        valueRange = -0.5f..0.5f,
        onValueChange = onValueChange,
    )
}

private fun CalibrationProfile.withSurfaceOffset(id: DrumId, offset: Vector3) =
    copy(surfaceOffsets = surfaceOffsets + (id to offset))

private fun latencyLabel(value: Float?) =
    if (value == null) "应用内延迟待测" else "应用内 ${format(value)} ms"

private fun metersToMillimeters(value: Float?) = value?.let(::millimeters) ?: "-- mm"

private fun millimeters(value: Float) = "${format(value * 1000f)} mm"

private fun signedCentimeters(value: Float) = String.format(Locale.US, "%+.1f cm", value * 100f)

private fun format(value: Float) = String.format(Locale.US, "%.1f", value)
