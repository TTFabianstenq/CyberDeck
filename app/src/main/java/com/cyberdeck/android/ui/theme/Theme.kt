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

val TermGreen = Color(0xFF33FF66)
val TermDim = Color(0xFF1FA344)
val TermAmber = Color(0xFFE6B800)
val TermRed = Color(0xFFFF5555)
val TermBg = Color(0xFF000000)
val TermPanel = Color(0xFF050805)
val TermBar = Color(0xFF0A120A)

val Neon = TermGreen
val NeonDim = TermDim
val Magenta = TermAmber
val Grid = TermBg
val Panel = TermPanel
val Stroke = TermDim
val Danger = TermRed

private val Colors = darkColorScheme(
    primary = TermGreen,
    onPrimary = Color.Black,
    secondary = TermAmber,
    background = TermBg,
    surface = TermPanel,
    onBackground = TermGreen,
    onSurface = TermGreen,
    error = TermRed
)

val CyberTypography = Typography(
    bodyLarge = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = TermGreen),
    bodyMedium = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = TermGreen),
    bodySmall = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TermDim),
    titleLarge = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TermGreen),
    titleMedium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TermGreen),
    labelSmall = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TermDim)
)

@Composable
fun CyberDeckTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = CyberTypography, content = content)
}
