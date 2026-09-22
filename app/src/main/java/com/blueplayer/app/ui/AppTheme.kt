package com.blueplayer.app.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.core.view.WindowCompat
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

        // Sync the Android system status bar & navigation bar with the
        // APP theme (not the system theme):
        // - bar background = current theme background (black on AMOLED)
        // - bar icons: light on dark themes, dark on light themes
        val view = LocalView.current
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = scheme.background.toArgb()
            window.navigationBarColor = scheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
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