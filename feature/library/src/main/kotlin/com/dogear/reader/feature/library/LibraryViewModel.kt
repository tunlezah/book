package com.dogear.reader.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.dogear.reader.core.datastore.SettingsRepository
import com.dogear.reader.core.model.BookFormat
import com.dogear.reader.core.model.BookShelfItem
import com.dogear.reader.core.model.LibraryFilter
import com.dogear.reader.core.model.LibraryQuery
import com.dogear.reader.core.model.ReadingState
import com.dogear.reader.core.model.SortOption
import com.dogear.reader.core.model.ViewMode
import com.dogear.reader.feature.library.data.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: LibraryRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(LibraryFilter.None)
    private val searchQuery = MutableStateFlow("")

    /** The composed query that drives paging — sort/order from settings, filters from local UI. */
    private val query: Flow<LibraryQuery> =
        combine(settingsRepository.settings, filter, searchQuery) { settings, f, q ->
            LibraryQuery(
                sort = settings.sort,
                ascending = settings.sortAscending,
                filter = f.copy(query = q.ifBlank { null }),
            )
        }

    val pagedBooks: Flow<PagingData<BookShelfItem>> = query
        .flatMapLatest { repository.pagedShelf(it) }
        .cachedIn(viewModelScope)

    private val options = combine(
        repository.observeAuthors(),
        repository.observeFormats(),
        repository.observeCollections(),
        repository.observeTags(),
    ) { authors, formats, collections, tags ->
        FilterOptions(authors, formats, collections, tags)
    }

    val uiState = combine(
        settingsRepository.settings,
        filter,
        searchQuery,
        repository.observeBookCount(),
        options,
    ) { settings, f, q, count, opts ->
        LibraryUiState(
            viewMode = settings.viewMode,
            sort = settings.sort,
            sortAscending = settings.sortAscending,
            filter = f,
            searchQuery = q,
            bookCount = count,
            options = opts,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = LibraryUiState(),
    )

    fun setViewMode(mode: ViewMode) = launchSettings { settingsRepository.setViewMode(mode) }

    fun setSort(sort: SortOption, ascending: Boolean) =
        launchSettings { settingsRepository.setSort(sort, ascending) }

    fun setSearchQuery(value: String) {
        searchQuery.value = value
    }

    fun setReadingStateFilter(state: ReadingState?) =
        filter.update { it.copy(readingState = state) }

    fun setFormatFilter(format: BookFormat?) = filter.update { it.copy(format = format) }

    fun setAuthorFilter(author: String?) = filter.update { it.copy(author = author) }

    fun clearFilters() {
        filter.value = LibraryFilter.None
    }

    fun setReadingState(bookId: Long, state: ReadingState) =
        viewModelScope.launch { repository.setReadingState(bookId, state) }

    fun deleteBook(bookId: Long) = viewModelScope.launch { repository.deleteBook(bookId) }

    fun seedSampleBooks(count: Int = 60) =
        viewModelScope.launch { repository.seedSampleBooks(count) }

    private fun launchSettings(block: suspend () -> Unit) = viewModelScope.launch { block() }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
