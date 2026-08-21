package com.example.spatialdemo.ui.drum

import com.example.spatialdemo.domain.model.DrumId

enum class CalibrationPage { GESTURE, SURFACES }

data class DrumUiState(
    val showCalibration: Boolean = false,
    val calibrationPage: CalibrationPage = CalibrationPage.GESTURE,
    val selectedDrum: DrumId = DrumId.SNARE,
)

sealed interface DrumEvent {
    data object OpenCalibration : DrumEvent
    data object CloseCalibration : DrumEvent
    data class SelectCalibrationPage(val page: CalibrationPage) : DrumEvent
    data class SelectDrum(val drumId: DrumId) : DrumEvent
}
