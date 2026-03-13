package fr.isen.sahartayssir.waltdisney.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AppleWhite,           // Texte et éléments importants en blanc
    secondary = AppleSystemGrey,    // Éléments secondaires
    background = AppleBlack,        // Fond noir pur immersif
    surface = AppleDarkGrey,        // Cartes et surfaces
    onPrimary = AppleBlack,
    onSecondary = AppleWhite,
    onBackground = AppleWhite,
    onSurface = AppleWhite,
    surfaceVariant = AppleMediumGrey // Pour les champs de saisie ou séparateurs
)

@Composable
fun WaltdisneyTheme(
    // On ignore le paramètre darkTheme pour forcer le style Cinéma
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}