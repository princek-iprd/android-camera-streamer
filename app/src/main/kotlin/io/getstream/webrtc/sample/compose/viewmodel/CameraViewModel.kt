package io.getstream.webrtc.sample.compose.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class CameraViewModel : ViewModel() {

  private val _uiState = MutableStateFlow(CameraUiState())
  val uiState: StateFlow<CameraUiState> = _uiState

  fun connectCamera() {
    _uiState.update {
      it.copy(
        isConnected = true,
        status = "Connected"
      )
    }
  }

  fun showPreview() {
    _uiState.update {
      it.copy(showPreview = true)
    }
  }

  fun setZoom(level: Float) {
    _uiState.update {
      it.copy(zoomLevel = level)
    }
  }
}