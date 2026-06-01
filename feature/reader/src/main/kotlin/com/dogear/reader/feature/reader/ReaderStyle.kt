package com.dogear.reader.feature.reader

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.dogear.reader.core.model.FontChoice
import com.dogear.reader.feature.reader.reflow.ReflowStyle

/** Maps the reading theme + chosen font into the CSS inputs for the reflow engine. */
internal fun reflowStyle(theme: ReadingThemeColors, font: FontChoice): ReflowStyle = ReflowStyle(
    fontFamilyCss = font.toCssFontStack(),
    textColor = theme.text.toCssHex(),
    backgroundColor = theme.background.toCssHex(),
    linkColor = theme.link.toCssHex(),
)

internal fun Color.toCssHex(): String = String.format("#%06X", 0xFFFFFF and toArgb())

/**
 * Until the OFL TTFs are bundled and served via @font-face, map each choice to a close generic
 * stack so the reader still honors serif/sans intent.
 */
private fun FontChoice.toCssFontStack(): String = when (this) {
    FontChoice.INTER, FontChoice.SOURCE_SANS, FontChoice.LEXEND, FontChoice.ATKINSON,
    FontChoice.OPEN_DYSLEXIC, FontChoice.NOTO_SANS,
    -> "system-ui, -apple-system, 'Segoe UI', sans-serif"
    else -> "Georgia, 'Times New Roman', serif"
}
