package com.dogear.reader.core.model

/** Light/dark behavior for the app chrome. */
enum class ThemeMode { LIGHT, DARK, SYSTEM }

/**
 * Identifiers for the curated app color palettes (see UX Report §9). DYNAMIC maps to
 * Material You on Android 12+ and falls back to INK elsewhere.
 */
enum class AppPalette(val displayName: String) {
    INK("Ink"),
    PAPER("Paper"),
    FOREST("Forest"),
    OCEAN("Ocean"),
    PLUM("Plum"),
    EMBER("Ember"),
    SLATE("Slate"),
    SAND("Sand"),
    ROSE("Rose"),
    TEAL("Teal"),
    HIGH_CONTRAST("High Contrast"),
    DYNAMIC("Dynamic (Material You)"),
}

/** Bundled font families (see Typography Report). Values are stable keys for persistence. */
enum class FontChoice(val displayName: String) {
    SYSTEM("System default"),
    LITERATA("Literata"),
    BITTER("Bitter"),
    MERRIWEATHER("Merriweather"),
    SOURCE_SERIF("Source Serif 4"),
    LORA("Lora"),
    NOTO_SERIF("Noto Serif"),
    PT_SERIF("PT Serif"),
    CRIMSON_PRO("Crimson Pro"),
    EB_GARAMOND("EB Garamond"),
    VOLLKORN("Vollkorn"),
    CHARTER("Charter"),
    SOURCE_SANS("Source Sans 3"),
    INTER("Inter"),
    LEXEND("Lexend"),
    ATKINSON("Atkinson Hyperlegible"),
    OPEN_DYSLEXIC("OpenDyslexic"),
    NOTO_SANS("Noto Sans"),
}

/**
 * App-wide settings persisted via DataStore. Reader-specific typography settings will be
 * added alongside the reader feature in a later phase.
 */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val palette: AppPalette = AppPalette.INK,
    val uiFont: FontChoice = FontChoice.INTER,
    val readerFont: FontChoice = FontChoice.LITERATA,
    val viewMode: ViewMode = ViewMode.GRID,
    val sort: SortOption = SortOption.RECENTLY_READ,
    val sortAscending: Boolean = false,
    val showTutorialOverlay: Boolean = true,
)
