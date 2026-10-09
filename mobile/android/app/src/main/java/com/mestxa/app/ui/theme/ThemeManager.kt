package com.mestxa.app.ui.theme

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.mestxa.app.MestxaApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// The 22 Curated Accent Swatches from prototype specification
val ACCENT_SWATCHES: List<Color?> = listOf(
    null,                   // 0: Default Monochrome
    Color(0xFF6B6B6B),      // 1: Graphite
    Color(0xFF0E7490),      // 2: Cyan
    Color(0xFF7E3AA0),      // 3: Violet
    Color(0xFFC2255C),      // 4: Pink Rose
    Color(0xFF1D4ED8),      // 5: Blue
    Color(0xFF0B5394),      // 6: Cobalt
    Color(0xFF5B3FD1),      // 7: Electric Indigo
    Color(0xFF5B8C26),      // 8: Lime
    Color(0xFF0F8F6F),      // 9: Emerald Green
    Color(0xFF0B6B63),      // 10: Teal
    Color(0xFF4B5D44),      // 11: Forest Sage
    Color(0xFFE0B400),      // 12: Gold
    Color(0xFF8B6F4E),      // 13: Sand Bronze
    Color(0xFF6B4226),      // 14: Mocha
    Color(0xFF9B1C31),      // 15: Crimson
    Color(0xFFE58A00),      // 16: Amber
    Color(0xFFC2410C),      // 17: Orange
    Color(0xFFE11D48),      // 18: Ruby
    Color(0xFF0EA5E9),      // 19: Sky Blue
    Color(0xFF14B8A6),      // 20: Aqua
    Color(0xFFA855F7)       // 21: Purple
)

data class AppThemeColors(
    val bg: Color,
    val tx: Color,
    val s2: Color,
    val sf: Color,
    val sh: Color,
    val ol: Color,
    val ac: Color,
    val acx: Color,
    val sfa: Color,
    val acl: Color,
    val isDark: Boolean
)

enum class ThemeMode {
    DARK, LIGHT, SYSTEM
}

val LocalAppTheme = staticCompositionLocalOf {
    ThemeManager.calculateColors("dark", 0)
}

object ThemeManager {
    private const val PREFS_NAME = "mestxa_theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode" // "dark", "light", "system"
    private const val KEY_ACCENT_IDX = "accent_idx" // 0..21

    private val context: Context get() = MestxaApplication.instance
    private val prefs get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(prefs.getString(KEY_THEME_MODE, "dark") ?: "dark")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    val currentMode: String get() = _themeMode.value

    private val _accentIndex = MutableStateFlow(prefs.getInt(KEY_ACCENT_IDX, 0))
    val accentIndex: StateFlow<Int> = _accentIndex.asStateFlow()

    private val _colors = MutableStateFlow(calculateColors(_themeMode.value, _accentIndex.value))
    val colorsFlow: StateFlow<AppThemeColors> = _colors.asStateFlow()

    val colors: AppThemeColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppTheme.current

    val currentColors: AppThemeColors
        get() = _colors.value

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
        _colors.value = calculateColors(mode, _accentIndex.value)
    }

    fun setAccentIndex(index: Int) {
        val safeIndex = index.coerceIn(0, ACCENT_SWATCHES.size - 1)
        _accentIndex.value = safeIndex
        prefs.edit().putInt(KEY_ACCENT_IDX, safeIndex).apply()
        _colors.value = calculateColors(_themeMode.value, safeIndex)
    }

    fun calculateColors(mode: String, accentIdx: Int): AppThemeColors {
        val isDark = mode != "light"
        val bg = if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF)
        val tx = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
        val s2 = if (isDark) Color(0xFF8A8A8A) else Color(0xFF6B6B6B)
        val sf = if (isDark) Color(0xFF1C1C1C) else Color(0xFFF2F2F2)
        val sh = if (isDark) Color(0xFF121212) else Color(0xFFFFFFFF)
        val ol = if (isDark) Color(0xFF3A3A3A) else Color(0xFFD9D9D9)

        val swatch = ACCENT_SWATCHES.getOrNull(accentIdx)
        val ac = swatch ?: tx

        // Prototype luminance calculation: lum = .299 * R + .587 * G + .114 * B
        val lum = (0.299f * ac.red + 0.587f * ac.green + 0.114f * ac.blue) * 255f
        val acx = if (lum > 150f) Color(0xFF000000) else Color(0xFFFFFFFF)

        // color-mix(in srgb, var(--ac) 26%, var(--bg))
        val sfa = if (swatch == null) sf else colorMix(ac, bg, 0.26f)

        // color-mix(in srgb, var(--ac) 55%, var(--tx))
        val acl = if (swatch == null) tx else colorMix(ac, tx, 0.55f)

        return AppThemeColors(
            bg = bg,
            tx = tx,
            s2 = s2,
            sf = sf,
            sh = sh,
            ol = ol,
            ac = ac,
            acx = acx,
            sfa = sfa,
            acl = acl,
            isDark = isDark
        )
    }

    private fun colorMix(color1: Color, color2: Color, weight1: Float): Color {
        val w2 = 1f - weight1
        return Color(
            red = (color1.red * weight1 + color2.red * w2).coerceIn(0f, 1f),
            green = (color1.green * weight1 + color2.green * w2).coerceIn(0f, 1f),
            blue = (color1.blue * weight1 + color2.blue * w2).coerceIn(0f, 1f),
            alpha = 1f
        )
    }
}
