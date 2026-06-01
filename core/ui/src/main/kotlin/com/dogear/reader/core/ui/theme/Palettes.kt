package com.dogear.reader.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.dogear.reader.core.model.AppPalette

/**
 * The curated app palettes (UX Report §9). Each [AppPalette] resolves to a coherent Material 3
 * light/dark [ColorScheme]. Static palettes give every device — old or new — the same
 * intentional result; DYNAMIC (Material You) is resolved at the app layer where context is
 * available, falling back here to INK.
 */
internal data class PaletteSpec(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val lightBackground: Color,
    val darkBackground: Color,
)

private fun spec(
    primary: Long,
    secondary: Long,
    tertiary: Long,
    lightBg: Long = 0xFFFCFCFC,
    darkBg: Long = 0xFF121212,
) = PaletteSpec(Color(primary), Color(secondary), Color(tertiary), Color(lightBg), Color(darkBg))

private val Specs: Map<AppPalette, PaletteSpec> = mapOf(
    AppPalette.INK to spec(0xFF3A3A3A, 0xFF6E6E6E, 0xFF8A7A66, lightBg = 0xFFFAF9F7, darkBg = 0xFF121212),
    AppPalette.PAPER to spec(0xFF7A6A52, 0xFF9C8463, 0xFFB99A5C, lightBg = 0xFFFBF6EC, darkBg = 0xFF1A1712),
    AppPalette.FOREST to spec(0xFF2E6B4F, 0xFF4E8A6B, 0xFF7A8C3F, lightBg = 0xFFF6FAF6, darkBg = 0xFF101613),
    AppPalette.OCEAN to spec(0xFF1E5E8A, 0xFF3E7FA8, 0xFF4FA0B0, lightBg = 0xFFF4F8FB, darkBg = 0xFF0F1519),
    AppPalette.PLUM to spec(0xFF6A4A8A, 0xFF8A6AA8, 0xFFB05A8A, lightBg = 0xFFF9F6FB, darkBg = 0xFF161018),
    AppPalette.EMBER to spec(0xFFB0502A, 0xFFCF6F3E, 0xFFD99A3E, lightBg = 0xFFFBF6F3, darkBg = 0xFF1A120E),
    AppPalette.SLATE to spec(0xFF44586A, 0xFF647888, 0xFF7E8C9A, lightBg = 0xFFF5F7F9, darkBg = 0xFF12151A),
    AppPalette.SAND to spec(0xFF9A7A3E, 0xFFB89A5E, 0xFFA88A4E, lightBg = 0xFFFCF8EF, darkBg = 0xFF171410),
    AppPalette.ROSE to spec(0xFFA85A6E, 0xFFC07A8A, 0xFFB06A9A, lightBg = 0xFFFBF5F6, darkBg = 0xFF1A1214),
    AppPalette.TEAL to spec(0xFF1E7A74, 0xFF3E9A92, 0xFF4FA0B0, lightBg = 0xFFF4FAF9, darkBg = 0xFF0F1716),
    AppPalette.HIGH_CONTRAST to spec(0xFF000000, 0xFF1A1A1A, 0xFF333333, lightBg = 0xFFFFFFFF, darkBg = 0xFF000000),
    AppPalette.DYNAMIC to spec(0xFF3A3A3A, 0xFF6E6E6E, 0xFF8A7A66, lightBg = 0xFFFAF9F7, darkBg = 0xFF121212),
)

internal fun staticColorScheme(palette: AppPalette, dark: Boolean): ColorScheme {
    val s = Specs[palette] ?: Specs.getValue(AppPalette.INK)
    return if (dark) {
        darkColorScheme(
            primary = s.primary.lighten(0.25f),
            secondary = s.secondary.lighten(0.2f),
            tertiary = s.tertiary.lighten(0.2f),
            background = s.darkBackground,
            surface = s.darkBackground,
        )
    } else {
        lightColorScheme(
            primary = s.primary,
            secondary = s.secondary,
            tertiary = s.tertiary,
            background = s.lightBackground,
            surface = s.lightBackground,
        )
    }
}

private fun Color.lighten(fraction: Float): Color = Color(
    red = red + (1f - red) * fraction,
    green = green + (1f - green) * fraction,
    blue = blue + (1f - blue) * fraction,
    alpha = alpha,
)
