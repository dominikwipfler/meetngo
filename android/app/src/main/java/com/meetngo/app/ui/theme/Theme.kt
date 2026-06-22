package com.meetngo.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Brand palette derived from the Figma design (--primary, --accent etc. tokens)
 * so the native app matches the design instead of stock Material3 colors.
 */
object MeetNGoColors {
    val BrandTeal = Color(0xFF1D9E75)
    val BrandCoral = Color(0xFFD85A30)
    val BrandDark = Color(0xFF2C2C2A)
    val BrandLight = Color(0xFFF1EFE8)
    val Destructive = Color(0xFFDC2626)
    val DestructiveDark = Color(0xFFEF4444)
}

private val LightColors = lightColorScheme(
    primary = MeetNGoColors.BrandTeal,
    onPrimary = Color.White,
    secondary = MeetNGoColors.BrandLight,
    onSecondary = MeetNGoColors.BrandDark,
    tertiary = MeetNGoColors.BrandCoral,
    onTertiary = Color.White,
    background = Color.White,
    onBackground = MeetNGoColors.BrandDark,
    surface = Color.White,
    onSurface = MeetNGoColors.BrandDark,
    surfaceVariant = MeetNGoColors.BrandLight,
    onSurfaceVariant = Color(0xFF6B6B69),
    error = MeetNGoColors.Destructive,
    onError = Color.White,
    outline = Color(0x1A2C2C2A),
)

private val DarkColors = darkColorScheme(
    primary = MeetNGoColors.BrandTeal,
    onPrimary = Color.White,
    secondary = Color(0xFF3A3A37),
    onSecondary = MeetNGoColors.BrandLight,
    tertiary = MeetNGoColors.BrandCoral,
    onTertiary = Color.White,
    background = Color(0xFF1A1A19),
    onBackground = MeetNGoColors.BrandLight,
    surface = Color(0xFF2C2C2A),
    onSurface = MeetNGoColors.BrandLight,
    surfaceVariant = Color(0xFF3A3A37),
    onSurfaceVariant = Color(0xFFA8A8A5),
    error = MeetNGoColors.DestructiveDark,
    onError = Color.White,
    outline = Color(0x26F1EFE8),
)

// Eckenradien aus dem Figma-Design (--radius: 0.75rem == 12dp bei Standarddichte).
val MeetNGoShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val MeetNGoTypography = Typography().let { base ->
    base.copy(
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Medium),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Medium),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Medium),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Medium),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Medium),
    )
}

/**
 * Wurzel-Theme der App. Wählt zwischen Hell-/Dunkel-Farbschema und wendet bei
 * Bedarf den Hoher-Kontrast-Modus an, bevor alles an [MaterialTheme] weitergegeben wird.
 *
 * @param darkTheme true = dunkles Farbschema, sonst helles.
 * @param highContrast true = erhöht Kontrast von Outline/Sekundärtext für bessere Lesbarkeit.
 */
@Composable
fun MeetNGoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    highContrast: Boolean = false,
    content: @Composable () -> Unit,
) {
    val base = if (darkTheme) DarkColors else LightColors
    // Hoher-Kontrast-Overrides: kräftigere Rahmen-Deckkraft und eine satte
    // Vordergrundfarbe statt des gedämpften Graus.
    val colors = if (!highContrast) {
        base
    } else if (darkTheme) {
        // Im Dunkelmodus: hellere, deckendere Outline- und Sekundärtextfarbe statt des gedämpften Grautons.
        base.copy(outline = Color(0x80F1EFE8), onSurfaceVariant = MeetNGoColors.BrandLight)
    } else {
        // Im Hellmodus: dunklere, deckendere Outline- und Sekundärtextfarbe statt des gedämpften Grautons.
        base.copy(outline = Color(0x732C2C2A), onSurfaceVariant = MeetNGoColors.BrandDark)
    }
    MaterialTheme(
        colorScheme = colors,
        shapes = MeetNGoShapes,
        typography = MeetNGoTypography,
        content = content,
    )
}
