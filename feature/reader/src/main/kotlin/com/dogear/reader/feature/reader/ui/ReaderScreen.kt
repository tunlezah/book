package com.dogear.reader.feature.reader.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dogear.reader.feature.reader.ReaderCommand
import com.dogear.reader.feature.reader.ReaderSystemUi
import com.dogear.reader.feature.reader.ReaderViewModel
import com.dogear.reader.feature.reader.reflowStyle
import com.dogear.reader.feature.reader.page.PagedReader
import com.dogear.reader.feature.reader.reflow.ReflowReader
import com.dogear.reader.format.api.content.BookContent
import com.dogear.reader.format.api.content.TocEntry
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ReaderScreen(
    onBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val content by viewModel.content.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val commands = remember { MutableSharedFlow<ReaderCommand>(extraBufferCapacity = 8) }
    var showToc by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }
    var showHighlights by remember { mutableStateOf(false) }
    var showNotes by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }

    fun jumpTo(progression: Float) {
        scope.launch { commands.emit(ReaderCommand.GoToProgression(progression)) }
        viewModel.setControlsVisible(false)
    }

    ReaderSystemUi(settings = state.settings)

    Box(modifier = Modifier.fillMaxSize().background(state.theme.background)) {
        val current = content
        when {
            state.loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            state.error != null -> Text(
                text = state.error!!,
                color = state.theme.text,
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
            )
            current is BookContent.Reflowable -> ReflowReader(
                content = current,
                initialLocator = state.initialLocator,
                style = reflowStyle(state.theme, state.readerFont, state.settings),
                smoothPaging = state.settings.pageAnimation == com.dogear.reader.core.model.PageAnimation.SLIDE,
                commands = commands,
                onProgress = { viewModel.onProgress(it, null) },
                onHighlight = { viewModel.saveHighlight(it) },
            )
            current is BookContent.FixedPage -> {
                val fixed = current
                PagedReader(
                    pageCount = state.totalPages,
                    initialLocator = state.initialLocator,
                    commands = commands,
                    renderPage = { index, target -> fixed.renderPage(index, target) },
                    onProgress = { viewModel.onProgress(it, null) },
                )
            }
            current is BookContent.ImagePager -> {
                val pager = current
                PagedReader(
                    pageCount = state.totalPages,
                    initialLocator = state.initialLocator,
                    commands = commands,
                    renderPage = { index, target -> pager.page(index, target) },
                    onProgress = { viewModel.onProgress(it, null) },
                )
            }
        }

        if (!state.loading && state.error == null) {
            TapZones(
                onPrevious = { scope.launch { commands.emit(ReaderCommand.Previous) } },
                onNext = { scope.launch { commands.emit(ReaderCommand.Next) } },
                onToggleControls = viewModel::toggleControls,
            )
        }

        val openTocAction: (() -> Unit)? =
            if (state.toc.isNotEmpty()) ({ showToc = true }) else null
        val isReflow = content is BookContent.Reflowable
        ReaderControls(
            visible = state.controlsVisible,
            title = state.title,
            chapter = state.chapter,
            progression = state.progression,
            isReflow = isReflow,
            onBack = onBack,
            onSeek = ::jumpTo,
            onAddBookmark = { viewModel.addBookmark() },
            onRequestHighlight = { scope.launch { commands.emit(ReaderCommand.RequestHighlight) } },
            onOpenSearch = { showSearch = true },
            onOpenToc = openTocAction,
            onOpenSettings = { showSettings = true },
            onOpenBookmarks = { showBookmarks = true },
            onOpenHighlights = { showHighlights = true },
            onOpenNotes = { showNotes = true },
        )

        if (state.showTutorial && !state.loading) {
            TutorialOverlay(onDismiss = viewModel::dismissTutorial)
        }
    }

    if (showBookmarks) {
        BookmarksSheet(
            bookmarks = viewModel.bookmarks.collectAsStateWithLifecycle().value,
            onJump = { showBookmarks = false; jumpTo(it) },
            onDelete = viewModel::deleteBookmark,
            onDismiss = { showBookmarks = false },
        )
    }
    if (showHighlights) {
        HighlightsSheet(
            highlights = viewModel.highlights.collectAsStateWithLifecycle().value,
            onJump = { showHighlights = false; jumpTo(it) },
            onDelete = viewModel::deleteHighlight,
            onDismiss = { showHighlights = false },
        )
    }
    if (showNotes) {
        NotesSheet(
            notes = viewModel.notes.collectAsStateWithLifecycle().value,
            onAdd = viewModel::addNote,
            onDelete = viewModel::deleteNote,
            onExport = { viewModel.exportNotes() },
            onDismiss = { showNotes = false },
        )
    }
    if (showSearch) {
        InBookSearchSheet(
            onSearch = viewModel::searchInBook,
            onJump = { showSearch = false; jumpTo(it) },
            onDismiss = { showSearch = false },
        )
    }

    if (showSettings) {
        ReaderControlsSheet(
            settings = state.settings,
            font = state.readerFont,
            onUpdate = viewModel::updateReader,
            onFontChange = viewModel::setReaderFont,
            onDismiss = { showSettings = false },
        )
    }

    if (showToc) {
        TocSheet(
            entries = state.toc,
            onDismiss = { showToc = false },
            onSelect = { entry ->
                showToc = false
                viewModel.setControlsVisible(false)
                scope.launch {
                    resolveSpineIndex(content, entry.href)?.let { index ->
                        commands.emit(ReaderCommand.GoTo(index, 0f))
                    }
                }
            },
        )
    }
}

