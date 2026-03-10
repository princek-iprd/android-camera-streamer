package io.getstream.webrtc.sample.compose.ui.screens.camera

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.webrtc.sample.compose.viewmodel.CameraViewModel

@Composable
fun MainScreen(
  cameraViewModel: CameraViewModel,
  selectedIp: String,
  onNavigateToSettings: () -> Unit
) {

  val state by cameraViewModel.uiState.collectAsState()

  // Track continuous zoom state from the ViewModel
  val currentZoom by cameraViewModel.zoomLevel.collectAsState()

  Scaffold(
    modifier = Modifier.systemBarsPadding(),
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text("Stream WebRTC", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(selectedIp, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
          }
        },
        actions = {
          IconButton(onClick = onNavigateToSettings) {
            Icon(Icons.Default.Settings, contentDescription = "Settings")
          }
        },
        elevation = 4.dp
      )
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {

      Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        shape = RoundedCornerShape(12.dp)
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = "Camera Control",
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = MaterialTheme.colors.primary
          )

          Spacer(modifier = Modifier.height(16.dp))

          /**
           * Connect / Disconnect button
           */
          if (!state.isConnected) {
            Button(
              onClick = { cameraViewModel.connectCamera(selectedIp) },
              modifier = Modifier.fillMaxWidth().height(48.dp),
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(20.dp))
              Spacer(Modifier.width(8.dp))
              Text("Connect Camera")
            }
          } else {
            OutlinedButton(
              onClick = { cameraViewModel.disconnectCamera() },
              modifier = Modifier.fillMaxWidth().height(48.dp),
              shape = RoundedCornerShape(8.dp),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colors.error)
            ) {
              Icon(Icons.Default.VideocamOff, contentDescription = null, modifier = Modifier.size(20.dp))
              Spacer(Modifier.width(8.dp))
              Text("Disconnect Camera")
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Text(
            text = "Status: ${state.status}",
            style = MaterialTheme.typography.caption,
            color = if (state.isConnected) Color(0xFF4CAF50) else Color.Gray,
            fontWeight = FontWeight.Medium
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      /**
       * Show Preview Button
       */
      if (state.isConnected) {
        if (!state.showPreview) {
          Button(
            onClick = { cameraViewModel.showPreview() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary)
          ) {
            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colors.onSecondary)
            Spacer(Modifier.width(8.dp))
            Text("Show Preview", color = MaterialTheme.colors.onSecondary)
          }
        }

        Spacer(modifier = Modifier.height(16.dp))
      }

      /**
       * Camera Preview + Zoom Controls
       */
      if (state.isConnected && state.showPreview) {
        Card(
          modifier = Modifier.fillMaxWidth(),
          elevation = 4.dp,
          shape = RoundedCornerShape(16.dp)
        ) {
          Column {
            CameraPreview(
              videoTrack = state.localVideoTrack
            )
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- NEW SLIDER UI ---
        Card(
          modifier = Modifier.fillMaxWidth(),
          elevation = 2.dp,
          shape = RoundedCornerShape(12.dp)
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "Zoom Level: ${String.format("%.1f", currentZoom)}x",
              fontWeight = FontWeight.SemiBold,
              fontSize = 14.sp
            )

            Spacer(Modifier.height(8.dp))

            Slider(
              value = currentZoom,
              onValueChange = { newZoom ->
                cameraViewModel.setZoom(newZoom)
              },
              valueRange = 1f..3f, // Min 1x, Max 3x
              colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colors.primary,
                activeTrackColor = MaterialTheme.colors.primary
              )
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("1x", fontSize = 12.sp, color = Color.Gray)
              Text("3x", fontSize = 12.sp, color = Color.Gray)
            }
          }
        }
      }
    }
  }
}