package com.example.spatialdemo.ui.drum

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DrumViewModel : ViewModel() {
    private val _state = MutableStateFlow(DrumUiState())
    val state: StateFlow<DrumUiState> = _state.asStateFlow()

    fun onEvent(event: DrumEvent) {
        _state.update { current ->
            when (event) {
                DrumEvent.OpenCalibration -> current.copy(showCalibration = true)
                DrumEvent.CloseCalibration -> current.copy(showCalibration = false)
                is DrumEvent.SelectCalibrationPage -> current.copy(calibrationPage = event.page)
                is DrumEvent.SelectDrum -> current.copy(selectedDrum = event.drumId)
            }
        }
    }
}
