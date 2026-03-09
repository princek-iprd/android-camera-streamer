package io.getstream.webrtc.sample.compose.ui.screens.camera

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import io.getstream.webrtc.sample.compose.viewmodel.CameraViewModel

@Composable
fun MainScreen(
  cameraViewModel: CameraViewModel,
  selectedIp: String,
  onNavigateToSettings: () -> Unit
) {

  val state by cameraViewModel.uiState.collectAsState()

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Stream WebRTC ($selectedIp)") },
        actions = {
          IconButton(onClick = onNavigateToSettings) {
            Icon(Icons.Default.Settings, contentDescription = "Settings")
          }
        }
      )
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {

      Text(text = "Android Camera Stream")

      Spacer(modifier = Modifier.height(20.dp))

      /**
       * Connect / Disconnect button
       */
      if (!state.isConnected) {

        Button(
          onClick = { cameraViewModel.connectCamera(selectedIp) }
        ) {
          Text("Connect Camera")
        }

      } else {

        Button(
          onClick = { cameraViewModel.disconnectCamera() }
        ) {
          Text("Disconnect Camera")
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(text = "Status: ${state.status}")

      Spacer(modifier = Modifier.height(20.dp))

      /**
       * Show Preview Button
       */
      if (state.isConnected) {

        Button(
          onClick = { cameraViewModel.showPreview() }
        ) {
          Text("Show Preview")
        }

        Spacer(modifier = Modifier.height(20.dp))
      }

      /**
       * Camera Preview + Zoom
       */
      if (state.isConnected && state.showPreview) {

        CameraPreview(
          videoTrack = state.localVideoTrack
        )

        Spacer(modifier = Modifier.height(20.dp))

        ZoomControls(
          onZoomSelected = { zoom ->
            cameraViewModel.setZoom(zoom)
          }
        )
      }
    }
  }
}