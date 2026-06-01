package com.dogear.reader.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dogear.reader.core.model.AppPalette
import com.dogear.reader.core.model.AppSettings
import com.dogear.reader.core.model.BrightnessMode
import com.dogear.reader.core.model.CustomReadingTheme
import com.dogear.reader.core.model.FontChoice
import com.dogear.reader.core.model.KeepAwakeMode
import com.dogear.reader.core.model.OrientationLock
import com.dogear.reader.core.model.PageAnimation
import com.dogear.reader.core.model.ReaderSettings
import com.dogear.reader.core.model.ReaderTextAlign
import com.dogear.reader.core.model.ReadingThemeId
import com.dogear.reader.core.model.SortOption
import com.dogear.reader.core.model.ThemeMode
import com.dogear.reader.core.model.ViewMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Typed, Flow-based access to app settings. Reading from a single source of truth means a
 * theme/palette/font/view change applies live across the shelf, settings, and (later) reader.
 * Unknown stored values degrade to defaults so a downgrade or corrupt value never crashes.
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val settings: Flow<AppSettings> = dataStore.data.map { prefs -> prefs.toAppSettings() }

    /** Reader-surface preferences, observed so changes apply live to the open book. */
    val readerSettings: Flow<ReaderSettings> = dataStore.data.map { prefs -> prefs.toReaderSettings() }

    suspend fun updateReaderSettings(transform: (ReaderSettings) -> ReaderSettings) {
        dataStore.edit { prefs ->
            val updated = transform(prefs.toReaderSettings())
            prefs.writeReaderSettings(updated)
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) = put(Keys.THEME_MODE, mode.name)
    suspend fun setPalette(palette: AppPalette) = put(Keys.PALETTE, palette.name)
    suspend fun setUiFont(font: FontChoice) = put(Keys.UI_FONT, font.name)
    suspend fun setReaderFont(font: FontChoice) = put(Keys.READER_FONT, font.name)
    suspend fun setViewMode(mode: ViewMode) = put(Keys.VIEW_MODE, mode.name)
    suspend fun setShowTutorialOverlay(show: Boolean) = put(Keys.TUTORIAL, show)

    suspend fun setSort(sort: SortOption, ascending: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.SORT] = sort.name
            prefs[Keys.SORT_ASC] = ascending
        }
    }

    /** Overwrites all settings (used by Restore). */
    suspend fun restore(app: AppSettings, reader: ReaderSettings) {
        dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = app.themeMode.name
            prefs[Keys.PALETTE] = app.palette.name
            prefs[Keys.UI_FONT] = app.uiFont.name
            prefs[Keys.READER_FONT] = app.readerFont.name
            prefs[Keys.VIEW_MODE] = app.viewMode.name
            prefs[Keys.SORT] = app.sort.name
            prefs[Keys.SORT_ASC] = app.sortAscending
            prefs[Keys.TUTORIAL] = app.showTutorialOverlay
            prefs.writeReaderSettings(reader)
        }
    }

    private suspend fun put(key: Preferences.Key<String>, value: String) {
        dataStore.edit { it[key] = value }
    }

    private suspend fun put(key: Preferences.Key<Boolean>, value: Boolean) {
        dataStore.edit { it[key] = value }
    }

    private fun Preferences.toAppSettings(): AppSettings = AppSettings(
        themeMode = enumOf(this[Keys.THEME_MODE], ThemeMode.SYSTEM),
        palette = enumOf(this[Keys.PALETTE], AppPalette.INK),
        uiFont = enumOf(this[Keys.UI_FONT], FontChoice.INTER),
        readerFont = enumOf(this[Keys.READER_FONT], FontChoice.LITERATA),
        viewMode = enumOf(this[Keys.VIEW_MODE], ViewMode.GRID),
        sort = enumOf(this[Keys.SORT], SortOption.RECENTLY_READ),
        sortAscending = this[Keys.SORT_ASC] ?: false,
        showTutorialOverlay = this[Keys.TUTORIAL] ?: true,
    )

    private fun Preferences.toReaderSettings(): ReaderSettings {
        val defaults = ReaderSettings()
        return ReaderSettings(
            themeId = enumOf(this[Keys.READING_THEME], defaults.themeId),
            custom = CustomReadingTheme(
                background = this[Keys.CUSTOM_BG] ?: defaults.custom.background,
                text = this[Keys.CUSTOM_TEXT] ?: defaults.custom.text,
                link = this[Keys.CUSTOM_LINK] ?: defaults.custom.link,
                highlight = this[Keys.CUSTOM_HIGHLIGHT] ?: defaults.custom.highlight,
            ),
            fontSizeSp = this[Keys.FONT_SIZE] ?: defaults.fontSizeSp,
            fontWeight = this[Keys.FONT_WEIGHT] ?: defaults.fontWeight,
            lineHeight = this[Keys.LINE_HEIGHT] ?: defaults.lineHeight,
            paragraphSpacingEm = this[Keys.PARAGRAPH_SPACING] ?: defaults.paragraphSpacingEm,
            marginHorizontalDp = this[Keys.MARGIN_H] ?: defaults.marginHorizontalDp,
            marginVerticalDp = this[Keys.MARGIN_V] ?: defaults.marginVerticalDp,
            textAlign = enumOf(this[Keys.TEXT_ALIGN], defaults.textAlign),
            hyphenation = this[Keys.HYPHENATION] ?: defaults.hyphenation,
            pageAnimation = enumOf(this[Keys.PAGE_ANIMATION], defaults.pageAnimation),
            brightnessMode = enumOf(this[Keys.BRIGHTNESS_MODE], defaults.brightnessMode),
            brightnessLevel = this[Keys.BRIGHTNESS_LEVEL] ?: defaults.brightnessLevel,
            keepAwake = enumOf(this[Keys.KEEP_AWAKE], defaults.keepAwake),
            orientation = enumOf(this[Keys.ORIENTATION], defaults.orientation),
            volumeKeyPaging = this[Keys.VOLUME_PAGING] ?: defaults.volumeKeyPaging,
        )
    }

    private fun MutablePreferences.writeReaderSettings(s: ReaderSettings) {
        this[Keys.READING_THEME] = s.themeId.name
        this[Keys.CUSTOM_BG] = s.custom.background
        this[Keys.CUSTOM_TEXT] = s.custom.text
        this[Keys.CUSTOM_LINK] = s.custom.link
        this[Keys.CUSTOM_HIGHLIGHT] = s.custom.highlight
        this[Keys.FONT_SIZE] = s.fontSizeSp
        this[Keys.FONT_WEIGHT] = s.fontWeight
        this[Keys.LINE_HEIGHT] = s.lineHeight
        this[Keys.PARAGRAPH_SPACING] = s.paragraphSpacingEm
        this[Keys.MARGIN_H] = s.marginHorizontalDp
        this[Keys.MARGIN_V] = s.marginVerticalDp
        this[Keys.TEXT_ALIGN] = s.textAlign.name
        this[Keys.HYPHENATION] = s.hyphenation
        this[Keys.PAGE_ANIMATION] = s.pageAnimation.name
        this[Keys.BRIGHTNESS_MODE] = s.brightnessMode.name
        this[Keys.BRIGHTNESS_LEVEL] = s.brightnessLevel
        this[Keys.KEEP_AWAKE] = s.keepAwake.name
        this[Keys.ORIENTATION] = s.orientation.name
        this[Keys.VOLUME_PAGING] = s.volumeKeyPaging
    }

    private inline fun <reified T : Enum<T>> enumOf(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val PALETTE = stringPreferencesKey("palette")
        val UI_FONT = stringPreferencesKey("ui_font")
        val READER_FONT = stringPreferencesKey("reader_font")
        val VIEW_MODE = stringPreferencesKey("view_mode")
        val SORT = stringPreferencesKey("sort")
        val SORT_ASC = booleanPreferencesKey("sort_ascending")
        val TUTORIAL = booleanPreferencesKey("show_tutorial_overlay")

        val READING_THEME = stringPreferencesKey("reading_theme")
        val CUSTOM_BG = longPreferencesKey("custom_bg")
        val CUSTOM_TEXT = longPreferencesKey("custom_text")
        val CUSTOM_LINK = longPreferencesKey("custom_link")
        val CUSTOM_HIGHLIGHT = longPreferencesKey("custom_highlight")
        val FONT_SIZE = intPreferencesKey("reader_font_size")
        val FONT_WEIGHT = intPreferencesKey("reader_font_weight")
        val LINE_HEIGHT = floatPreferencesKey("reader_line_height")
        val PARAGRAPH_SPACING = floatPreferencesKey("reader_paragraph_spacing")
        val MARGIN_H = intPreferencesKey("reader_margin_h")
        val MARGIN_V = intPreferencesKey("reader_margin_v")
        val TEXT_ALIGN = stringPreferencesKey("reader_text_align")
        val HYPHENATION = booleanPreferencesKey("reader_hyphenation")
        val PAGE_ANIMATION = stringPreferencesKey("reader_page_animation")
        val BRIGHTNESS_MODE = stringPreferencesKey("reader_brightness_mode")
        val BRIGHTNESS_LEVEL = floatPreferencesKey("reader_brightness_level")
        val KEEP_AWAKE = stringPreferencesKey("reader_keep_awake")
        val ORIENTATION = stringPreferencesKey("reader_orientation")
        val VOLUME_PAGING = booleanPreferencesKey("reader_volume_paging")
    }
}
