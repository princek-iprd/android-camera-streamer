package io.getstream.webrtc.sample.compose.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.getstream.webrtc.sample.compose.webrtc.sessions.WebRtcSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.webrtc.VideoTrack

class CameraViewModel(
  private val sessionManager: WebRtcSessionManager
) : ViewModel() {

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
      it.copy(
        showPreview = true,
        status = "Starting camera..."
      )
    }

    observeLocalVideoTrack()

    sessionManager.onSessionScreenReady()
  }

  private fun observeLocalVideoTrack() {

    viewModelScope.launch {

      sessionManager.localVideoSinkFlow.collect { track ->

        _uiState.update {
          it.copy(
            localVideoTrack = track,
            status = "Camera started"
          )
        }
      }
    }
  }

  fun setZoom(level: Float) {
    _uiState.update {
      it.copy(zoomLevel = level)
    }
  }

  override fun onCleared() {
    super.onCleared()
    sessionManager.disconnect()
  }
}