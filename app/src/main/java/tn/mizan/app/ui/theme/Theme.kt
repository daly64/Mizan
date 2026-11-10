package tn.mizan.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Palette Mizan
val Emerald = Color(0xFF0F6B5C)
val EmeraldLight = Color(0xFF2E8467)
val Gold = Color(0xFFC9A45C)
val Cream = Color(0xFFF7F1E5)
val Slate = Color(0xFF24302F)
val Muted = Color(0xFF5E6B65)
val Mint = Color(0xFFE3EFE9)
val Line = Color(0xFFE2D9C5)
val CardWhite = Color(0xFFFFFCF6)
val Danger = Color(0xFFB33A2E)
val OnEmeraldSoft = Color(0xFFCFE3DA)

private val MizanColors = lightColorScheme(
    primary = Emerald,
    onPrimary = Color.White,
    secondary = Gold,
    onSecondary = Slate,
    background = Cream,
    onBackground = Slate,
    surface = CardWhite,
    onSurface = Slate,
    surfaceVariant = Mint,
    onSurfaceVariant = Muted,
    outline = Line,
    error = Danger
)

// Polices système pour cette ébauche (Poppins / Inter / Cairo à ajouter ensuite).
private val MizanTypography = Typography(
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyMedium = TextStyle(fontSize = 14.sp),
    bodySmall = TextStyle(fontSize = 12.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun MizanTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MizanColors, typography = MizanTypography, content = content)
}
