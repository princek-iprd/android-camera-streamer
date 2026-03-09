package io.getstream.webrtc.sample.compose.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

  private val SAVED_IPS_KEY = stringPreferencesKey("saved_ips")
  private val SELECTED_IP_KEY = stringPreferencesKey("selected_ip")

  val savedIpsFlow: Flow<List<String>> = context.dataStore.data
    .map { preferences ->
      val ipsString = preferences[SAVED_IPS_KEY] ?: "10.102.10.112" // default IP
      ipsString.split(",").filter { it.isNotBlank() }
    }

  val selectedIpFlow: Flow<String> = context.dataStore.data
    .map { preferences ->
      preferences[SELECTED_IP_KEY] ?: "10.102.10.112" // default IP
    }

  suspend fun saveIp(ip: String) {
    context.dataStore.edit { preferences ->
      val currentIpsString = preferences[SAVED_IPS_KEY] ?: "10.102.10.112"
      val currentIps = currentIpsString.split(",").filter { it.isNotBlank() }.toMutableSet()
      currentIps.add(ip)
      preferences[SAVED_IPS_KEY] = currentIps.joinToString(",")
    }
  }

  suspend fun selectIp(ip: String) {
    context.dataStore.edit { preferences ->
      preferences[SELECTED_IP_KEY] = ip
    }
  }

  suspend fun removeIp(ipToRemove: String) {
    context.dataStore.edit { preferences ->
      // 1. Get current IPs and split them into a MutableSet
      val currentIpsString = preferences[SAVED_IPS_KEY] ?: "10.102.10.112"
      val currentIps = currentIpsString.split(",").filter { it.isNotBlank() }.toMutableSet()

      // 2. Remove the target IP and save back as comma-separated string
      if (currentIps.contains(ipToRemove)) {
        currentIps.remove(ipToRemove)
        preferences[SAVED_IPS_KEY] = currentIps.joinToString(",")
      }

      // 3. If the user deleted the currently selected IP, default to the first available one
      val currentSelectedIp = preferences[SELECTED_IP_KEY] ?: "10.102.10.112"
      if (currentSelectedIp == ipToRemove) {
        preferences[SELECTED_IP_KEY] = currentIps.firstOrNull() ?: ""
      }
    }
  }
}