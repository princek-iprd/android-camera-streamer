package io.getstream.webrtc.sample.compose.ui.screens.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CameraPreview() {

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(300.dp)
      .background(Color.Black),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = "Camera Preview Placeholder",
      color = Color.White
    )
  }
}