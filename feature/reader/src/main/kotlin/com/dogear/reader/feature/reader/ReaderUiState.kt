package com.dogear.reader.feature.reader

import androidx.compose.ui.graphics.Color
import com.dogear.reader.core.model.FontChoice
import com.dogear.reader.core.model.Locator
import com.dogear.reader.format.api.content.TocEntry

enum class ReaderKind { NONE, REFLOW, FIXED, IMAGE }

/** Page colors for the reading surface (a minimal subset of the Phase-4 reading themes). */
data class ReadingThemeColors(
    val background: Color,
    val text: Color,
    val link: Color,
) {
    companion object {
        val Cream = ReadingThemeColors(Color(0xFFFBF0D9), Color(0xFF5F4B32), Color(0xFF8A5A2B))
        val DarkGrey = ReadingThemeColors(Color(0xFF2A2A2A), Color(0xFFD6D2C8), Color(0xFF9BB8D3))
    }
}

data class ReaderUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val title: String = "",
    val kind: ReaderKind = ReaderKind.NONE,
    val toc: List<TocEntry> = emptyList(),
    val readerFont: FontChoice = FontChoice.LITERATA,
    val theme: ReadingThemeColors = ReadingThemeColors.Cream,
    val initialLocator: Locator? = null,
    val totalPages: Int = 0,
    val controlsVisible: Boolean = false,
    val showTutorial: Boolean = false,
    val progression: Float = 0f,
    val chapter: String = "",
)
