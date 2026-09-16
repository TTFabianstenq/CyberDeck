package com.cyberdeck.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Neon = Color(0xFF00FFC6)
val NeonDim = Color(0xFF00A884)
val Magenta = Color(0xFFFF2BD6)
val Grid = Color(0xFF0B1210)
val Panel = Color(0xFF101816)
val Stroke = Color(0xFF1C3D34)
val Danger = Color(0xFFFF5A5A)

private val Colors = darkColorScheme(
    primary = Neon,
    onPrimary = Color.Black,
    secondary = Magenta,
    background = Color(0xFF050706),
    surface = Panel,
    onBackground = Neon,
    onSurface = Color(0xFFD7FFF4),
    error = Danger
)

val CyberTypography = Typography(
    bodyLarge = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = Neon),
    bodyMedium = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NeonDim),
    titleLarge = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Neon),
    titleMedium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Neon),
    labelSmall = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = NeonDim)
)

@Composable
fun CyberDeckTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = CyberTypography, content = content)
}
