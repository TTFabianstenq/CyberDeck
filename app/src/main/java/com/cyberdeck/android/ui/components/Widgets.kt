package com.cyberdeck.android.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberdeck.android.ui.theme.Neon
import com.cyberdeck.android.ui.theme.NeonDim
import com.cyberdeck.android.ui.theme.Panel
import com.cyberdeck.android.ui.theme.Stroke

@Composable
fun AsciiHeader(title: String) {
    Text("// $title ////////////////////////////", color = Neon, fontFamily = FontFamily.Monospace, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
fun CyberCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().border(1.dp, Stroke, RoundedCornerShape(6.dp)).background(Panel, RoundedCornerShape(6.dp)).padding(12.dp), content = content)
}

@Composable
fun MetricBar(label: String, valueText: String, fraction: Float) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("$label  $valueText", color = Neon, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        LinearProgressIndicator(progress = { fraction.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(6.dp), color = Neon, trackColor = Color(0xFF1A2A24))
    }
}

@Composable
fun ScanLine(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "scan")
    val y by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart), label = "y")
    Box(modifier.drawBehind {
        val yy = size.height * y
        drawLine(Neon.copy(alpha = 0.35f), Offset(0f, yy), Offset(size.width, yy), 2f)
    })
}

@Composable
fun Mono(text: String, color: Color = NeonDim) {
    Text(text, color = color, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
}
