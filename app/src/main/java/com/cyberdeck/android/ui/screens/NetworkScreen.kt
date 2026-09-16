package com.cyberdeck.android.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cyberdeck.android.core.AppPermissions
import com.cyberdeck.android.data.NetworkTools
import com.cyberdeck.android.data.WifiAp
import com.cyberdeck.android.data.WifiRepository
import com.cyberdeck.android.ui.components.AsciiHeader
import com.cyberdeck.android.ui.components.CyberCard
import com.cyberdeck.android.ui.components.Mono
import com.cyberdeck.android.ui.theme.Neon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun NetworkScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var host by remember { mutableStateOf("1.1.1.1") }
    var output by remember { mutableStateOf(NetworkTools.networkStatus(context)) }
    var aps by remember { mutableStateOf<List<WifiAp>>(emptyList()) }
    var wifiMsg by remember { mutableStateOf("Grant scan permission, then Scan Wi-Fi.") }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        wifiMsg = if (result.values.all { it }) "Permission granted. Scan again."
        else "Permission denied. SSID listing needs location / nearby Wi-Fi on modern Android."
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AsciiHeader("NETWORK / DIAG")
        CyberCard {
            Mono(output)
            Button(onClick = { output = NetworkTools.networkStatus(context) }) { Text("REFRESH LOCAL") }
        }
        CyberCard {
            OutlinedTextField(host, { host = it }, label = { Text("host") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { scope.launch { output = withContext(Dispatchers.IO) { NetworkTools.ping(host) } } }) { Text("PING") }
                Button(onClick = { scope.launch { output = withContext(Dispatchers.IO) { NetworkTools.dnsLookup(host) } } }) { Text("DNS") }
                Button(onClick = { scope.launch { output = withContext(Dispatchers.IO) { NetworkTools.traceroute(host) } } }) { Text("TRACE") }
            }
            Mono("Ping uses the system ping binary when present; otherwise InetAddress.isReachable.")
            Mono("Traceroute is best-effort. Android does not expose raw traceroute sockets.")
        }
        AsciiHeader("WIFI ANALYZER")
        CyberCard {
            Mono(wifiMsg)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { launcher.launch(AppPermissions.wifiScanPermissions()) }) { Text("REQUEST PERMS") }
                Button(onClick = {
                    WifiRepository.scan(context).fold({ aps = it; wifiMsg = "${it.size} access points" }, { wifiMsg = it.message ?: "scan failed" })
                }) { Text("SCAN WIFI") }
            }
            if (aps.isNotEmpty()) {
                Canvas(Modifier.fillMaxWidth().height(80.dp).padding(vertical = 8.dp)) {
                    val slice = aps.take(8); val w = size.width / 8
                    slice.forEachIndexed { i, ap ->
                        val h = size.height * (((ap.rssi + 100).coerceIn(0, 70)) / 70f)
                        drawRect(Neon.copy(alpha = 0.7f), topLeft = Offset(i * w + 4, size.height - h), size = Size(w - 8, h))
                    }
                }
                val band24 = aps.filter { it.frequency in 2400..2500 }
                Canvas(Modifier.fillMaxWidth().height(60.dp)) {
                    val w = size.width / 13f
                    (1..13).forEach { ch ->
                        val h = (band24.count { it.channel == ch } * 12f).coerceAtMost(size.height)
                        drawRect(Neon.copy(alpha = 0.45f), topLeft = Offset((ch - 1) * w + 2, size.height - h), size = Size(w - 4, h))
                    }
                }
                aps.take(30).forEach { Mono("${it.rssi}dBm  ch${it.channel}  ${it.frequency}MHz  ${it.security}  ${it.ssid}  ${it.bssid}") }
            }
        }
    }
}
