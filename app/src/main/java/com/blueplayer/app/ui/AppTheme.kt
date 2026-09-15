package com.blueplayer.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.blueplayer.core.domain.model.TextSize
import com.blueplayer.core.domain.model.ThemeMode
import com.blueplayer.ui.theme.BluePlayerTheme

@Composable
fun AppTheme(
    themeMode: ThemeMode,
    textSize: TextSize,
    dynamicColor: Boolean,
    seedColor: Color?,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }

    BluePlayerTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        seedColor = seedColor
    ) {
        val base = MaterialTheme.colorScheme
        val scheme = if (themeMode == ThemeMode.AMOLED) {
            base.copy(
                background = Color.Black,
                onBackground = Color.White,
                surface = Color.Black,
                onSurface = Color.White,
                surfaceVariant = Color(0xFF121212),
                onSurfaceVariant = Color(0xFFD0D0D0)
            )
        } else {
            base
        }

        MaterialTheme(
            colorScheme = scheme,
            typography = scaleTypography(MaterialTheme.typography, textSize.scale),
            shapes = MaterialTheme.shapes
        ) {
            content()
        }
    }
}

private fun scaleTypography(base: Typography, scale: Float): Typography = base.copy(
    displayLarge = base.displayLarge.scale(scale),
    displayMedium = base.displayMedium.scale(scale),
    displaySmall = base.displaySmall.scale(scale),
    headlineLarge = base.headlineLarge.scale(scale),
    headlineMedium = base.headlineMedium.scale(scale),
    headlineSmall = base.headlineSmall.scale(scale),
    titleLarge = base.titleLarge.scale(scale),
    titleMedium = base.titleMedium.scale(scale),
    titleSmall = base.titleSmall.scale(scale),
    bodyLarge = base.bodyLarge.scale(scale),
    bodyMedium = base.bodyMedium.scale(scale),
    bodySmall = base.bodySmall.scale(scale),
    labelLarge = base.labelLarge.scale(scale),
    labelMedium = base.labelMedium.scale(scale),
    labelSmall = base.labelSmall.scale(scale)
)

private fun TextStyle.scale(scale: Float): TextStyle = copy(
    fontSize = fontSize * scale,
    lineHeight = lineHeight * scale
)