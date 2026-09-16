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
import androidx.compose.runtime.LaunchedEffect
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
import com.cyberdeck.android.data.ProcTools
import com.cyberdeck.android.data.TerminalEngine
import com.cyberdeck.android.ui.theme.TermAmber
import com.cyberdeck.android.ui.theme.TermGreen
import kotlinx.coroutines.launch

@Composable
fun TerminalScreen() {
    val context = LocalContext.current
    val engine = remember { TerminalEngine(context.applicationContext) }
    val host = remember { ProcTools.hostname() }
    var buffer by remember {
        mutableStateOf(
            "Linux cyberdeck  ${android.os.Build.VERSION.RELEASE}  (Android)\n" +
                "Last login: local console\n" +
                "Type help. This shell only inspects this device.\n"
        )
    }
    var input by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    val blink = rememberInfiniteTransition(label = "cur")
    val alpha by blink.animateFloat(
        0.2f, 1f,
        infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
        label = "a"
    )
    LaunchedEffect(buffer) { scroll.animateScrollTo(scroll.maxValue) }

    fun run() {
        val cmd = input
        input = ""
        buffer += "\ndeck@$host:~$ $cmd\n"
        scope.launch {
            val out = engine.execute(cmd)
            buffer = if (out == "__CLEAR__") "buffer cleared.\n" else buffer + out + "\n"
        }
    }

    Column(Modifier.fillMaxSize().background(Color.Black).padding(10.dp)) {
        Column(Modifier.weight(1f).verticalScroll(scroll).fillMaxWidth()) {
            Text(buffer, color = TermGreen, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            Text("deck@$host:~$ ", color = TermAmber, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(color = TermGreen, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                cursorBrush = SolidColor(TermGreen.copy(alpha = alpha)),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { run() })
            )
        }
    }
}
