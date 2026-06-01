package com.dogear.reader.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dogear.reader.core.cover.CoverCache
import com.dogear.reader.core.datastore.SettingsRepository
import com.dogear.reader.core.model.AppPalette
import com.dogear.reader.core.model.AppSettings
import com.dogear.reader.core.model.FontChoice
import com.dogear.reader.core.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val coverCache: CoverCache,
    private val backupManager: BackupManager,
) : ViewModel() {

    val settings = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _cacheBytes = MutableStateFlow(0L)
    val cacheBytes = _cacheBytes.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    init {
        refreshCacheSize()
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    fun setPalette(palette: AppPalette) = viewModelScope.launch { settingsRepository.setPalette(palette) }
    fun setUiFont(font: FontChoice) = viewModelScope.launch { settingsRepository.setUiFont(font) }
    fun setReaderFont(font: FontChoice) = viewModelScope.launch { settingsRepository.setReaderFont(font) }
    fun setTutorial(show: Boolean) = viewModelScope.launch { settingsRepository.setShowTutorialOverlay(show) }

    fun clearCoverCache() = viewModelScope.launch {
        withContext(Dispatchers.IO) { coverCache.clearAll() }
        _message.value = "Cover cache cleared"
        refreshCacheSize()
    }

    fun exportBackup(uri: Uri) = viewModelScope.launch {
        _message.value = if (backupManager.export(uri)) "Backup saved" else "Backup failed"
    }

    fun restoreBackup(uri: Uri) = viewModelScope.launch {
        _message.value = if (backupManager.import(uri)) "Library restored" else "Restore failed"
    }

    fun consumeMessage() {
        _message.value = null
    }

    private fun refreshCacheSize() = viewModelScope.launch {
        _cacheBytes.value = withContext(Dispatchers.IO) { coverCache.totalBytes() }
    }
}
