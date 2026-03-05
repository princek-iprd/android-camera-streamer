package io.getstream.webrtc.sample.compose.ui.screens.camera

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ZoomControls(
  onZoomSelected: (Float) -> Unit
) {

  Row(
    horizontalArrangement = Arrangement.SpaceEvenly,
    modifier = Modifier.fillMaxWidth()
  ) {

    Button(onClick = { onZoomSelected(1f) }) {
      Text("1x")
    }

    Button(onClick = { onZoomSelected(2f) }) {
      Text("2x")
    }

    Button(onClick = { onZoomSelected(3f) }) {
      Text("3x")
    }
  }
}