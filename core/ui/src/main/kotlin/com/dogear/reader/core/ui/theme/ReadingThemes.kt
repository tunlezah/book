package com.dogear.reader.core.ui.theme

import androidx.compose.ui.graphics.Color
import com.dogear.reader.core.model.CustomReadingTheme
import com.dogear.reader.core.model.ReadingThemeId

/** Resolved colors for the reading surface (background/text/link/highlight). */
data class ReadingThemeColors(
    val background: Color,
    val text: Color,
    val link: Color,
    val highlight: Color,
)

/** Maps a [ReadingThemeId] (+ the user's custom theme) to concrete colors (UX Report §8). */
object ReadingThemes {

    fun colors(themeId: ReadingThemeId, custom: CustomReadingTheme): ReadingThemeColors = when (themeId) {
        ReadingThemeId.PURE_WHITE -> ReadingThemeColors(Color(0xFFFFFFFF), Color(0xFF111111), Color(0xFF1A66B0), Color(0xFFFFE08A))
        ReadingThemeId.CREAM -> ReadingThemeColors(Color(0xFFFBF0D9), Color(0xFF5F4B32), Color(0xFF8A5A2B), Color(0xFFEBD27A))
        ReadingThemeId.SEPIA -> ReadingThemeColors(Color(0xFFF4ECD8), Color(0xFF5B4636), Color(0xFF8A5A2B), Color(0xFFE3C97A))
        ReadingThemeId.WARM_PAPER -> ReadingThemeColors(Color(0xFFF2E8D5), Color(0xFF3D3526), Color(0xFF7A5A2B), Color(0xFFE0C77A))
        ReadingThemeId.NEWSPAPER -> ReadingThemeColors(Color(0xFFEDEAE2), Color(0xFF22211E), Color(0xFF2B5A8A), Color(0xFFD9D08A))
        ReadingThemeId.LIGHT_GREY -> ReadingThemeColors(Color(0xFFE8E8E8), Color(0xFF1E1E1E), Color(0xFF2B5A8A), Color(0xFFCFC98A))
        ReadingThemeId.DARK_GREY -> ReadingThemeColors(Color(0xFF2A2A2A), Color(0xFFD6D2C8), Color(0xFF9BB8D3), Color(0xFF6E6438))
        ReadingThemeId.AMOLED_BLACK -> ReadingThemeColors(Color(0xFF000000), Color(0xFFC9C5BC), Color(0xFF8FB0CC), Color(0xFF5A5230))
        ReadingThemeId.CUSTOM -> ReadingThemeColors(
            Color(custom.background),
            Color(custom.text),
            Color(custom.link),
            Color(custom.highlight),
        )
    }

    /** Whether the theme is dark, used to tint reader chrome appropriately. */
    fun isDark(themeId: ReadingThemeId, custom: CustomReadingTheme): Boolean = when (themeId) {
        ReadingThemeId.DARK_GREY, ReadingThemeId.AMOLED_BLACK -> true
        ReadingThemeId.CUSTOM -> luminance(custom.background) < 0.5
        else -> false
    }

    private fun luminance(argb: Long): Double {
        val r = ((argb shr 16) and 0xFF) / 255.0
        val g = ((argb shr 8) and 0xFF) / 255.0
        val b = (argb and 0xFF) / 255.0
        return 0.299 * r + 0.587 * g + 0.114 * b
    }
}
