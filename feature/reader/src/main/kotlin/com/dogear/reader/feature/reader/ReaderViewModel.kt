package com.dogear.reader.feature.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dogear.reader.core.datastore.SettingsRepository
import com.dogear.reader.core.model.Bookmark
import com.dogear.reader.core.model.FontChoice
import com.dogear.reader.core.model.Highlight
import com.dogear.reader.core.model.Locator
import com.dogear.reader.core.model.Note
import com.dogear.reader.core.model.ReaderSettings
import com.dogear.reader.core.ui.theme.ReadingThemes
import com.dogear.reader.feature.reader.data.AnnotationRepository
import com.dogear.reader.feature.reader.data.OpenOutcome
import com.dogear.reader.feature.reader.data.ReaderRepository
import com.dogear.reader.format.api.content.BookContent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val repository: ReaderRepository,
    private val annotationRepository: AnnotationRepository,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val bookId: Long = savedStateHandle.get<Long>(ARG_BOOK_ID) ?: 0L

    private var lastLocator: Locator = Locator()

    val bookmarks: StateFlow<List<Bookmark>> = annotationRepository.bookmarks(bookId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val highlights: StateFlow<List<Highlight>> = annotationRepository.highlights(bookId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val notes: StateFlow<List<Note>> = annotationRepository.notes(bookId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
            val outcome = repository.open(bookId)
            if (outcome is OpenOutcome.Failure) {
                _uiState.update { it.copy(loading = false, error = outcome.message) }
                return@launch
            }
            val opened = (outcome as OpenOutcome.Success).opened
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
        lastLocator = locator
        _uiState.update { it.copy(progression = locator.progression, chapter = chapter ?: it.chapter) }
        viewModelScope.launch { repository.saveProgress(bookId, locator, chapter) }
    }

    fun addBookmark() = viewModelScope.launch {
        val pct = (lastLocator.progression * 100).roundToInt()
        annotationRepository.addBookmark(
            Bookmark(bookId = bookId, locator = lastLocator, excerpt = "$pct%", createdAt = now()),
        )
    }

    fun deleteBookmark(id: Long) = viewModelScope.launch { annotationRepository.deleteBookmark(id) }

    fun addNote(body: String) = viewModelScope.launch {
        if (body.isBlank()) return@launch
        annotationRepository.addNote(
            Note(bookId = bookId, body = body.trim(), locator = lastLocator, createdAt = now(), updatedAt = now()),
        )
    }

    fun updateNote(note: Note) =
        viewModelScope.launch { annotationRepository.updateNote(note.copy(updatedAt = now())) }

    fun deleteNote(id: Long) = viewModelScope.launch { annotationRepository.deleteNote(id) }

    fun saveHighlight(text: String) = viewModelScope.launch {
        annotationRepository.addHighlight(
            Highlight(
                bookId = bookId,
                color = DEFAULT_HIGHLIGHT,
                selectedText = text,
                start = lastLocator,
                end = lastLocator,
                createdAt = now(),
            ),
        )
    }

    fun deleteHighlight(id: Long) = viewModelScope.launch { annotationRepository.deleteHighlight(id) }

    suspend fun exportNotes(): String = annotationRepository.exportMarkdown(bookId, _uiState.value.title)

    /** Scans the open reflowable book for [query], returning matches as jumpable results. */
    suspend fun searchInBook(query: String): List<InBookResult> {
        val reflow = _content.value as? BookContent.Reflowable ?: return emptyList()
        if (query.length < 2) return emptyList()
        return withContext(Dispatchers.Default) {
            val spine = runCatching { reflow.spine() }.getOrDefault(emptyList())
            val needle = query.lowercase()
            val results = mutableListOf<InBookResult>()
            for (item in spine) {
                if (results.size >= MAX_RESULTS) break
                val html = runCatching { reflow.document(item.index).html }.getOrNull() ?: continue
                val text = html.replace(Regex("(?s)<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim()
                val idx = text.lowercase().indexOf(needle)
                if (idx >= 0) {
                    val start = (idx - 40).coerceAtLeast(0)
                    val end = (idx + needle.length + 40).coerceAtMost(text.length)
                    val progression = if (spine.isEmpty()) 0f else item.index.toFloat() / spine.size
                    results += InBookResult("…" + text.substring(start, end) + "…", progression)
                }
            }
            results
        }
    }

    private fun now() = System.currentTimeMillis()

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
        private const val DEFAULT_HIGHLIGHT = 0xFFFFE08A.toInt()
        private const val MAX_RESULTS = 100
    }
}

/** A single in-book search match: a text snippet and the position to jump to. */
data class InBookResult(val snippet: String, val progression: Float)
