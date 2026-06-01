package com.dogear.reader.core.model

/** Built-in reading-surface themes (UX Report §8) plus a user-defined custom theme. */
enum class ReadingThemeId(val displayName: String) {
    PURE_WHITE("Pure White"),
    CREAM("Cream"),
    SEPIA("Sepia"),
    WARM_PAPER("Warm Paper"),
    NEWSPAPER("Newspaper"),
    LIGHT_GREY("Light Grey"),
    DARK_GREY("Dark Grey"),
    AMOLED_BLACK("AMOLED Black"),
    CUSTOM("Custom"),
}

enum class ReaderTextAlign(val displayName: String, val css: String) {
    LEFT("Left", "left"),
    JUSTIFY("Justify", "justify"),
}

enum class PageAnimation(val displayName: String) {
    NONE("None"), SLIDE("Slide"), FADE("Fade"), CURL("Curl")
}

enum class BrightnessMode(val displayName: String) {
    SYSTEM("System"), READER("Reader"), AUTO("Auto")
}

enum class KeepAwakeMode(val displayName: String) {
    NEVER("Never"), WHILE_READING("While reading"), WHILE_CHARGING("While charging")
}

enum class OrientationLock(val displayName: String) {
    AUTO("Auto"), PORTRAIT("Portrait"), LANDSCAPE("Landscape")
}

/** A user-defined reading theme. Colors are ARGB longs so the model stays pure-Kotlin. */
data class CustomReadingTheme(
    val background: Long = 0xFFFBF0D9,
    val text: Long = 0xFF5F4B32,
    val link: Long = 0xFF8A5A2B,
    val highlight: Long = 0xFFFFE08A,
)

/**
 * All reader-surface preferences. Persisted via DataStore and observed so every change applies
 * live to the open book (Phase 4). Font family lives in [AppSettings.readerFont].
 */
data class ReaderSettings(
    val themeId: ReadingThemeId = ReadingThemeId.CREAM,
    val custom: CustomReadingTheme = CustomReadingTheme(),
    val fontSizeSp: Int = 18,
    val fontWeight: Int = 400,
    val lineHeight: Float = 1.5f,
    val paragraphSpacingEm: Float = 0.8f,
    val marginHorizontalDp: Int = 24,
    val marginVerticalDp: Int = 24,
    val textAlign: ReaderTextAlign = ReaderTextAlign.LEFT,
    val hyphenation: Boolean = true,
    val pageAnimation: PageAnimation = PageAnimation.NONE,
    val brightnessMode: BrightnessMode = BrightnessMode.SYSTEM,
    val brightnessLevel: Float = 0.5f,
    val keepAwake: KeepAwakeMode = KeepAwakeMode.WHILE_READING,
    val orientation: OrientationLock = OrientationLock.AUTO,
    val volumeKeyPaging: Boolean = false,
) {
    companion object {
        const val MIN_FONT_SP = 12
        const val MAX_FONT_SP = 32
    }
}