@Composable
private fun TapZones(
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleControls: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        TapZone(weight = 0.32f, onClick = onPrevious)
        TapZone(weight = 0.36f, onClick = onToggleControls)
        TapZone(weight = 0.32f, onClick = onNext)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TapZone(weight: Float, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(weight)
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    )
}

@Composable
private fun ReaderControls(
    visible: Boolean,
    title: String,
    chapter: String,
    progression: Float,
    isReflow: Boolean,
    onBack: () -> Unit,
    onSeek: (Float) -> Unit,
    onAddBookmark: () -> Unit,
    onRequestHighlight: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenToc: (() -> Unit)?,
    onOpenSettings: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHighlights: () -> Unit,
    onOpenNotes: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Surface(tonalElevation = 3.dp, shadowElevation = 3.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                    )
                    IconButton(onClick = onAddBookmark) {
                        Icon(Icons.Outlined.BookmarkAdd, contentDescription = "Add bookmark")
                    }
                    if (isReflow) {
                        IconButton(onClick = onOpenSearch) {
                            Icon(Icons.Filled.Search, contentDescription = "Search in book")
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.FormatSize, contentDescription = "Reading settings")
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            if (onOpenToc != null) {
                                DropdownMenuItem(
                                    text = { Text("Contents") },
                                    onClick = { menuOpen = false; onOpenToc() },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Bookmarks") },
                                onClick = { menuOpen = false; onOpenBookmarks() },
                            )
                            if (isReflow) {
                                DropdownMenuItem(
                                    text = { Text("Highlight selection") },
                                    onClick = { menuOpen = false; onRequestHighlight() },
                                )
                                DropdownMenuItem(
                                    text = { Text("Highlights") },
                                    onClick = { menuOpen = false; onOpenHighlights() },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Notes") },
                                onClick = { menuOpen = false; onOpenNotes() },
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Surface(tonalElevation = 3.dp, shadowElevation = 3.dp) {
                Column(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    // Drag to seek anywhere in the book (Architecture: bottom scrubber).
                    Slider(
                        value = progression.coerceIn(0f, 1f),
                        onValueChange = onSeek,
                    )
                    Text(
                        text = buildString {
                            append("${(progression * 100).roundToInt()}%")
                            if (chapter.isNotBlank()) append("  ·  $chapter")
                        },
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun TutorialOverlay(onDismiss: () -> Unit) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3500)
        onDismiss()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            TutorialLine("Tap the left or right edge to turn pages")
            TutorialLine("Tap the center for the menu and table of contents")
            TutorialLine("The bar at the bottom shows your progress")
        }
    }
}

@Composable
private fun TutorialLine(text: String) {
    Text(
        text = text,
        color = Color.White,
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(vertical = 10.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TocSheet(
    entries: List<TocEntry>,
    onDismiss: () -> Unit,
    onSelect: (TocEntry) -> Unit,
) {
    if (entries.isEmpty()) {
        onDismiss()
        return
    }
    val flattened = remember(entries) { flatten(entries) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(flattened) { (entry, depth) ->
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(entry) }
                        .padding(start = (16 + depth * 16).dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                )
            }
        }
    }
}

private fun flatten(entries: List<TocEntry>, depth: Int = 0): List<Pair<TocEntry, Int>> =
    entries.flatMap { entry ->
        listOf(entry to depth) + flatten(entry.children, depth + 1)
    }

private suspend fun resolveSpineIndex(content: BookContent?, href: String): Int? {
    val reflow = content as? BookContent.Reflowable ?: return null
    val path = href.substringBefore('#')
    return reflow.spine().indexOfFirst { it.href == path }.takeIf { it >= 0 }
}
