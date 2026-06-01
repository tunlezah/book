package com.dogear.reader.feature.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dogear.reader.core.model.Book
import com.dogear.reader.core.model.ReadingState
import com.dogear.reader.feature.library.data.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookDetailViewModel @Inject constructor(
    private val repository: LibraryRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val bookId: Long = savedStateHandle.get<Long>(ARG_BOOK_ID) ?: 0L

    val book = repository.observeBook(bookId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null as Book?)

    fun setReadingState(state: ReadingState) =
        viewModelScope.launch { repository.setReadingState(bookId, state) }

    fun delete() = viewModelScope.launch { repository.deleteBook(bookId) }

    companion object {
        const val ARG_BOOK_ID = "bookId"
    }
}
