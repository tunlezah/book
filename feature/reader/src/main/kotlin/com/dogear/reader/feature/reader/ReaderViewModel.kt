package com.dogear.reader.feature.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dogear.reader.core.datastore.SettingsRepository
import com.dogear.reader.core.model.FontChoice
import com.dogear.reader.core.model.Locator
import com.dogear.reader.core.model.ReaderSettings
import com.dogear.reader.core.ui.theme.ReadingThemes
import com.dogear.reader.feature.reader.data.ReaderRepository
import com.dogear.reader.format.api.content.BookContent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val repository: ReaderRepository,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val bookId: Long = savedStateHandle.get<Long>(ARG_BOOK_ID) ?: 0L

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private val _content = MutableStateFlow<BookContent?>(null)
    val content: StateFlow<BookContent?> = _content.asStateFlow()

    init {
        open()
        observeSettings()
    }

    /** Keep reader typography/theme/font live as the user changes controls. */
    private fun observeSettings() {
        viewModelScope.launch {
            combine(settingsRepository.settings, settingsRepository.readerSettings) { app, reader ->
                app.readerFont to reader
            }.collect { (font, reader) ->
                _uiState.update {
                    it.copy(
                        readerFont = font,
                        settings = reader,
                        theme = ReadingThemes.colors(reader.themeId, reader.custom),
                    )
                }
            }
        }
    }

    private fun open() {
        viewModelScope.launch {
            val appSettings = settingsRepository.settings.first()
            val opened = repository.open(bookId)
            if (opened == null) {
                _uiState.update { it.copy(loading = false, error = "Could not open this book") }
                return@launch
            }
            val kind = when (opened.content) {
                is BookContent.Reflowable -> ReaderKind.REFLOW
                is BookContent.FixedPage -> ReaderKind.FIXED
                is BookContent.ImagePager -> ReaderKind.IMAGE
            }
            val toc = (opened.content as? BookContent.Reflowable)
                ?.runCatching { toc() }?.getOrNull().orEmpty()
            val totalPages = when (val c = opened.content) {
                is BookContent.FixedPage -> c.pageCount
                is BookContent.ImagePager -> c.pageCount
                else -> 0
            }
            val initial = repository.loadProgress(bookId)
            _content.value = opened.content
            _uiState.update {
                it.copy(
                    loading = false,
                    title = opened.book.title,
                    kind = kind,
                    toc = toc,
                    initialLocator = initial,
                    totalPages = totalPages,
                    showTutorial = appSettings.showTutorialOverlay,
                    progression = initial?.progression ?: 0f,
                )
            }
        }
    }

    fun onProgress(locator: Locator, chapter: String?) {
        _uiState.update { it.copy(progression = locator.progression, chapter = chapter ?: it.chapter) }
        viewModelScope.launch { repository.saveProgress(bookId, locator, chapter) }
    }

    fun updateReader(transform: (ReaderSettings) -> ReaderSettings) {
        viewModelScope.launch { settingsRepository.updateReaderSettings(transform) }
    }

    fun setReaderFont(font: FontChoice) {
        viewModelScope.launch { settingsRepository.setReaderFont(font) }
    }

    fun toggleControls() = _uiState.update { it.copy(controlsVisible = !it.controlsVisible) }

    fun setControlsVisible(visible: Boolean) = _uiState.update { it.copy(controlsVisible = visible) }

    fun dismissTutorial() {
        _uiState.update { it.copy(showTutorial = false) }
        viewModelScope.launch { settingsRepository.setShowTutorialOverlay(false) }
    }

    override fun onCleared() {
        super.onCleared()
        _content.value?.close()
    }

    companion object {
        const val ARG_BOOK_ID = "bookId"
    }
}
