package com.cyberdeck.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cyberdeck.android.ui.CyberDeckRoot
import com.cyberdeck.android.ui.theme.CyberDeckTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CyberDeckTheme {
                CyberDeckRoot()
            }
        }
    }
}
