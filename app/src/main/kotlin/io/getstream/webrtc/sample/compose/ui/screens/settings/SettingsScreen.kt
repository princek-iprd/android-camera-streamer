package io.getstream.webrtc.sample.compose.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Regex to validate a standard IPv4 address strictly
val IP_REGEX = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$".toRegex()

@Composable
fun SettingsScreen(
  viewModel: SettingsViewModel,
  onNavigateBack: () -> Unit
) {
  val savedIps by viewModel.savedIps.collectAsState()
  val selectedIp by viewModel.selectedIp.collectAsState()
  val context = LocalContext.current

  // Scanner States
  val isScanning by viewModel.isScanning.collectAsState()
  val discoveredServer by viewModel.discoveredServer.collectAsState()
  val scanMessage by viewModel.scanMessage.collectAsState()

  var newIpText by remember { mutableStateOf("") }

  val isIpValid = newIpText.matches(IP_REGEX)

  // Collect toast events from ViewModel
  LaunchedEffect(Unit) {
    viewModel.toastMessageFlow.collect { message ->
      Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
  }

  Scaffold(
    modifier = Modifier.systemBarsPadding(),
    topBar = {
      TopAppBar(
        title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedTextField(
              value = newIpText,
              onValueChange = { input ->
                newIpText = input.filter { it.isDigit() || it == '.' }
              },
              label = { Text("IP Address (e.g., 192.168.1.5)") },
              modifier = Modifier.weight(1f),
              singleLine = true,
              shape = RoundedCornerShape(8.dp),
              isError = newIpText.isNotEmpty() && !isIpValid
            )
            Spacer(modifier = Modifier.width(12.dp))
            Button(
              onClick = {
                viewModel.addAndSelectIp(newIpText)
                newIpText = ""
              },
              enabled = isIpValid,
              modifier = Modifier
                .height(56.dp)
                .padding(top = 8.dp),
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(Icons.Default.Add, contentDescription = "Add IP")
            }
          }

          if (newIpText.isNotEmpty() && !isIpValid) {
            Text(
              text = "Invalid IP format. Use numbers and dots only.",
              color = MaterialTheme.colors.error,
              fontSize = 12.sp,
              modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // --- Auto Discovery Section ---
      Text(
        text = "Auto Discover ORIN Server",
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
        Column(modifier = Modifier.padding(16.dp)) {
          Button(
            onClick = { viewModel.startScan() },
            enabled = !isScanning,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
          ) {
            Icon(Icons.Default.Search, contentDescription = "Scan Icon")
            Spacer(Modifier.width(8.dp))
            Text(if (isScanning) "Scanning..." else "Scan for ORIN Server")
          }

          if (isScanning) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
          }

          scanMessage?.let { msg ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = msg,
              color = if (msg.contains("No ORIN") || msg.contains("failed")) MaterialTheme.colors.error else MaterialTheme.colors.onSurface,
              fontSize = 14.sp,
              modifier = Modifier.align(Alignment.CenterHorizontally)
            )
          }

          discoveredServer?.let { server ->
            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            Text("Discovered ORIN Server", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text("IP: ${server.ip}", fontSize = 14.sp)
            Text("Port: ${server.port}", fontSize = 14.sp)

            Spacer(modifier = Modifier.height(12.dp))

            Button(
              onClick = { viewModel.saveServerDetails(server.ip) },
              modifier = Modifier.align(Alignment.End),
              shape = RoundedCornerShape(8.dp)
            ) {
              Text("Save & Connect")
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // --- Saved Servers Section ---
      Text(
        text = "Saved Server IPs",
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = MaterialTheme.colors.primary,
        modifier = Modifier.padding(bottom = 8.dp)
      )

      Card(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
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
                .padding(vertical = 12.dp, horizontal = 16.dp),
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
                Spacer(modifier = Modifier.width(8.dp))
              }

              IconButton(
                onClick = { viewModel.removeIp(ip) },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = "Remove IP",
                  tint = MaterialTheme.colors.error.copy(alpha = 0.8f)
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