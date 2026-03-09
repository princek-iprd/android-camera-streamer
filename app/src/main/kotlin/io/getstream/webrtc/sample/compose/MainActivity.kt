package io.getstream.webrtc.sample.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import io.getstream.webrtc.sample.compose.ui.screens.camera.MainScreen
import io.getstream.webrtc.sample.compose.ui.theme.WebrtcSampleComposeTheme
import io.getstream.webrtc.sample.compose.viewmodel.CameraViewModel
import io.getstream.webrtc.sample.compose.webrtc.SignalingClient
import io.getstream.webrtc.sample.compose.webrtc.peer.StreamPeerConnectionFactory
import io.getstream.webrtc.sample.compose.webrtc.sessions.LocalWebRtcSessionManager
import io.getstream.webrtc.sample.compose.webrtc.sessions.WebRtcSessionManagerImpl
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.getstream.webrtc.sample.compose.data.SettingsDataStore
import io.getstream.webrtc.sample.compose.ui.screens.settings.SettingsScreen
import io.getstream.webrtc.sample.compose.ui.screens.settings.SettingsViewModel

class MainActivity : ComponentActivity() {

  private val signalingClient by lazy {
    SignalingClient()
  }

  private val peerConnectionFactory by lazy {
    StreamPeerConnectionFactory(this)
  }

  private val sessionManager by lazy {
    WebRtcSessionManagerImpl(
      signalingClient = signalingClient,
      peerConnectionFactory = peerConnectionFactory,
      context = this,
    )
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val cameraViewModel = CameraViewModel(sessionManager)

    setContent {

      CompositionLocalProvider(
        LocalWebRtcSessionManager provides sessionManager
      ) {

        WebrtcSampleComposeTheme {
          val navController = rememberNavController()
          val settingsDataStore = remember { SettingsDataStore(this@MainActivity) }
          val settingsViewModel = remember { SettingsViewModel(settingsDataStore) }
          val selectedIp by settingsViewModel.selectedIp.collectAsState()

          NavHost(navController = navController, startDestination = "main") {
            composable("main") {
              MainScreen(
                cameraViewModel = cameraViewModel,
                selectedIp = selectedIp,
                onNavigateToSettings = { navController.navigate("settings") }
              )
            }
            composable("settings") {
              SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
              )
            }
          }
        }
      }
    }
  }
}