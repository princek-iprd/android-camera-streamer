package io.getstream.webrtc.sample.compose.viewmodel
import org.webrtc.VideoTrack

data class CameraUiState(
  val isConnected: Boolean = false,
  val showPreview: Boolean = false,
  val zoomLevel: Float = 1f,
  val status: String = "Disconnected",
  val localVideoTrack: VideoTrack? = null
)