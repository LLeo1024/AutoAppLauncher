package com.leo.autoapplaucher.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF0F111A)

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = SurfaceLight,
    primaryContainer = PrimaryLight,
    secondary = Accent,
    onSecondary = SurfaceLight,
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = SurfaceLight,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = Error,
    outline = TextTertiary
)

private val DarkColors = darkColorScheme(
    primary = PrimaryDarkTheme,
    onPrimary = SurfaceDark,
    primaryContainer = PrimaryDark,
    secondary = Accent,
    onSecondary = SurfaceDark,
    background = BackgroundDark,
    onBackground = SurfaceLight,
    surface = SurfaceDark,
    onSurface = SurfaceLight,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextTertiary,
    error = Error,
    outline = TextTertiary
)

@Composable
fun AutoAppLauncherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
