package io.getstream.webrtc.sample.compose

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import io.getstream.webrtc.sample.compose.data.SettingsDataStore
import io.getstream.webrtc.sample.compose.ui.screens.camera.MainScreen
import io.getstream.webrtc.sample.compose.ui.screens.settings.SettingsScreen
import io.getstream.webrtc.sample.compose.ui.screens.settings.SettingsViewModel
import io.getstream.webrtc.sample.compose.ui.theme.WebrtcSampleComposeTheme
import io.getstream.webrtc.sample.compose.viewmodel.CameraViewModelFactory
import io.getstream.webrtc.sample.compose.webrtc.SignalingClient
import io.getstream.webrtc.sample.compose.webrtc.peer.StreamPeerConnectionFactory
import io.getstream.webrtc.sample.compose.webrtc.sessions.LocalWebRtcSessionManager
import io.getstream.webrtc.sample.compose.webrtc.sessions.WebRtcSessionManagerImpl

class MainActivity : ComponentActivity() {

  private val signalingClient by lazy { SignalingClient() }

  private val peerConnectionFactory by lazy { StreamPeerConnectionFactory(this) }

  private val sessionManager by lazy {
    WebRtcSessionManagerImpl(
      signalingClient = signalingClient,
      peerConnectionFactory = peerConnectionFactory,
      context = this,
    )
  }

  // viewModels() retains the ViewModel across background/foreground & config changes
  private val cameraViewModel by viewModels<io.getstream.webrtc.sample.compose.viewmodel.CameraViewModel> {
    CameraViewModelFactory(applicationContext, sessionManager)
  }

  @OptIn(ExperimentalPermissionsApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    setContent {
      WebrtcSampleComposeTheme {

        val permissionsState = rememberMultiplePermissionsState(
          permissions = listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        )

        if (permissionsState.allPermissionsGranted) {
          // All permissions granted — show the real app
          CompositionLocalProvider(LocalWebRtcSessionManager provides sessionManager) {
            val navController = rememberNavController()
            val settingsDataStore = remember { SettingsDataStore(this@MainActivity) }
            val settingsViewModel = remember { SettingsViewModel(this@MainActivity, settingsDataStore) }
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
        } else {
          // Show permission rationale screen
          PermissionRequestScreen(
            onRequestPermissions = { permissionsState.launchMultiplePermissionRequest() }
          )

          // Auto-launch dialog on first composition (before user has been asked once)
          LaunchedEffect(Unit) {
            if (!permissionsState.shouldShowRationale) {
              permissionsState.launchMultiplePermissionRequest()
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PermissionRequestScreen(onRequestPermissions: () -> Unit) {
  Scaffold { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Text(text = "📷", fontSize = 64.sp)
      Spacer(Modifier.height(24.dp))
      Text(
        text = "Camera Access Required",
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp
      )
      Spacer(Modifier.height(12.dp))
      Text(
        text = "This app needs access to your camera and microphone to stream video to the server. Please grant the required permissions to continue.",
        fontSize = 15.sp,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
      )
      Spacer(Modifier.height(32.dp))
      Button(
        onClick = onRequestPermissions,
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp),
        shape = RoundedCornerShape(12.dp)
      ) {
        Text("Grant Permissions", fontWeight = FontWeight.SemiBold)
      }
    }
  }
}