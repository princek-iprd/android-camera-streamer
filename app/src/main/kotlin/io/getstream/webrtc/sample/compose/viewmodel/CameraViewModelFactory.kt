package io.getstream.webrtc.sample.compose.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.getstream.webrtc.sample.compose.webrtc.sessions.WebRtcSessionManager

class CameraViewModelFactory(
  private val context: Context,
  private val sessionManager: WebRtcSessionManager
) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(CameraViewModel::class.java)) {
      return CameraViewModel(context.applicationContext, sessionManager) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
  }
}
