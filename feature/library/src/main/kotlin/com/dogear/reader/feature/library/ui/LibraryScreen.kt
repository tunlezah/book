package com.dogear.reader.feature.library.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.dogear.reader.core.model.ReadingState
import com.dogear.reader.core.model.SortOption
import com.dogear.reader.core.model.ViewMode
import com.dogear.reader.feature.library.LibraryUiState
import com.dogear.reader.feature.library.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onBookClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val books = viewModel.pagedBooks.collectAsLazyPagingItems()
    val importMessage by viewModel.importMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::importBook) }

    LaunchedEffect(importMessage) {
        importMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumeImportMessage()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LibraryTopBar(
                state = state,
                onSearchChange = viewModel::setSearchQuery,
                onImportClick = { importLauncher.launch(arrayOf("*/*")) },
                onToggleView = {
                    val next = if (state.viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID
                    viewModel.setViewMode(next)
                },
                onSortSelected = { sort -> viewModel.setSort(sort, defaultAscending(sort)) },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ReadingStateFilters(
                selected = state.filter.readingState,
                onSelect = viewModel::setReadingStateFilter,
            )
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isEmpty -> EmptyLibrary(onSeed = { viewModel.seedSampleBooks() })
                    state.viewMode == ViewMode.GRID -> ShelfGrid(books, onBookClick)
                    else -> ShelfList(books, onBookClick)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryTopBar(
    state: LibraryUiState,
    onSearchChange: (String) -> Unit,
    onImportClick: () -> Unit,
    onToggleView: () -> Unit,
    onSortSelected: (SortOption) -> Unit,
) {
    var searching by remember { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            if (searching) {
                LibrarySearchField(
                    value = state.searchQuery,
                    onValueChange = onSearchChange,
                )
            } else {
                Text("Library", style = MaterialTheme.typography.titleLarge)
            }
        },
        actions = {
            IconButton(onClick = {
                searching = !searching
                if (!searching) onSearchChange("")
            }) {
                Icon(
                    imageVector = if (searching) Icons.Filled.Clear else Icons.Filled.Search,
                    contentDescription = if (searching) "Close search" else "Search",
                )
            }
            IconButton(onClick = onImportClick) {
                Icon(Icons.Filled.Add, contentDescription = "Import book")
            }
            IconButton(onClick = onToggleView) {
                val isGrid = state.viewMode == ViewMode.GRID
                Icon(
                    imageVector = if (isGrid) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView,
                    contentDescription = if (isGrid) "List view" else "Grid view",
                )
            }
            Box {
                IconButton(onClick = { sortMenuOpen = true }) {
                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                }
                DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                    SortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.displayName) },
                            onClick = {
                                onSortSelected(option)
                                sortMenuOpen = false
                            },
                        )
                    }
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadingStateFilters(
    selected: ReadingState?,
    onSelect: (ReadingState?) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ReadingState.entries.forEach { stateOption ->
            FilterChip(
                selected = selected == stateOption,
                onClick = {
                    onSelect(if (selected == stateOption) null else stateOption)
                },
                label = { Text(stateOption.displayName) },
            )
        }
    }
}

private fun defaultAscending(sort: SortOption): Boolean = when (sort) {
    SortOption.TITLE, SortOption.AUTHOR -> true
    else -> false
}

@Composable
private fun ShelfGrid(
    books: androidx.paging.compose.LazyPagingItems<com.dogear.reader.core.model.BookShelfItem>,
    onBookClick: (Long) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 110.dp),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(count = books.itemCount, key = books.itemKey { it.id }) { index ->
            val book = books[index]
            if (book != null) {
                BookGridItem(book = book, onClick = { onBookClick(book.id) })
            }
        }
    }
}

@Composable
private fun ShelfList(
    books: androidx.paging.compose.LazyPagingItems<com.dogear.reader.core.model.BookShelfItem>,
    onBookClick: (Long) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(count = books.itemCount, key = books.itemKey { it.id }) { index ->
            val book = books[index]
            if (book != null) {
                BookListItem(book = book, onClick = { onBookClick(book.id) })
            }
        }
    }
}
