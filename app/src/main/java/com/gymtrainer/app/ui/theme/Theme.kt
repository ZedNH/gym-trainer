package com.gymtrainer.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.gymtrainer.app.R

// ---------------- Brand palette ----------------
object Brand {
    val Blue = Color(0xFF2F6BFF)
    val BlueLight = Color(0xFF5B8CFF)
    val Orange = Color(0xFFFF7A45)
    val Green = Color(0xFF16B26A)
    val Amber = Color(0xFFFFA726)
    val Red = Color(0xFFF0435A)
    val Purple = Color(0xFF7B61FF)
    val Teal = Color(0xFF12B5A5)
    val Pink = Color(0xFFFF5C8A)
}

// Kept for older call sites; map onto the new palette.
val Accent = Brand.Blue
val WarnColor = Brand.Orange
val DangerColor = Brand.Red

/** Semantic colors that Material's scheme doesn't cover. */
@Immutable
data class AppColors(
    val card: Color,
    val cardAlt: Color,
    val border: Color,
    val textMuted: Color,
    val textFaint: Color,
    val energy: Color,
    val success: Color,
    val danger: Color,
    val isDark: Boolean
)

private val LightApp = AppColors(
    card = Color.White, cardAlt = Color(0xFFF1F4F9), border = Color(0xFFE4E8EF),
    textMuted = Color(0xFF667085), textFaint = Color(0xFF98A2B3),
    energy = Brand.Orange, success = Brand.Green, danger = Brand.Red, isDark = false
)
private val DarkApp = AppColors(
    card = Color(0xFF171C24), cardAlt = Color(0xFF1F2631), border = Color(0xFF2A3240),
    textMuted = Color(0xFF9AA4B2), textFaint = Color(0xFF6B7585),
    energy = Brand.Orange, success = Brand.Green, danger = Brand.Red, isDark = true
)

val LocalAppColors = staticCompositionLocalOf { LightApp }

object AppTheme {
    val colors: AppColors @Composable get() = LocalAppColors.current
}

private val LightScheme = lightColorScheme(
    primary = Brand.Blue, onPrimary = Color.White,
    primaryContainer = Color(0xFFE6EEFF), onPrimaryContainer = Color(0xFF0B2A80),
    secondary = Brand.Orange, onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFEDE5), onSecondaryContainer = Color(0xFF7A2A0A),
    tertiary = Brand.Green,
    background = Color(0xFFF5F7FB), onBackground = Color(0xFF101828),
    surface = Color.White, onSurface = Color(0xFF101828),
    surfaceVariant = Color(0xFFF1F4F9), onSurfaceVariant = Color(0xFF667085),
    outline = Color(0xFFD0D5DD), outlineVariant = Color(0xFFE4E8EF),
    error = Brand.Red
)

private val DarkScheme = darkColorScheme(
    primary = Brand.BlueLight, onPrimary = Color.White,
    primaryContainer = Color(0xFF1B2A55), onPrimaryContainer = Color(0xFFD6E2FF),
    secondary = Brand.Orange, onSecondary = Color.White,
    secondaryContainer = Color(0xFF4A2414), onSecondaryContainer = Color(0xFFFFDBCC),
    tertiary = Brand.Green,
    background = Color(0xFF0E1116), onBackground = Color(0xFFF2F4F7),
    surface = Color(0xFF171C24), onSurface = Color(0xFFF2F4F7),
    surfaceVariant = Color(0xFF1F2631), onSurfaceVariant = Color(0xFF9AA4B2),
    outline = Color(0xFF3A4352), outlineVariant = Color(0xFF2A3240),
    error = Brand.Red
)

// ---------------- Type ----------------
val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold)
)

private fun style(size: Int, weight: FontWeight, line: Int, spacing: Double = 0.0) =
    TextStyle(fontFamily = Poppins, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = spacing.sp)

val AppTypography = Typography(
    displayLarge = style(44, FontWeight.Bold, 50, -1.0),
    displayMedium = style(36, FontWeight.Bold, 42, -0.8),
    displaySmall = style(30, FontWeight.Bold, 36, -0.5),
    headlineLarge = style(28, FontWeight.Bold, 34, -0.5),
    headlineMedium = style(24, FontWeight.Bold, 30, -0.3),
    headlineSmall = style(20, FontWeight.SemiBold, 26, -0.2),
    titleLarge = style(18, FontWeight.SemiBold, 24),
    titleMedium = style(16, FontWeight.SemiBold, 22),
    titleSmall = style(14, FontWeight.SemiBold, 20),
    bodyLarge = style(16, FontWeight.Normal, 24),
    bodyMedium = style(14, FontWeight.Normal, 21),
    bodySmall = style(12, FontWeight.Normal, 17),
    labelLarge = style(15, FontWeight.SemiBold, 20),
    labelMedium = style(13, FontWeight.Medium, 18),
    labelSmall = style(11, FontWeight.Medium, 15, 0.2)
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// ---------------- Theme mode (live-switchable) ----------------
enum class ThemeMode { SYSTEM, LIGHT, DARK }

object ThemeState {
    var mode by mutableStateOf(ThemeMode.LIGHT)
}

@Composable
fun GymTrainerTheme(content: @Composable () -> Unit) {
    val dark = when (ThemeState.mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val scheme = if (dark) DarkScheme else LightScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = scheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    CompositionLocalProvider(LocalAppColors provides if (dark) DarkApp else LightApp) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, shapes = AppShapes, content = content)
    }
}
