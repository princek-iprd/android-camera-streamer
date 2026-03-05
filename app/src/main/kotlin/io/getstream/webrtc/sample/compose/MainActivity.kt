package io.getstream.webrtc.sample.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.getstream.webrtc.sample.compose.ui.screens.camera.MainScreen
import io.getstream.webrtc.sample.compose.ui.theme.WebrtcSampleComposeTheme

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {

      WebrtcSampleComposeTheme {

        MainScreen()

      }
    }
  }
}