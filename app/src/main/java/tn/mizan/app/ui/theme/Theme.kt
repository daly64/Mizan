package tn.mizan.app.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Couleurs de marque fixes, identiques en clair et en sombre (splash, logo). */
val BrandEmerald = Color(0xFF0F5C4D)
val BrandGold = Color(0xFFC9A45C)
val BrandCream = Color(0xFFFAF7EF)

/** Palette AL MIZAN (planche UI/UX), déclinée en thème clair et en thème sombre. */
@Immutable
data class MizanPalette(
    val emerald: Color,
    val emeraldLight: Color,
    val gold: Color,
    val cream: Color,
    val slate: Color,
    val muted: Color,
    val mint: Color,
    val line: Color,
    val cardWhite: Color,
    val danger: Color,
    val onEmeraldSoft: Color
)

val LightPalette = MizanPalette(
    emerald = Color(0xFF0F5C4D),
    emeraldLight = Color(0xFF2B7A66),
    gold = Color(0xFFC9A45C),
    cream = Color(0xFFF3EFE3),
    slate = Color(0xFF24302F),
    muted = Color(0xFF5D6966),
    mint = Color(0xFFE2ECE5),
    line = Color(0xFFE2DBCB),
    cardWhite = Color(0xFFFAF7EF),
    danger = Color(0xFFA8402C),
    onEmeraldSoft = Color(0xFFCFE3DA)
)

val DarkPalette = MizanPalette(
    emerald = Color(0xFF1E8C76),
    emeraldLight = Color(0xFF166B5B),
    gold = Color(0xFFD4B06A),
    cream = Color(0xFF111917),
    slate = Color(0xFFECE6D8),
    muted = Color(0xFF9AA8A2),
    mint = Color(0xFF1F3530),
    line = Color(0xFF2B3633),
    cardWhite = Color(0xFF1A2421),
    danger = Color(0xFFE8705F),
    onEmeraldSoft = Color(0xFFD7EBE3)
)

val LocalMizanPalette = staticCompositionLocalOf { LightPalette }

// Raccourcis utilisés dans tous les écrans : ils suivent le thème actif.
val Emerald: Color @Composable @ReadOnlyComposable get() = LocalMizanPalette.current.emerald
val EmeraldLight: Color @Composable @ReadOnlyComposable get() = LocalMizanPalette.current.emeraldLight
val Gold: Color @Composable @ReadOnlyComposable get() = LocalMizanPalette.current.gold
val Cream: Color @Composable @ReadOnlyComposable get() = LocalMizanPalette.current.cream
val Slate: Color @Composable @ReadOnlyComposable get() = LocalMizanPalette.current.slate
val Muted: Color @Composable @ReadOnlyComposable get() = LocalMizanPalette.current.muted
val Mint: Color @Composable @ReadOnlyComposable get() = LocalMizanPalette.current.mint
val Line: Color @Composable @ReadOnlyComposable get() = LocalMizanPalette.current.line
val CardWhite: Color @Composable @ReadOnlyComposable get() = LocalMizanPalette.current.cardWhite
val Danger: Color @Composable @ReadOnlyComposable get() = LocalMizanPalette.current.danger
val OnEmeraldSoft: Color @Composable @ReadOnlyComposable get() = LocalMizanPalette.current.onEmeraldSoft

/** Choix du thème, enregistré sur le téléphone. */
enum class ThemeMode(val label: String) {
    SYSTEME("Comme le téléphone"),
    CLAIR("Clair"),
    SOMBRE("Sombre")
}

object ThemeSettings {
    private const val PREFS = "mizan_reglages"
    private const val KEY = "theme"

    /** État observé par Compose : changer le thème s'applique immédiatement. */
    var mode by mutableStateOf(ThemeMode.SYSTEME)
        private set

    fun load(context: Context) {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        mode = ThemeMode.entries.firstOrNull { it.name == saved } ?: ThemeMode.SYSTEME
    }

    fun set(context: Context, value: ThemeMode) {
        mode = value
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, value.name).apply()
    }
}

@Composable
fun isMizanDark(): Boolean = when (ThemeSettings.mode) {
    ThemeMode.SYSTEME -> isSystemInDarkTheme()
    ThemeMode.CLAIR -> false
    ThemeMode.SOMBRE -> true
}

// Polices système pour cette ébauche.
private val MizanTypography = Typography(
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyMedium = TextStyle(fontSize = 14.sp),
    bodySmall = TextStyle(fontSize = 12.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)
)

private fun schemeFor(p: MizanPalette, dark: Boolean) =
    if (dark) darkColorScheme(
        primary = p.emerald, onPrimary = Color.White,
        secondary = p.gold, onSecondary = Color(0xFF1A1A1A),
        background = p.cream, onBackground = p.slate,
        surface = p.cardWhite, onSurface = p.slate,
        surfaceVariant = p.mint, onSurfaceVariant = p.muted,
        surfaceContainerHigh = p.cardWhite, surfaceContainerHighest = p.mint,
        outline = p.line, error = p.danger
    ) else lightColorScheme(
        primary = p.emerald, onPrimary = Color.White,
        secondary = p.gold, onSecondary = p.slate,
        background = p.cream, onBackground = p.slate,
        surface = p.cardWhite, onSurface = p.slate,
        surfaceVariant = p.mint, onSurfaceVariant = p.muted,
        outline = p.line, error = p.danger
    )

@Composable
fun MizanTheme(content: @Composable () -> Unit) {
    val dark = isMizanDark()
    val palette = if (dark) DarkPalette else LightPalette
    CompositionLocalProvider(LocalMizanPalette provides palette) {
        MaterialTheme(colorScheme = schemeFor(palette, dark), typography = MizanTypography, content = content)
    }
}
