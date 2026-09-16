package com.cyberdeck.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cyberdeck.android.data.SystemDiagnostics
import com.cyberdeck.android.ui.components.AsciiHeader
import com.cyberdeck.android.ui.components.CyberCard
import com.cyberdeck.android.ui.components.Mono

@Composable
fun SystemScreen() {
    val context = LocalContext.current
    val report = remember { SystemDiagnostics.deviceReport(context) }
    val battery = remember { SystemDiagnostics.batteryReport(context) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AsciiHeader("SYSTEM / HARDWARE")
        CyberCard { Mono(report) }
        AsciiHeader("BATTERY")
        CyberCard { Mono(battery) }
        CyberCard {
            Mono("Kernel string comes from os.version.")
            Mono("Some SoC fields require API 31+.")
        }
    }
}
