package com.dogear.reader.feature.library.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dogear.reader.core.model.Book
import com.dogear.reader.core.model.ReadingState
import com.dogear.reader.core.ui.component.BookCover
import com.dogear.reader.feature.library.BookDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    onBack: () -> Unit,
    onRead: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookDetailViewModel = hiltViewModel(),
) {
    val book by viewModel.book.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }

    val coverPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri -> uri?.let(viewModel::replaceCover) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(book?.title ?: "Details", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit metadata") },
                            onClick = { menuOpen = false; editing = true },
                        )
                        DropdownMenuItem(
                            text = { Text("Replace cover") },
                            onClick = { menuOpen = false; coverPicker.launch("image/*") },
                        )
                        DropdownMenuItem(
                            text = { Text("Refresh metadata") },
                            onClick = { menuOpen = false; viewModel.refreshMetadata() },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = { menuOpen = false; viewModel.delete(); onBack() },
                        )
                    }
                },
            )
        },
    ) { padding ->
        val current = book
        if (current == null) {
            // Brief loading/empty placeholder; never blocks.
            return@Scaffold
        }
        BookDetailContent(
            book = current,
            onStateChange = viewModel::setReadingState,
            onRead = onRead,
            modifier = Modifier.padding(padding),
        )
        if (editing) {
            EditMetadataDialog(
                book = current,
                onSave = { viewModel.saveMetadata(it); editing = false },
                onDismiss = { editing = false },
            )
        }
    }
}

@Composable
private fun EditMetadataDialog(book: Book, onSave: (Book) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(book.title) }
    var author by remember { mutableStateOf(book.author) }
    var series by remember { mutableStateOf(book.series.orEmpty()) }
    var description by remember { mutableStateOf(book.description.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit metadata") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(author, { author = it }, label = { Text("Author") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                OutlinedTextField(series, { series = it }, label = { Text("Series") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                OutlinedTextField(description, { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    book.copy(
                        title = title.ifBlank { book.title },
                        author = author.ifBlank { "Unknown" },
                        series = series.ifBlank { null },
                        description = description.ifBlank { null },
                    ),
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookDetailContent(
    book: Book,
    onStateChange: (ReadingState) -> Unit,
    onRead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Button(
            onClick = onRead,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        ) {
            Text("Read")
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            BookCover(
                title = book.title,
                author = book.author,
                coverPath = book.coverPath,
                isGenerated = book.coverIsGenerated,
                modifier = Modifier.width(120.dp).height(180.dp),
            )
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(book.title, style = MaterialTheme.typography.titleLarge)
                book.subtitle?.let { Text(it, style = MaterialTheme.typography.titleSmall) }
                Text(
                    book.author,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    book.format.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        Text(
            "Reading status",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReadingState.entries.forEach { state ->
                FilterChip(
                    selected = book.readingState == state,
                    onClick = { onStateChange(state) },
                    label = { Text(state.displayName) },
                )
            }
        }

        book.description?.takeIf { it.isNotBlank() }?.let { description ->
            Text(
                "Description",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            )
            Text(description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
