/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.getstream.webrtc.sample.compose.webrtc.sessions

import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.getSystemService
import io.getstream.log.taggedLogger
import io.getstream.webrtc.android.ktx.stringify
import io.getstream.webrtc.sample.compose.webrtc.MediaMTXPublisher
import io.getstream.webrtc.sample.compose.webrtc.SignalingClient
import io.getstream.webrtc.sample.compose.webrtc.SignalingCommand
import io.getstream.webrtc.sample.compose.webrtc.audio.AudioHandler
import io.getstream.webrtc.sample.compose.webrtc.audio.AudioSwitchHandler
import io.getstream.webrtc.sample.compose.webrtc.peer.StreamPeerConnection
import io.getstream.webrtc.sample.compose.webrtc.peer.StreamPeerConnectionFactory
import io.getstream.webrtc.sample.compose.webrtc.peer.StreamPeerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import org.webrtc.AudioTrack
import org.webrtc.Camera2Capturer
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerationAndroid
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStreamTrack
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoTrack
import java.util.UUID

private const val ICE_SEPARATOR = '$'

val LocalWebRtcSessionManager: ProvidableCompositionLocal<WebRtcSessionManager> =
  staticCompositionLocalOf { error("WebRtcSessionManager was not initialized!") }

class WebRtcSessionManagerImpl(
  private val context: Context,
  override val signalingClient: SignalingClient,
  override val peerConnectionFactory: StreamPeerConnectionFactory,
) : WebRtcSessionManager {
  private val logger by taggedLogger("Call:LocalWebRtcSessionManager")

  private val sessionManagerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  // used to send local video track to the fragment - Replay = 1 ensures Compose gets the latest track
  private val _localVideoSinkFlow = MutableSharedFlow<VideoTrack>(replay = 1)
  override val localVideoSinkFlow: SharedFlow<VideoTrack> = _localVideoSinkFlow

  // used to send remote video track to the sender - Replay = 1 ensures Compose gets the latest track
  private val _remoteVideoSinkFlow = MutableSharedFlow<VideoTrack>(replay = 1)
  override val remoteVideoSinkFlow: SharedFlow<VideoTrack> = _remoteVideoSinkFlow

  private val publisher = MediaMTXPublisher()

  // declaring video constraints and setting OfferToReceiveVideo to true
  // this step is mandatory to create valid offer and answer
  private val mediaConstraints = MediaConstraints().apply {
    mandatory.addAll(
      listOf(
        MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"),
        MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"),
      ),
    )
  }

  private val cameraManager by lazy { context.getSystemService<CameraManager>() }
  private val cameraEnumerator: Camera2Enumerator by lazy {
    Camera2Enumerator(context)
  }

  private val resolution: CameraEnumerationAndroid.CaptureFormat
    get() {
      val frontCamera = cameraEnumerator.deviceNames.first { cameraName ->
        cameraEnumerator.isFrontFacing(cameraName)
      }
      val supportedFormats = cameraEnumerator.getSupportedFormats(frontCamera) ?: emptyList()
      return supportedFormats.firstOrNull {
        (it.width == 720 || it.width == 480 || it.width == 360)
      } ?: error("There is no matched resolution!")
    }

  private val audioManager by lazy {
    context.getSystemService<AudioManager>()
  }

  private val audioConstraints: MediaConstraints by lazy {
    buildAudioConstraints()
  }

  // used to initialize video capturer
  private var surfaceTextureHelper: SurfaceTextureHelper? = null

  private var _videoCapturer: VideoCapturer? = null
  private val videoCapturer: VideoCapturer
    get() = _videoCapturer ?: buildCameraCapturer().also { _videoCapturer = it }

  private var _videoSource: org.webrtc.VideoSource? = null
  private val videoSource: org.webrtc.VideoSource
    get() = _videoSource ?: peerConnectionFactory.makeVideoSource(videoCapturer.isScreencast).apply {
      _videoSource = this
      if (surfaceTextureHelper == null) {
        surfaceTextureHelper = SurfaceTextureHelper.create(
          "SurfaceTextureHelperThread",
          peerConnectionFactory.eglBaseContext,
        )
      }
      videoCapturer.initialize(surfaceTextureHelper, context, this.capturerObserver)
      videoCapturer.startCapture(resolution.width, resolution.height, 30)
    }

  private var _localVideoTrack: VideoTrack? = null
  private val localVideoTrack: VideoTrack
    get() = _localVideoTrack ?: peerConnectionFactory.makeVideoTrack(
      source = videoSource,
      trackId = "Video${UUID.randomUUID()}",
    ).also { _localVideoTrack = it }

  /** Audio properties */

  private var _audioHandler: AudioHandler? = null
  private val audioHandler: AudioHandler
    get() = _audioHandler ?: AudioSwitchHandler(context).also { _audioHandler = it }

  private var _audioSource: org.webrtc.AudioSource? = null
  private val audioSource: org.webrtc.AudioSource
    get() = _audioSource ?: peerConnectionFactory.makeAudioSource(audioConstraints).also { _audioSource = it }

  private var _localAudioTrack: AudioTrack? = null
  private val localAudioTrack: AudioTrack
    get() = _localAudioTrack ?: peerConnectionFactory.makeAudioTrack(
      source = audioSource,
      trackId = "Audio${UUID.randomUUID()}",
    ).also { _localAudioTrack = it }

  private var offer: String? = null

  private var _peerConnection: StreamPeerConnection? = null
  private val peerConnection: StreamPeerConnection
    get() = _peerConnection ?: peerConnectionFactory.makePeerConnection(
      coroutineScope = sessionManagerScope,
      configuration = peerConnectionFactory.rtcConfig,
      type = StreamPeerType.SUBSCRIBER,
      mediaConstraints = mediaConstraints,
      onIceCandidateRequest = { iceCandidate, _ ->
        signalingClient.sendCommand(
          SignalingCommand.ICE,
          "${iceCandidate.sdpMid}$ICE_SEPARATOR" +
            "${iceCandidate.sdpMLineIndex}$ICE_SEPARATOR${iceCandidate.sdp}",
        )
      },
      onVideoTrack = { rtpTransceiver ->
        val track = rtpTransceiver?.receiver?.track() ?: return@makePeerConnection
        if (track.kind() == MediaStreamTrack.VIDEO_TRACK_KIND) {
          val videoTrack = track as VideoTrack
          sessionManagerScope.launch {
            _remoteVideoSinkFlow.emit(videoTrack)
          }
        }
      },
    ).also { _peerConnection = it }

  init {
    sessionManagerScope.launch {
      signalingClient.signalingCommandFlow
        .collect { commandToValue ->
          when (commandToValue.first) {
            SignalingCommand.OFFER -> handleOffer(commandToValue.second)
            SignalingCommand.ANSWER -> handleAnswer(commandToValue.second)
            SignalingCommand.ICE -> handleIce(commandToValue.second)
            else -> Unit
          }
        }
    }
  }

  override fun flipCamera() {
    (videoCapturer as? Camera2Capturer)?.switchCamera(null)
  }

  override fun setZoom(ratio: Float) {
    try {
      (videoCapturer as? org.webrtc.CameraCapturer)?.setZoom(ratio)
    } catch (e: Exception) {
      logger.e { "Failed to apply zoom natively: ${e.message}" }
      e.printStackTrace()
    }
  }

  override fun enableMicrophone(enabled: Boolean) {
    audioManager?.isMicrophoneMute = !enabled
  }

  override fun disconnect() {
    // 1. Capture currently active tracks from cache
    val cachedRemoteTracks = remoteVideoSinkFlow.replayCache.toList()
    val cachedLocalTracks = localVideoSinkFlow.replayCache.toList()

    // 2. Clear flows so Compose UI drops the tracks immediately
    _remoteVideoSinkFlow.resetReplayCache()
    _localVideoSinkFlow.resetReplayCache()

    // 3. Safely dispose cached tracks
    cachedRemoteTracks.forEach { videoTrack ->
      try { videoTrack.dispose() } catch (e: Exception) { e.printStackTrace() }
    }
    cachedLocalTracks.forEach { videoTrack ->
      try { videoTrack.dispose() } catch (e: Exception) { e.printStackTrace() }
    }

    // 4. Safely dispose backing properties
    try { _localAudioTrack?.dispose() } catch (e: Exception) { e.printStackTrace() }
    try { _localVideoTrack?.dispose() } catch (e: Exception) { e.printStackTrace() }
    _localAudioTrack = null
    _localVideoTrack = null

    // dispose audio handler and video capturer.
    _audioHandler?.stop()
    _audioHandler = null

    try { _videoCapturer?.stopCapture() } catch (e: Exception) { e.printStackTrace() }
    try { _videoCapturer?.dispose() } catch (e: Exception) { e.printStackTrace() }
    _videoCapturer = null

    try { _videoSource?.dispose() } catch (e: Exception) { e.printStackTrace() }
    _videoSource = null

    try { _audioSource?.dispose() } catch (e: Exception) { e.printStackTrace() }
    _audioSource = null

    try { surfaceTextureHelper?.dispose() } catch (e: Exception) { e.printStackTrace() }
    surfaceTextureHelper = null

    try { _peerConnection?.connection?.close() } catch (e: Exception) { e.printStackTrace() }
    try { _peerConnection?.connection?.dispose() } catch (e: Exception) { e.printStackTrace() }
    _peerConnection = null

    offer = null

    // dispose signaling clients and socket.
    signalingClient.dispose()
  }

  override fun onSessionScreenReady(ip: String) {
    peerConnection.connection.addTrack(localVideoTrack)
//    peerConnection.connection.addTrack(localAudioTrack)
    sessionManagerScope.launch {
      // sending local video track to show local video from start
      _localVideoSinkFlow.emit(localVideoTrack)

      if (offer != null) {
        sendAnswer()
      } else {
        sendOffer(ip)
      }
    }
  }

  private suspend fun sendOffer(ip: String) {
    val offer = peerConnection.createOffer().getOrThrow()
    peerConnection.setLocalDescription(offer)
    publisher.publishOffer(ip, offer) { answer ->
      sessionManagerScope.launch {
        peerConnection.setRemoteDescription(answer)
      }
    }
  }

  private suspend fun sendAnswer() {
    peerConnection.setRemoteDescription(
      SessionDescription(SessionDescription.Type.OFFER, offer),
    )
    val answer = peerConnection.createAnswer().getOrThrow()
    val result = peerConnection.setLocalDescription(answer)
    result.onSuccess {
      signalingClient.sendCommand(SignalingCommand.ANSWER, answer.description)
    }
    logger.d { "[SDP] send answer: ${answer.stringify()}" }
  }

  private fun handleOffer(sdp: String) {
    logger.d { "[SDP] handle offer: $sdp" }
    offer = sdp
  }

  private suspend fun handleAnswer(sdp: String) {
    logger.d { "[SDP] handle answer: $sdp" }
    peerConnection.setRemoteDescription(
      SessionDescription(SessionDescription.Type.ANSWER, sdp),
    )
  }

  private suspend fun handleIce(iceMessage: String) {
    val iceArray = iceMessage.split(ICE_SEPARATOR)
    peerConnection.addIceCandidate(
      IceCandidate(
        iceArray[0],
        iceArray[1].toInt(),
        iceArray[2],
      ),
    )
  }

  private fun buildCameraCapturer(): VideoCapturer {
    val manager = cameraManager ?: throw RuntimeException("CameraManager was not initialized!")

    val ids = manager.cameraIdList
    var foundCamera = false
    var cameraId = ""

    for (id in ids) {
      val characteristics = manager.getCameraCharacteristics(id)
      val cameraLensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)

      // SELECT REAR CAMERA
      if (cameraLensFacing == CameraMetadata.LENS_FACING_BACK) {
        foundCamera = true
        cameraId = id
        break
      }
    }

    // fallback if rear camera not found
    if (!foundCamera && ids.isNotEmpty()) {
      cameraId = ids.first()
    }

    return Camera2Capturer(context, cameraId, null)
  }

  private fun buildAudioConstraints(): MediaConstraints {
    val mediaConstraints = MediaConstraints()
    val items = listOf(
      MediaConstraints.KeyValuePair(
        "googEchoCancellation",
        true.toString(),
      ),
      MediaConstraints.KeyValuePair(
        "googAutoGainControl",
        true.toString(),
      ),
      MediaConstraints.KeyValuePair(
        "googHighpassFilter",
        true.toString(),
      ),
      MediaConstraints.KeyValuePair(
        "googNoiseSuppression",
        true.toString(),
      ),
      MediaConstraints.KeyValuePair(
        "googTypingNoiseDetection",
        true.toString(),
      ),
    )

    return mediaConstraints.apply {
      with(optional) {
        add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
        addAll(items)
      }
    }
  }

  private fun setupAudio() {
    logger.d { "[setupAudio] #sfu; no args" }
    audioHandler.start()
    audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val devices = audioManager?.availableCommunicationDevices ?: return
      val deviceType = AudioDeviceInfo.TYPE_BUILTIN_SPEAKER

      val device = devices.firstOrNull { it.type == deviceType } ?: return

      val isCommunicationDeviceSet = audioManager?.setCommunicationDevice(device)
      logger.d { "[setupAudio] #sfu; isCommunicationDeviceSet: $isCommunicationDeviceSet" }
    }
  }
}