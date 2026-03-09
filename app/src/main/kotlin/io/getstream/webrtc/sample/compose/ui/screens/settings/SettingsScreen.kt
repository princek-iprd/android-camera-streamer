package io.getstream.webrtc.sample.compose.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
  viewModel: SettingsViewModel,
  onNavigateBack: () -> Unit
) {
  val savedIps by viewModel.savedIps.collectAsState()
  val selectedIp by viewModel.selectedIp.collectAsState()

  var newIpText by remember { mutableStateOf("") }

  Scaffold(
    modifier = Modifier.systemBarsPadding(),
    topBar = {
      TopAppBar(
        title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
          }
        },
        elevation = 4.dp
      )
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(16.dp)
    ) {
      Text(
        text = "Add New Server IP",
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = MaterialTheme.colors.primary,
        modifier = Modifier.padding(bottom = 8.dp)
      )

      Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        shape = RoundedCornerShape(12.dp)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = newIpText,
            onValueChange = { newIpText = it },
            label = { Text("IP Address (e.g., 192.168.1.5)") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
          )
          Spacer(modifier = Modifier.width(12.dp))
          Button(
            onClick = {
              viewModel.addAndSelectIp(newIpText)
              newIpText = ""
            },
            enabled = newIpText.isNotBlank(),
            modifier = Modifier.height(56.dp).padding(top = 8.dp),
            shape = RoundedCornerShape(8.dp)
          ) {
            Icon(Icons.Default.Add, contentDescription = "Add IP")
          }
        }
      }

      Spacer(modifier = Modifier.height(32.dp))

      Text(
        text = "Saved Server IPs",
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = MaterialTheme.colors.primary,
        modifier = Modifier.padding(bottom = 8.dp)
      )

      Card(
        modifier = Modifier.fillMaxWidth().weight(1f),
        elevation = 2.dp,
        shape = RoundedCornerShape(12.dp)
      ) {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(vertical = 8.dp)
        ) {
          items(savedIps) { ip ->
            val isSelected = ip == selectedIp
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.selectIp(ip) }
                .padding(vertical = 16.dp, horizontal = 16.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Dns,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
              )

              Spacer(modifier = Modifier.width(16.dp))

              Text(
                text = ip,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colors.onSurface else MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
                modifier = Modifier.weight(1f)
              )

              if (isSelected) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = "Selected",
                  tint = MaterialTheme.colors.primary
                )
              }
            }
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
          }
        }
      }
    }
  }
}
