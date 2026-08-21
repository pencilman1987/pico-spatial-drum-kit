package com.example.spatialdemo.ui.drum

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.spatialdemo.content.HomeStage

@Composable
fun DrumScreen(viewModel: DrumViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DrumContent(state = state, onEvent = viewModel::onEvent)
}

@Composable
internal fun DrumContent(
    state: DrumUiState,
    onEvent: (DrumEvent) -> Unit,
) {
    HomeStage(uiState = state, onEvent = onEvent)
}
