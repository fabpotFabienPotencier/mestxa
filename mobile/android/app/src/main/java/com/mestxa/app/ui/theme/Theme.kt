package com.mestxa.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun MestxaTheme(
    content: @Composable () -> Unit
) {
    val themeColors by ThemeManager.colorsFlow.collectAsState()

    val colorScheme = if (themeColors.isDark) {
        darkColorScheme(
            primary = themeColors.ac,
            onPrimary = themeColors.acx,
            primaryContainer = themeColors.sf,
            onPrimaryContainer = themeColors.tx,
            background = themeColors.bg,
            onBackground = themeColors.tx,
            surface = themeColors.bg,
            onSurface = themeColors.tx,
            surfaceVariant = themeColors.sf,
            onSurfaceVariant = themeColors.s2,
            outline = themeColors.ol,
            error = DestructiveRed,
            onError = themeColors.tx
        )
    } else {
        lightColorScheme(
            primary = themeColors.ac,
            onPrimary = themeColors.acx,
            primaryContainer = themeColors.sf,
            onPrimaryContainer = themeColors.tx,
            background = themeColors.bg,
            onBackground = themeColors.tx,
            surface = themeColors.bg,
            onSurface = themeColors.tx,
            surfaceVariant = themeColors.sf,
            onSurfaceVariant = themeColors.s2,
            outline = themeColors.ol,
            error = DestructiveRed,
            onError = themeColors.tx
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = themeColors.bg.toArgb()
            window.navigationBarColor = themeColors.bg.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !themeColors.isDark
            controller.isAppearanceLightNavigationBars = !themeColors.isDark
        }
    }

    CompositionLocalProvider(LocalAppTheme provides themeColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
