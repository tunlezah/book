package com.dogear.reader.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dogear.reader.core.model.AppPalette
import com.dogear.reader.core.model.AppSettings
import com.dogear.reader.core.model.FontChoice
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
    }
}
