package io.getstream.webrtc.sample.compose.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.getstream.webrtc.sample.compose.data.SettingsDataStore
import io.getstream.webrtc.sample.compose.network.OrinServerScanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DiscoveredServer(val ip: String, val port: Int)

class SettingsViewModel(
  private val context: Context,
  private val dataStore: SettingsDataStore
) : ViewModel() {

  private val scanner = OrinServerScanner(context)

  val savedIps: StateFlow<List<String>> = dataStore.savedIpsFlow
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

  val selectedIp: StateFlow<String> = dataStore.selectedIpFlow
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = ""
    )

  // Toast events for the UI to consume
  private val _toastMessageFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
  val toastMessageFlow: SharedFlow<String> = _toastMessageFlow.asSharedFlow()

  // Scanning States
  private val _isScanning = MutableStateFlow(false)
  val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

  private val _discoveredServer = MutableStateFlow<DiscoveredServer?>(null)
  val discoveredServer: StateFlow<DiscoveredServer?> = _discoveredServer.asStateFlow()

  private val _scanMessage = MutableStateFlow<String?>(null)
  val scanMessage: StateFlow<String?> = _scanMessage.asStateFlow()

  fun addAndSelectIp(ip: String) {
    if (ip.isNotBlank()) {
      viewModelScope.launch {
        dataStore.saveIp(ip)
        dataStore.selectIp(ip)
        _toastMessageFlow.emit("Server IP saved: $ip")
      }
    }
  }

  fun selectIp(ip: String) {
    viewModelScope.launch {
      dataStore.selectIp(ip)
    }
  }

  fun removeIp(ip: String) {
    viewModelScope.launch {
      dataStore.removeIp(ip)
    }
  }

  fun startScan() {
    if (_isScanning.value) return

    _isScanning.value = true
    _discoveredServer.value = null
    _scanMessage.value = "Scanning for ORIN server..."

    scanner.startScan(
      onServerFound = { ip, port ->
        viewModelScope.launch {
          _discoveredServer.value = DiscoveredServer(ip, port)
          _scanMessage.value = "ORIN Server found!"
          stopScan()
        }
      },
      onError = { error ->
        viewModelScope.launch {
          _scanMessage.value = error
          _isScanning.value = false
        }
      }
    )

    viewModelScope.launch {
      delay(8000)
      if (_isScanning.value) {
        stopScan()
        if (_discoveredServer.value == null) {
          _scanMessage.value = "No ORIN server found on the network. Ensure the device is on the same WiFi network."
        }
      }
    }
  }

  private fun stopScan() {
    scanner.stopScan()
    _isScanning.value = false
  }

  fun saveServerDetails(ip: String) {
    addAndSelectIp(ip)
    _scanMessage.value = "Configuration saved successfully."
  }

  override fun onCleared() {
    super.onCleared()
    scanner.stopScan()
  }
}