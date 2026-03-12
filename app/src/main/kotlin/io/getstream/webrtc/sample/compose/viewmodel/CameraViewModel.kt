package io.getstream.webrtc.sample.compose.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.getstream.webrtc.sample.compose.CameraForegroundService
import io.getstream.webrtc.sample.compose.network.WebSocketManager
import io.getstream.webrtc.sample.compose.webrtc.sessions.WebRtcSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CameraViewModel(
  private val appContext: Context,
  private val sessionManager: WebRtcSessionManager
) : ViewModel() {

  private val _uiState = MutableStateFlow(CameraUiState())
  private val webSocketManager = WebSocketManager()
  val uiState: StateFlow<CameraUiState> = _uiState
  private var targetIp: String = "10.102.10.112"
  private val _zoomLevel = MutableStateFlow(2.0f)
  val zoomLevel: StateFlow<Float> = _zoomLevel

  fun connectCamera(ip: String) {
    targetIp = ip

    _uiState.update {
      it.copy(status = "Connecting to server...")
    }

    webSocketManager.connect(
      url = "ws://$ip/android-camera-service/ws_android",

      onConnected = {
        _uiState.update {
          it.copy(
            isConnected = true,
            status = "Connected to server"
          )
        }
      },

      onDisconnected = {
        _uiState.update {
          it.copy(
            isConnected = false,
            showPreview = false,
            status = "Disconnected"
          )
        }
        stopForegroundService()
      },

      onMessage = { message ->
        println("Server message: $message")
      }
    )
  }

  fun disconnectCamera() {
    stopForegroundService()
    webSocketManager.disconnect()
    sessionManager.disconnect()

    _uiState.update {
      it.copy(
        isConnected = false,
        showPreview = false,
        status = "Disconnected"
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

    startForegroundService()
    observeLocalVideoTrack()
    sessionManager.onSessionScreenReady(targetIp)
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

  fun setZoom(ratio: Float) {
    _zoomLevel.value = ratio
    sessionManager.setZoom(ratio)
  }

  private fun startForegroundService() {
    val intent = Intent(appContext, CameraForegroundService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      appContext.startForegroundService(intent)
    } else {
      appContext.startService(intent)
    }
  }

  private fun stopForegroundService() {
    appContext.stopService(Intent(appContext, CameraForegroundService::class.java))
  }

  override fun onCleared() {
    super.onCleared()
    sessionManager.disconnect()
  }
}