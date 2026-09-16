package com.cyberdeck.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BluetoothSearching
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
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
import com.cyberdeck.android.ui.theme.Grid
import com.cyberdeck.android.ui.theme.Neon
import com.cyberdeck.android.ui.theme.NeonDim

private data class Dest(val route: String, val label: String, val icon: ImageVector)

private val dests = listOf(
    Dest("dash", "Deck", Icons.Outlined.Dashboard),
    Dest("term", "Term", Icons.Outlined.Terminal),
    Dest("net", "Net", Icons.Outlined.Wifi),
    Dest("ble", "BLE", Icons.Outlined.BluetoothSearching),
    Dest("tools", "Tools", Icons.Outlined.Science),
    Dest("sys", "Sys", Icons.Outlined.Memory)
)

@Composable
fun CyberDeckRoot() {
    val nav = rememberNavController()
    val back by nav.currentBackStackEntryAsState()
    val current = back?.destination?.route
    Scaffold(
        containerColor = Grid,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF070B0A), contentColor = Neon) {
                dests.forEach { d ->
                    NavigationBarItem(
                        selected = current == d.route,
                        onClick = {
                            nav.navigate(d.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(d.icon, d.label) },
                        label = { Text(d.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Neon,
                            selectedTextColor = Neon,
                            unselectedIconColor = NeonDim,
                            unselectedTextColor = NeonDim,
                            indicatorColor = Color(0xFF12352C)
                        )
                    )
                }
            }
        }
    ) { pad ->
        NavHost(navController = nav, startDestination = "dash", modifier = Modifier.padding(pad).background(Grid)) {
            composable("dash") { DashboardScreen() }
            composable("term") { TerminalScreen() }
            composable("net") { NetworkScreen() }
            composable("ble") { BleScreen() }
            composable("tools") { ToolsScreen() }
            composable("sys") { SystemScreen() }
        }
    }
}
