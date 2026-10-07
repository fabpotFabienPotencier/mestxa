package com.mestxa.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AccentWhite,
    onPrimary = OledBlack,
    primaryContainer = SurfaceDark,
    onPrimaryContainer = TextPrimary,
    background = OledBlack,
    onBackground = TextPrimary,
    surface = OledBlack,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = TextSecondary,
    outline = BorderHairline,
    error = DestructiveRed,
    onError = TextPrimary
)

@Composable
fun MestxaTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = OledBlack.toArgb()
            window.navigationBarColor = OledBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
