package com.cyberdeck.android.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberdeck.android.data.TerminalEngine
import com.cyberdeck.android.ui.components.AsciiHeader
import com.cyberdeck.android.ui.theme.Neon
import com.cyberdeck.android.ui.theme.NeonDim
import kotlinx.coroutines.launch

@Composable
fun TerminalScreen() {
    val context = LocalContext.current
    val engine = remember { TerminalEngine(context.applicationContext) }
    var buffer by remember { mutableStateOf("CyberDeck terminal. Type help.\n") }
    var input by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val blink = rememberInfiniteTransition(label = "cur")
    val alpha by blink.animateFloat(0.15f, 1f, infiniteRepeatable(tween(650, easing = LinearEasing), RepeatMode.Reverse), label = "a")
    fun run() {
        val cmd = input; input = ""; buffer += "\n> $cmd\n"
        scope.launch {
            val out = engine.execute(cmd)
            buffer = if (out == "__CLEAR__") "buffer cleared.\n" else buffer + out + "\n"
        }
    }
    Column(Modifier.fillMaxSize().background(Color(0xFF030605)).padding(12.dp)) {
        AsciiHeader("TTY / ROOT@CYBERDECK")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).fillMaxWidth()) {
            Text(buffer, color = Neon, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("> ", color = Neon, fontFamily = FontFamily.Monospace)
            BasicTextField(
                value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f),
                textStyle = TextStyle(color = Neon, fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                cursorBrush = SolidColor(Neon.copy(alpha = alpha)), singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { run() })
            )
        }
        Text("enter to execute", color = NeonDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}
