package com.oscarruiz.birthdates.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.oscarruiz.birthdates.domain.model.ThemeMode

// Paleta del diseño: baya (primario) y azafrán (solo "Hoy" y Pro).
private val LightColors = lightColorScheme(
    primary = Color(0xFFB3226B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFADCEA),
    onPrimaryContainer = Color(0xFF5C0F35),
    secondary = Color(0xFF6E5C79),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF2ECF7),
    onSecondaryContainer = Color(0xFF2B1735),
    tertiary = Color(0xFFF5B02E),
    onTertiary = Color(0xFF2A1A00),
    tertiaryContainer = Color(0xFFFFE9B8),
    onTertiaryContainer = Color(0xFF2A1A00),
    background = Color(0xFFFBF8FD),
    onBackground = Color(0xFF2B1735),
    surface = Color(0xFFFBF8FD),
    onSurface = Color(0xFF2B1735),
    surfaceVariant = Color(0xFFF2ECF7),
    onSurfaceVariant = Color(0xFF6E5C79),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF6F1F9),
    surfaceContainerHigh = Color(0xFFF2ECF7),
    surfaceContainerHighest = Color(0xFFECE5F1),
    outline = Color(0xFF6E5C79),
    outlineVariant = Color(0xFFE6DDED),
    inverseSurface = Color(0xFF33253D),
    inverseOnSurface = Color(0xFFF4EAF8),
    inversePrimary = Color(0xFFFF9FCB),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF8DC1),
    onPrimary = Color(0xFF4A0A2C),
    primaryContainer = Color(0xFF5E1640),
    onPrimaryContainer = Color(0xFFFFD9EA),
    secondary = Color(0xFFBCABC9),
    onSecondary = Color(0xFF2B1735),
    secondaryContainer = Color(0xFF2E2040),
    onSecondaryContainer = Color(0xFFF4EAF8),
    tertiary = Color(0xFFF5B02E),
    onTertiary = Color(0xFF2A1A00),
    tertiaryContainer = Color(0xFF4B3610),
    onTertiaryContainer = Color(0xFFFFDF9E),
    background = Color(0xFF170F1F),
    onBackground = Color(0xFFF4EAF8),
    surface = Color(0xFF170F1F),
    onSurface = Color(0xFFF4EAF8),
    surfaceVariant = Color(0xFF2E2040),
    onSurfaceVariant = Color(0xFFBCABC9),
    surfaceContainerLowest = Color(0xFF120B18),
    surfaceContainerLow = Color(0xFF22172D),
    surfaceContainer = Color(0xFF261B32),
    surfaceContainerHigh = Color(0xFF2E2040),
    surfaceContainerHighest = Color(0xFF372849),
    outline = Color(0xFFBCABC9),
    outlineVariant = Color(0xFF3A2A4B),
    inverseSurface = Color(0xFFEADFF0),
    inverseOnSurface = Color(0xFF2B1735),
    inversePrimary = Color(0xFFB3226B),
    error = Color(0xFFF2665C),
)

/** Colores de los avatares con la inicial: (fondo, texto). */
@Immutable
data class AvatarColors(val pairs: List<Pair<Color, Color>>)

private val LightAvatars = AvatarColors(
    listOf(
        Color(0xFFFADCEA) to Color(0xFF8A1750),
        Color(0xFFD3EFEC) to Color(0xFF0B5E58),
        Color(0xFFFFE9B8) to Color(0xFF6B4700),
        Color(0xFFDCE2FA) to Color(0xFF2C3F96),
        Color(0xFFDDF1D4) to Color(0xFF2F6B1C),
        Color(0xFFFFDDD0) to Color(0xFF9A3412),
    ),
)

private val DarkAvatars = AvatarColors(
    listOf(
        Color(0xFF5E1640) to Color(0xFFFFD0E5),
        Color(0xFF0F4B47) to Color(0xFFBDF0EB),
        Color(0xFF4B3610) to Color(0xFFFFDF9E),
        Color(0xFF26305F) to Color(0xFFD0D8FF),
        Color(0xFF23421A) to Color(0xFFCDEEBF),
        Color(0xFF5A2612) to Color(0xFFFFD3BF),
    ),
)

val LocalAvatarColors = staticCompositionLocalOf { LightAvatars }

private val base = Typography()
private val AppTypography = Typography(
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Bold),
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 30.sp),
)

@Composable
fun BirthdaysTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    androidx.compose.runtime.CompositionLocalProvider(
        LocalAvatarColors provides if (dark) DarkAvatars else LightAvatars,
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = AppTypography,
            content = content,
        )
    }
}
