package io.getstream.webrtc.sample.compose.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
  viewModel: SettingsViewModel,
  onNavigateBack: () -> Unit
) {
  val savedIps by viewModel.savedIps.collectAsState()
  val selectedIp by viewModel.selectedIp.collectAsState()

  var newIpText by remember { mutableStateOf("") }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Settings") },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
          }
        }
      )
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(16.dp)
    ) {
      Text("Add New Server IP", fontWeight = FontWeight.Bold)
      Spacer(modifier = Modifier.height(8.dp))

      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
          value = newIpText,
          onValueChange = { newIpText = it },
          label = { Text("IP Address (e.g., 192.168.1.5)") },
          modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(
          onClick = {
            viewModel.addAndSelectIp(newIpText)
            newIpText = ""
          },
          enabled = newIpText.isNotBlank()
        ) {
          Text("Add")
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
      Text("Saved Server IPs", fontWeight = FontWeight.Bold)
      Spacer(modifier = Modifier.height(8.dp))

      LazyColumn(modifier = Modifier.weight(1f)) {
        items(savedIps) { ip ->
          val isSelected = ip == selectedIp
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { viewModel.selectIp(ip) }
              .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = ip,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              color = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
            )
            if (isSelected) {
              Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colors.primary)
            }
          }
          Divider()
        }
      }
    }
  }
}
