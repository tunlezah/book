package com.dogear.reader.feature.reader.ui

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dogear.reader.core.model.Bookmark
import com.dogear.reader.core.model.Highlight
import com.dogear.reader.core.model.Note
import com.dogear.reader.feature.reader.InBookResult
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BookmarksSheet(
    bookmarks: List<Bookmark>,
    onJump: (Float) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        SheetTitle("Bookmarks")
        if (bookmarks.isEmpty()) {
            EmptyHint("No bookmarks yet. Use the bookmark button while reading.")
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                items(bookmarks, key = { it.id }) { bookmark ->
                    AnnotationRow(
                        primary = bookmark.name ?: "Bookmark",
                        secondary = "${(bookmark.locator.progression * 100).roundToInt()}%",
                        onClick = { onJump(bookmark.locator.progression) },
                        onDelete = { onDelete(bookmark.id) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HighlightsSheet(
    highlights: List<Highlight>,
    onJump: (Float) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        SheetTitle("Highlights")
        if (highlights.isEmpty()) {
            EmptyHint("Select text and choose “Highlight selection”.")
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                items(highlights, key = { it.id }) { highlight ->
                    AnnotationRow(
                        primary = highlight.selectedText,
                        secondary = "${(highlight.start.progression * 100).roundToInt()}%",
                        onClick = { onJump(highlight.start.progression) },
                        onDelete = { onDelete(highlight.id) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotesSheet(
    notes: List<Note>,
    onAdd: (String) -> Unit,
    onDelete: (Long) -> Unit,
    onExport: suspend () -> String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var draft by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Notes", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = {
                scope.launch {
                    val markdown = onExport()
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/markdown"
                        putExtra(Intent.EXTRA_TEXT, markdown)
                    }
                    context.startActivity(Intent.createChooser(intent, "Export notes"))
                }
            }) {
                Icon(Icons.Filled.Share, contentDescription = null)
                Text("  Export")
            }
        }
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            label = { Text("Add a note at this position") },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
        Button(
            onClick = { onAdd(draft); draft = "" },
            enabled = draft.isNotBlank(),
            modifier = Modifier.padding(horizontal = 16.dp),
        ) { Text("Add note") }

        LazyColumn(modifier = Modifier.heightIn(max = 340.dp).padding(top = 8.dp)) {
            items(notes, key = { it.id }) { note ->
                AnnotationRow(
                    primary = note.body,
                    secondary = "${((note.locator?.progression ?: 0f) * 100).roundToInt()}%",
                    onClick = {},
                    onDelete = { onDelete(note.id) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InBookSearchSheet(
    onSearch: suspend (String) -> List<InBookResult>,
    onJump: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<InBookResult>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        SheetTitle("Search in book")
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Find") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        Button(
            onClick = {
                scope.launch {
                    searching = true
                    results = onSearch(query)
                    searching = false
                }
            },
            enabled = query.length >= 2 && !searching,
            modifier = Modifier.padding(16.dp),
        ) { Text(if (searching) "Searching…" else "Search") }

        LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
            items(results) { result ->
                Text(
                    text = result.snippet,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onJump(result.progression) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun SheetTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(16.dp),
    )
}

@Composable
private fun AnnotationRow(
    primary: String,
    secondary: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f).padding(vertical = 12.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(primary, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(secondary, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete")
        }
    }
}
