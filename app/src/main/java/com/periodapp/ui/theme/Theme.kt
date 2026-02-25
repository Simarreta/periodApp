package com.periodapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Rose400,
    onPrimary = Color.White,
    primaryContainer = Rose100,
    onPrimaryContainer = TextPrimary,
    secondary = Sage400,
    onSecondary = Color.White,
    background = WarmWhite,
    onBackground = TextPrimary,
    surface = WarmWhite,
    onSurface = TextPrimary,
    surfaceVariant = WarmGray,
    onSurfaceVariant = TextSecondary,
    outline = Rose200
)

private val DarkColorScheme = darkColorScheme(
    primary = Rose300,
    onPrimary = TextPrimary,
    primaryContainer = Rose500,
    onPrimaryContainer = Color.White,
    secondary = Sage200,
    onSecondary = TextPrimary,
    background = Color(0xFF1C1B1A),
    onBackground = Color(0xFFE6E2DE),
    surface = Color(0xFF252422),
    onSurface = Color(0xFFE6E2DE),
    surfaceVariant = Color(0xFF3D3A37),
    onSurfaceVariant = Color(0xFFCAC6C1),
    outline = Rose400
)

@Composable
fun PeriodAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
