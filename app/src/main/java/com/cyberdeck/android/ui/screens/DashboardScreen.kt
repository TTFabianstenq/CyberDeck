package com.cyberdeck.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cyberdeck.android.core.formatBytes
import com.cyberdeck.android.data.SystemDiagnostics
import com.cyberdeck.android.ui.components.AsciiHeader
import com.cyberdeck.android.ui.components.CyberCard
import com.cyberdeck.android.ui.components.MetricBar
import com.cyberdeck.android.ui.components.Mono
import com.cyberdeck.android.ui.components.ScanLine
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    var snap by remember { mutableStateOf(SystemDiagnostics.snapshot(context)) }
    LaunchedEffect(Unit) { while (true) { snap = SystemDiagnostics.snapshot(context); delay(1000) } }
    ScanLine(Modifier.fillMaxSize())
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AsciiHeader("CYBERDECK / DASHBOARD")
        CyberCard {
            val cpu = snap.cpuPercent
            MetricBar("CPU", cpu?.let { String.format("%.1f%%", it) } ?: "sampling...", (cpu ?: 0f) / 100f)
            MetricBar("RAM", "${formatBytes(snap.ramUsed)} / ${formatBytes(snap.ramTotal)}", snap.ramUsed.toFloat() / snap.ramTotal.coerceAtLeast(1))
            MetricBar("DISK", "${formatBytes(snap.storageUsed)} / ${formatBytes(snap.storageTotal)}", snap.storageUsed.toFloat() / snap.storageTotal.coerceAtLeast(1))
        }
        CyberCard {
            Mono("BATT     ${snap.batteryPercent?.let { "$it%" } ?: "n/a"}")
            Mono("TEMP     ${snap.batteryTempC?.let { "$it C" } ?: "n/a"}")
            Mono("NET      ${snap.networkType}")
            Mono("IP       ${snap.localIp}")
            Mono("DNS      ${snap.dns}")
            Mono("HOST     ${snap.deviceModel}")
            Mono("OS       ${snap.androidVersion}")
            Mono("UPTIME   ${snap.uptime}")
        }
    }
}
