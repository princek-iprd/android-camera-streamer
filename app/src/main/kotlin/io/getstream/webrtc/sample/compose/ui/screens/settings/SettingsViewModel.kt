package io.getstream.webrtc.sample.compose.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.getstream.webrtc.sample.compose.data.SettingsDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
  private val dataStore: SettingsDataStore
) : ViewModel() {

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
      initialValue = "10.102.10.112"
    )

  fun addAndSelectIp(ip: String) {
    if (ip.isNotBlank()) {
      viewModelScope.launch {
        dataStore.saveIp(ip)
        dataStore.selectIp(ip)
      }
    }
  }

  fun selectIp(ip: String) {
    viewModelScope.launch {
      dataStore.selectIp(ip)
    }
  }
}
