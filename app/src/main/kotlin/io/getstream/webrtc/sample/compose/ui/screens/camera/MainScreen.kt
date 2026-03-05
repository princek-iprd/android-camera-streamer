package io.getstream.webrtc.sample.compose.ui.screens.camera

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.getstream.webrtc.sample.compose.viewmodel.CameraViewModel

@Composable
fun MainScreen(
  cameraViewModel: CameraViewModel = viewModel()
) {

  val state by cameraViewModel.uiState.collectAsState()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(20.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {

    Text(text = "Android Camera Stream")

    Spacer(modifier = Modifier.height(20.dp))

    Button(
      onClick = { cameraViewModel.connectCamera() }
    ) {
      Text("Connect Camera")
    }

    Spacer(modifier = Modifier.height(10.dp))

    Text("Status: ${state.status}")

    Spacer(modifier = Modifier.height(20.dp))

    if (state.isConnected) {

      Button(
        onClick = { cameraViewModel.showPreview() }
      ) {
        Text("Show Preview")
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    if (state.showPreview) {

      CameraPreview()

      Spacer(modifier = Modifier.height(20.dp))

      ZoomControls(
        onZoomSelected = {
          cameraViewModel.setZoom(it)
        }
      )
    }
  }
}