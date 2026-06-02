package com.dogear.reader.core.ui.theme

import androidx.compose.ui.text.font.FontFamily
import com.dogear.reader.core.model.FontChoice

/**
 * Single source of truth mapping a [FontChoice] to a Compose [FontFamily] for the shelf and
 * app UI (the reader will additionally map each choice to an embeddable @font-face asset).
 *
 * NOTE: the bundled OFL/free TTFs are added under `res/font` in a follow-up commit (they are
 * binary assets). Until then each family resolves to its closest generic so the app builds and
 * runs; swapping in the real `FontFamily(Font(R.font.literata))` etc. is a localized change.
 */
object FontRegistry {

    private val serif = setOf(
        FontChoice.LITERATA, FontChoice.BITTER, FontChoice.MERRIWEATHER, FontChoice.SOURCE_SERIF,
        FontChoice.LORA, FontChoice.NOTO_SERIF, FontChoice.PT_SERIF, FontChoice.CRIMSON_PRO,
        FontChoice.EB_GARAMOND, FontChoice.VOLLKORN, FontChoice.CHARTER,
    )

    private val sans = setOf(
        FontChoice.SOURCE_SANS, FontChoice.INTER, FontChoice.LEXEND, FontChoice.ATKINSON,
        FontChoice.OPEN_DYSLEXIC, FontChoice.NOTO_SANS,
    )

    fun fontFamily(choice: FontChoice): FontFamily = when (choice) {
        FontChoice.SYSTEM -> FontFamily.Default
        in serif -> FontFamily.Serif
        in sans -> FontFamily.SansSerif
        else -> FontFamily.Default
    }

    /** All selectable families, in display order, for settings pickers. */
    val all: List<FontChoice> = FontChoice.entries
}
