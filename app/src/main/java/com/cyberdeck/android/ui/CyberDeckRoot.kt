package com.cyberdeck.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cyberdeck.android.ui.screens.BleScreen
import com.cyberdeck.android.ui.screens.DashboardScreen
import com.cyberdeck.android.ui.screens.NetworkScreen
import com.cyberdeck.android.ui.screens.SystemScreen
import com.cyberdeck.android.ui.screens.TerminalScreen
import com.cyberdeck.android.ui.screens.ToolsScreen
import com.cyberdeck.android.ui.theme.TermAmber
import com.cyberdeck.android.ui.theme.TermBar
import com.cyberdeck.android.ui.theme.TermBg
import com.cyberdeck.android.ui.theme.TermDim
import com.cyberdeck.android.ui.theme.TermGreen

private val dests = listOf(
    "dash" to "dash",
    "term" to "tty",
    "net" to "net",
    "ble" to "ble",
    "tools" to "lab",
    "sys" to "sys"
)

@Composable
fun CyberDeckRoot() {
    val nav = rememberNavController()
    val back by nav.currentBackStackEntryAsState()
    val current = back?.destination?.route ?: "dash"
    Column(Modifier.fillMaxSize().background(TermBg)) {
        Text(
            "cyberdeck tty1  linux-userland  pts/0",
            color = TermDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            modifier = Modifier.background(TermBar).fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)
        )
        Row(
            Modifier.fillMaxWidth().background(Color(0xFF081108)).horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            dests.forEach { (route, label) ->
                val on = current == route
                Text(
                    text = if (on) "[$label]" else " $label ",
                    color = if (on) TermAmber else TermGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(end = 10.dp).clickable {
                        nav.navigate(route) { launchSingleTop = true }
                    }
                )
            }
        }
        NavHost(navController = nav, startDestination = "dash", modifier = Modifier.weight(1f)) {
            composable("dash") { DashboardScreen() }
            composable("term") { TerminalScreen() }
            composable("net") { NetworkScreen() }
            composable("ble") { BleScreen() }
            composable("tools") { ToolsScreen() }
            composable("sys") { SystemScreen() }
        }
        Text(
            "deck@android:~$",
            color = TermDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
