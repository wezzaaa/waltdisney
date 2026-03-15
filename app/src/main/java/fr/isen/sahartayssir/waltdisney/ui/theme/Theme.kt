package fr.isen.sahartayssir.waltdisney.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private val MagicColorScheme = darkColorScheme(
    primary = Color.White,
    secondary = MagicCyan,
    background = MagicDeepPurple,
    surface = GlassWhite
)

val MysticalGradient = Brush.verticalGradient(
    colors = listOf(MagicDeepPurple, Color(0xFF0A061E), MagicPink.copy(alpha = 0.3f))
)

@Composable
fun WaltdisneyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MagicColorScheme,
        typography = Typography,
        content = content
    )
}