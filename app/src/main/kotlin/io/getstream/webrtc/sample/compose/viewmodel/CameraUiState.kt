package io.getstream.webrtc.sample.compose.viewmodel

data class CameraUiState(
  val isConnected: Boolean = false,
  val showPreview: Boolean = false,
  val zoomLevel: Float = 1f,
  val status: String = "Disconnected"
)