package io.getstream.webrtc.sample.compose.webrtc

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.webrtc.SessionDescription
import java.io.IOException

class MediaMTXPublisher(
  private val serverUrl: String
) {

  private val client = OkHttpClient()

  fun publishOffer(
    offer: SessionDescription,
    onAnswer: (SessionDescription) -> Unit
  ) {

    val body = offer.description
      .toRequestBody("application/sdp".toMediaType())

    val request = Request.Builder()
      .url("$serverUrl/mystream/whip")
      .post(body)
      .build()

    client.newCall(request).enqueue(object : Callback {

      override fun onFailure(call: Call, e: IOException) {
        e.printStackTrace()
      }

      override fun onResponse(call: Call, response: Response) {

        val responseBody = response.body?.string() ?: return

        val sdp = responseBody

        val answer = SessionDescription(
          SessionDescription.Type.ANSWER,
          sdp
        )

        onAnswer(answer)
      }
    })
  }
}