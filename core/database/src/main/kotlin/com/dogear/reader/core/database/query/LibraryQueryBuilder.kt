package com.dogear.reader.core.database.query

import com.dogear.reader.core.model.LibraryFilter
import com.dogear.reader.core.model.LibraryQuery
import com.dogear.reader.core.model.SortOption

/** A SQL string plus its bound arguments — pure data so the builder is unit-testable on the JVM. */
data class RawLibraryQuery(val sql: String, val args: List<Any?>)

/**
 * Builds the shelf paging query from a [LibraryQuery]. All user-supplied values are passed as
 * bound arguments (never string-concatenated) to prevent SQL injection; only the ORDER BY
 * column is selected from a fixed whitelist. Room's paging wrapper appends LIMIT/OFFSET, so
 * this query must not include them.
 */
object LibraryQueryBuilder {

    private const val SELECT = """
        SELECT b.id AS id, b.title AS title, b.author AS author,
               b.cover_path AS cover_path, b.cover_is_generated AS cover_is_generated,
               b.format AS format, b.reading_state AS reading_state,
               COALESCE(rp.progression, 0) AS progress
        FROM books b
        LEFT JOIN reading_progress rp ON rp.book_id = b.id
    """

    fun build(query: LibraryQuery): RawLibraryQuery {
        val args = mutableListOf<Any?>()
        val where = mutableListOf<String>()
        val filter = query.filter

        filter.readingState?.let {
            where += "b.reading_state = ?"
            args += it.name
        }
        filter.format?.let {
            where += "b.format = ?"
            args += it.name
        }
        filter.author?.let {
            where += "b.author = ?"
            args += it
        }
        filter.collectionId?.let {
            where += "b.id IN (SELECT book_id FROM book_collections WHERE collection_id = ?)"
            args += it
        }
        filter.tagId?.let {
            where += "b.id IN (SELECT book_id FROM book_tags WHERE tag_id = ?)"
            args += it
        }
        ftsMatch(filter.query)?.let { match ->
            where += "(b.id IN (SELECT rowid FROM books_fts WHERE books_fts MATCH ?) " +
                "OR b.id IN (SELECT bt.book_id FROM book_tags bt " +
                "JOIN tags t ON t.id = bt.tag_id WHERE t.name LIKE ?))"
            args += match
            args += "%${filter.query!!.trim()}%"
        }

        val whereClause = if (where.isEmpty()) "" else "WHERE " + where.joinToString(" AND ")
        val orderClause = orderBy(query.sort, query.ascending)

        val sql = "$SELECT $whereClause $orderClause".trimIndent().replace(Regex("\\s+"), " ").trim()
        return RawLibraryQuery(sql, args)
    }

    private fun orderBy(sort: SortOption, ascending: Boolean): String {
        val direction = if (ascending) "ASC" else "DESC"
        // Column is chosen from this fixed set — never from user input.
        val column = when (sort) {
            SortOption.RECENTLY_READ -> "b.last_opened_at"
            SortOption.RECENTLY_ADDED -> "b.added_at"
            SortOption.TITLE -> "b.title COLLATE NOCASE"
            SortOption.AUTHOR -> "b.author COLLATE NOCASE"
            SortOption.FILE_SIZE -> "b.file_size"
            SortOption.PROGRESS -> "COALESCE(rp.progression, 0)"
        }
        // Push NULLs (e.g. never-opened books) to the end, and add a stable id tiebreaker so
        // paging produces a deterministic total order.
        val nullsLast = "CASE WHEN $column IS NULL THEN 1 ELSE 0 END"
        return "ORDER BY $nullsLast, $column $direction, b.id ASC"
    }

    /** Turns a user query into a safe FTS prefix match, or null if there is nothing to search. */
    private fun ftsMatch(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val tokens = raw.trim()
            .split(Regex("\\s+"))
            .map { it.replace(Regex("[^\\p{L}\\p{Nd}]"), "") }
            .filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return null
        return tokens.joinToString(" ") { "$it*" }
    }
}
