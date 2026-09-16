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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cyberdeck.android.core.AppPermissions
import com.cyberdeck.android.data.BleDevice
import com.cyberdeck.android.data.BleRepository
import com.cyberdeck.android.ui.components.AsciiHeader
import com.cyberdeck.android.ui.components.CyberCard
import com.cyberdeck.android.ui.components.Mono
import com.cyberdeck.android.ui.theme.Neon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BleScreen() {
    val context = LocalContext.current
    val repo = remember { BleRepository() }
    val devices by repo.devices.collectAsState()
    val scanning by repo.scanning.collectAsState()
    val error by repo.error.collectAsState()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.all { it }) repo.start(context)
    }
    DisposableEffect(Unit) { onDispose { repo.stop() } }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AsciiHeader("BLE EXPLORER / PASSIVE")
        CyberCard {
            Mono("Observation only. No pairing, no write, no advertising flood.")
            Mono(if (scanning) "SCANNING" else "IDLE")
            error?.let { Mono("ERR  $it") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { launcher.launch(AppPermissions.blePermissions()) }) { Text("PERMS") }
                Button(onClick = { repo.start(context) }) { Text("START") }
                Button(onClick = { repo.stop() }) { Text("STOP") }
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(devices, key = { it.address }) { d -> BleCard(d) }
        }
    }
}

@Composable
private fun BleCard(d: BleDevice) {
    val df = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }
    CyberCard {
        Mono("${d.rssi} dBm  ${d.name}")
        Mono(d.address)
        Mono("connectable=${d.connectable}")
        if (d.uuids.isNotEmpty()) Mono("uuid ${d.uuids.joinToString()}")
        if (d.manufacturer.isNotBlank()) Mono("mfg  ${d.manufacturer}")
        Mono("first ${df.format(Date(d.firstSeen))}  last ${df.format(Date(d.lastSeen))}")
        if (d.rssiHistory.size >= 2) {
            Canvas(Modifier.fillMaxWidth().height(36.dp)) {
                val pts = d.rssiHistory
                val min = (pts.minOrNull() ?: -100).toFloat()
                val max = (pts.maxOrNull() ?: -40).toFloat()
                val span = (max - min).takeIf { it > 1f } ?: 1f
                val dx = size.width / (pts.size - 1).coerceAtLeast(1)
                for (i in 1 until pts.size) {
                    val y1 = size.height - ((pts[i - 1] - min) / span) * size.height
                    val y2 = size.height - ((pts[i] - min) / span) * size.height
                    drawLine(Neon, Offset((i - 1) * dx, y1), Offset(i * dx, y2), 2f)
                }
            }
        }
    }
}
