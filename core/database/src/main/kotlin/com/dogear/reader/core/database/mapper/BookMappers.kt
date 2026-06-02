package com.dogear.reader.core.database.mapper

import com.dogear.reader.core.database.entity.BookEntity
import com.dogear.reader.core.database.model.ShelfRow
import com.dogear.reader.core.model.Book
import com.dogear.reader.core.model.BookFormat
import com.dogear.reader.core.model.BookShelfItem
import com.dogear.reader.core.model.ReadingState

/** Enum names are the stable persisted form; unknown values degrade gracefully. */
internal fun String?.toBookFormat(): BookFormat =
    BookFormat.entries.firstOrNull { it.name == this } ?: BookFormat.UNKNOWN

internal fun String?.toReadingState(): ReadingState =
    ReadingState.entries.firstOrNull { it.name == this } ?: ReadingState.UNREAD

fun BookEntity.toDomain(): Book = Book(
    id = id,
    contentHash = contentHash,
    title = title,
    subtitle = subtitle,
    author = author,
    authors = JsonList.decode(authorsJson),
    publisher = publisher,
    publishedDate = publishedDate,
    description = description,
    isbn = isbn,
    language = language,
    series = series,
    seriesIndex = seriesIndex,
    categories = JsonList.decode(categories),
    format = format.toBookFormat(),
    fileSizeBytes = fileSize,
    coverPath = coverPath,
    coverIsGenerated = coverIsGenerated,
    readingState = readingState.toReadingState(),
    addedAt = addedAt,
    lastOpenedAt = lastOpenedAt,
    metadataLocked = metadataLocked,
)

fun Book.toEntity(): BookEntity = BookEntity(
    id = id,
    contentHash = contentHash,
    title = title,
    subtitle = subtitle,
    author = author,
    authorsJson = JsonList.encode(authors),
    publisher = publisher,
    publishedDate = publishedDate,
    description = description,
    isbn = isbn,
    language = language,
    series = series,
    seriesIndex = seriesIndex,
    categories = JsonList.encode(categories),
    format = format.name,
    fileSize = fileSizeBytes,
    coverPath = coverPath,
    coverIsGenerated = coverIsGenerated,
    readingState = readingState.name,
    addedAt = addedAt,
    lastOpenedAt = lastOpenedAt,
    metadataLocked = metadataLocked,
)

fun ShelfRow.toDomain(): BookShelfItem = BookShelfItem(
    id = id,
    title = title,
    author = author,
    coverPath = coverPath,
    coverIsGenerated = coverIsGenerated,
    format = format.toBookFormat(),
    readingState = readingState.toReadingState(),
    progress = progress.coerceIn(0f, 1f),
)
