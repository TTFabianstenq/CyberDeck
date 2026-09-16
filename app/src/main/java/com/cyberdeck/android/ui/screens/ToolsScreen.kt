package com.cyberdeck.android.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.cyberdeck.android.core.EventLog
import com.cyberdeck.android.core.formatBytes
import com.cyberdeck.android.data.AppInspector
import com.cyberdeck.android.data.CryptoTools
import com.cyberdeck.android.data.NetworkTools
import com.cyberdeck.android.data.SystemDiagnostics
import com.cyberdeck.android.ui.components.AsciiHeader
import com.cyberdeck.android.ui.components.CyberCard
import com.cyberdeck.android.ui.components.MetricBar
import com.cyberdeck.android.ui.components.Mono
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ToolsScreen() {
    var tab by remember { mutableIntStateOf(0) }
    val titles = listOf("DNS", "HASH", "ENCODE", "APPS", "PERMS", "DISK", "LOG")
    Column(Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = tab) {
            titles.forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) }) }
        }
        when (tab) {
            0 -> DnsLab(); 1 -> HashLab(); 2 -> EncodeLab(); 3 -> AppsLab(); 4 -> PermsLab(); 5 -> DiskLab(); 6 -> LogLab()
        }
    }
}

@Composable private fun DnsLab() {
    val scope = rememberCoroutineScope()
    var host by remember { mutableStateOf("example.com") }
    var out by remember { mutableStateOf("") }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AsciiHeader("DNS LAB")
        OutlinedTextField(host, { host = it }, label = { Text("domain") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { scope.launch { out = withContext(Dispatchers.IO) { NetworkTools.dnsLookup(host) } } }) { Text("SYSTEM") }
            Button(onClick = { scope.launch { out = withContext(Dispatchers.IO) { NetworkTools.compareResolvers(host) } } }) { Text("COMPARE") }
        }
        CyberCard { Mono(out.ifBlank { "System resolver uses InetAddress. Compare adds user-initiated DoH to Cloudflare/Google." }) }
    }
}

@Composable private fun HashLab() {
    val context = LocalContext.current
    var text by remember { mutableStateOf("CyberDeck") }
    var algo by remember { mutableStateOf("SHA-256") }
    var out by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            out = runCatching { "$algo file\n" + CryptoTools.hashUri(context, algo, uri) }.getOrElse { it.message ?: "hash failed" }
            EventLog.add("Hashed file $algo")
        }
    }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AsciiHeader("HASH LAB")
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("MD5", "SHA-1", "SHA-256", "SHA-512").forEach { a -> FilterChip(selected = algo == a, onClick = { algo = a }, label = { Text(a) }) }
        }
        OutlinedTextField(text, { text = it }, label = { Text("text") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { out = CryptoTools.hashText(algo, text); EventLog.add("Hashed text $algo") }) { Text("HASH TEXT") }
            Button(onClick = { picker.launch("*/*") }) { Text("HASH FILE") }
        }
        CyberCard { Mono(out) }
    }
}

@Composable private fun EncodeLab() {
    var text by remember { mutableStateOf("CyberDeck") }
    var mode by remember { mutableStateOf("base64") }
    var out by remember { mutableStateOf("") }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AsciiHeader("ENCODING LAB")
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("base64", "url", "hex", "binary", "utf8").forEach { m -> FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(m) }) }
        }
        OutlinedTextField(text, { text = it }, label = { Text("input") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { out = CryptoTools.encode(mode, text) }) { Text("ENCODE") }
            Button(onClick = { out = CryptoTools.decode(mode, text) }) { Text("DECODE") }
        }
        CyberCard { Mono(out) }
    }
}

@Composable private fun AppsLab() {
    val context = LocalContext.current
    val apps = remember { AppInspector.listApps(context) }
    var q by remember { mutableStateOf("") }
    val filtered = apps.filter { q.isBlank() || it.label.contains(q, true) || it.packageName.contains(q, true) }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AsciiHeader("APP INSPECTOR")
        Mono("Visible packages: ${apps.size}. Package visibility is limited by Android.")
        OutlinedTextField(q, { q = it }, label = { Text("filter") }, modifier = Modifier.fillMaxWidth())
        filtered.take(80).forEach { app -> CyberCard { Mono(AppInspector.describe(app)) } }
    }
}

@Composable private fun PermsLab() {
    val context = LocalContext.current
    val groups = remember { AppInspector.permissionGroups(AppInspector.listApps(context)) }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AsciiHeader("PERMISSION AUDITOR")
        groups.forEach { (name, items) ->
            CyberCard {
                Mono("$name  (${items.size})")
                items.take(25).forEach { Mono(it) }
                if (items.size > 25) Mono("... ${items.size - 25} more")
            }
        }
    }
}

@Composable private fun DiskLab() {
    val snap = SystemDiagnostics.snapshot(LocalContext.current)
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AsciiHeader("STORAGE")
        CyberCard {
            MetricBar("USED", "${formatBytes(snap.storageUsed)} / ${formatBytes(snap.storageTotal)}", snap.storageUsed.toFloat() / snap.storageTotal.coerceAtLeast(1))
            Mono(SystemDiagnostics.storageReport())
        }
    }
}

@Composable private fun LogLab() {
    val clip = LocalClipboardManager.current
    val entries by EventLog.entries.collectAsState()
    var q by remember { mutableStateOf("") }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AsciiHeader("EVENT LOG")
        OutlinedTextField(q, { q = it }, label = { Text("search") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { EventLog.clear() }) { Text("CLEAR") }
            Button(onClick = { clip.setText(AnnotatedString(EventLog.dump())) }) { Text("COPY") }
        }
        CyberCard { entries.filter { q.isBlank() || it.line.contains(q, true) }.forEach { Mono(it.line) } }
    }
}
