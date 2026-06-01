package com.dogear.reader.core.database.mapper

import com.dogear.reader.core.database.entity.BookmarkEntity
import com.dogear.reader.core.database.entity.HighlightEntity
import com.dogear.reader.core.database.entity.NoteEntity
import com.dogear.reader.core.model.Bookmark
import com.dogear.reader.core.model.Highlight
import com.dogear.reader.core.model.Locator
import com.dogear.reader.core.model.Note

fun BookmarkEntity.toDomain(): Bookmark = Bookmark(
    id = id,
    bookId = bookId,
    name = name,
    locator = Locator(spineIndex, selector, charOffset, textSnippet, pageIndex, progression),
    excerpt = excerpt,
    createdAt = createdAt,
)

fun Bookmark.toEntity(): BookmarkEntity = BookmarkEntity(
    id = id,
    bookId = bookId,
    name = name,
    spineIndex = locator.spineIndex,
    selector = locator.selector,
    charOffset = locator.charOffset,
    textSnippet = locator.textSnippet,
    pageIndex = locator.pageIndex,
    progression = locator.progression,
    excerpt = excerpt,
    createdAt = createdAt,
)

fun HighlightEntity.toDomain(): Highlight = Highlight(
    id = id,
    bookId = bookId,
    color = color,
    selectedText = selectedText,
    start = Locator(startSpineIndex, startSelector, startCharOffset, textSnippet, null, progression),
    end = Locator(endSpineIndex, endSelector, endCharOffset, textSnippet, null, progression),
    createdAt = createdAt,
)

fun Highlight.toEntity(): HighlightEntity = HighlightEntity(
    id = id,
    bookId = bookId,
    color = color,
    selectedText = selectedText,
    startSpineIndex = start.spineIndex,
    startSelector = start.selector,
    startCharOffset = start.charOffset,
    endSpineIndex = end.spineIndex,
    endSelector = end.selector,
    endCharOffset = end.charOffset,
    textSnippet = start.textSnippet,
    progression = start.progression,
    createdAt = createdAt,
)

fun NoteEntity.toDomain(): Note = Note(
    id = id,
    bookId = bookId,
    highlightId = highlightId,
    body = body,
    locator = Locator(spineIndex, selector, charOffset, null, null, progression ?: 0f),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Note.toEntity(): NoteEntity = NoteEntity(
    id = id,
    bookId = bookId,
    highlightId = highlightId,
    body = body,
    spineIndex = locator?.spineIndex,
    selector = locator?.selector,
    charOffset = locator?.charOffset,
    progression = locator?.progression,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
