package io.getstream.webrtc.sample.compose.network

import okhttp3.*
import okio.ByteString
import org.json.JSONObject

class WebSocketManager {

  private val client = OkHttpClient.Builder()
    .pingInterval(10, java.util.concurrent.TimeUnit.SECONDS)
    .build()

  private var webSocket: WebSocket? = null

  fun connect(
    url: String,
    onConnected: () -> Unit,
    onDisconnected: () -> Unit,
    onMessage: (String) -> Unit
  ) {

    val request = Request.Builder()
      .url(url)
      .build()

    webSocket = client.newWebSocket(request, object : WebSocketListener() {

      override fun onOpen(webSocket: WebSocket, response: Response) {

        onConnected()

        // Send heartbeat
        val heartbeat = JSONObject()
        heartbeat.put("HEARTBEAT", "CONNECTED")

        webSocket.send(heartbeat.toString())

        // Send supported resolutions
        val resolutions = JSONObject()
        resolutions.put(
          "RESOLUTIONS",
          "[1920x1080,1280x720,640x480]"
        )

        webSocket.send(resolutions.toString())
      }

      override fun onMessage(webSocket: WebSocket, text: String) {
        onMessage(text)
      }

      override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        onMessage(bytes.utf8())
      }

      override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        onDisconnected()
      }

      override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        onDisconnected()
      }
    })
  }

  fun disconnect() {
    webSocket?.close(1000, "Android requested disconnect")
    webSocket = null
  }
}