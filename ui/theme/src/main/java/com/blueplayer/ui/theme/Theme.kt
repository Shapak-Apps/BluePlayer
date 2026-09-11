package com.blueplayer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

private val LightColors = lightColorScheme(
    primary = Color(0xFF1976D2),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFBBDEFB),
    onPrimaryContainer = Color(0xFF0A3A6B),

    secondary = Color(0xFF0061A4),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD1E4FF),
    onSecondaryContainer = Color(0xFF001D36),

    tertiary = Color(0xFF1976D2),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBBDEFB),
    onTertiaryContainer = Color(0xFF0A3A6B),

    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF000000),

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C1E),

    surfaceVariant = Color(0xFFE9ECF0),
    onSurfaceVariant = Color(0xFF42474E),

    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFDBD9DD),

    outline = Color(0xFF72787E),
    outlineVariant = Color(0xFFC2C7CD),

    surfaceTint = Color.Transparent,

    inverseSurface = Color(0xFF2F3033),
    inverseOnSurface = Color(0xFFF1F0F4),
    inversePrimary = Color(0xFFA8C8FF),

    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),

    scrim = Color(0xFF000000),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF2196F3),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF0D47A1),
    onPrimaryContainer = Color(0xFFBBDEFB),

    secondary = Color(0xFF64B5F6),
    onSecondary = Color(0xFF001A33),
    secondaryContainer = Color(0xFF10304A),
    onSecondaryContainer = Color(0xFFD6E3F3),

    tertiary = Color(0xFF2196F3),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF0D47A1),
    onTertiaryContainer = Color(0xFFBBDEFB),

    background = Color(0xFF121212),
    onBackground = Color(0xFFE3E2E6),

    surface = Color(0xFF121212),
    onSurface = Color(0xFFE3E2E6),

    surfaceVariant = Color(0xFF1E1F22),
    onSurfaceVariant = Color(0xFFC4C6D0),

    surfaceBright = Color(0xFF38393C),
    surfaceDim = Color(0xFF121212),

    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF44474E),

    surfaceTint = Color(0xFF2196F3),

    inverseSurface = Color(0xFFE3E2E6),
    inverseOnSurface = Color(0xFF303034),
    inversePrimary = Color(0xFF1976D2),

    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),

    scrim = Color(0xFF000000),
)

private fun Color.mix(other: Color, fraction: Float): Color = Color(
    red = red + (other.red - red) * fraction,
    green = green + (other.green - green) * fraction,
    blue = blue + (other.blue - blue) * fraction,
    alpha = 1f
)

private val White = Color(0xFFFFFFFF)
private val Black = Color(0xFF000000)

private fun seededLight(seed: Color) = LightColors.copy(
    primary = seed,
    onPrimary = White,
    primaryContainer = seed.mix(White, 0.85f),
    onPrimaryContainer = seed.mix(Black, 0.45f),
    secondary = seed.mix(Black, 0.1f),
    onSecondary = White,
    secondaryContainer = seed.mix(White, 0.8f),
    onSecondaryContainer = seed.mix(Black, 0.45f),
    tertiary = seed.mix(Black, 0.2f),
    onTertiary = White,
    tertiaryContainer = seed.mix(White, 0.75f),
    onTertiaryContainer = seed.mix(Black, 0.45f),
    surfaceTint = seed
)

private fun seededDark(seed: Color) = DarkColors.copy(
    primary = seed.mix(White, 0.35f),
    onPrimary = Black,
    primaryContainer = seed.mix(Black, 0.55f),
    onPrimaryContainer = seed.mix(White, 0.85f),
    secondary = seed.mix(White, 0.25f),
    onSecondary = Black,
    secondaryContainer = seed.mix(Black, 0.65f),
    onSecondaryContainer = seed.mix(White, 0.8f),
    tertiary = seed.mix(White, 0.15f),
    onTertiary = Black,
    tertiaryContainer = seed.mix(Black, 0.6f),
    onTertiaryContainer = seed.mix(White, 0.75f),
    surfaceTint = seed.mix(White, 0.35f)
)

private val base = Typography()

private fun TextStyle.sharp(weight: FontWeight): TextStyle =
    copy(fontFamily = FontFamily.Default, fontWeight = weight)

private val AppTypography = Typography(
    displayLarge = base.displayLarge.sharp(FontWeight.Bold),
    displayMedium = base.displayMedium.sharp(FontWeight.Bold),
    displaySmall = base.displaySmall.sharp(FontWeight.Bold),
    headlineLarge = base.headlineLarge.sharp(FontWeight.SemiBold),
    headlineMedium = base.headlineMedium.sharp(FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.sharp(FontWeight.SemiBold),
    titleLarge = base.titleLarge.sharp(FontWeight.SemiBold),
    titleMedium = base.titleMedium.sharp(FontWeight.Medium),
    titleSmall = base.titleSmall.sharp(FontWeight.Medium),
    bodyLarge = base.bodyLarge.sharp(FontWeight.Normal),
    bodyMedium = base.bodyMedium.sharp(FontWeight.Normal),
    bodySmall = base.bodySmall.sharp(FontWeight.Normal),
    labelLarge = base.labelLarge.sharp(FontWeight.Medium),
    labelMedium = base.labelMedium.sharp(FontWeight.Medium),
    labelSmall = base.labelSmall.sharp(FontWeight.Medium)
)

@Composable
fun BluePlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    seedColor: Color? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        seedColor != null ->
            if (darkTheme) seededDark(seedColor) else seededLight(seedColor)
        else ->
            if (darkTheme) DarkColors else LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            content = content
        )
    }
}