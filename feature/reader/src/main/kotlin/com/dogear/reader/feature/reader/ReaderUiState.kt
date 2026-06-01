package com.dogear.reader.feature.reader

import com.dogear.reader.core.model.CustomReadingTheme
import com.dogear.reader.core.model.FontChoice
import com.dogear.reader.core.model.Locator
import com.dogear.reader.core.model.ReaderSettings
import com.dogear.reader.core.model.ReadingThemeId
import com.dogear.reader.core.ui.theme.ReadingThemeColors
import com.dogear.reader.core.ui.theme.ReadingThemes
import com.dogear.reader.format.api.content.TocEntry

enum class ReaderKind { NONE, REFLOW, FIXED, IMAGE }

data class ReaderUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val title: String = "",
    val kind: ReaderKind = ReaderKind.NONE,
    val toc: List<TocEntry> = emptyList(),
    val readerFont: FontChoice = FontChoice.LITERATA,
    val settings: ReaderSettings = ReaderSettings(),
    val theme: ReadingThemeColors = ReadingThemes.colors(ReadingThemeId.CREAM, CustomReadingTheme()),
    val initialLocator: Locator? = null,
    val totalPages: Int = 0,
    val controlsVisible: Boolean = false,
    val showTutorial: Boolean = false,
    val progression: Float = 0f,
    val chapter: String = "",
)
