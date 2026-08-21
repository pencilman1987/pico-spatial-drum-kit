package com.example.spatialdemo.ui.drum

import com.example.spatialdemo.domain.model.DrumId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrumViewModelTest {
    @Test
    fun initialStateIsCompactAndTargetsSnare() {
        val state = DrumViewModel().state.value

        assertFalse(state.showCalibration)
        assertEquals(DrumId.SNARE, state.selectedDrum)
    }

    @Test
    fun openCalibrationExpandsHud() {
        val viewModel = DrumViewModel()

        viewModel.onEvent(DrumEvent.OpenCalibration)

        assertTrue(viewModel.state.value.showCalibration)
    }

    @Test
    fun openPlacementExpandsHudOnPlacementPage() {
        val viewModel = DrumViewModel()

        viewModel.onEvent(DrumEvent.OpenPlacement)

        assertTrue(viewModel.state.value.showCalibration)
        assertEquals(CalibrationPage.PLACEMENT, viewModel.state.value.calibrationPage)
    }

    @Test
    fun closeCalibrationReturnsToPerformanceHud() {
        val viewModel = DrumViewModel()
        viewModel.onEvent(DrumEvent.OpenCalibration)

        viewModel.onEvent(DrumEvent.CloseCalibration)

        assertFalse(viewModel.state.value.showCalibration)
    }

    @Test
    fun calibrationPageAndSelectedDrumAreReducedIndependently() {
        val viewModel = DrumViewModel()

        viewModel.onEvent(DrumEvent.SelectCalibrationPage(CalibrationPage.SURFACES))
        viewModel.onEvent(DrumEvent.SelectDrum(DrumId.RIDE))

        assertEquals(CalibrationPage.SURFACES, viewModel.state.value.calibrationPage)
        assertEquals(DrumId.RIDE, viewModel.state.value.selectedDrum)
    }
}
